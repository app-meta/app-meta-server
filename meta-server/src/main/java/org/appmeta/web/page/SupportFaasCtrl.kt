package org.appmeta.web.page

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.apache.commons.lang3.exception.ExceptionUtils
import org.appmeta.F.ID
import org.appmeta.F.TEMPLATE
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.domain.Page
import org.appmeta.domain.Page.Companion.FAAS
import org.appmeta.model.IdModel
import org.appmeta.module.faas.*
import org.appmeta.service.AccountHelper
import org.appmeta.service.AccountService
import org.appmeta.service.LogAsync
import org.appmeta.service.PageService
import org.appmeta.tool.AuthHelper
import org.appmeta.web.AnonymousAbleCtrl
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Const.NEW_LINE
import org.nerve.boot.util.Timing
import org.springframework.http.HttpHeaders
import org.springframework.util.StreamUtils
import org.springframework.util.StringUtils.hasText
import org.springframework.web.bind.annotation.*

/*
 * @project app-meta-server
 * @file    org.appmeta.web.page.SupportFaasCtrl
 * CREATE   2023年12月27日 16:57 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
class SupportFaasCtrl(
    private val pageS: PageService,
    private val authHelper: AuthHelper,
    private val accountHelper: AccountHelper,
    private val accountService: AccountService,
    private val runner: FaasRunnerImpl,
    private val faasHelper: FaasHelper,
    private val logAsync: LogAsync
) : AnonymousAbleCtrl() {

    private fun _error(msg:String, page:Page):Void = throw Exception("$msg，请联系管理者<${page.uid}>")

    /**
     * 方便前端通过 H.service 调用
     */
    @PostMapping("service/_f_a_a_s_/{id}")
    fun faasWithService(@PathVariable id:Long) = faas(id)

    @RequestMapping("faas/{id}")
    fun faas(@PathVariable id:Long) = resultWithData {
        val contentType = request.getHeader(HttpHeaders.CONTENT_TYPE)?: EMPTY

        if(logger.isDebugEnabled){
            logger.debug("请求调用 FaaS 函数#${id} CONTENT_TYPE=${contentType}")
            val params = request.parameterNames
            while (params.hasMoreElements()) {
                val key = params.nextElement()
                logger.debug("[PARAMS] {} = {}", key, request.getParameterValues(key))
            }
        }

        val channel = getChannel()

        runner.execute(
            id,
            //获取原始请求参数，兼容 json 及传统表单
            if(contentType.lowercase().startsWith("application/json")){
                val jsonBody = String(StreamUtils.copyToByteArray(request.inputStream), Charsets.UTF_8)
                if(hasText(jsonBody))
                    JSON.parseObject(jsonBody)
                else
                    mutableMapOf()
            }
            else{
                reqQueryParams
            },
            getUserOrNull()
                .let { u->
                    if(u == null) UserContext.empty() else UserContext(u)
                }
                .also {
                    uc-> uc.channel = channel
                }
        ) { log ->
            log.channel = channel
            log.method = request.method
        }
    }

    /**
     * 调用日志记录到 TerminalLog
     * host 为功能 ID
     * code 恒定为 0
     * url  为参数
     * summary 为函数输出的日志+报错信息
     */
