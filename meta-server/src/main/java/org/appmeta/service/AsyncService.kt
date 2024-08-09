package org.appmeta.service

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import org.appmeta.F
import org.appmeta.S
import org.appmeta.component.AppConfig
import org.appmeta.domain.*
import org.appmeta.model.PageModel
import org.appmeta.tool.LimitMap
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.module.setting.Setting
import org.nerve.boot.module.setting.SettingService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil


/*
 * @project app-meta-server
 * @file    org.appmeta.service.AsyncService
 * CREATE   2023年01月10日 13:33 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class AppAsync(
    private val settingS: SettingService,
    private val config: AppConfig,
    private val mapper: AppMapper,
    private val accountHelper: AccountHelper,
    private val launchM:PageLaunchMapper,
    private val pageM:PageMapper) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val launchMap = LimitMap<Long>(500)

    /**
     * 数值类型字段的增减
     */
    private fun modifyIncrement(id:String, field:String, step:Int = 1){
        mapper.update(
            null,
            UpdateWrapper<App>()
                .eq(F.ID, id)
                .setSql("$field = $field ${if(step<0) "-" else "+"} ${abs(step)}")
        )
        logger.info("[APP-ASYNC] #$id $field += $step")
    }

    @Async
    fun afterLaunch(model: PageModel, uid:String, ip:String=EMPTY) {
        val key = "${model}-${uid}-${model.channel}"
        val cur = System.currentTimeMillis()
        if(logger.isDebugEnabled)   logger.debug("[LAUNCH] IP=$ip KEY=${key} TIME=$cur CACHE=${launchMap[key]}")

        val hasKey = launchMap.containsKey(key)
        if(!hasKey || (cur - launchMap[key]!! > config.appLaunchWindow*60*1000L)){
            if(logger.isDebugEnabled)   logger.debug("[LAUNCH] 即将记录 $uid 访问 $model 的数据...")
            modifyIncrement(model.aid, F.LAUNCH)

            if(StringUtils.hasText(model.pid)){
                //更新页面运行计数
                pageM.onLaunch(model.pid)

                //记录信息
                PageLaunch(model).also {
                    it.uid      = uid
                    it.ip       = ip
                    it.channel  = if(StringUtils.hasText(model.channel)) model.channel else settingS.value(S.SYS_DEFAULT_CHANNEL)
                    it.depart   = accountHelper.getDepartByUid(uid)

                    launchM.insert(it)
                }
            }

            launchMap[key] = cur
        }
        else{
            if(!hasKey) launchMap[key] = cur
        }
    }

    @Async
    fun afterMark(id: String, isRemove:Boolean = false) = modifyIncrement(id, F.MARK, if(isRemove) -1 else 1)

    @Async
    fun afterLike(id: String) = modifyIncrement(id, F.THUMB)
}

/**
 * 系统级别的异步处理
 */
@Service
class SystemAsync(private val accountS:AccountService,private val settingS:SettingService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun _log(msg:String) = logger.info("[系统异步作业] $msg")

    @Async
    fun onSettingChange(setting: Setting) {
        when (setting.id) {
            S.SYS_ACCOUNT_REMOTE.name -> {
                if(StringUtils.hasText(setting.content) && settingS.intValue(S.SYS_ACCOUNT_INTERVAL, 0) > 0){
                    _log("检测到用户同步地址变更，即将进行数据同步...")
                    // 立即进行用户数据同步
                    accountS.refreshFromRemote(setting.content)
                }
            }
        }
    }
}

@Service
class LogAsync(
    private val appM:AppMapper,
    private val logM:TerminalLogMapper,
    private val logDetailM:TerminalLogDetailMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val counter = ConcurrentHashMap<String, Int>()

    @Scheduled(fixedDelay = 2*60*1000)
    protected fun cleanCount() {
        if(counter.isEmpty())   return

        /*
        热度只同步到 App，避免对应的 Page（后端服务） 因热度过高进入排行榜
        同时，接口调用转化为应用热度有一定的折算（默认是 3 分之一）
         */
        counter.forEach { (id, v) ->
            appM.updateLaunch(id, ceil(v / 3.0).toInt())
        }
        logger.info("同步应用热度 $counter")

        counter.clear()
    }

    @Async
    fun save(log: TerminalLog, detail: TerminalLogDetail?=null){
        if(log.addOn <= 0L) log.addOn = System.currentTimeMillis()
        else {
            if(log.used <= 0L)  log.used = System.currentTimeMillis() - log.addOn
        }

        if(logger.isDebugEnabled)   logger.debug("记录后端/FaaS日志 ${log.url} (${log.used} ms) 保存详情=${detail != null}")

        logM.insert(log)

        if(log.code != HttpStatus.INTERNAL_SERVER_ERROR.value()){
            // 对应的应用热度+1
            counter[log.aid] = counter.getOrDefault(log.aid, 0)+1
        }

        if(detail != null){
            detail.id = log.id
            logDetailM.insert(detail)
        }
    }
}