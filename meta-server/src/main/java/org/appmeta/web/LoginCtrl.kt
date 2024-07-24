package org.appmeta.web

import io.jsonwebtoken.lang.Assert
import jakarta.servlet.http.HttpServletResponse
import org.appmeta.Auth
import org.appmeta.S
import org.appmeta.component.AppConfig
import org.appmeta.model.LoginModel
import org.appmeta.model.TextModel
import org.appmeta.service.AccountPwdService
import org.appmeta.tool.JWTTool
import org.nerve.boot.exception.ServiceException
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.AESProvider
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.web.bind.annotation.*


/*
 * @project app-meta-server
 * @file    org.appmeta.web.LoginCtrl
 * CREATE   2023年01月09日 10:18 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
class LoginCtrl(
    private val jwtTool: JWTTool,
    private val config: AppConfig,
    private val settingS: SettingService, private val pwdService: AccountPwdService) : BasicController() {

    private fun _createToken(uid: String): String {
        val ip = requestIP
        return jwtTool.create(uid, ip)
    }

    @RequestMapping("login_with_pwd", name = "账密登录")
    fun loginWithPwd(@RequestBody model: LoginModel) = resultWithData {
        Assert.isTrue(settingS.value(S.AUTH_METHOD) == Auth.Methods.PWD, "账密认证方式未被允许")

        val available = pwdService.check(model.uid, model.pwd)
        logger.info("${model.uid} 尝试使用密码登录，通过=$available")

        if (!available) throw ServiceException("认证失败，请检查后重试")
        _createToken(model.uid)
    }

    @RequestMapping("login_with_pick", name = "抓取式登录")
    fun loginWithPick(@RequestBody model: TextModel) = resultWithData {
        Assert.isTrue(settingS.value(S.AUTH_METHOD) == Auth.Methods.PICK, "抓取式认证方式未被允许")


    }

    val INVALID_TOKEN = "TOKEN 无效，请重新操作"
    val tokenMap    = mutableMapOf<String, String>()
    val tickMap     = mutableMapOf<String, String>()

    @GetMapping("login_with_cas", name = "CAS 登录")
    fun loginWithCas(token: String, response:HttpServletResponse) {
        Assert.hasText(token, "登录 token 不能为空")
        logger.info("[CAS] 尝试通过 CAS 登录，token=${token}")

        val host = settingS.value(S.AUTH_CAS_HOST)
        Assert.hasText(host, "请先设置 AUTH_CAS_HOST 配置参数")

        tokenMap[token] = requestIP
        response.sendRedirect("$host?${settingS.value(S.AUTH_CAS_PARAM)}=${token}")
    }

    @PostMapping("login_with_cas", name = "CAS 登录")
    fun loginWidthCasCheck(token: String) = resultWithData {
        Assert.hasText(token, "登录 token 不能为空")
        logger.info("[CAS] 尝试通过 CAS 登录认证，token=${token}")

        Assert.isTrue(
            tickMap.contains(token) && tokenMap.contains(token) && tokenMap[token] == requestIP,
            INVALID_TOKEN
        )

        val uid = tickMap[token]!!
        tickMap.remove(token)
        tokenMap.remove(token)

        _createToken(uid)
    }

    @GetMapping("login_with_cas_bck", name = "CAS 登录回调")
    fun casCallBack(ticket:String, token:String, response: HttpServletResponse) {
        logger.info("[CAS] 登录回调函数 token=$token ticket=$ticket")
        Assert.isTrue(tokenMap.contains(token), INVALID_TOKEN)

        val provider = AESProvider(settingS.value(S.AUTH_CAS_SECRET))
        val idInfo = provider.decrypt(ticket)
        Assert.hasText(idInfo, "无效的 TICKET")

        val tmp = idInfo.split("|")
        if(tmp.size > 1 && config.authCheckIP)
            Assert.isTrue(tokenMap[token] == tmp[1], "客户端IP前后不一致，请重新操作")

        tickMap[token] = tmp[0]
        response.sendRedirect(settingS.value(S.AUTH_CAS_URL))
    }
}