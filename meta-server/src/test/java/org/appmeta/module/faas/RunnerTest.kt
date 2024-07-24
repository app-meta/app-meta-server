package org.appmeta.module.faas

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.junit.jupiter.api.Test


/*
 * @project app-meta-server
 * @file    org.appmeta.module.faas.RunnerTest
 * CREATE   2024年01月09日 15:00 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class RunnerTest : AppTest() {

    @Resource
    lateinit var runner: FaasRunner

    @Test
    fun faas(){
        println(
            runner.execute(
                38,
                mutableMapOf("aid"  to AID_DEMO),
                UserContext(getUser())
            )
        )
    }
}