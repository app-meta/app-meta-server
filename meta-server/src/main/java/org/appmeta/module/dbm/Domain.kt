package org.appmeta.module.dbm

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.appmeta.domain.LongSummaryBean
import org.nerve.boot.annotation.CN
import org.nerve.boot.domain.IDLong

@CN("数据源")
@TableName("dbm_source")
class DatabaseSource: LongSummaryBean() {
    companion object {
        const val MYSQL = "mysql"
    }

    var type        = MYSQL
    var host        = ""
    var port        = 0
    var username    = ""
    var pwd         = ""
    var db          = ""
    var encoding    = "utf-8"

    override fun toString() =
        "[${type.uppercase()}] $host:$port USER=$username ENCODING=$encoding ${if(db.isNotEmpty()) "DB=$db " else ""}SUMMARY=$summary"
}

@CN("数据源操作权限")
@TableName("dbm_auth")
class DatabaseAuth: LongSummaryBean() {
    var uid         = ""
    var sourceId    = -1L
    var allow       = ""
}

@CN("数据源操作日志")
@TableName("dbm_log")
class DatabaseLog: IDLong() {
    var uid         = ""
    var sourceId    = -1L
    var name        = ""

    var action      = ""
    var target:String?  = null
    var ps:String?  = null

    var used        = 0L //单位 ms
    var summary:String? = null
    var addOn       = 0L

    fun of(source: DatabaseSource){
        sourceId    = source.id
        name        = source.name
    }
}

@Mapper
interface DatabaseSourceMapper:BaseMapper<DatabaseSource>

@Mapper
interface DatabaseAuthMapper:BaseMapper<DatabaseAuth>
@Mapper
interface DatabaseLogMapper:BaseMapper<DatabaseLog>