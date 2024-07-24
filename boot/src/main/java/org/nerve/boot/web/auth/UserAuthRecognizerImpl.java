package org.nerve.boot.web.auth;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.nerve.boot.cache.CacheManage;
import org.nerve.boot.domain.AuthUser;
import org.nerve.boot.domain.UserAuthRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.nerve.boot.Const.COMMA;
import static org.nerve.boot.Const.EMPTY;


@Component
public class UserAuthRecognizerImpl implements UserAuthRecognizer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static String ALL = "/**";

    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleLinkService roleLinkService;

    @Override
    public boolean hasAuth(AuthUser user, String url) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("检测 {} 对 {} 的授权...", user.getId(), url);

        //获取该用户的全部角色及权限
        List<String> urls = CacheManage.get(
                "AUTH-" + user.getId(),
                () -> {
                    final List<String> userUrls = new ArrayList<>();
                    RoleLink roleLink = loadRoleLink(user);
                    if(roleLink != null){
                        //验证IP
                        if(StringUtils.isNotEmpty(roleLink.getIp()) && !ArrayUtils.contains(roleLink.getIp().split(COMMA), user.getIp())){
                            logger.info("用户 {} 被限定在 {} 等IP地址操作，与当前IP {} 不匹配，拒绝授权...", user.getId(), roleLink.getIp(), user.getIp());

                            return userUrls;
                        }

                        if(StringUtils.isNotEmpty(roleLink.getRoles())){
                            for (String roleId : roleLink.getRoles().split(COMMA)) {
                                Role  role = roleService.getById(roleId);
                                if(role!=null) {
                                    if (logger.isDebugEnabled())
                                        logger.debug("加载 {} 归属角色 {}：{}", user.getId(), role.getName(), role.getSummary());
                                    userUrls.addAll(role.isAdmin()? Collections.singletonList(ALL) :role.getUrlList());
                                }
                            }
                        }
                    }
                    logger.info("缓存 {} 的授权 URL：{}", user.getId(), StringUtils.join(userUrls, COMMA));
                    return userUrls;
                },
                3600            //默认缓存 1 个小时
        );
        final AntPathMatcher matcher = new AntPathMatcher();
        for (String path : urls) {
            if(matcher.match(path, url))
                return true;
        }

        return false;
    }

    @Override
    public boolean hasRole(AuthUser user, String roleId) {
        RoleLink link = null;
        try {
            link = loadRoleLink(user);
        } catch (Exception ignored) {}
        return link != null && link.hasRole(roleId);
    }

    /**
     * 2021年8月25日   方法未实现
     * 2021年11月2日   已实现
     * @param user
     * @return
     */
    @Override
    public List<String> loadRoleByUser(AuthUser user) {
        List<String> roles = new ArrayList<>();
        try {
            final RoleLink link = loadRoleLink(user);
            if(link != null && StringUtils.isNotEmpty(StringUtils.trim(link.getRoles())))
                Collections.addAll(roles, link.getRoles().split(COMMA));
        } catch (Exception ignored) {}
        return roles;
    }

    /**
     * 2021年10月20日  方法未实现
     * 2021年11月2日   已实现
     * @param user
     * @return
     */
    @Override
    public String getDepartment(AuthUser user) {
        try{
            final RoleLink link = loadRoleLink(user);
            return link==null? EMPTY : link.getDept()==null? EMPTY: link.getDept();
        }catch (Exception e){
            return EMPTY;
        }
    }

    private RoleLink loadRoleLink(AuthUser user) throws Exception {
        return CacheManage.get("AUTH-ROLE-"+user.getId(), ()-> {
            final RoleLink link = roleLinkService.getById(user.getId());
            if(link != null){
                link.setRoleList(
                        Arrays.stream(link.getRoles().split(COMMA))
                                .map(Role::new)
                                .collect(Collectors.toList())
                );
            }
            return link;
        });
    }
}
