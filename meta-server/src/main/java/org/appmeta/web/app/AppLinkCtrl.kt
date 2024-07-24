package org.appmeta.web.app

import org.appmeta.domain.AppLink
import org.appmeta.service.AppLinkService
import org.nerve.boot.web.ctrl.BaseController
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.web.app.AppLinkCtrl
 * CREATE   2023年01月09日 10:53 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class LinkModel{
    var from    = 0     //数据查询开始
}

@RestController
@RequestMapping("app/link")
class AppLinkCtrl(private val service: AppLinkService) : BaseController() {

    @RequestMapping("marked", name = "查看我的收藏")
    fun marked(@RequestBody model: LinkModel) = resultWithData {
        val user = authHolder.get()

        service.byUid(user.id, AppLink.MARK, model.from)
    }

    @RequestMapping("make", name = "创建关联")
    fun makeLink(@RequestBody link: AppLink) = resultWithData {
        link.uid = authHolder.get().id

        service.create(link)
    }

    @RequestMapping("remove", name = "移除关联")
    fun rmLink(@RequestBody link: AppLink) = resultWithData {
        link.uid = authHolder.get().id

        service.remove(link)
    }

    @RequestMapping("check", name = "检测是否存在关联")
    fun check(@RequestBody link: AppLink) = resultWithData {
        link.uid = authHolder.get().id

        service.exist(link)
    }
}