package org.appmeta.service

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.domain.*
import org.junit.jupiter.api.Test

class AppServiceTest:AppTest() {

    @Resource
    lateinit var appRoleM:AppRoleMapper
    @Resource
    lateinit var appLinkS:AppLinkService
    @Resource
    lateinit var pageM:PageMapper
    @Resource
    lateinit var mapper: AppMapper

    @Test
    fun checkRole(){
        appRoleM.selectList(null).also { roles->
            println("共查询到${roles.size}个角色")
            roles.forEach { r-> println(r) }
        }

        println(appRoleM.load(AID_DEMO, "admin"))
    }

    @Test
    fun createLink(){
        try{
            appLinkS.create(AppLink(AID, UID))
        }
        catch (e:Exception){
            logger.info("无法创建关联：${e.message}")
        }

        json(appLinkS.byUid(UID, AppLink.MARK))
    }

    @Test
    fun pageList(){
        json(pageM.selectList(null))
        json(pageM.getContent(1))
    }

    @Test
    fun pageCreate(){
        with(Page("SJCQ-BDB", UID)){
            content = "{}"
            main    = true
            active  = true
            name    = "数据填报"

            pageM.insert(this)
        }
    }

    @Test
    fun withCache(){
        json(mapper.withCache("SJCQ-BDB"))
    }
}