package org.appmeta.module.faas

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONReader
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner
import com.github.jknack.handlebars.Handlebars
import org.appmeta.ALL
import org.appmeta.F
import org.appmeta.domain.Authable
import org.appmeta.domain.Page
import org.appmeta.domain.TerminalLog
import org.appmeta.model.DataCreateModel
import org.appmeta.model.DataDeleteModel
import org.appmeta.model.DataReadModel
import org.appmeta.model.PageModel
import org.appmeta.module.dbm.DatabaseService
import org.appmeta.module.dbm.DatabaseSourceService
import org.appmeta.module.dbm.DbmModel
import org.appmeta.service.*
import org.appmeta.tool.AuthHelper
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.io.IOAccess
import org.nerve.boot.Const
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.util.DateUtil
import org.nerve.boot.util.Timing
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import java.io.OutputStream
import java.io.Serializable
import java.nio.charset.Charset


/*
 * @project app-meta-server
 * @file    org.appmeta.module.faas.Service
 * CREATE   2023年12月26日 15:45 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Component
class FaasHelper(private val appRoleS:AppRoleService) {
    private val TRUE_VALUES = listOf("true", "1")

    /**
     * 参数校验与修复
     */
    fun checkParams(func: Func, params: MutableMap<String, Any>){
        func.params.onEach { p ->
            // 赋予默认值
            if(StringUtils.hasText(p.value) && !params.containsKey(p.id))
                params[p.id] = p.value!!

            if(p.required && (!params.containsKey(p.id) || !StringUtils.hasText(params[p.id].toString())))
                throw Exception("参数 ${p.id}/${p.name} 为必填")

            //检验
            if(StringUtils.hasText(p.regex)){
                if(!Regex(p.regex).matches("${params[p.id]}"))    throw Exception("参数 ${p.id}/${p.name} 格式不合规")
            }

            //类型转换
            if(params[p.id] is String){
                val v = params[p.id] as String

                params[p.id] = when(p.type){
                    FuncParmeter.NUMBER     -> v.toLong()
                    FuncParmeter.BOOLEAN    -> TRUE_VALUES.contains(v.lowercase())
                    else                    -> v
                }
            }
        }

        if(func.paramsLimit){
            val ids = func.params.map { it.id }
            params.keys.filter { !ids.contains(it) }.forEach{ k-> params.remove(k) }
        }
    }


    fun buildUserContext(user: AuthUser, aid:String) = UserContext(user).also {
        it.ip = user.ip
        fillUpUserContext(it, aid)
    }

    /**
     * 填充用户信息
     */
    fun fillUpUserContext(userContext: UserContext, aid: String) {
        val cache = appRoleS.loadRoleAndAuthOfUser(aid, userContext.ip)
        userContext.appRoles = cache.first
        userContext.appAuths = cache.second

        userContext.setInited()
    }
}

interface FaasRunner {
    fun execute(pageId: Serializable, params: MutableMap<String, Any>, userContext: UserContext) =
        execute(pageId, params, userContext, null)

    fun execute(pageId:Serializable, params: MutableMap<String, Any>, userContext: UserContext, logTransfer: ((TerminalLog) -> Unit)?):Any?
}

/**
 * 为了更好地递归调用，将 Excecutor 合并到同一个实现类中
 */
