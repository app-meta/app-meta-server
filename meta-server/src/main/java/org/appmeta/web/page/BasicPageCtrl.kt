package org.appmeta.web.page

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import jakarta.annotation.Resource
import org.appmeta.F
import org.appmeta.domain.Page
import org.appmeta.domain.PageMapper
import org.appmeta.service.CacheRefresh
import org.appmeta.tool.AuthHelper
import org.appmeta.web.CommonCtrl
import org.nerve.boot.Result
import org.nerve.boot.domain.AuthUser
import org.springframework.http.HttpStatus
import java.io.Serializable


/*
 * @project app-meta-server
 * @file    org.appmeta.web.page.BasicPageCtrl
 * CREATE   2023年04月10日 11:33 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

open class BasicPageCtrl : CommonCtrl() {
    @Resource
    lateinit var cacheRefresh: CacheRefresh
    @Resource
    lateinit var authHelper: AuthHelper
    @Resource
    lateinit var pageM:PageMapper

    protected fun _loadPage(id:Serializable) = pageM.selectById(id)?: throw Exception("页面/功能 #$id 不存在")

    /**
     * 判断是否具备指定页面的访问权限
     */
    protected fun _checkServiceResult(pageId: Serializable, worker:(Page, AuthUser)->Any?): Result =
        _checkServiceAuth(pageId) { page, user ->
            resultWithData { worker(page, user) }
        }

    protected fun _checkEditResult(pageId: Serializable, worker:(Page, AuthUser)->Any?): Result =
        _checkEditAuth(pageId) { page, user ->
            resultWithData { worker(page, user) }
        }

    protected fun <R> _checkServiceAuth(pageId: Serializable, worker: (Page, AuthUser) -> R):R {
        val page = _loadPage(pageId)
        val user = authHolder.get()
        if(authHelper.checkService(page, user))
            return worker(page, user)

        throw Exception(HttpStatus.UNAUTHORIZED.name)
    }

    protected fun <R> _checkEditAuth(pageId: Serializable, worker:(Page, AuthUser)->R): R {
        val page = _loadPage(pageId)
        val user = authHolder.get()
        if(authHelper.checkEdit(page, user))
            return worker(page, user)

        throw Exception(HttpStatus.UNAUTHORIZED.name)
    }

    protected fun updateDateById(id:Serializable) = pageM.update(
        null,
        UpdateWrapper<Page>().eq(F.ID, id).set(F.UPDATE_ON, System.currentTimeMillis())
    )
}