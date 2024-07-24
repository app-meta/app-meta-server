package org.nerve.boot.web.ctrl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.nerve.boot.Result;
import org.nerve.boot.web.listener.RequestMappingListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ConditionalOnProperty(value = "nerve.auth.enable", havingValue = "true", matchIfMissing = true)
public class RequestMappingCtrl extends BaseController {

    @Resource
    private RequestMappingListener mappingListener;

    @PostConstruct
    private void init(){
        if(logger.isDebugEnabled()){
            logger.debug("开放 RequestMapping 接口，如需关闭请设置：nerve.auth.enable=false");
        }
    }

    @RequestMapping(value = "sys/mapping", name = "查看接口（操作）列表")
    public Result mappingList(){
        return resultWithData(()->{
            List<RequestMappingListener.UrlBean> urls = mappingListener.mappingList();

            return urls;
        });
    }
}
