package org.appmeta.domain

import com.baomidou.mybatisplus.annotation.FieldStrategy
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update
import org.appmeta.model.PageModel
import org.nerve.boot.annotation.CN
import java.io.Serializable


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Page
 * CREATE   2023年02月17日 09:18 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@CN("应用页面")
@TableName("page")
class Page : AppWithUser, Authable, Launchable {
    companion object {
        const val H5        = "h5"
        const val MARKDOWN  = "markdown"
        const val ROBOT     = "robot"
        const val FAAS      = "faas"
        const val SERVER    = "server"
    }

    var name        = ""
    var template    = "form"
    var main        = false
    var active      = false
    var search      = true
    override var launch = 0
    var updateOn    = 0L
    var addOn       = 0L

    @TableField(select = false, updateStrategy = FieldStrategy.NEVER)
    var content     = ""    //页面内容

    constructor()
    constructor(aid: String, uid: String) {
        this.aid = aid
        this.uid = uid
        this.addOn = System.currentTimeMillis()
    }

    override var serviceAuth = ""
    override var editAuth = ""
}

@CN("应用页面关联")
@TableName("page_link")
class PageLink:AppWithUser {
    var pid     = ""
    var name    = ""
    var template= ""
    var active  = true
    var weight  = 0
    var addOn   = 0L

    constructor()
    constructor(id:Long) {
        setId(id)
    }
}

@CN("应用页面运行记录")
@TableName("page_launch")
class PageLaunch:AppWithUser {
    var pid     = ""
    var ip      = ""
    var channel = ""        //渠道/频道
    var depart  = ""        //部门信息
    var addOn   = 0L

    constructor()
    constructor(model:PageModel) {
        aid = model.aid
        pid = model.pid

        addOn = System.currentTimeMillis()
    }
}

@Mapper
interface PageMapper: BaseMapper<Page> {
    @Select("SELECT content FROM page WHERE id=#{0}")
    fun getContent(id:Long):String

    @Update("UPDATE page SET launch=launch+1 WHERE id=#{0}")
    fun onLaunch(id:Serializable)

    @Select("SELECT COUNT(*) FROM page WHERE aid=#{aid} AND template=#{template}")
    fun countByTemplate(@Param("aid") aid:String, @Param("template") template:String):Long
}

@Mapper
interface PageLinkMapper: BaseMapper<PageLink> {

    @Select("SELECT * FROM page_link WHERE uid=#{0} AND active=1 ORDER BY weight DESC,id DESC")
    fun byUser(uid:String):List<PageLink>

    @Select("SELECT * FROM page_link WHERE pid=#{pid} AND uid=#{uid} LIMIT 1")
    fun byPageAndUser(@Param("pid") pid:String, @Param("uid") uid:String):PageLink?
}

@Mapper
interface PageLaunchMapper:BaseMapper<PageLaunch>