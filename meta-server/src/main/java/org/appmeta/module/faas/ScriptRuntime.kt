package org.appmeta.module.faas

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONReader
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner
import org.appmeta.Channels
import org.appmeta.domain.DataBlock
import org.appmeta.model.DataCreateModel
import org.appmeta.model.DataDeleteModel
import org.appmeta.model.DataReadModel
import org.appmeta.model.DataUpdateModel
import org.appmeta.module.dbm.DatabaseService
import org.appmeta.module.dbm.DbmModel
import org.appmeta.service.DataService
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.StringUtils


/*
 * @project app-meta-server
 * @file    org.appmeta.module.faas.ScriptRuntime
 * CREATE   2024年01月09日 15:34 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class ScriptEnv {
    val regexList = Regex("(^\\([0-9]+\\))\\[.*]$")

    val engine: Engine = Engine.newBuilder()
        .option("engine.WarnInterpreterOnly", "false")
        .build()

    /**
     * 转换为指定的数据对象，支持不加双引号
     */
    private fun <T> parse(v:Any, clazz: Class<T>) = JSON.parseObject(
        v.toString(),
        clazz,
        JSONReader.Feature.AllowUnQuotedFieldNames
    )

    /**
     * 自定义 Java 、JS 互通规则
     *
     * 参数定义规则：https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/HostAccess.Builder.html#targetTypeMapping-java.lang.Class-java.lang.Class-java.util.function.Predicate-java.util.function.Function-
     */
    val hostAccess = HostAccess.newBuilder()
        .allowPublicAccess(true)
        .allowAllImplementations(true)
        .allowAllClassImplementations(true)
        .allowArrayAccess(true)
        .allowListAccess(true)
        .allowBufferAccess(true)
        .allowIterableAccess(true)
        .allowIteratorAccess(true)
        .allowMapAccess(true)
        .allowAccessInheritance(true)
        .also {
            it.targetTypeMapping(Map::class.java, DataReadModel::class.java, { _ -> true}, { v: Map<*, *> -> parse(v, DataReadModel::class.java)} )
            it.targetTypeMapping(Map::class.java, DataCreateModel::class.java, { _ -> true}, { v: Map<*, *> -> parse(v, DataCreateModel::class.java)} )
            it.targetTypeMapping(Map::class.java, DataDeleteModel::class.java, { _ -> true}, { v: Map<*, *> -> parse(v, DataDeleteModel::class.java)} )
        }
        .build()



    /**
     * 目前会话级别的数据存储，平台重启后失效
     */
    val sessionData = mutableMapOf<String, MutableMap<String, Any?>>()
}

interface MetaRuntime {
    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    fun _log(msg:String, isDebug:Boolean=true){
        if(isDebug)
            if(logger.isDebugEnabled)   logger.debug("[META] $msg")
            else
                logger.info("[META] $msg")
    }

    fun sql(text: String):Any

    fun getBlock(uuid: String):String?
    fun setBlock(uuid: String, text: String)

    /**
     * 新增一条数据行，不指定 pid（如需指定，请使用 insertData(List, pid)
     */
    fun insertData(row: Map<String, Any>) = insertData(listOf(row), DataCreateModel())
    fun insertData(rows: List<Map<String, Any>>, model: DataCreateModel)
    fun updateData(dataId:Long, obj:Map<String, Any>, merge:Boolean)
    fun queryData(model: DataReadModel):Any?
    fun removeData(model: DataDeleteModel)

    fun getSession(uuid: String) = getSession(uuid, null)
    fun getSession(uuid: String, defaultVal:Any?): Any?
    fun setSession(uuid: String, obj:Any?)

    /**
     * 调用外部的 FaaS 函数
     */
    fun faas(id:Int, params:MutableMap<String, Any>):Any?
//    fun service(id: Int, params: MutableMap<String, Any>):Any?
}

/**
 * 测试模式下的 JS 环境
 */
class MetaRuntimeDevImpl(val context: FuncContext) : MetaRuntime {
    companion object {
        // 默认的返回值
        const val DEFAULT_RESULT = "_DEFAULT_RETURN_"
    }

    private fun logToContext(msg: String) = "[DEV-JS] $msg".also {
        context.appendLog(it)
        _log(it)
    }

    override fun sql(text: String): Any = logToContext("执行SQL > $text")

    override fun getBlock(uuid: String): String? {
        logToContext("<BLOCK> 获取数据块 #$uuid （AID=${context.appId}）")
        return uuid
    }

