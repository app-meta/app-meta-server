package org.nerve.boot.domain;


import java.util.List;

public class AuthUser {
    private String id;              //用户ID
    private String name;            //用户名称
    private String ip;              //授权IP
    private List<String> roles;     //本地角色标识码

    public String getShowName(){
        return String.format("%s(%s)", name, id);
    }

    public AuthUser(){}
    public AuthUser(String id, String name, String ip){
        this.id = id;
        this.name = name;
        this.ip = ip;
    }

    @Override
    public String toString() {
        return String.format("[ %s#%s %s %s ]", id, name, ip, roles);
    }

    public String getId() {
        return id;
    }

    public AuthUser setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public AuthUser setName(String name) {
        this.name = name;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public AuthUser setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public List<String> getRoles() {
        return roles;
    }

    public AuthUser setRoles(List<String> roles) {
        this.roles = roles;
        return this;
    }

    public boolean hasRole(String role){
        return roles.contains(role);
    }

    public boolean hasRole(Enum roleName) {
        return hasRole(roleName.name());
    }
}
