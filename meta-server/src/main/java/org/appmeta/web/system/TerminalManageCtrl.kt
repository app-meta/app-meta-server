package org.appmeta.web.system

import org.appmeta.component.deploy.Deployer
import org.appmeta.model.IdStringModel
import org.appmeta.model.QueryModel
import org.appmeta.service.TerminalService
import org.nerve.boot.cache.CacheManage
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("system/terminal")
class TerminalManageCtrl(
    private val service:TerminalService,
    private val deployer: Deployer):BasicController() {

    val KEY = "SYS-TERMINAL-OVERVIEW"

    @PostMapping("requirement")
    fun requirement() = resultWithData {
        deployer.checkRequirement()
    }

    @PostMapping("overview")
    fun overview() = resultWithData {
        CacheManage.get(KEY, { deployer.overview() }, 5*60)
    }

    @RequestMapping("trace", name = "后端服务记录列表")
    fun logList(@RequestBody model: QueryModel) = result {
        it.data = service.logList(model.form, model.pagination)
        it.total= model.pagination.total
    }

    @PostMapping("stop", name = "关闭后端服务")
    fun stop(@RequestBody model:IdStringModel) = result {
        deployer.stop(model.id)
        CacheManage.clear(KEY)
        opLog("关闭「${model.id}」后端服务", null, Operation.MODIFY)
    }

    @PostMapping("restart", name = "重启后端服务")
    fun restart(@RequestBody model:IdStringModel) = result {
        deployer.restart(model.id)
        CacheManage.clear(KEY)
        opLog("重启「${model.id}」后端服务", null, Operation.MODIFY)
    }

    @PostMapping("remove", name = "永久删除后端服务")
    fun remove(@RequestBody model:IdStringModel) = result {
        deployer.remove(model.id)
        CacheManage.clear(KEY)
        opLog("永久删除「${model.id}」后端服务", null, Operation.DELETE)
    }
}