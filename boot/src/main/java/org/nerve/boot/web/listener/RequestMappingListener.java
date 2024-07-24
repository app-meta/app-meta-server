package org.nerve.boot.web.listener;

import jakarta.annotation.Resource;
import org.nerve.boot.annotation.Public;
import org.nerve.boot.util.ClassNameUtil;
import org.nerve.boot.web.auth.AuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestMappingListener {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private WebApplicationContext webContext;
    @Resource
    private AuthConfig authConfig;

    private static final String SKIP = "BasicErrorController";

    public List<UrlBean> mappingList(){
        final List<UrlBean> urlBeans = new ArrayList<>();

        final RequestMappingHandlerMapping mapping = webContext.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> reqMap = mapping.getHandlerMethods();

        AtomicInteger order = new AtomicInteger(10000);

        reqMap.forEach((info, handler)->{
            Set<String> urls = info.getPatternsCondition().getPatterns();
            Method method = handler.getMethod();
            if(logger.isDebugEnabled()) logger.debug("获取到 RequestMapping ：{} => {}", urls, method.getName());
            String name = info.getName();
            if(name == null){
                name = method.getName();
            }
            if(handler.hasMethodAnnotation(Public.class))   return;

            String scope = handler.getBeanType().getSimpleName();
            if(!SKIP.equals(scope)){
                urlBeans.add(
                        new UrlBean(
                                ClassNameUtil.get(handler.getBeanType(), aClass -> authConfig.getCtrlName(scope)),
                                name,
                                urls.iterator().next(),
                                order.getAndIncrement()
                        )
                );
            }
        });
        return urlBeans;
    }

    public class UrlBean{
        private String scope;
        private String name;
        private String url;
        private int order;          //排序，值越小越重要

        public UrlBean(){}
        public UrlBean(String scope, String name, String url){
            this.scope = scope;
            this.name = name;
            this.url = url;
        }
        public UrlBean(String scope, String name, String url, int order){
            this(scope, name, url);
            this.order = order;
        }

        public String getScope() {
            return scope;
        }

        public UrlBean setScope(String scope) {
            this.scope = scope;
            return this;
        }

        public String getName() {
            return name;
        }

        public UrlBean setName(String name) {
            this.name = name;
            return this;
        }

        public String getUrl() {
            return url;
        }

        public UrlBean setUrl(String url) {
            this.url = url;
            return this;
        }

        public int getOrder() {
            return order;
        }

        public UrlBean setOrder(int order) {
            this.order = order;
            return this;
        }
    }
}
