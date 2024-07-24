package org.appmeta.service

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.S
import org.appmeta.component.SettingChangeEvent
import org.appmeta.web.ProxyService
import org.junit.jupiter.api.Test
import org.nerve.boot.module.setting.Setting
import org.springframework.context.ApplicationEventPublisher


/*
 * @project app-meta-server
 * @file    org.appmeta.service.TerminalService
 * CREATE   2023年04月06日 17:31 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class TerminalServiceTest : AppTest() {
    @Resource
    lateinit var service: TerminalService

    @Resource
    lateinit var proxyService:ProxyService
    @Resource
    lateinit var publisher: ApplicationEventPublisher

    @Test
    fun logOverview(){
        json(service.logOverview("SJCQ-BDB"))
    }

    @Test
    fun changeSetting(){
        getUser().let {user->
            println(proxyService.buildHeader("", user))

            publisher.publishEvent(
                SettingChangeEvent(Setting().also {
                    it.id = S.SYS_TERMINAL_HEADER_VALUE.name
                    it.content = "{{id}}@{{name}}@{{ip}}@{{time}}"
                })
            )

            Thread.sleep(4000)
            println(proxyService.buildHeader("", user))
        }
    }
}