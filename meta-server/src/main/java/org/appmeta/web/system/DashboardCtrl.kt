package org.appmeta.web.system

import org.appmeta.service.DashboardService
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.web.system.DashboardCtrl
 * CREATE   2023年03月30日 11:45 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("system/dashboard")
class DashboardCtrl(private val service: DashboardService):BasicController() {

    @PostMapping("overview", name = "数据总览")
    fun overview() = resultWithData { service.overview() }
}