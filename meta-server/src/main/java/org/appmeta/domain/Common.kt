package org.appmeta.domain

import jakarta.validation.constraints.NotBlank
import org.nerve.boot.db.StringEntity
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.domain.IDLong

open class NameBean : StringEntity() {
    @NotBlank(message = "名称不能为空")
    var name = ""
}

open class SummaryBean : NameBean() {
    var summary = ""
}

open class UserBean : SummaryBean() {
    var uid = ""
    var uname = ""

    fun of(user: AuthUser?) {
        if (user != null) {
            uid = user.id
            uname = user.name
        }
    }
}

open class LongSummaryBean : IDLong() {
    @NotBlank(message = "名称不能为空")
    var name = ""
    var summary = ""
}

open class LongUserBean : LongSummaryBean() {
    var uid = ""
    var uname = ""

    fun of(user: AuthUser?) {
        if (user != null) {
            uid = user.id
            uname = user.name
        }
    }
}

open class WithApp : IDLong() {
    @NotBlank(message = "关联应用ID不能为空")
    var aid 	= ""
}

open class AppWithUser : WithApp() {
    var uid 	= ""

    fun of(user: AuthUser?) {
        if(user != null)
            uid = user.id
    }
}

interface WithPage {
    var aid:String
    var pid:String

    fun of(page:Page) {
        aid = page.aid
        pid = "${page.id}"
    }

    fun of(bean:WithPage){
        aid = bean.aid
        pid = bean.pid
    }

    fun toText() = "应用#$aid（页面 #$pid）"
}

open class PageWithUser: WithPage {
    override var aid    = ""
    override var pid    = ""
    var uid             = ""
}

interface ServiceAuthable {
    var serviceAuth:String      //使用授权
    var uid:String
}

interface EditAuthable {
    var editAuth:String         //编辑授权
    var uid:String
}

interface Authable: ServiceAuthable, EditAuthable {
    override var uid:String
}

interface Launchable {
    var launch:Int              //运行次数统计
}

interface LogicRemove {
    var hide:Boolean
}
