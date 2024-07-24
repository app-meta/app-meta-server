package org.appmeta.web.system

import org.appmeta.domain.Notice
import org.appmeta.model.IdModel
import org.appmeta.model.QueryModel
import org.appmeta.service.CacheRefresh
import org.appmeta.service.NoticeService
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.web.system.NoticeCtrl
 * CREATE   2023年03月22日 14:55 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("notice")
class NoticeCtrl(private val cacheR: CacheRefresh, private val service:NoticeService) : BasicController() {

    @PostMapping("valid", name = "获取可用的公告")
    fun avlid() = resultWithData { service.findByUser(authHolder.get()) }

    @RequestMapping("list", name = "公告列表查询")
    fun list(@RequestBody model: QueryModel) = result {
        it.data = service.list(model.form, model.pagination)
        it.total= model.pagination.total
    }

    @PostMapping("add", name = "新增公告")
    fun add(@RequestBody notice: Notice) = result {
        notice.of(authHolder.get())
        service.create(notice)
        opLog("新增公告：${notice.name}", notice)
    }

    @PostMapping("delete", name = "删除公告")
    fun delete(@RequestBody model:IdModel) = result {
        service.removeById(model.id)
        cacheR.noticeList()
        opLog("删除公告 #${model.id}", Notice(model.id))
    }

    @PostMapping("lines", name = "获取投放记录")
    fun lines(@RequestBody model:IdModel) = resultWithData { service.loadLines(model.id) }

    @PostMapping("done", name = "公告已阅")
    fun readDone(@RequestBody model: IdModel) = result {
        val user = authHolder.get()
        service.afterRead(model.id, user.id)
        logger.info("${user.showName} 已完成公告 #${model.id} 的阅读...")
    }
}