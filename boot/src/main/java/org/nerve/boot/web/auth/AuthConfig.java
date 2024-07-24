package org.nerve.boot.web.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "nerve.auth")
public class AuthConfig {
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private boolean enable = true;                              //是否开启权限判断，true时对每次请求都判断当前用户是否具备权限
    private List<String> whiteIps = new ArrayList<>();          //IP白名单
    private List<String> whiteIds = new ArrayList<>();          //ID白名单
    private List<String> popularUrls = new ArrayList<>();       //公开URL列表
    private List<String> innerUrls = new ArrayList<>();         //内部URL列表（只要登录就能访问）
    private boolean attach = true;                              //是否公开静态资源访问
    private String attachDir = "attach";
    private Map<String,String> ctrlNames = new HashMap<>();     //控制器名称
    private String tokenName    = "UA";

    public boolean isIpInWhite(String ip){
        return whiteIps.contains(ip);
    }

    public boolean isIdInWhite(String id){
        return whiteIds.contains(id);
    }

    public String getCtrlName(String name){
        return ctrlNames.getOrDefault(name, name);
    }

    /**
     * 判断是否为公开 URL（暂不支持规则匹配）
     * @param url
     * @return
     */
    public boolean isPopular(String url) {
        for (String popularUrl : popularUrls) {
            if(pathMatcher.match(popularUrl, url))
                return true;
        }
        return false;
    }

    public boolean isInner(String url){
        for (String innerUrl : innerUrls) {
            if(pathMatcher.match(innerUrl, url))
                return true;
        }
        return false;
    }

    public List<String> getWhiteIps() {
        return whiteIps;
    }

    public AuthConfig setWhiteIps(List<String> whiteIps) {
        this.whiteIps = whiteIps;
        return this;
    }

    public boolean isEnable() {
        return enable;
    }

    public AuthConfig setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public List<String> getPopularUrls() {
        return popularUrls;
    }

    public AuthConfig setPopularUrls(List<String> popularUrls) {
        this.popularUrls = popularUrls;
        return this;
    }

    public List<String> getInnerUrls() {
        return innerUrls;
    }

    public AuthConfig setInnerUrls(List<String> innerUrls) {
        this.innerUrls = innerUrls;
        return this;
    }

    public boolean isAttach() {
        return attach;
    }

    public AuthConfig setAttach(boolean attach) {
        this.attach = attach;
        return this;
    }

    public String getAttachDir() {
        return attachDir;
    }

    public void setAttachDir(String attachDir) {
        this.attachDir = attachDir;
    }

    public Map<String, String> getCtrlNames() {
        return ctrlNames;
    }

    public AuthConfig setCtrlNames(Map<String, String> ctrlNames) {
        this.ctrlNames = ctrlNames;
        return this;
    }

    public List<String> getWhiteIds() {
        return whiteIds;
    }

    public AuthConfig setWhiteIds(List<String> whiteIds) {
        this.whiteIds = whiteIds;
        return this;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }
}
