package org.appmeta.model

import org.appmeta.domain.*


/*
 * @project app-meta-server
 * @file    org.appmeta.model.Responses
 * CREATE   2023年03月10日 12:16 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class PageResultModel(val app: App?, val page:Page?)

class UserResultModel(val id:String, val name:String?, val ip:String?, val depart: Department?, val roles:List<String>?) {
    var appRoles:List<String>? = null
}

class WelcomeResultModel(val settings:Map<String, Any>, val user:Any?)

class OverviewResultModel(
    val total:List<Any>,
    val templates:Map<Any, Any?>,
    val tops:Map<String, Int>,
    val platform:Map<String, Any>
)

class TerminalLogOverview(
    val aid:String,
    val total:Long,     //历史总数
    val today:Long,     //今日总数
    val used:Long,      //总耗时，单位 ms
    val error:Long      //错误数
)

class TerminalDetailResult(val log:TerminalLog?, val detail: TerminalLogDetail?)