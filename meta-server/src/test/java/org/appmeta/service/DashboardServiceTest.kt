package org.appmeta.service

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.junit.jupiter.api.Test


/*
 * @project app-meta-server
 * @file    org.appmeta.service.DashboardServiceTest
 * CREATE   2023年03月30日 12:37 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class DashboardServiceTest : AppTest() {
    @Resource
    lateinit var service: DashboardService

    @Test
    fun overview(){
        json(service.overview())
    }

    @Test
    fun appOverview(){
        json(service.ofApp(""))
    }
}