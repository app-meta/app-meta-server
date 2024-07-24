package org.nerve.boot.web.auth;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.ArrayUtils;
import org.nerve.boot.db.IgnoreField;
import org.nerve.boot.db.StringEntity;

import java.util.List;

import static org.nerve.boot.Const.COMMA;

@TableName("sys_role_link")
public class RoleLink extends StringEntity {

    private String name;        //姓名
    private String dept;        //部门
    private String ip;          //ip绑定
    private String roles;   //授权的用户角色
    @IgnoreField
    @TableField(exist = false)
    private List<Role> roleList;

    public boolean hasRole(String roleId){
        if(roleList == null)   return false;
        return ArrayUtils.contains(roles.split(COMMA), roleId);
    }

    public String getName() {
        return name;
    }

    public RoleLink setName(String name) {
        this.name = name;
        return this;
    }

    public String getDept() {
        return dept;
    }

    public RoleLink setDept(String dept) {
        this.dept = dept;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public RoleLink setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public List<Role> getRoleList() {
        return roleList;
    }

    public RoleLink setRoleList(List<Role> roleList) {
        this.roleList = roleList;
        return this;
    }

    public String getRoles() {
        return roles;
    }

    public RoleLink setRoles(String roles) {
        this.roles = roles;
        return this;
    }
}
