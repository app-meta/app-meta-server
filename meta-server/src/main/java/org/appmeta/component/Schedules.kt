package org.appmeta.component

import org.appmeta.S
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