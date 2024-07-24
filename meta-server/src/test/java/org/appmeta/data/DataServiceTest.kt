package org.appmeta.data

import com.apifan.common.random.source.*
import com.apifan.common.random.source.InternetSource.getInstance
import jakarta.annotation.Resource
import org.apache.commons.lang3.RandomUtils
import org.appmeta.AppTest
import org.appmeta.domain.Data
import org.appmeta.domain.DataMapper
import org.appmeta.model.*
import org.appmeta.service.DataService
import org.junit.jupiter.api.Test
import org.nerve.boot.util.DateUtil
import java.io.File
import java.io.FileOutputStream

class DataServiceTest:AppTest() {

    @Resource
    lateinit var service: DataService
    @Resource
    lateinit var mapper:DataMapper

    private val IP = "103.205.6.250"

    /**
     * 一般概率使用常量的 IP
     */
    private fun item() = mapOf(
        "name"      to PersonInfoSource.getInstance().randomChineseName(),
        "address"   to AreaSource.getInstance().randomAddress(),
        "age"       to NumberSource.getInstance().randomInt(18, 60),
        "degree"    to EducationSource.getInstance().randomDegree(),
        "college"   to EducationSource.getInstance().randomCollege(),
        "area"      to AreaSource.getInstance().randomProvince(),
        "phone"     to PersonInfoSource.getInstance().randomChineseMobile(),
        "mobileModel" to OtherSource.getInstance().randomMobileModel(),
        "plateNumber" to OtherSource.getInstance().randomPlateNumber(),
        "ip"        to if(RandomUtils.nextInt() % 2==0) IP else getInstance().randomPublicIpv4(),
        "company"   to OtherSource.getInstance().randomCompanyName("广西"),
        "email"     to getInstance().randomEmail(10),
        "date"      to DateTimeSource.getInstance().randomDate(2022, DateUtil.DATE)
    )

    private fun show(it: Data?) = if(it!=null) println("#${it.id} AID=${it.aid} ${it.v}") else println("-----NULL-----")

    private fun getCreateModel(): DataCreateModel {
        val model = DataCreateModel()
        model.aid = AID
        model.uid = UID
        return model
    }

    @Test
    fun create(){
        val model = getCreateModel()
        model.obj = item()
        service.create(model)
    }

    @Test
    fun createWithBatch() {
        val model   = getCreateModel()
        model.objs  = (1..20).map { item() }
        model.batch = DateUtil.getDateTimeSimple()
        model.origin= "UNIT TEST"

        service.create(model)
    }

    @Test
    fun query(){
        val model = DataReadModel()
        model.aid = AID
        model.match = mutableListOf(
            QueryItem("ip", QueryFilter.EQ, IP),
//            QueryItem("age LE 35")
            QueryItem("age", QueryFilter.LTE, 35),
            QueryItem("degree", QueryFilter.IN, listOf("博士", "硕士", "大学本科"))
        )

        service.read(model).forEach { show(it) }
    }

    @Test
    fun export(){
        val model = DataExportModel()
        model.aid = AID
        model.match = mutableListOf(
            QueryItem("ip", QueryFilter.EQ, IP),
            QueryItem("age", QueryFilter.LTE, 35),
            QueryItem("degree", QueryFilter.IN, listOf("博士", "硕士", "大学本科"))
        )
        model.headers =  mutableListOf(
            HeaderItem("name", "姓名"),
            HeaderItem("age", "年龄"),
            HeaderItem("college","毕业院校"),
            HeaderItem("phone","手机号"),
            HeaderItem("address", "家庭地址")
        )

//        FileOutputStream(File("attach/export-data-${DateUtil.getDateTimeSimple()}.xlsx")).use {
//            service.export(model, it)
//            it.flush()
//        }
        FileOutputStream(File("attach/export-data-${DateUtil.getDateTimeSimple()}.csv")).use {
            service.exportToCSV(model, it)
            it.flush()
        }
    }

    @Test
    fun loadById(){
        show(mapper.loadById(52))
    }

    @Test
    fun update(){
        val id = 5L
        show(service.readBy(id, AID))

        val model = DataUpdateModel()
        model.aid = AID
        model.id = id
        model.obj = mapOf(
            "date"  to "2022-12-12",
            "email" to "big-boss@google.com",
            "name"  to "集成显卡"
        )
        service.update(model)

        show(service.readBy(id, AID))
    }

    @Test
    fun delete(){
        val model = DataDeleteModel()
        model.aid = AID
        model.match = mutableListOf(QueryItem("ip = $IP"))

        service.delete(model)
    }

    @Test
    fun deleteById(){
        service.delete(DataDeleteModel().also { it.id =  0})
    }
}