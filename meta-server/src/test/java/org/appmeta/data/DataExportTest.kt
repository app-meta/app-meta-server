package org.appmeta.data

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy
import com.apifan.common.random.source.AreaSource
import com.apifan.common.random.source.PersonInfoSource
import org.appmeta.model.DataExportModel
import org.appmeta.model.HeaderItem
import org.junit.jupiter.api.Test
import org.nerve.boot.util.DateUtil


/*
 * @project app-meta-server
 * @file    org.appmeta.data.DataExportTest
 * CREATE   2023年04月11日 09:41 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class DataExportTest {

    /**
     * 自由导出数据
     */
    @Test
    fun exportFreedom(){
        val p = PersonInfoSource.getInstance()
        val a = AreaSource.getInstance()
        EasyExcel.write("attach/export-demo-${DateUtil.getDateTimeSimple()}.xlsx")
            .head(
                listOf(
                    listOf("姓名"),
                    listOf("籍贯"),
                    listOf("手机号"),
                    listOf("家庭地址")
                )
            )
            .registerWriteHandler(LongestMatchColumnWidthStyleStrategy())
            .sheet()
            .doWrite(
                (1..20).map {
                    listOf(
                        p.randomChineseName(),
                        a.randomProvince(),
                        p.randomChineseMobile(),
                        a.randomAddress()
                    )
                }
            )
    }

    /**
     * 通过 DataExportModel 实现流式导出
     */
    @Test
    fun exportWithModel(){
        val p = PersonInfoSource.getInstance()
        val a = AreaSource.getInstance()

        val model = DataExportModel()
        model.headers = mutableListOf(
            HeaderItem("xm", "姓名"),
            HeaderItem("jg","籍贯"),
            HeaderItem("sjh","手机号"),
            HeaderItem("jtdz", "家庭地址")
        )

//        val data = (1..20).map {
//            mapOf(
//                "xm"    to p.randomChineseName(),
//                "jg"    to a.randomProvince(),
//                "sjh"   to p.randomChineseMobile(),
//                "jtdz"  to a.randomAddress()
//            )
//        }

        val fields = model.headerFields()

        val writer = EasyExcel.write("attach/export-demo-${DateUtil.getDateTimeSimple()}.xlsx")
            .head(model.headers.map { listOf(it.label) })
            .registerWriteHandler(LongestMatchColumnWidthStyleStrategy())
            .build()
        val sheet = EasyExcel.writerSheet(model.sheetName).build()

        (1..10000).map {
            val d = mapOf(
                "xm"    to p.randomChineseName(),
                "jg"    to a.randomProvince(),
                "sjh"   to p.randomChineseMobile(),
                "jtdz"  to a.randomAddress()
            )

            writer.write(listOf(fields.map { d[it] }), sheet)
        }

        writer.finish()
    }
}