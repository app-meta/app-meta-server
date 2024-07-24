package org.appmeta.service

import com.alibaba.fastjson2.JSON
import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.F
import org.appmeta.domain.Data
import org.appmeta.domain.DataBlock
import org.appmeta.domain.DataMapper
import org.junit.jupiter.api.Test
import org.nerve.boot.util.DateUtil


/*
 * @project app-meta-server
 * @file    org.appmeta.service.DataServiceTest
 * CREATE   2022年12月16日 10:28 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class DataServiceTest : AppTest() {
    @Resource
    lateinit var service: DataService
    @Resource
    lateinit var mapper: DataMapper

    @Test
    fun insert(){
        val d = Data(AID, UID)
        d.v = mapOf(
            "day"   to DateUtil.getDate(),
            "value" to "阴性",
            "uid"   to "000001"
        )

        mapper.insert(d)
    }

    /**
     * SQL 语句
     * select * from data where v->'$.value'='阴性';
     *
     * select * from data where v->>'$.value' like "阴%";
     */
    @Test
    fun query() {
        val q = service.lambdaQuery()
        q.apply("${F.AID}='$AID'").apply("v->'$.value'='阴性'").apply("v->'$.uid'='000001'")

        q.list().forEach { d-> println(JSON.toJSONString(d)) }
    }

    private fun blockBean() = DataBlock(AID, AID)

    @Test
    fun getBlock(){
        json(service.getBlockBy(blockBean()))
    }

    @Test
    fun setBlock(){
        val block = blockBean()

        block.text = JSON.toJSONString(mapOf("name" to UNAME, "date" to System.currentTimeMillis()))
        service.setBlockTo(block)

        json(service.getBlockBy(block))

        block.text = ""
        service.setBlockTo(block)

        json(service.getBlockBy(block))
    }
}