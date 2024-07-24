package org.nerve.boot.web.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nerve.boot.db.StringEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.nerve.boot.Const.COMMA;


@TableName("sys_role")
public class Role extends StringEntity {
    private String name;                    //角色名称
    private String summary;                 //描述
    private String urls;                    //授权的url
    private boolean admin;                  //是否为管理员

    public Role(){}
    public Role(String roleId){
        this.id = roleId;
    }

    public boolean isAdmin() {
        return admin;
    }

    public Role setAdmin(boolean admin) {
        this.admin = admin;
        return this;
    }

    public String getName() {
        return name;
    }

    public Role setName(String name) {
        this.name = name;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Role setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    @JsonIgnore
    public List<String> getUrlList(){
        return urls != null? Arrays.asList(urls.split(COMMA)) : Collections.emptyList();
    }

    public String getUrls() {
        return urls;
    }

    public Role setUrls(String urls) {
        this.urls = urls;
        return this;
    }
}
