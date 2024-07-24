package org.appmeta.web.page

import org.appmeta.F
import org.appmeta.domain.PageLink
import org.appmeta.domain.PageLinkMapper
import org.appmeta.model.QueryModel
import org.appmeta.service.PageLinkService
import org.nerve.boot.module.operation.Operation
import org.springframework.web.bind.annotation.*


/*
 * @project app-meta-server
 * @file    org.appmeta.web.page.PageLinkCtrl
 * CREATE   2023年03月20日 15:50 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("page/link")
class PageLinkCtrl(
    private val mapper:PageLinkMapper,
    private val service:PageLinkService) : BasicPageCtrl() {

    @PostMapping("list", name = "已关注页面清单")
    fun list(@RequestBody model:QueryModel) = resultWithData {
        val user = authHolder.get()

        if(model.form.isEmpty())
            service.listByUser(user.id)
        else{
            model.form["EQ_${F.UID}"] = user.id
            service.listWithLimit(model.form, model.pagination.pageSize)
        }
    }

    @PostMapping("refresh", name = "刷新我已关注页面缓存")
    fun refresh() = result { cacheRefresh.pageLinkOfUser(authHolder.get().id) }

    @PostMapping("add", name = "新增页面关注")
    fun add(@RequestBody link: PageLink) = resultWithData {
        val user = authHolder.get()
        link.uid = user.id

        service.add(link)
        opLog("新增页面（#${link.pid}）关注", link, Operation.CREATE)
    }

    @PostMapping("remove/{pid}", name = "取消页面关注")
    fun remove(@PathVariable pid:String) = resultWithData {
        service.remove(pid, authHolder.get().id)
        opLog("取消页面（#$pid）的关注", PageLink())
    }

    @PostMapping("check/{pid}", name = "检测页面关注")
    fun check(@PathVariable pid:String) = resultWithData { service.check(pid, authHolder.get().id) }
}