package org.appmeta.web.system

import org.appmeta.H
import org.appmeta.Role
import org.appmeta.S
import org.appmeta.domain.Account
import org.appmeta.model.FieldModel
import org.appmeta.model.IdStringModel
import org.appmeta.model.QueryModel
import org.appmeta.model.TextModel
import org.appmeta.service.AccountService
import org.appmeta.service.CacheRefresh
import org.nerve.boot.Const.COMMA
import org.nerve.boot.cache.CacheManage
import org.nerve.boot.module.auth.RoleLinkMapper
import org.nerve.boot.module.auth.RoleMapper
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.web.auth.RoleLink
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.util.Assert
import org.springframework.util.ObjectUtils
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*


/*
 * @project app-meta-server
 * @file    org.appmeta.web.system.PageManageCtrl
 * CREATE   2023年03月23日 15:20 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("system/account")
class AccountManageCtrl(
    private val cacheR: CacheRefresh,
    private val roleM: RoleMapper,
    private val roleLinkM:RoleLinkMapper,
    private val settingS:SettingService,
    private val service:AccountService):BasicController() {

    @PostMapping("detail", name = "获取用户信息")
    fun detail(@RequestBody model:IdStringModel) = resultWithData { service.toAuthUser(model.id, requestIP) }

    @RequestMapping("list", name = "用户清单")
    fun list(@RequestBody model: QueryModel) = result {
        it.data = service.list(model.form, model.pagination)
        it.total= model.pagination.total
    }

    @PostMapping("add", name = "新增用户")
    fun add(@RequestBody account:Account) = resultWithData {
        service.saveOrUpdate(account)
        opLog("新增用户 $account", account, Operation.CREATE)
    }

    @PostMapping("delete", name = "删除用户")
    fun remove(@PathVariable id:Long) = resultWithData {
        val account = service.getById(id)
        if(account != null){
            //判断是否为管理员
            val roleLink = roleLinkM.selectById(id)?: RoleLink()
            if(roleLink.hasRole(Role.ADMIN.name)){
                throw Exception("${account.name}(${account.id})具备角色[管理员/${Role.ADMIN}]，不能直接删除")
            }

            service.removeById(account)
            opLog("删除用户 $account", account, Operation.DELETE)
        }
    }

    @PostMapping("refresh", name = "执行远程数据同步")
    fun refreshRemote(@RequestBody model:TextModel) = resultWithData {
        val remote = if(StringUtils.hasText(model.text))
            model.text
        else
            settingS.value(S.SYS_ACCOUNT_REMOTE)

        if(!StringUtils.hasText(remote))    throw Exception("请传递数据或设置远程同步地址[${S.SYS_ACCOUNT_REMOTE}]")

        service.refreshFromRemote(remote)
    }

    @PostMapping("update-role", name = "更新用户角色")
    fun updateRole(@RequestBody model: FieldModel) = result {
        val user = authHolder.get()
        //仅允许管理员操作管理员的权限
        val roleValue = model.value.toString()
        if(roleValue.uppercase().split(COMMA).contains(Role.ADMIN.name)){
            if(!user.hasRole(Role.ADMIN))   throw Exception("[管理员/${Role.ADMIN}]权限仅限管理员操作")
        }

        val account = service.getById(model.id)?: throw Exception("用户#${model.id}不存在")

        val roleLink = roleLinkM.selectById(model.id)?: RoleLink().also {
            logger.info("${account.id} 没有分配任何权限，即将创建...")
            it.name = account.name
            it.dept = account.did
            it.roles = ""
        }

        roleLink.roles = if(model.key == "add" || model.key == "remove"){
            Assert.isTrue(!StringUtils.hasText(roleValue), "角色参数不能为空")
            val role = roleM.selectById(roleValue)?: throw Exception("角色 $roleValue 不存在")

            //roleLink.roles.split(COMMA).map { it.trim() }
            val roles = H.splitToList(roleLink.roles).toMutableSet()
            if(model.key == "add")
                roles.add(role.id)
            else
                roles.remove(role.id)
            roles.filter { StringUtils.hasText(it) }.joinToString(COMMA)
        }
        else{
            roleValue
        }

        if(roleLink.using()){
            roleLinkM.updateById(roleLink)
            opLog("更新 ${account.id} 的角色：action=${model.key} value=${roleValue}", roleLink)
        }
        else{
            roleLink.id = account.id
            roleLinkM.insert(roleLink)
            opLog("新建 ${account.id} 的角色: ${roleLink.roles}", roleLink)
        }

        //清空缓存
        CacheManage.clear("AUTH-ROLE-${account.id}")
        CacheManage.clearWithPrefix("AUTH-${account.id}")
        cacheR.authUser(account.id)
    }
}