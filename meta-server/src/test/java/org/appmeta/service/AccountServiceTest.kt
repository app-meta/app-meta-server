package org.appmeta.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.F
import org.appmeta.domain.Account
import org.appmeta.domain.AccountMapper
import org.appmeta.tool.AuthHelper
import org.appmeta.tool.JWTTool
import org.junit.jupiter.api.Test
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.MD5Util
import org.nerve.boot.web.JWTConfig
import java.util.*


/*
 * @project app-meta-server
 * @file    org.appmeta.service.AccountServiceTest
 * CREATE   2022年12月19日 12:10 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class AccountServiceTest : AppTest() {
    @Resource
    lateinit var pwdService: AccountPwdService
    @Resource
    lateinit var jwtTool: JWTTool
    @Resource
    lateinit var jwtConfig: JWTConfig
    @Resource
    lateinit var settingS:SettingService
    @Resource
    lateinit var accountMapper: AccountMapper
    @Resource
    lateinit var service: AccountService
    @Resource
    lateinit var authHelper: AuthHelper

    @Test
    fun resetPwd() {
        // 新增用户
        val account = Account()
        if(!accountMapper.exists(QueryWrapper<Account>().eq(F.ID, UID))){
            account.name = UNAME
            account.did = "000"
            account.id  = UID

            accountMapper.insert(account)
        }
        val pwd = "Abc123."
        pwdService.reset(UID, pwd)

        println("校验密码：${pwdService.check(UID, Base64.getEncoder().encodeToString(MD5Util.encode(pwd).toByteArray()))}")
    }

    @Test
    fun createJWTUserToken(){
        val token = jwtTool.create(UID, "127.0.0.1", 50*365*24*60)

        println(token)
    }

    @Test
    fun refreshFromRemote(){
//        service.refreshFromRemote("demo/account-list.json")
        service.refreshFromRemote("demo/account-array.json")
    }

    @Test
    fun refreshFromRemoteWithString(){
        service.refreshFromRemote("""
            [
              ["00001","张三","001 办公室"],
              ["00002","李四","001 办公室"],
              ["00003","王五","001 办公室"],
              ["00004","莫六","001 办公室"],
              ["00005","曾七","001 办公室"],
              ["00006","刘八","001 办公室"]
            ]
            """.trimIndent()
        )
    }

    @Test
    fun checkAuth(){
        val user = AuthUser()
        user.id = UID
        user.roles = listOf()
        user.name = UNAME

        json(authHelper.checkService("", UID, user))
        json(authHelper.checkService("", UID, user))
        json(authHelper.checkService("", "", user))
    }
}