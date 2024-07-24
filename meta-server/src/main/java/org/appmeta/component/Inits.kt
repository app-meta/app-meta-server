package org.appmeta.component

import org.apache.commons.lang3.RandomStringUtils
import org.appmeta.domain.Account
import org.appmeta.domain.AccountMapper
import org.appmeta.service.AccountPwdService
import org.nerve.boot.web.InitWorker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING


/*
 * @project app-meta-server
 * @file    org.appmeta.component.Inits
 * CREATE   2023年06月12日 13:07 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Component
class UserIniter(
    private val config: SystemConfig,
    private val accountM:AccountMapper,
    private val pwdService: AccountPwdService):InitWorker {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun get() = if(accountM.selectCount(null) == 0L){
        accountM.insert(
            Account().also {
                it.id = config.adminId
                it.name = config.adminName
            }
        )

        RandomStringUtils.randomAlphanumeric(32).let {
            pwdService.reset(config.adminId, it)
            //写入到文件
            Files.writeString(Paths.get(".${config.adminId.uppercase()}"), it, CREATE, TRUNCATE_EXISTING)
            "创建初始用户 ${config.adminId}/${config.adminName}，密码写入到应用根目录"
        }
    }
    else
        "无需初始化 User ..."
}