@Service
class FaasRunnerImpl (
    private val dataS: DataService,
    private val logAsync: LogAsync,
    private val appAsync: AppAsync,
    private val authHelper: AuthHelper,
    private val pageS:PageService,
    private val dataSourceS: DatabaseSourceService,
    private val dbService: DatabaseService,
    private val helper: FaasHelper): FaasRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 在指定数据源执行 SQL 语句，暂不支持动态参数
     */
    private fun sqlExecute(func: Func, context: FuncContext):Any? {
        Assert.isTrue(func.sourceId != null, "函数未配置数据源")

        val sql = Handlebars().compileInline(func.cmd).apply(context)
        if(context.devMode){
            context.appendLog("[DEV-SQL] 执行语句 $sql")
            return DateUtil.getDateTime()
        }

        logger.info("执行sql：${sql}")

        return if(func.sourceId == 0L){
            //使用主数据源，只能执行查询
            val items = SqlRunner.db().selectList(sql)

            if(func.resultType == Func.RESULT_ARRAY)
                items.map { it.values }
            else
                items
        }
        else {
            // 指定了 DataBaseSource 时，调用相应的模块
            val source = dataSourceS.withCache(func.sourceId!!)?: throw Exception("数据源#${func.sourceId} 未定义")
            val dbmResult = dbService.runSQL(DbmModel().also {
                it.sourceId = source.id
                it.batch = false
                it.action = DbmModel.SQL
                it.sql = sql
            })

            dbmResult as List<*>
        }
    }

    private val env = ScriptEnv()

    private fun jsExecute(func: Func, context: FuncContext):Any? {
        func.sourceId?.also {
            if(it>0L)
                dataSourceS.withCache(it)?: throw Exception("数据源#${func.sourceId} 未定义")
        }

        if(!env.sessionData.contains(context.appId))
            env.sessionData[context.appId] = mutableMapOf()

        val out = object : OutputStream(){
            val bytes = mutableListOf<Byte>()
            private var cur = 0

            override fun write(b: Int) {
                bytes.add(b.toByte())

                if(b==10){
                    val line = String(bytes.subList(cur, bytes.size-1).toByteArray(), Charset.defaultCharset())
                    logger.info("[JS引擎] $line")
                    context.appendLog(line)

                    cur = bytes.size
                }
            }
        }

        val ctx = Context.newBuilder(Func.JS)
            .engine(env.engine)
            //设置为 HostAccess.ALL 后，可以在 js 中调用 java 方法（通过 Bindings 传递），但是不支持使用 Java.type 功能
            .allowHostAccess(env.hostAccess)
            //设置 JS 与 JAVA 的交互性（如 Java.type、Packages ）
            //.allowAllAccess(true)
            //不允许IO（如引入外部文件）
            .allowIO(IOAccess.NONE)
            .out(out)
            .build()


        val ctxBindings = ctx.getBindings(Func.JS)
        ctxBindings.putMember("params", context.params)
        ctxBindings.putMember("user", context.user.toMap())
        ctxBindings.putMember("appId", context.appId)
        ctxBindings.putMember(
            "meta",
            if(context.devMode)
                MetaRuntimeDevImpl(context)
            else
                MetaRuntimeImpl(
                    context,
                    func.sourceId,
                    dbService,
                    dataS,
                    env.sessionData[context.appId]!!,
                    this
                )
        )

        return ctx.eval(Func.JS, func.cmd).let {
            if (it.isNull) return null
            if (it.isException) return it.throwException()

            var body = it.toString()
            if (logger.isDebugEnabled) logger.debug("JS代码执行结果：$body")

            env.regexList.find(body)?.also { m ->
                if (logger.isDebugEnabled) logger.debug("结果为数组，即将替换开头的 ([0-9]+)")
                body = body.replaceFirst(m.groupValues.last(), "")
            }

//            if(it.isNull)           return null
//            if(it.isString)         return it.asString()
//            if(it.isHostObject)     return it.asHostObject()
//            if(it.isBoolean)        return it.asBoolean()
//            if(it.isDate)           return it.asDate()
//            if(it.fitsInInt())      return it.asInt()
//            if(it.fitsInLong())     return it.asLong()
//            if(it.fitsInShort())    return it.asShort()
//            if(it.fitsInDouble())   return it.asDouble()
//            if(it.fitsInByte())     return it.asByte()
//            if(it.isException)      return it.throwException()
//
//            return it.asString()

            //转换 JSON 格式
            JSON.parse(body, JSONReader.Feature.AllowUnQuotedFieldNames)

        }
    }

    private fun _error(msg:String, bean:Authable):Void = throw Exception("$msg，请联系管理者<${bean.uid}>")

    override fun execute(pageId:Serializable, params: MutableMap<String, Any>, userContext: UserContext, logTransfer: ((TerminalLog) -> Unit)?):Any? {
        val page = pageS.getOne(QueryWrapper<Page>().eq(F.ID, pageId).eq(F.TEMPLATE, Page.FAAS))?: throw Exception("FaaS函数 #$pageId 不存在")
        if(!page.active)    _error("功能未开放", page)

        val canCall = if(page.serviceAuth == ALL){
            logger.info("访问完全公开的 FaaS 函数#${pageId}")
            true
        }
        else {
            authHelper.checkService(page, userContext.toAuthUser())
        }

        if(!canCall)        _error("您未授权访问该功能", page)

        if(logger.isDebugEnabled){
            logger.debug("FaaS 函数开始执行，用户=${userContext.showName()}")
            logger.debug("原始入参：${params}")
        }

        val log = TerminalLog(page.aid, "$pageId", Const.EMPTY)
        val timing = Timing()

        if(!userContext.checkInited())  helper.fillUpUserContext(userContext, page.aid)

        return FuncContext(page.aid, params, userContext).let { context->
            context.appendLog("[参数] ${JSON.toJSONString(params)}")

            execute( JSON.parseObject(pageS.buildContent(page, false), Func::class.java), context ).also { funResult->
                if(logger.isDebugEnabled)   logger.debug("FaaS 函数执行完成: {}", funResult)

                log.uid         = userContext.id

                if(logTransfer != null) logTransfer(log)

                log.used        = timing.toMillSecond()
                log.summary     = context.logs.joinToString(Const.NEW_LINE)
                if(context.params.isNotEmpty())
                    log.url     = JSON.toJSONString(context.params)

                logAsync.save(log)

                // 执行次数增加
                appAsync.afterLaunch(
                    PageModel(page.aid, page.id, userContext.channel),
                    userContext.id,
                    userContext.ip
                )
            }
        }
    }

    fun execute(func: Func, context: FuncContext):Any? {
        helper.checkParams(func, context.params)
        if(logger.isDebugEnabled)   logger.debug("修正后参数：${context.params}")

        return when(func.mode){
            Func.SQL    -> sqlExecute(func, context)
            Func.JS     -> jsExecute(func, context)
            else        -> throw Exception("无效的类型<${func.mode}>")
        }
    }
}