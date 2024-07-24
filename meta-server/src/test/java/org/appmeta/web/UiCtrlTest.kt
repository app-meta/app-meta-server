package org.appmeta.web

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.web.system.UiCtrl
import org.junit.jupiter.api.Test


/*
 * @project app-meta-server
 * @file    org.appmeta.web.UiCtrlTest
 * CREATE   2023年02月09日 18:18 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class UiCtrlTest : AppTest() {
    @Resource
    lateinit var uiCtrl: UiCtrl

    @Test
    fun unzip(){
        println(uiCtrl.unzipDo())
    }
}