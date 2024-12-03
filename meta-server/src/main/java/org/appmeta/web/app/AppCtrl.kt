package org.appmeta.web.app

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import jakarta.validation.Valid
import org.apache.commons.lang3.exception.ExceptionUtils
import org.appmeta.F
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.component.deploy.Deployer
import org.appmeta.domain.*
import org.appmeta.model.*
import org.appmeta.service.*
import org.appmeta.web.CommonCtrl
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Result
import org.nerve.boot.db.service.QueryHelper
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.operation.Operation
import org.springframework.http.HttpStatus
import org.springframework.util.AntPathMatcher
import org.springframework.util.Assert
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
    private val deployer: Deployer,
    private val mapper: AppMapper,
    private val logAsync: LogAsync,
    private val appLogM:AppLogMapper,
    private val service: AppService) : CommonCtrl() {

    protected fun _checkEditAuth(id: Serializable):Pair<App, AuthUser>{
        val app = mapper.withCache(id)?: throw Exception("应用[${id}]不存在")
        val user = authHolder.get()
        if(app.uid == user.id || H.hasAnyRole(user, Role.ADMIN, Role.APP_MANAGER))
            return Pair(app, user)

        throw Exception(HttpStatus.UNAUTHORIZED.name)
    }

    protected fun _checkEditAuth(id: Serializable, worker:(App, AuthUser)->Any?): Result {
//        val app = mapper.withCache(id)?: throw Exception("应用[${id}]不存在")
//        val user = authHolder.get()
//        if(app.uid == user.id || H.hasAnyRole(user, Role.ADMIN, Role.APP_MANAGER))
//            return resultWithData { worker(app, user) }
//
//        throw Exception(HttpStatus.UNAUTHORIZED.name)
        val data = _checkEditAuth(id)
        return resultWithData { worker(data.first, data.second) }
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

    @PostMapping("offline", name = "上/下架应用")
    fun offline(@RequestBody model:IdStringModel) = _checkEditAuth(model.id) { app, user->
        logger.info("${user.showName}操作应用 ${app.id} 上下架...")

        if(!app.offline){
            //判断是否有后端服务
            if(service.hasTerminal(app.id)){
                try{
                    deployer.stop(app.id)
                    logger.info("停止后端服务/${app.id}")
                }catch (e:Exception){
                    logger.error("停止 ${app.id} 后端服务失败：${ExceptionUtils.getMessage(e)}")
                }
            }
        }
        mapper.update(null, UpdateWrapper<App>().eq(F.ID, app.id).set(F.OFFLINE, !app.offline))
        refresh.app(app.id)

        opLog("${if(app.offline) "上架" else "下架"}应用#${app.id}", app)
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

    @PostMapping("role/links", name="查看应用角色分配汇总")
    fun roleLinkList(@RequestBody model: AppRoleLink) = _checkEditAuth(model.aid) {_, _->
        roleS.loadLinks(model.aid)
    }

    @PostMapping("role/clean-cache", name = "删除指定应用的授权缓存")
    fun roleCacheClean(@RequestBody model:AppRole) = result { roleS.cleanCache(model.aid) }

    @PostMapping("log-list-{aid}", name="应用日志")
    fun logList(@RequestBody model: QueryModel, @PathVariable aid:String) = QueryHelper<AppLog>().let { h->
        _checkEditAuth(aid)

        model.form["EQ_aid"] = aid
        val p = Page.of<AppLog>(model.pagination.page.toLong(), model.pagination.pageSize.toLong())
        val list = appLogM.selectList(p, h.buildFromMap(model.form))

        logger.info("SIZE={}", list.size)
        Result(p.total, list)
    }

    @PostMapping("log-add", name="录入应用日志")
    fun logAdd(@RequestBody log: AppLog) = result {
        Assert.hasText(log.msg, "日志内容不能为空")

        val app = mapper.withCache(log.aid)?: throw Exception("应用[${log.aid}]不存在")
        val user = authHolder.get()

        logAsync.saveAppLog(app.id, user.id, log.msg, getChannel())
    }
}