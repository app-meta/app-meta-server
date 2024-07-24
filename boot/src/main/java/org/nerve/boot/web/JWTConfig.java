package org.nerve.boot.web;
/*
 * @project boot-starter
 * @file    org.nerve.boot.web.JWTConfig
 * CREATE   2022年11月11日 18:37 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nerve.jwt")
public class JWTConfig {
    String key;
    long expire = 12 * 60 * 60;
    String issuer = "NERVE";

    public JWTConfig(){}
    public JWTConfig(String key){
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
