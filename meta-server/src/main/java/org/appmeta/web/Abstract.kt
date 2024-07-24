package org.appmeta.web

import jakarta.annotation.Resource
import org.appmeta.component.AppConfig
import org.nerve.boot.web.auth.AuthConfig
import org.nerve.boot.web.auth.UserLoader
import org.nerve.boot.web.ctrl.BasicController


/*
 * @project app-meta-server
 * @file    org.appmeta.web.Abstract
 * CREATE   2023年07月21日 09:25 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

abstract class CommonCtrl : BasicController() {

    @Resource
    lateinit var appConfig: AppConfig

    /**
     * 获取渠道信息
     */
    protected fun getChannel() = getHeader(appConfig.headerChannel)
}

/**
 * 允许匿名访问的 Controller 基类
 */
abstract class AnonymousAbleCtrl:CommonCtrl() {
    @Resource
    lateinit var userLoader: UserLoader
    @Resource
    lateinit var authConfig: AuthConfig

    /**
     *
     */
    protected fun getUserOrNull() = userLoader.from(request.getHeader(authConfig.tokenName))
}
