package org.appmeta.module.faas

import org.apache.commons.lang3.exception.ExceptionUtils
import org.appmeta.F
import org.appmeta.domain.Account
import org.appmeta.domain.Department
import org.appmeta.domain.NameBean
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.domain.AuthUser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Exception


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Api
 * CREATE   2023年12月25日 17:16 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class FuncParmeter {
    companion object {
        const val STRING = "string"
        const val NUMBER = "number"
        const val BOOLEAN= "boolean"
    }

    var id              = ""
    var name            = ""
    var value: String?  = null
    var required        = false
    var regex           = ""
    var type            = STRING

    constructor()
    constructor(id: String, name: String, required: Boolean = false, regex: String = "") {
        this.id = id
        this.name = name
        this.required = required
        this.regex = regex
    }
}

class Func {
    companion object {
        const val SQL               = "sql"
        const val JS                = "js"

        const val RESULT_ARRAY      = "Array"
        const val RESULT_OBJECT     = "Object"
    }

    var mode                        = SQL
    var summary                     = ""
    var params:List<FuncParmeter>   = listOf()          //入参配置
    var paramsLimit                 = false
    var sourceId:Long?              = null              //数据源ID
    var cmd                         = ""                //代码或者脚本
    var resultType                  = RESULT_OBJECT     //结果格式（针对 mode=sql）
}

class FuncContext(
    val appId:String,                                   //应用ID
    val params:MutableMap<String, Any>,                 //入参
    val user:UserContext,                               //用户信息上下文
    val devMode:Boolean = false,                        //是否为开发者模式
) {
    val logs = mutableListOf<String>()
    var result:Any?     = null

    /**
     * 增加日志行
     */
    fun appendLog(msg:String):Unit {
        logs.add(msg)
    }

    fun appendException(e:Throwable) {
        logs.add("-------------------------- EXCEPTION --------------------------")
        logs.add(ExceptionUtils.getMessage(e))
    }
}

/**
 * 登录用户上下文对象
 */
class UserContext : NameBean {
    var channel                     = ""
    var ip                          = ""
    var depart:Department?          = null
    var roles                       = listOf<String>()
    var appRoles                    = listOf<String>()
    var appAuths                    = listOf<String>()
    private var inited              = false

    constructor()
    constructor(id:String, name:String) {
        this.id = id
        this.name = name
    }
    constructor(account: Account):this(account.id, account.name)
    constructor(user:AuthUser): this(user.id, user.name) {
        this.roles = user.roles
        this.ip = user.ip?:EMPTY
    }

    companion object {
        fun empty() = UserContext()
    }

    fun showName() = "$name($id)"

    fun toMap() = mapOf(
        F.ID        to id,
        F.NAME      to name,
        F.DEPART    to if(depart == null) mapOf() else mapOf(
            F.ID    to depart!!.id,
            F.NAME  to depart!!.name
        ),
        "roles"     to roles,
        "appRoles"  to appRoles,
        "appAuths"  to appAuths
    )

    fun toAuthUser() = AuthUser(id, name, ip).also { it.roles = roles }

    fun checkInited() = inited
    fun setInited() {
        inited = true
    }
}