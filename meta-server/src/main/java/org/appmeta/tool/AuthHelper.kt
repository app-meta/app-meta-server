package org.appmeta.tool

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import jakarta.annotation.Resource
import org.appmeta.*
import org.appmeta.component.AppConfig
import org.appmeta.domain.Account
import org.appmeta.domain.AccountMapper
import org.appmeta.domain.EditAuthable
import org.appmeta.domain.ServiceAuthable
import org.appmeta.service.AppRoleService
import org.appmeta.service.AppService
import org.nerve.boot.Const
import org.nerve.boot.cache.CacheManage
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.setting.SettingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


/*
 * @project app-meta-server
 * @file    org.appmeta.component.Auth
 * CREATE   2023年03月22日 15:42 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Component
class AuthHelper (
    private val settingS:SettingService,
    private val config: AppConfig,
    private val appRoleS: AppRoleService,
    private val accountM: AccountMapper) {

    private val logger = LoggerFactory.getLogger(AuthHelper::class.java)

    /**
     * 默认缓存 5 分钟
     */
    fun checkService(authText:String, authOwner:String, user:AuthUser):Boolean {
        return CacheManage.get(
            "auth-${user.id}-$authText",
            {
                if(logger.isDebugEnabled)   logger.debug("判断 ${user.id} 是否匹配权限 $authText")
                if(authText.isEmpty())  return@get true
                if(authText == ALL) return@get true
                //判断超级管理员与数据从属关系
                if(user.hasRole(Role.ADMIN) || user.id == authOwner) return@get true

                val items = authText.split(Const.COMMA)

                //优先匹配用户授权
                if(items.contains("U${user.id}"))  return@get true

                //判断部门
                val account = accountM.selectOne(QueryWrapper<Account>().eq(F.ID, user.id))
                if(account != null && items.contains("D${account.did}"))   return@get true

                //判断角色
                if(items.filter { it.startsWith("R") }
                        .map { it.substring(1) }
                        .any { user.roles.contains(it) } )
                    return@get true

                //判断应用角色
                if(items.filter { it.startsWith("A") }
                        .map { it.substring(1).split(Const.AT) }
                        .any { rs->
                            if(rs.size < 2)
                                false
                            else{
                                appRoleS.loadRoleAndAuthOfUser(rs[0], user.id).first.let { appRoles-> appRoles.contains(rs[1]) }
                            }
                        }
                ){
                    return@get true
                }

                false
            },
            config.authCacheExpire * 60
        )
    }

    /**
     *
     */
    fun checkService(auth:ServiceAuthable, user: AuthUser) = checkService(auth.serviceAuth, auth.uid, user)

    fun checkEdit(auth:EditAuthable, user: AuthUser) = checkService(auth.editAuth, auth.uid, user)

    /**
     * 判断管理员是否在指定的设备进行操作
     */
    fun checkAdminAndWhiteIP(user: AuthUser) {
        val whiteIps = settingS.value(S.SYS_WHITE_IP)?.split(Const.COMMA)?: emptyList()
        if(H.hasAnyRole(user, Role.ADMIN, Role.SYS_ADMIN) && whiteIps.contains(user.ip))
            return

        throw Exception("该功能需要 ${Role.ADMIN}/${Role.SYS_ADMIN} 在特定设备下执行")
    }
}