//    @RequestMapping("faas/{id}")
//    fun faas(@PathVariable id:Long) = resultWithData {
//        val contentType = request.getHeader(HttpHeaders.CONTENT_TYPE)?: EMPTY
//
//        if(logger.isDebugEnabled){
//            logger.debug("请求调用 FaaS 函数#${id} CONTENT_TYPE=${contentType}")
//            val params = request.parameterNames
//            while (params.hasMoreElements()) {
//                val key = params.nextElement()
//                logger.debug("[PARAMS] {} = {}", key, request.getParameterValues(key))
//            }
//        }
//
//        val page = pageS.getOne(QueryWrapper<Page>().eq(ID, id).eq(TEMPLATE, FAAS))?: throw Exception("FaaS函数 #$id 不存在")
//        if(!page.active)    _error("功能未开放", page)
//
//        val user = getUserOrNull()
//        val canCall = if(page.serviceAuth == ALL){
//            logger.info("访问完全公开的 FaaS 函数#${id}")
//            true
//        }
//        else {
//            authHelper.checkService(page, user)
//        }
//
//        if(!canCall)        _error("您未授权访问该功能", page)
//
//        if(logger.isDebugEnabled)   logger.debug("FaaS 函数开始执行，用户=${user?.showName}")
//
//        //获取原始请求参数，兼容 json 及传统表单
//        val originParams = if(contentType.lowercase().startsWith("application/json")){
//            val jsonBody = String(StreamUtils.copyToByteArray(request.inputStream), Charsets.UTF_8)
//            if(hasText(jsonBody))
//                JSON.parseObject(jsonBody)
//            else
//                mutableMapOf()
//        }
//        else{
//            reqQueryParams
//        }
//
//        if(logger.isDebugEnabled)   logger.debug("原始入参：${originParams}")
//        val log = TerminalLog(page.aid, "$id", EMPTY)
//        val timing = Timing()
//
//        FuncContext(
//            page.aid,
//            originParams,
//            if(user == null) UserContext.empty() else buildUserContext(user, page.aid)
//        ).let { context->
//            context.appendLog("[参数] ${JSON.toJSONString(originParams)}")
//            service.execute( JSON.parseObject(pageS.buildContent(page, false), Func::class.java), context ).also { funResult->
//                if(logger.isDebugEnabled)   logger.debug("FaaS 函数执行完成: {}", funResult)
//
//                log.channel     = getChannel()
//                log.method      = request.method
//                log.uid         = if(user == null) EMPTY else user.id
//                log.used        = timing.toMillSecond()
//                log.summary     = context.logs.joinToString(NEW_LINE)
//                if(context.params.isNotEmpty())
//                    log.url     = JSON.toJSONString(context.params)
//
//                logAsync.save(log)
//            }
//        }
//    }

    class FaasDevModel: IdModel() {
        var func:Func?  = null
        var params      = mutableMapOf<String, Any>()
        var uid         = ""
    }

    @PostMapping("page/faas/dev")
    fun faasWithDev(@RequestBody model:FaasDevModel) = resultWithData {
        val page = pageS.getOne(QueryWrapper<Page>().eq(ID, model.id).eq(TEMPLATE, FAAS))?: throw Exception("FaaS函数不存在（请先创建再调试）")
        val user = authHolder.get()
        logger.info("${user.showName} 进行函数#${page.id}的测试，参数：${model.params}")

        //判断权限
        if(!H.hasAnyRole(user, Role.DEVELOPER, Role.ADMIN))
            throw Exception("该功能仅限 ${Role.ADMIN}、${Role.DEVELOPER} 角色")
        if(!authHelper.checkEdit(page, user)) throw Exception("未授权编辑功能#${page.id}")

        if(model.func == null)  throw Exception("FaaS对象不能为空")

        val timing = Timing()
        model.func!!.let { func ->
            val context = FuncContext(
                page.aid,
                model.params,
                faasHelper.buildUserContext(if(hasText(model.uid)) accountService.toAuthUser(model.uid) else user , page.aid),
                true
            )
            context.appendLog("开始进行函数#${model.id}的模拟运行：\n\t[参数] ${model.params}\n\t[用户] ${context.user.id}")
            context.appendLog(NEW_LINE)

            try {
                runner.execute(func, context).also { funResult->
                    if(logger.isDebugEnabled)   logger.debug("FaaS 函数模拟完成: {}", funResult)

                    context.result = funResult
                }
            }catch (e:Exception){
                logger.error("模拟运行FaaS#${model.id}出错：${ExceptionUtils.getMessage(e)}")
                context.appendException(e)
            }finally {
                context.appendLog("\n函数执行完毕，耗时 ${timing.toSecondStr()} 秒")
            }

            context
        }
    }
}