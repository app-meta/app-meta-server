package org.appmeta.module.worker

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.type.JdbcType
import org.appmeta.F
import org.appmeta.domain.Fastjson2TypeHandler
import org.nerve.boot.annotation.CN
import org.nerve.boot.db.StringEntity
import org.nerve.boot.enums.Status
import java.util.*


/*
 * @project app-meta-server
 * @file    org.appmeta.module.worker.Domain
 * CREATE   2023年10月16日 17:49 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

object WorkerMethods {
    const val STATUS  = "status"
    const val START   = "start"
    const val STOP    = "stop"
}

/**
 * 任务对象
 */
@CN("工作者任务")
@TableName(autoResultMap = true)
class WorkerTask:StringEntity {
    var uid         = ""
    var worker      = ""
    var method      = ""
    @TableField(typeHandler = Fastjson2TypeHandler::class, jdbcType = JdbcType.BLOB)
    var params      = mutableMapOf<String, Any>()
    var addOn       = 0L

    var status      = Status.PENDING
    var response    = ""
    var doneOn      = 0L

    constructor()
    constructor(uid:String, method: String):this(uid, method, mapOf())
    constructor(uid:String, method:String, params:Map<String, Any>){
        this.uid    = uid
        this.method = method
        this.params = params.toMutableMap()
        this.addOn  = System.currentTimeMillis()
    }

    fun toTaskBean() = mapOf(
        F.ID        to id,
        F.TIME      to addOn,
        F.METHOD    to method,
        F.PARAMS    to params
    )

    fun init(workerId:String) {
        setId(UUID.randomUUID().toString())
        worker = workerId
    }

    fun done(obj:Any) {
        this.doneOn     = System.currentTimeMillis()
        this.status     = Status.SUCCESS
        this.response   = if(obj is String) obj else JSON.toJSONString(obj)
    }

    fun fail(msg:String) {
        this.status     = Status.FAIL
        this.doneOn     = System.currentTimeMillis()
        this.response   = msg
    }
}

@Mapper
interface WorkerTaskMapper:BaseMapper<WorkerTask>