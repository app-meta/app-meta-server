package org.appmeta.web.page

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import org.appmeta.S
import org.appmeta.component.RobotFinishEvent
import org.appmeta.domain.RobotLog
import org.appmeta.domain.RobotLogMapper
import org.appmeta.model.QueryModel
import org.nerve.boot.db.service.QueryHelper
import org.nerve.boot.module.setting.SettingService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.web.page.SupportRobotCtrl
 * CREATE   2023年05月08日 15:43 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("page/robot")
class SupportRobotCtrl(
    private val eventPublisher: ApplicationEventPublisher,
    private val settingS:SettingService, private val mapper:RobotLogMapper) : BasicPageCtrl() {

    @PostMapping("save", name = "保存机器人运行记录")
    fun save(@RequestBody log:RobotLog) = if(settingS.booleanValue(S.SYS_ROBOT_TRACE, true))
        _checkServiceResult(log.pid) { _, user ->
            log.uid = user.id
            log.addOn = System.currentTimeMillis()
            log.ip = requestIP

            if(logger.isDebugEnabled)   logger.debug("ROBOT 运行记录：\n{}", JSON.toJSONString(log, JSONWriter.Feature.PrettyFormat))
            mapper.insert(log)
            logger.info("保存来自 ${log.ip} 的 ROBOT 记录 PID=${log.pid} OS=${log.os} CHROME=${log.chrome} 耗时=${log.used} 秒")

            eventPublisher.publishEvent(RobotFinishEvent(log))
            log.id
        }
    else
        result { }

    @PostMapping("list", name = "机器人运行记录清单")
    fun list(@RequestBody model:QueryModel) = result {
        val p = Page.of<RobotLog>(model.pagination.page.toLong(), model.pagination.pageSize.toLong())
        mapper.selectPage(p, QueryHelper<RobotLog>().buildFromMap(model.form))

        it.total = p.total
        it.data = p.records
    }
}