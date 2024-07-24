package org.appmeta.web

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.appmeta.domain.DataBlock
import org.appmeta.domain.DataMapper
import org.appmeta.domain.WithApp
import org.appmeta.model.*
import org.appmeta.service.DataService
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.util.DateUtil
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder


@RestController
@RequestMapping("data")
class DataCtrl(private val service: DataService, private val mapper:DataMapper):CommonCtrl() {

    private fun _detectRole(model: DataModel) {
//        val user = authHolder.get()
        /*
        权限规则待定，2023-03-16

        未来考虑针对 aid、pid 进行检查
         */
        // 如果非管理员权限，则只显示自己的数据
//        if(!appRoleS.isAdmin(model.aid, user.id)){
//            model.uid = user.id
//        }
    }

    @RequestMapping("create", name = "新增数据")
    fun create(@Valid @RequestBody model: DataCreateModel) = resultWithData {
        val user = authHolder.get()
        if(user != null)    model.uid   = user.id

        model.channel = getChannel()
        service.create(model)
    }

    @RequestMapping("update", name = "更新数据")
    fun update(@Valid @RequestBody model: DataUpdateModel) = resultWithData {
        service.update(model)
    }

    @RequestMapping("query", name = "查询数据")
    fun query(@Valid @RequestBody model: DataReadModel) = result {
        _detectRole(model)

        /*
        如果 pageSize 为 1 则直接返回 Data 对象
         */
        it.data     = service.read(model).let { ds->
            if(model.pageSize == 1L)
                ds.firstOrNull()
            else
                ds
        }
        it.total    = model.total
    }

    @RequestMapping("delete", name = "删除数据")
    fun delete(@Valid @RequestBody model: DataDeleteModel) = resultWithData {
        _detectRole(model)

        service.delete(model)
    }

    @PostMapping("export", name = "导出数据")
    fun export(@RequestBody model:DataExportModel, response:HttpServletResponse)  {
        if(model.headers.isEmpty()) throw Exception("标题列不能为空")
        if(model.aid.isEmpty())     throw Exception("应用ID 不能为空")
        if(model.pid.isEmpty())     throw Exception("PID 不能为空")
        if(!model.checkFormat())    throw Exception("未知的导出格式")

        _detectRole(model)

        val fileName = URLEncoder.encode(
            if(StringUtils.hasText(model.filename)) model.filename else "DATA-EXPORT-${DateUtil.getDateTimeSimple()}.${model.format}",
            "UTF-8"
        )
        response.setHeader("Content-disposition", "attachment; filename=${URLEncoder.encode(fileName, "UTF-8")}")
        response.characterEncoding = "utf-8"

        if(model.format == DataExportModel.XLSX){
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            service.export(model, response.outputStream)
        }
        else{
            response.contentType = "application/force-download"
            service.exportToCSV(model, response.outputStream)
        }
    }

    /**
     * 返回 text 字段（如果数据块不为 null），否则返回空字符串
     */
    @RequestMapping("block/get", name = "获取数据块")
    fun getBlock(@Valid @RequestBody block: DataBlock) = resultWithData {
        service.getBlockBy(block)?.text
    }

    @RequestMapping("block/set", name = "设置数据块")
    fun setBlock(@Valid @RequestBody block: DataBlock) = result {
        service.setBlockTo(block)

        opLog("更新数据块 ${block.info()}", block, Operation.MODIFY)
    }

    @RequestMapping("block/list", name = "数据块列表")
    fun listBlock(@RequestBody model:WithApp) = resultWithData {
        service.getBlockList(model.aid)
    }
}