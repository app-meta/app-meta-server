package org.nerve.boot.web;

import jakarta.annotation.Resource;
import org.nerve.boot.FileStore;
import org.nerve.boot.web.auth.AuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebComponent {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    RestTemplateBuilder restTemplateBuilder;

    @Bean
    public RestTemplate restTemplate(){
        return restTemplateBuilder.build();
    }

    @Resource
    private AuthConfig config;

    @Bean
    public FileStore fileStore(){
        FileStore fileStore = new FileStore(config.getAttachDir());
        if(logger.isDebugEnabled()) logger.debug("构建 FileStore，数据目录为 {}", config.getAttachDir());
        return fileStore;
    }
}
