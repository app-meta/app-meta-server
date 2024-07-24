package org.appmeta.web.system

import org.appmeta.domain.Member
import org.appmeta.domain.MemberMapper
import org.appmeta.model.IdStringModel
import org.appmeta.model.QueryModel
import org.appmeta.service.MemberService
import org.nerve.boot.db.service.QueryHelper
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/*
 * @project app-meta-server
 * @file    org.appmeta.web.system.MemberCtrl
 * CREATE   2023年04月14日 10:17 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("system/member")
class MemberCtrl(private val mapper:MemberMapper, private val service:MemberService) : BasicController() {

    @PostMapping("list", name = "会员终端清单")
    fun list(@RequestBody model:QueryModel) = resultWithData {
        service.list(QueryHelper<Member>().buildFromMap(model.form))
    }

    @PostMapping("create", name = "新增会员终端")
    fun create(@RequestBody bean:Member) = result {
        service.add(bean)
        opLog("新增会员终端 ${bean.id}（允许用户 ${bean.ids}）", bean, Operation.CREATE)
    }

    @PostMapping("delete", name = "删除会员终端")
    fun delete(@RequestBody model:IdStringModel) = result {
        mapper.deleteById(model.id)
        opLog("删除会员终端 ${model.id}", Member(model.id), Operation.DELETE)
    }
}