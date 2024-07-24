package org.appmeta.model

import org.appmeta.domain.App
import org.appmeta.domain.AppProperty
import org.appmeta.domain.AppWithUser
import org.appmeta.domain.WithPage
import org.nerve.boot.Pagination
import org.springframework.util.ObjectUtils.nullSafeHashCode
import java.io.Serializable

open class IdModel {
    var id      = 0L

    operator fun component1() = id
}
open class IdStringModel {
    var id      = ""

    operator fun component1() = id
}

open class KeyModel {
    var key     = ""
}

class SizeModel {
    var size    = 20
}

class FieldModel {
    var id:Serializable = ""
    var key             = ""
    var value:Any       = ""
}

class TextModel {
    var text    = ""

    constructor()
    constructor(txt:String) {
        text = txt
    }
}

class SwitchModel:KeyModel() {
    var enable:Boolean = false
}

class AppModel {
    var app     = App()
    var property= AppProperty()

    operator fun component1() = app
    operator fun component2() = property
}

class QueryModel {
    var pagination  = Pagination()
    var form        = mutableMapOf<String, Any>()
    var fields      = listOf<String>()
    var countOnly   = false

    override fun hashCode() = nullSafeHashCode(form) + nullSafeHashCode(fields) + pagination.page * pagination.pageSize
}

class LoginModel {
    var uid     = ""
    var pwd     = ""
}

class PageModel:WithPage {
    var id:Serializable?= null
    var channel = ""

    override var aid    = ""
    override var pid    = ""

    constructor()
    constructor(_aid:String, _pid:Serializable){
        aid = _aid
        pid = "$_pid"
    }
    constructor(_aid: String, _pid: Serializable, c:String):this(_aid, _pid) {
        channel = c
    }

    override fun toString() = "$aid-$pid"
}

class TerminalLogModel:AppWithUser() {
    var path    = ""
    var channel = ""
    var ip      = ""
}

/**
 * 机器人远程执行
 */
class RemoteRobotModel {
    var uid     = ""
    var robotId = ""
    var worker  = ""
    var params  = mutableMapOf<String, Any>()
}