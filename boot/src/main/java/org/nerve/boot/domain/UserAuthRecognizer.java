package org.nerve.boot.domain;

/*
 * 用户与接口（URL）权限识别器
 */

import java.util.List;

public interface UserAuthRecognizer {

    boolean hasAuth(AuthUser user, String url) throws Exception;

    boolean hasRole(AuthUser user, String roleId);

    List<String> loadRoleByUser(AuthUser user);

    /**
     * 获取归属的部门
     * @param user
     * @return
     */
    String getDepartment(AuthUser user);
}
