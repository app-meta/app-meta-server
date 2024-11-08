package org.appmeta.web

import com.alibaba.fastjson2.JSON
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.lang3.exception.ExceptionUtils
import org.appmeta.F
import org.appmeta.H
import org.appmeta.S
import org.appmeta.S.SYS_TERMINAL_HEADER_VALUE
import org.appmeta.component.AppConfig
import org.appmeta.component.ServiceRoute
import org.appmeta.component.SettingChangeEvent
import org.appmeta.domain.Terminal
import org.appmeta.domain.TerminalLog
import org.appmeta.domain.TerminalLogDetail
import org.appmeta.domain.TerminalLogDetailMapper
import org.appmeta.service.AppRoleService
import org.appmeta.service.LogAsync
import org.appmeta.service.TerminalService
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Result
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.web.filter.UserFilter
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.AntPathMatcher
import org.springframework.util.StreamUtils
import org.springframework.util.StringUtils.hasText
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.util.*


/*
 * @project app-meta-server
 * @file    org.appmeta.web.ProxyCtrl
 * CREATE   2023年03月22日 09:09 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 请求转发，文章参考
 *
 * spring boot通用转发请求                     https://blog.csdn.net/liufang1991/article/details/129559677
 * spring boot实现超轻量级网关（反向代理及转发）   https://blog.csdn.net/m0_37657556/article/details/121159548
 */


@Service
class ProxyService(
    private val appRoleService: AppRoleService,
    private val settingS:SettingService, private val config: AppConfig){
    val logger = LoggerFactory.getLogger(javaClass)

    lateinit var template:Template

    @PostConstruct
    private fun init(){
        var value = settingS.value(SYS_TERMINAL_HEADER_VALUE)
        if(!hasText(value))
            value = "{{ ${F.ID} }}-{{ ${F.NAME} }}-{{ ${F.IP} }}"
        template = Handlebars().compileInline(value)
    }

    @EventListener(SettingChangeEvent::class, condition = "#e.setting.id=='SYS_TERMINAL_HEADER_VALUE'")
    fun onSettingChange(e: SettingChangeEvent) {
        val setting = e.setting
        if(logger.isDebugEnabled)   logger.debug("检测到 $SYS_TERMINAL_HEADER_VALUE 变动：${setting.content}")

        template = Handlebars().compileInline(setting.content)
        logger.info("[TEMPLATE] $SYS_TERMINAL_HEADER_VALUE 模版更新为 ${template.text()}")
    }

    fun buildHeader(aid: String, user:AuthUser) = mapOf(
        "from"                                      to config.name,         //平台名称
        "origin_ip"                                 to user.ip,             //客户端源IP
        settingS.value(S.SYS_TERMINAL_HEADER_NAME)  to template.apply(
            mapOf(
                F.ID    to user.id,
                F.NAME  to URLEncoder.encode(user.name, Charsets.UTF_8.name()),
                F.IP    to user.ip,
                F.TIME  to System.currentTimeMillis(),
                F.ROLE  to appRoleService.loadRoleOfUser(aid, user.id)
            )
        )
    )

    fun checkAuth(aid: String, user:AuthUser, url:String) {
        if(!appRoleService.checkAuth(aid, user, url))
            throw Exception("用户 ${user.id} 未授权访问应用${H.wrapText(aid)}的资源 $url")
    }
}

@RestController
class ProxyCtrl(
    private val logDetailM:TerminalLogDetailMapper,
    private val settingS: SettingService,
    private val service: ProxyService,
    private val route: ServiceRoute,
    private val logAsync: LogAsync,
    private val terminalS:TerminalService) : AnonymousAbleCtrl(){

    private val matcher = AntPathMatcher()

    @RequestMapping("service/{aid}/**", name = "应用后台服务")
    fun redirect(@PathVariable aid:String, response:HttpServletResponse):ResponseEntity<*> {
        val path = request.servletPath.replace("/service/${aid}", "")

        val terminal = terminalS.load(aid)

        val isPublic = terminal.publics.any { matcher.match(it, path) }

        var user = getUserOrNull()
        if(user == null){
            if(isPublic){
                if(logger.isDebugEnabled)   logger.debug("应用[$aid] $path 允许匿名访问...")
                user = AuthUser(EMPTY, "匿名", requestIP)
            }
            else {
                logger.info("拦截来自 $requestIP 的匿名访问：应用#$aid $path")
                return ResponseEntity.ok(Result.fail(UserFilter.LOGIN_REQUIRED))
            }
        }
        //判断是否具备访问权限
        if(!isPublic)
            service.checkAuth(aid, user, path)

        val host = if(terminal.mode == Terminal.OUTSIDE) terminal.url else "${settingS.value(S.SYS_TERMINAL_HOST)}:${terminal.port}"

        val url = "${host}${path}${if(hasText(request.queryString)) "?${request.queryString}" else ""}"

        if(logger.isDebugEnabled)    logger.debug("转发请求（APP=$aid） 到 $url")

        val log = TerminalLog(aid, host, path)
        log.method  = request.method
        log.uid = user.id
        log.channel = getChannel()

        var saveDetail = settingS.booleanValue(S.TERMINAL_DETAIL, false)
        //判断渠道
        if(saveDetail){
            settingS.valueOfList(S.TERMINAL_CHANNEL).also {
                if(it!=null && !it.contains(log.channel))
                    saveDetail = false
            }
        }

        val logDetail = if(!saveDetail) null else TerminalLogDetail()

        return try{
            val result = route.redirectDo( request, response, url, service.buildHeader(aid, user) )
            val resEntity = result.second
            log.code = resEntity.statusCode.value()

            if(logDetail != null){
                //记录请求信息
                logDetail.reqHeader = settingS.valueOfList(S.TERMINAL_HEADER)
                    .let { validNames->
                        Collections.list(request.headerNames)
                            .filter { n-> validNames.contains(n) }
                            .associateWith { n -> request.getHeader(n) }
                    }
                    .let { JSON.toJSONString(it) }

                logDetail.reqBody = result.first

                //记录响应值
                val headers = mutableMapOf<String, Any?>()
                resEntity.headers.mapKeys { h-> headers[h.key.lowercase()] = h.value.first() }
                logDetail.resHeader = JSON.toJSONString(headers)

                if(resEntity.hasBody() && resEntity.body!!.size <= settingS.intValue(S.TERMINAL_MAX, 10) * 1024L)
                    logDetail.resBody = Base64.getEncoder().encodeToString(resEntity.body)
            }

            resEntity
        }
        catch (e:Exception) {
            logger.error("[SERVICE-ROUTE] 转发失败", e)
            log.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            log.summary = ExceptionUtils.getMessage(e)

            ResponseEntity(Result.fail(e), HttpStatus.INTERNAL_SERVER_ERROR)
        }
        finally {
            logAsync.save(log, if(saveDetail) logDetail else null)
        }
    }
}