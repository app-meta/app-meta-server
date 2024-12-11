package org.appmeta.domain

import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableLogic
import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler
import jakarta.validation.constraints.NotBlank
import org.apache.ibatis.annotations.*
import org.apache.ibatis.mapping.ResultSetType
import org.apache.ibatis.session.ResultHandler
import org.apache.ibatis.type.JdbcType
import org.nerve.boot.annotation.CN

/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Data
 * CREATE   2022年12月16日 10:26 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@CN("应用数据")
@TableName(autoResultMap = true)
open class Data : AppWithUser, LogicRemove {
    var pid             = ""
    var channel         = ""

    @TableField(typeHandler = Fastjson2TypeHandler::class, jdbcType = JdbcType.BLOB)
    var v               = mapOf<String, Any>()

    @TableLogic
    override var hide   = false
    var hideOn:Int?     = null

    var addOn           = 0L

    constructor()
    constructor(aid:String, uid:String) {
        this.aid    = aid
        this.uid    = uid
        addOn       = System.currentTimeMillis()
    }
    constructor(aid: String, uid: String, channel:String):this(aid, uid) {
        this.channel= channel
    }
}

@CN("应用数据块")
@TableName("data_block")
class DataBlock : WithApp {
    @NotBlank(message = "UUID不能为空")
    var uuid    = ""
    var text    = ""

    constructor()
    constructor(aid:String, uuid:String) {
        this.aid    = aid
        this.uuid   = uuid
    }
    constructor(aid: String, uuid: String, text:String):this(aid, uuid) {
        this.text   = text
    }

    fun info() = "AID=$aid UUID=$uuid"
}

@CN("应用数据批次")
@TableName("data_batch")
class DataBatch : AppWithUser {
    var pid     = ""
    var batch   = ""
    var size    = 0
    var origin  = ""
    var active  = true
    var addOn   = 0L

    constructor()
    constructor(model:PageWithUser) {
        aid     = model.aid
        pid     = model.pid
        uid     = model.uid
        addOn   = System.currentTimeMillis()
    }
}

@CN("机器人执行记录")
@TableName("data_robot")
class RobotLog() : AppWithUser() {
    var pid     = ""
    var startOn = 0L
    var used    = 0L
    var ip      = ""
    var os      = ""        // 操作系统信息
    var chrome  = ""        // chrome 版本

    var params  = ""
    var origin  = ""
    var logs    = ""

    /**
     * add on 2023-10-25
     * 关联对象，通常是工作者任务
     * 此属性有值时，会自动更新关联工作者任务的状态
     */
    var link:String? = null

    @TableField(exist = false)
    var caches  = mapOf<String, Any?>()

    var addOn   = 0L
}

@Mapper
interface DataMapper:BaseMapper<Data> {

    /**
     * ew 参数不可修改
     *
     * 通过 @Select 得到的对象 TypeHandler 不生效（原因未知）
     * 故这里通过 Map 进行转换
     */
    @Select("SELECT * FROM data \${ew.customSqlSegment}")
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 2000)
    @ResultType(Map::class)
    fun loadWithStream(@Param("ew") queryWrapper: QueryWrapper<Data>, handler: ResultHandler<Map<String, Any?>>)

    @Select("SELECT * FROM data WHERE id=#{0}")
    fun loadById(id:Long):Data
}
@Mapper
interface DataBlockMapper:BaseMapper<DataBlock>
@Mapper
interface DataBatchMapper:BaseMapper<DataBatch>
@Mapper
interface RobotLogMapper: BaseMapper<RobotLog>