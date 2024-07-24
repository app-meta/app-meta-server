package org.appmeta.web

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.model.IdStringModel
import org.appmeta.web.page.PageCtrl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nerve.boot.web.auth.AuthHolder


/*
 * @project app-meta-server
 * @file    org.appmeta.web.PageCtrlTest
 * CREATE   2023年08月15日 11:08 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class PageCtrlTest : AppTest() {
    @Resource
    lateinit var ctrl: PageCtrl
    @Resource
    lateinit var authHolder: AuthHolder

    @BeforeEach
    fun initUser(){
        authHolder.set(getUser())
    }

    @Test
    fun listOfAuthableByApp(){
        json(ctrl.listOfAuthableByApp(IdStringModel().also { it.id = AID_DEMO }).data)
    }
}