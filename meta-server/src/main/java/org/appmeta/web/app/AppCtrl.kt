package org.appmeta.web.app

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import jakarta.validation.Valid
import org.appmeta.F
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.domain.*
import org.appmeta.model.*
import org.appmeta.service.*
import org.appmeta.web.CommonCtrl
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Result
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.exception.ServiceException
import org.nerve.boot.module.operation.Operation
import org.springframework.http.HttpStatus
import org.springframework.util.AntPathMatcher
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import java.io.Serializable


/*
 * @project app-meta-server
 * @file    org.appmeta.web.app.AppCtrl
 * CREATE   2022年12月06日 13:39 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("app")
class AppCtrl(
    private val terminalS: TerminalService,
    private val refresh: CacheRefresh,
    private val appAsync: AppAsync,
    private val dashboardS: DashboardService,
    private val roleS: AppRoleService,
    private val mapper: AppMapper, private val service: AppService) : CommonCtrl() {

    protected fun _checkEditAuth(id: Serializable, worker:(App, AuthUser)->Any?): Result {
        val app = mapper.withCache(id)?: throw Exception("应用[${id}]不存在")
        val user = authHolder.get()
        if(app.uid == user.id || H.hasAnyRole(user, Role.ADMIN, Role.APP_MANAGER))
            return resultWithData { worker(app, user) }

        throw Exception(HttpStatus.UNAUTHORIZED.name)
    }

    @PostMapping("overview", name = "应用统计总览")
    fun overview(@RequestBody model: IdStringModel) = resultWithData {
        dashboardS.ofApp(model.id)
    }

    @RequestMapping("top", name = "最新TOP10")
    fun top(@RequestBody model: KeyModel) = resultWithData {
        val list = mapper.loadOrderBy(
            when (model.key) {
                F.LAUNCH    -> F.LAUNCH
                F.MARK      -> F.MARK
                else        -> F.ADD_ON
            }
        )
        list
    }

    @RequestMapping("launch", name = "运行应用")
    fun launch(@RequestBody model: PageModel) = result {
        val user = authHolder.get()
        model.channel = getChannel()
        appAsync.afterLaunch(
            model,
            if(user == null) EMPTY else user.id,
            requestIP
        )
    }

    @RequestMapping("list", name = "应用列表查询")
    fun list(@RequestBody model: QueryModel) = result {
        it.data = service.list(model.form, model.pagination)
        it.total= model.pagination.total
    }

    @RequestMapping("list-mine", name = "我授权的应用列表")
    fun listOfMine(@RequestBody model: QueryModel) = result {
        val user = authHolder.get()
        model.form["EQ_uid"] = user.id

        it.data = service.list(model.form, model.pagination)
        it.total= model.pagination.total
    }

    @RequestMapping("detail", name = "获取应用基本信息")
    fun detail(@RequestBody model: IdStringModel) = resultWithData {
        service.detailOf(model.id)
    }

    @RequestMapping("create", name = "创建新应用")
    fun create(@Valid @RequestBody model: AppModel) = resultWithData {
        val user = authHolder.get()
        val app = model.app

        app.of(user)

        service.create(model)
        opLog("新增应用[${app.name}] 作者=${app.author}", app, Operation.CREATE)

        "[${app.name}]创建成功"
    }

    @RequestMapping("update", name = "更新应用信息")
    fun update(@Valid @RequestBody model: AppModel) = _checkEditAuth(model.app.id) { _, user->
        val app = model.app
        if(!StringUtils.hasText(app.uid))
            app.of(user)

        service.update(model)
        opLog("更新应用[${app.name}] 作者=${app.author}", app, Operation.MODIFY)

        "应用[${app.name}]更新成功"
    }

    @PostMapping("modify", name = "修改应用属性")
    fun modifyField(@RequestBody model: FieldModel) = _checkEditAuth(model.id) { app, _->
        val wrapper = UpdateWrapper<App>()
        when(model.key) {
            F.ACTIVE    -> {
                mapper.update(null, wrapper.eq(F.ID, model.id).set(F.ACTIVE, model.value))
            }
            else        -> throw Exception("暂不支持修改属性 ${model.key}")
        }

        opLog("修改应用#${model.id} 的属性 ${model.key}=${model.value}", app)
    }

    @RequestMapping("delete", name = "删除应用")
    fun delete(@RequestBody model: IdStringModel) = resultWithData {
        val (id) = model

        opLog(
            "删除应用#${id}",
            service.remove(id, authHolder.get()),
            Operation.DELETE
        )
    }

    /*
     * ============================ 角色/权限 ============================
     * 针对后端服务
     */
    @PostMapping("role/mine/{aid}", name = "获取当前用户的应用角色")
    fun roleMine(@PathVariable aid:String) = resultWithData { roleS.loadRoleOfUser(aid, authHolder.get().id) }

    @PostMapping("role/list", name = "查看应用下角色清单")
    fun roleList(@RequestBody model: AppRoleLink) = _checkEditAuth(model.aid) { _, _ ->
        if(StringUtils.hasText(model.uid))
            roleS.loadLink(model.aid, model.uid)
        else
            roleS.roleList(model.aid)
    }

    @PostMapping("role/add", name = "新增应用角色")
    fun addRole(@RequestBody role: AppRole) = _checkEditAuth(role.aid) { app,_ ->
        roleS.addRole(role)
        opLog("新增应用角色${role.uuid}(${role.name})", app, Operation.CREATE)
    }

    @PostMapping("role/delete", name = "删除应用角色")
    fun removeRole(@RequestBody role: AppRole) = _checkEditAuth(role.aid) { app,_ ->
        roleS.removeRole(role.aid, role.uuid).also { opLog("删除应用角色${role.uuid}（结果=${it}）", app, Operation.DELETE) }
    }

    @PostMapping("role/update", name = "更新应用角色")
    fun updateRole(@RequestBody role: AppRole) = _checkEditAuth(role.aid) { _,_ -> roleS.updateRole(role) }

    @PostMapping("role/check", name = "检测用户对指定URL的访问权限")
    fun checkRoleAuth(@RequestBody model:AppRole) = resultWithData {
        val terminal = terminalS.load(model.aid)
        val matcher = AntPathMatcher()

        if(terminal.publics.any { matcher.match(it, model.auth) }) {
            if(logger.isDebugEnabled)   logger.debug("${H.wrapText(model.aid)}的资源 ${model.auth} 为公开访问")
            "true（授权依据：资源公开）"
        } else
            roleS.checkAuth(
                model.aid,
                AuthUser(model.uuid, EMPTY, model.ip),
                model.auth
            )
    }

    @PostMapping("role/link", name = "分配应用角色到用户")
    fun roleLink(@RequestBody link:AppRoleLink) = _checkEditAuth(link.aid) { app,_ ->
        roleS.updateLink(link)
        opLog("分配应用角色[${link.role}]到用户${link.uid}", app, Operation.MODIFY)
    }

    @PostMapping("role/clean-cache", name = "删除指定应用的授权缓存")
    fun roleCacheClean(@RequestBody model:AppRole) = result { roleS.cleanCache(model.aid) }
}