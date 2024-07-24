package org.appmeta.service

import com.alibaba.fastjson2.JSON
import jakarta.annotation.Resource
import org.appmeta.ALL
import org.appmeta.AppTest
import org.appmeta.F
import org.appmeta.domain.Page
import org.appmeta.domain.PageMapper
import org.appmeta.model.QueryModel
import org.appmeta.module.faas.Func
import org.appmeta.module.faas.FuncParmeter
import org.junit.jupiter.api.Test


/*
 * @project app-meta-server
 * @file    org.appmeta.service.PageServiceTest
 * CREATE   2023年03月21日 09:19 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class PageServiceTest:AppTest() {
    @Resource
    lateinit var service: PageService
    @Resource
    lateinit var mapper:PageMapper

    @Test
    fun list(){
        val model = QueryModel()
        model.form = mutableMapOf("EQ_aid" to "FKZX_CLSCQ")

        println("筛选页面数量=${service.list(model).size}, HASH=${model.hashCode()}")
        println("筛选页面数量=${service.list(model).size}, HASH=${model.hashCode()}")

        model.fields = listOf("name", "template")
        println("筛选页面数量=${service.list(model).size}, HASH=${model.hashCode()}")

        val model2 = QueryModel()
        model2.form = mutableMapOf("EQ_aid" to "FKZX_CLSCQ")
        println("筛选页面数量=${service.list(model).size}, HASH=${model.hashCode()}")
        println("筛选页面数量=${service.list(model2).size}, HASH=${model2.hashCode()}")
    }

    /**
     * 创建一个 template=faas 的功能页面
     */
    @Test
    fun initFaas(){
        mapper.insert(Page(AID_DEMO, UID).also {
            it.name = "[FaaS]SQL脚本示例"
            it.template = Page.FAAS
            it.active = true
            it.addOn = System.currentTimeMillis()
            it.serviceAuth = ALL
            it.content = JSON.toJSONString(
                Func().also { f->
                    f.cmd = "SELECT id,name,template FROM page where aid='{{ params.aid }}'"
                    f.mode = Func.SQL
                    f.params = listOf(FuncParmeter(F.AID, "应用ID", true))
                    f.sourceId = 0L
                }
            )
        })

        mapper.insert(Page(AID_DEMO, UID).also {
            it.name = "[FaaS]JavaScript脚本示例"
            it.template = Page.FAAS
            it.active = true
            it.addOn = System.currentTimeMillis()
            it.serviceAuth = ALL
            it.content = JSON.toJSONString(
                Func().also { f->
                    f.cmd = """
                        console.log(`调用JS脚本，参数`, params, "用户ID", user.id)
                        
                        const ID = "counter"
                        let count = meta.getSession(ID) || 0
                        meta.setSession(ID, count++)
                        console.debug(`[会话值] COUNTER=`, count)
                        
                        [{ time: Date.now(), user: user.id, data: count }]
                    """.trimIndent()
                    f.mode = Func.JS
                    f.params = listOf(FuncParmeter(F.AID, "应用ID", true))
                    f.sourceId = null
                }
            )
        })
    }
}