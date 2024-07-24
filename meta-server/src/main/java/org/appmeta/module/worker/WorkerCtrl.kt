package org.appmeta.module.worker

import org.appmeta.model.RemoteRobotModel
import org.nerve.boot.Result
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.module.worker.WorkerCtrl
 * CREATE   2023年10月24日 15:15 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("worker")
class WorkerCtrl(private val service: WorkerService):BasicController() {

    @PostMapping("run-robot", name = "执行远程机器人任务")
    fun runRobot(@RequestBody model: RemoteRobotModel): Result {
        val user = authHolder.get()
        logger.info("${user.showName} 呼叫工作者[${model.worker}]执行 ROBOT#${model.robotId}")

        model.uid = user.id
        return service.runRobot(model)
    }

    @PostMapping("status", name = "检查工作者状态")
    fun status(@RequestBody model: RemoteRobotModel): Result {
        val user = authHolder.get()
        logger.info("${user.showName} 检查工作者[${model.worker}]状态")

        model.uid = user.id
        return service.status(model)
    }

    @RequestMapping("fetch/{id}", name = "获取远程机器人结果")
    fun fetch(@PathVariable("id") id:String) = result {
        val user = authHolder.get()
        if(logger.isDebugEnabled)   logger.debug("${user.showName} 查询远程任务 $id 的执行结果...")

        val task = service.fetchResult(id, user.id)
        it.data = task.second
        it.setSuccess(task.first)
    }
}