package org.appmeta.domain

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.ResultType
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.mapping.ResultSetType
import org.apache.ibatis.session.ResultHandler
import org.nerve.boot.annotation.CN
import org.nerve.boot.domain.IDLong
import org.springframework.util.StringUtils
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Terminal
 * CREATE   2023年04月06日 15:31 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class Terminal {
    companion object {
        const val OUTSIDE = "outside"
        const val INSIDE = "inside"

        const val LANG_NODE = "node"
        const val LANG_JAVA = "java"
    }
    var pid     = ""

    var mode    = INSIDE
    var language= ""
    var port    = 0
    var url     = ""
    var args    = ""                //应用启动参数
    var publics = listOf<String>()  //公开访问的地址

    // 数据库相关
    var useDB   = false
    var useSource = false
    var dbSource = 0L
    var dbHost  = ""
    var dbPort  = 3306
    var dbName  = ""
    var dbUser  = ""
    var dbPwd   = ""

    var custom  = mapOf<String, Any>()
}

@CN("后端服务记录")
@TableName("terminal_log")
class TerminalLog:AppWithUser {
    var host    = ""
    var url     = ""
    var method  = ""

    var code    = 0         //响应 http code
    var summary = ""        //描述信息，通常是报错内容

    var addOn   = 0L
    var used    = 0L        //单位 ms
    var channel = ""

    constructor()
    constructor(aid:String, url:String) {
        this.aid    = aid
        this.url    = url
        addOn       = System.currentTimeMillis()
    }
    constructor(aid: String, host:String, url: String):this(aid, url) {
        this.host   = host
    }
}

@CN("后端服务请求详细")
class TerminalLogDetail: IDLong() {
    var reqHeader   = ""
    var reqBody     = ""
    var resHeader   = ""
    var resBody     = ""
}

@Mapper
interface TerminalLogMapper:BaseMapper<TerminalLog> {

    @Select("SELECT * FROM terminal_log WHERE aid=#{0}")
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 2000)
    @ResultType(TerminalLog::class)
    fun streamByAid(aid: String, handler:ResultHandler<TerminalLog>)

    /**
     * 返回指定时点后的数据
     */
    @Select("SELECT * FROM terminal_log WHERE addOn>=#{0}")
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 2000)
    @ResultType(TerminalLog::class)
    fun streamByTime(time:Long, handler:ResultHandler<TerminalLog>)
}

@Mapper
interface TerminalLogDetailMapper:BaseMapper<TerminalLogDetail>