package org.appmeta.web

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.model.TextModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nerve.boot.web.auth.AuthHolder


/*
 * @project app-meta-server
 * @file    org.appmeta.web.IndexCtrlTest
 * CREATE   2023年03月23日 09:02 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class IndexCtrlTest : AppTest() {
    @Resource
    lateinit var ctrl: IndexCtrl
    @Resource
    lateinit var authHolder: AuthHolder

    @BeforeEach
    fun initUser(){
        authHolder.set(getUser("00000"))
    }

    @Test
    fun query(){
        json(ctrl.query(TextModel("业务")))
    }
}