    override fun setBlock(uuid: String, text: String) {
        logToContext("<BLOCK> 更新数据块 #$uuid （AID=${context.appId}）为：$text")
    }

    override fun insertData(rows: List<Map<String, Any>>, model: DataCreateModel) {
        println("新增数据：${JSON.toJSONString(model)}")
        logToContext("<DATA> 新增数据行（PID=${model.pid}） $rows")
    }

    override fun updateData(dataId: Long, obj: Map<String, Any>, merge: Boolean) {
        logToContext("<DATA> 更新数据行 ID=$dataId(MERGE=$merge) > $obj")
    }

    override fun queryData(model: DataReadModel): Any? {
        logToContext("<DATA> 查询数据行 id=${model.id} match=${model.match}")
        return emptyList<Any>()
    }

    override fun removeData(model: DataDeleteModel) {
        logToContext("<DATA> 删除数据行 id=${model.id} match=${model.match}")
    }

    override fun getSession(uuid: String, defaultVal:Any?): Any? {
        logToContext("<SESSION> 获取会话值 #$uuid （默认值=${defaultVal}）")
        return defaultVal
    }

    override fun setSession(uuid: String, obj: Any?) {
        logToContext("<SESSION> 更新会话值 #$uuid 为：${JSON.toJSONString(obj)}")
    }

    override fun faas(id: Int, params: MutableMap<String, Any>): Any? {
        logToContext("<FAAS> 调用函数#$id 参数 $params")
        return params.getOrDefault(DEFAULT_RESULT, 0)
    }
}

class MetaRuntimeImpl(
    val context: FuncContext,
    val sourceId:Long?,
    val dbService: DatabaseService,
    val dataService: DataService,
    val sessionStore: MutableMap<String, Any?>,
    val faasRunner: FaasRunner
):MetaRuntime  {

    override fun sql(text:String):Any {
        if(sourceId == null)    throw Exception("未关联数据源，无法执行SQL")
        if(sourceId == 0L)      return SqlRunner.db().selectList(text)

        val model = DbmModel()
        model.sourceId = sourceId
        model.sql = text

        return dbService.runSQL(model)
    }

    override fun setBlock(uuid:String, text: String) {
        _log("设置(AID=${context.appId}) uuid=$uuid 的 Block...")
        dataService.setBlockTo(DataBlock(context.appId, uuid, text))
    }

    override fun getBlock(uuid: String): String? {
        _log("获取(AID=${context.appId}) uuid=${uuid} 的 Block...")
        return dataService.getBlockBy(DataBlock(context.appId, uuid))?.text
    }

    override fun insertData(rows: List<Map<String, Any>>, model: DataCreateModel) {
        if(StringUtils.hasText(model.batch) && !StringUtils.hasText(model.pid))
            throw Exception("按批次导入数据请指明 pid")

        model.channel = Channels.FAAS
        model.aid = context.appId
        model.uid = context.user.id

        _log("新增数据行 ${rows.size} 条 UID=${model.uid} BATCH=${model.batch}（ORIGIN=${model.origin}）")
        if(logger.isDebugEnabled){
            rows.forEachIndexed { index, d -> _log("数据${index+1} > $d") }
        }
        dataService.create(model)
    }

    override fun updateData(dataId: Long, obj: Map<String, Any>, merge: Boolean) {
        dataService.update(DataUpdateModel().also {
            it.aid = context.appId
            it.id = dataId
            it.merge = merge
            it.obj = obj
        })
        _log("更新数据行 id=$dataId merge=$merge > $obj")
    }

    override fun queryData(model: DataReadModel): Any? {
        model.aid = context.appId
        _log("查询数据行 id=${model.id} pid=${model.pid} match=${model.match}")
        return dataService.read(model)
    }

    override fun removeData(model: DataDeleteModel) {
        model.aid = context.appId
        dataService.delete(model)

        _log("删除数据行 id=${model.id} pid/pids=${model.pid}/${model.pids} match=${model.match}")
    }

    override fun getSession(uuid: String, defaultVal:Any?): Any? {
        _log("获取会话值 ID=$uuid （默认值=${defaultVal}）")
        return sessionStore.getOrDefault(uuid, defaultVal)
    }

    override fun setSession(uuid: String, obj:Any?) {
        _log("设置会话值 $uuid = $obj")
        sessionStore[uuid] = obj
    }

    override fun faas(id: Int, params: MutableMap<String, Any>): Any? {
        _log("调用 FaaS#$id，参数 $params")
        return faasRunner.execute(id, params, context.user)
    }
}