package org.nerve.boot.web.filter;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.nerve.boot.Result;
import org.nerve.boot.domain.AuthUser;
import org.nerve.boot.domain.UserAuthRecognizer;
import org.nerve.boot.web.WebUtil;
import org.nerve.boot.web.auth.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Enumeration;

@Component
@ConditionalOnProperty(value = "nerve.auth.enable", havingValue = "true" , matchIfMissing = true)
public class UserFilter implements AsyncHandlerInterceptor {

    private Logger logger = LoggerFactory.getLogger(getClass());

    public static final String LOGIN_REQUIRED = "LOGIN REQUIRED";

    @Resource
    AuthHolder authHolder;
    @Resource
    AuthConfig config;
    @Resource
    UserAuthRecognizer recognizer;

    @Value(("${spring.mvc.static-path-pattern:}"))
    private String staticPathPattern;

    @Value(("${nerve.web.debug:false}"))
    private boolean webDebug;

    private AntPathMatcher attachMatcher = new AntPathMatcher();

    @Autowired(required = false)
    private UserLoader userLoader = new UserLoaderImpl();

    private boolean isSkip(final HttpServletRequest request){
        String path = request.getServletPath();

        if("/error".equals(path))
            return true;

        if(HttpMethod.GET.matches(request.getMethod())){
            //访问静态资源可跳过
            if(attachMatcher.match(staticPathPattern, path)) return true;
            //访问附件
            return config.isAttach() && attachMatcher.match("/"+config.getAttachDir()+"/**", path);
        }
        if(config.isAttach())
            return ("GET".equalsIgnoreCase(request.getMethod()) && attachMatcher.match("/"+config.getAttachDir()+"/**", path));

        return false;
    }

    private String HR= "---------------------------------------------------------------------------";
    private void showRequestInfo(final HttpServletRequest request){
        logger.info(HR);
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()){
            String key = headers.nextElement();
            logger.info("[HEADER] {} = {}", key, request.getHeader(key));
        }
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()){
            String key = params.nextElement();
            logger.info("[PARAMS] {} = {}", key, request.getParameterValues(key));
        }
        logger.info(HR);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final String uri = request.getServletPath();

        if(webDebug)    showRequestInfo(request);

        if(isSkip(request)){
            if(logger.isDebugEnabled()) logger.debug("访问公开路径 {}", uri);

            return true;
        }

        final String ip = WebUtil.getIp(request);
        if(logger.isDebugEnabled())
            logger.debug("来源IP={}, 白名单：{}", ip, JSON.toJSONString(config.getWhiteIps()));

        if(config.isIpInWhite(ip)){
            authHolder.set(new AuthUser("LOCAL", "本地白名单用户", ip));
            return true;
        }

        final AuthUser user = userLoader.from(request.getHeader(config.getTokenName()));
        if(user == null){
            logger.info("[{}] {} 匿名请求 {}", LOGIN_REQUIRED, ip, uri);
            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            response.getWriter().write(JSON.toJSONString(Result.fail(LOGIN_REQUIRED)));
            return false;
        }

        //验证 IP
        if(!StringUtils.equals(ip, user.getIp())){
            throw new AuthException(String.format("授权IP (%s) 与当前IP不符", user.getIp()));
        }

        //验证权限
        if(!config.isIdInWhite(user.getId()) && !config.isInner(uri) && !recognizer.hasAuth(user, uri)){
            logger.info("{}尝试访问未授权接口 {}", user.getShowName(), uri);
            throw new AuthException("访问未授权（IP限定或"+user.getId()+"未授权），请联系管理员");
        }

        if(logger.isDebugEnabled()) logger.debug("[AUTH-USER] ID={} NAME={} IP={}", user.getId(), user.getName(), user.getIp());

        //注入用户角色，add on 2021年8月12日
        user.setRoles(recognizer.loadRoleByUser(user));
        authHolder.set(user);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        authHolder.clean();
        if(logger.isDebugEnabled()) logger.debug("[AUTH-USER] 清空用户...");
    }
}
