package org.appmeta.web

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner
import jakarta.servlet.http.HttpServletResponse
import org.appmeta.F
import org.appmeta.S
import org.appmeta.domain.AppMapper
import org.appmeta.model.TextModel
import org.appmeta.model.WelcomeResultModel
import org.appmeta.service.AccountHelper
import org.appmeta.tool.AuthHelper
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.DateUtil
import org.nerve.boot.util.Timing
import org.nerve.boot.web.auth.AuthConfig
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.management.ManagementFactory
import java.util.*


/*
 * @project app-meta-server
 * @file    org.appmeta.web.IndexCtrl
 * CREATE   2022年12月06日 13:06 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
class IndexCtrl(
    private val appM:AppMapper,
    private val authHelper: AuthHelper,
    private val accountHelper: AccountHelper,
    private val authConfig:AuthConfig,
    private val settingS:SettingService):BasicController(){

    @RequestMapping("time", name = "服务器时间")
    fun time() = resultWithData {
        val started = DateUtil.formatDate(Date(ManagementFactory.getRuntimeMXBean().startTime), DateUtil.DATE_TIME)
        "${DateUtil.getDateTime()} (RUNED ON $started)"
    }

    // authHolder 未被注入，故直接通过 JWT token 获取用户信息
    private fun _buildUserBean() =  accountHelper.buildUserBean(request.getHeader(authConfig.tokenName))

    @PostMapping("whoami", name = "当前登录信息")
    fun whoami() = resultWithData { _buildUserBean() }

    @PostMapping("welcome", name = "公共配置")
    fun welcome() = resultWithData {
        WelcomeResultModel(
            settingS.loadByCategory(S.COMMON.name)
                .associate {
                    Pair(it.id.lowercase().replaceFirst("com_", EMPTY), it.content)
                },
            _buildUserBean()
        )
    }

    @PostMapping("query", name = "内容检索")
    fun query(@RequestBody model:TextModel) = resultWithData {
        val user = authHolder.get()
        val timing = Timing()
        var totalCount: Int

        SqlRunner.db()
            .selectList(
                """
                    select uid, id as aid,null as pid, name,launch, null as ${F.SERVICE_AUTH}  from app where ${F.NAME} like {0}
                    union
                    select uid, aid, id as pid, name,launch,${F.SERVICE_AUTH}  from page where ${F.SEARCH}=1 and ${F.ACTIVE}=1 and ${F.NAME} like {0};
                """.trimIndent(),
                "%${model.text}%"
            )
            .also { totalCount = it.size }
            //进行权限过滤
            .filter {
                /*
                下面两段代码均可实现判断权限，使用 let 存在 5% 左右的耗时增加 🙂
                 */
//                it[F.SERVICE_AUTH].let { auth->
//                    auth == null || authHelper.checkService(auth as String, it[F.UID] as String, user)
//                }

                val auth = it[F.SERVICE_AUTH]?: return@filter true
                authHelper.checkService(auth as String, it[F.UID] as String, user)
            }
            .also {
                logger.info("${user?.showName}查询关键字 [${model.text}] 共 ${it.size}/${totalCount} 个结果，耗时 ${timing.toSecondStr()} 秒")
            }
    }
}