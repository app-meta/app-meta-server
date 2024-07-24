package org.appmeta.web.system

import org.appmeta.domain.PageLink
import org.appmeta.model.QueryModel
import org.appmeta.service.PageLinkService
import org.appmeta.service.PageService
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.web.bind.annotation.*


/*
 * @project app-meta-server
 * @file    org.appmeta.web.system.PageManageCtrl
 * CREATE   2023年03月23日 15:20 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("system/page")
class PageManageCtrl(
    private val service:PageService,
    private val linkS:PageLinkService):BasicController() {

    @RequestMapping("link/list", name = "页面关注列表")
    fun list(@RequestBody model: QueryModel) = result {
        it.data = linkS.list(model.form, model.pagination)
        it.total= model.pagination.total
    }

    @PostMapping("link/add", name = "新增页面关注")
    fun add(@RequestBody link: PageLink) = resultWithData {
        linkS.add(link)
        opLog("新增 ${link.uid} 对页面（#${link.pid}）的关注", link, Operation.CREATE)
    }

    @PostMapping("link/remove/{id}", name = "取消页面关注")
    fun remove(@PathVariable id:Long) = resultWithData {
        opLog("删除页面关注信息#${id} = ${linkS.removeById(id)}", PageLink(id), Operation.DELETE)
    }
}