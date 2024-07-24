package org.appmeta.module.worker

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import org.appmeta.F
import org.appmeta.component.RobotFinishEvent
import org.appmeta.domain.Member
import org.appmeta.domain.MemberMapper
import org.appmeta.domain.Page
import org.appmeta.domain.PageMapper
import org.appmeta.model.RemoteRobotModel
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Result
import org.nerve.boot.enums.Status
import org.nerve.boot.util.RSAProvider
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.http.HttpEntity
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets


/*
 * @project app-meta-server
 * @file    org.appmeta.module.worker.WorkerService
 * CREATE   2023年10月16日 18:06 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class WorkerService(private val workerTaskM:WorkerTaskMapper,private val memberM:MemberMapper, private val pageM:PageMapper) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val PORT = 9900

    val restTemplate = RestTemplate().also {
        //配置编码，避免传送时乱码
        it.messageConverters[1] = StringHttpMessageConverter(StandardCharsets.UTF_8)
    }

    fun call(worker: Member, task: WorkerTask): Result {
        task.init(worker.id)
        val body = restTemplate.postForObject(
            "http://${worker.id}:${PORT}",
            HttpEntity(RSAProvider(worker.pubKey, null).encrypt(JSON.toJSONString(task.toTaskBean()))),
            String::class.java
        ) ?: throw Exception("调用远程Worker失败：响应为空")

        if(logger.isDebugEnabled)
            logger.debug("调用 WORKER#${worker.id}[METHOD=${task.method}] 结果={}", body)
        else
            logger.info("调用 WORKER#${worker.id}[METHOD=${task.method}]")

        var isPromise = false
        val result = JSON.parseObject(body, Result::class.java)
        //对于非执行类操作，直接记录结果
        if(!result.isSuccess){
            task.fail(result.message)
        }
        else if(task.method != WorkerMethods.START){
            task.status     = Status.SUCCESS
            task.response   = body
            task.done(body)
        }
        else
            isPromise = true

        workerTaskM.insert(task)
        //如果是异步任务，则返回ID，客户端可根据该ID后续查询结果
        return if(isPromise) Result.ok(task.id) else result
    }

    private fun <R> withWorker(idOrUuid:String, job:(Member)->R) =
        memberM.loadByIdOrUuid(idOrUuid).let { worker ->
            if (worker == null || worker.category != Member.WORKER) throw Exception("终端[${idOrUuid}]不存在或不是一个有效的 WORKER")
            job(worker)
        }

    /**
     * 执行远程机器人
     */
    fun runRobot(model: RemoteRobotModel) = withWorker(model.worker) { worker->
        //获取 Robot 信息
        val robot = pageM.selectOne(
            QueryWrapper<Page>().eq(F.ID, model.robotId).eq(F.TEMPLATE, Page.ROBOT)
        )?: throw Exception("ROBOT#${model.robotId}不存在或不适用")

        if(logger.isDebugEnabled) logger.debug("调用终端 ${worker.id} 执行 ROBOT#${robot.id}(${robot.name})，参数 ${model.params}")
        call(
            worker,
            WorkerTask(
                model.uid,
                WorkerMethods.START,
                mapOf(F.ID to robot.id, F.PARAMS to model.params)
            )
        )
    }

    @EventListener(RobotFinishEvent::class, condition = "#e.robotLog.link?.length()>0")
    fun onRobotDone(e:RobotFinishEvent){
        val log = e.robotLog
        if(logger.isDebugEnabled)   logger.debug("检测到ROBOT完成，LINK=${log.link}，数据=${log.caches}")

        workerTaskM.update(
            null,
            UpdateWrapper<WorkerTask>()
                .eq(F.ID, log.link)
                .set(F.DONE_ON, System.currentTimeMillis())
                .set(F.STATUS, Status.SUCCESS)
                .set(F.RESPONSE, if(log.caches.isNotEmpty()) JSON.toJSONString(log.caches) else EMPTY)
        )
        logger.info("更新工作者任务 ${log.link} 的状态...")
    }

    /**
     *
     */
    fun status(model: RemoteRobotModel) = withWorker(model.worker) {worker->
        call(worker, WorkerTask(model.uid, WorkerMethods.STATUS))
    }

    fun fetchResult(id:String, uid:String): Pair<Boolean, String> {
        val task = workerTaskM.selectOne(QueryWrapper<WorkerTask>().eq(F.ID, id).eq(F.UID, uid))?: throw Exception("任务记录不存在")

        return Pair(task.status == Status.SUCCESS, task.response)
    }
}