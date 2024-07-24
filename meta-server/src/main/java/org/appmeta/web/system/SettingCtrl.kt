package org.appmeta.web.system

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.appmeta.component.SettingChangeEvent
import org.nerve.boot.module.setting.Setting
import org.nerve.boot.module.setting.SettingMapper
import org.nerve.boot.web.ctrl.BaseController
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.web.system.SettingCtrl
 * CREATE   2023年01月12日 16:09 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class SettingLoader(private val settingM:SettingMapper) {
    private val logger = LoggerFactory.getLogger(javaClass)

//    @CacheEvict("settings", key = "#setting.id")
    @CacheEvict("settings", allEntries = true)
    fun update(setting: Setting) {
        settingM.updateById(setting)
        logger.info("更新配置项 ${setting.id} = ${setting.content}")
    }
}

@RestController
@RequestMapping("system/setting")
class SettingCtrl(
    private val publisher: ApplicationEventPublisher,
//    private val systemAsync: SystemAsync,
    private val loader: SettingLoader,
    private val settingM:SettingMapper) : BaseController() {

    @RequestMapping("list", name = "配置列表")
    fun list() = resultWithData {
        settingM.selectList(QueryWrapper())
    }

    @RequestMapping("modify", name = "修改配置")
    fun modify(@RequestBody setting: Setting) = result {
        loader.update(setting)

//        systemAsync.onSettingChange(setting)
        publisher.publishEvent(SettingChangeEvent(setting))
    }
}