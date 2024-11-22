package org.appmeta.component

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.appmeta.F
import org.appmeta.S
import org.appmeta.domain.TerminalLog
import org.appmeta.domain.TerminalLogDetail
import org.appmeta.domain.TerminalLogDetailMapper
import org.appmeta.domain.TerminalLogMapper
import org.appmeta.service.AccountService
import org.nerve.boot.module.schedule.AbstractSchedule
import org.nerve.boot.module.setting.SettingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils


/*
 * @project app-meta-server
 * @file    org.appmeta.component.Schedules
 * CREATE   2023年03月24日 13:16 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Component
class DailySchedule(private val accountS:AccountService, private val settingS:SettingService) : AbstractSchedule() {
    override fun getCategoryName() = "每日跑批"

    /**
     * 默认每天一点执行
     */
    @Scheduled(cron = "\${app.schedule-daily:0 0 1 * * ?}")
    fun doEveryDay() = doWork {
        val workResult = mutableListOf<String>()

        refreshAccount()?.let { workResult.add(it) }

        workResult.joinToString("；")
    }

    private fun refreshAccount():String? {
        if(settingS.intValue(S.SYS_ACCOUNT_INTERVAL, 1)>0){
            val remote = settingS.value(S.SYS_ACCOUNT_REMOTE)
            if(StringUtils.hasText(remote))
                return "Account 更新：${accountS.refreshFromRemote(remote)}"
        }
        return null
    }
}

@Component
class LogDetailCleanSchedule(
    private val settingS: SettingService,
    private val logM: TerminalLogMapper,
    private val logDetailM: TerminalLogDetailMapper
) : AbstractSchedule() {
    override fun getCategoryName() = "应用访问日志清理"

    //每天凌晨2点执行
    @Scheduled(cron = "0 0 2 * * ?")
    fun doClean() = doWork {
        val day = settingS.intValue(S.TERMINAL_EXPIRE, 90)
        //查询到最后可存活的ID值
        val log = logM.selectOne(
            QueryWrapper<TerminalLog>()
                .lt(F.ADD_ON, System.currentTimeMillis() - day*24*60*60*100)
                .orderByDesc(F.ID)
        )
        val count = if(log != null){
            logDetailM.delete(QueryWrapper<TerminalLogDetail>().le(F.ID, log.id))
        }
        else
            0
        "清空${day}天之前的请求详情共${count}条"
    }
}