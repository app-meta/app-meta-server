package org.appmeta.dbm

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.model.DataModel
import org.appmeta.module.dbm.DatabaseService
import org.appmeta.module.dbm.DatabaseSource
import org.appmeta.module.dbm.DatabaseSourceService
import org.appmeta.module.dbm.DbmModel
import org.junit.jupiter.api.Test
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.util.DateUtil

class DataBaseManageTest:AppTest() {

    @Resource
    lateinit var service: DatabaseService
    @Resource
    lateinit var sourceS: DatabaseSourceService

    private val TABLE = "page"

    private fun model() = DbmModel().also {
        it.db = "app-meta"
        it.sourceId = 1L
    }

    @Test
    fun sourceCache(){
        val id = 1L
        val source = sourceS.withCache(id)
        println(source)

        sourceS.insertOrUpdate(
            source?.also {
                    it.pwd = EMPTY
                    it.summary = "更新于 ${DateUtil.getDateTime()}"
                }
                ?: DatabaseSource().also {
                    it.name = "本地MySQL"
                    it.username = "root"
                    it.port = 3306
                    it.summary = "单元测试创建"
                    it.host = "localhost"
                }
        )
        println(sourceS.withCache(id))
    }

    @Test
    fun listDataBases(){
        println(service.listOfDataBase(model()))
    }

    @Test
    fun listTables(){
        println(service.listOfTable(model()))
    }

    @Test
    fun tableDetail(){
        val m = model()
        m.table = TABLE
        println(service.tableDetail(m))
    }

    @Test
    fun runSQL(){
        val m = model()
        listOf(
            "SELECT id,name,template,launch FROM `app-meta`.page",
            "UPDATE page SET launch=launch+1 WHERE id=1",
//                "DELETE FROM sys_operation ORDER BY id DESC LIMIT 1"
        ).forEach { sql->
            m.sql = sql
            println(service.runSQL(m))
        }
    }

    @Test
    fun read(){
       val m = model()
        m.table = TABLE
        m.action = DataModel.CREATE
        m.condition = "template='form'"
        m.columns = "id, name, main, active, template, launch, addOn"

        service.read(m).also { list->
            list.forEach { println(it) }
            println("结果 ${list.size} 行，共 ${list.size - 1} 条数据")
        }
    }

    @Test
    fun update(){
        model().also {
            it.table = TABLE
            it.condition = "id=4"
            it.obj = mapOf("launch" to 12, "active" to false)

            println(service.updateRow(it))
        }
    }

    @Test
    fun buildSqliteForApp(){
        println(sourceS.buildSqlitePathForApp("demo/data/text.db", "demo"))
        println(sourceS.buildSqlitePathForApp("demo/text.db", "test"))
    }
}