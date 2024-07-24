package org.appmeta.service

import io.jsonwebtoken.lang.Assert
import org.appmeta.Caches
import org.appmeta.F
import org.appmeta.component.PageDeleteEvent
import org.appmeta.component.PageNameUpdateEvent
import org.appmeta.component.func.MarkdownFunc
import org.appmeta.domain.Page
import org.appmeta.domain.PageLink
import org.appmeta.domain.PageLinkMapper
import org.appmeta.domain.PageMapper
import org.appmeta.model.QueryModel
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.enums.Fields
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils.hasText


/*
 * @project app-meta-server
 * @file    org.appmeta.service.PageService
 * CREATE   2023年03月20日 15:52 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class PageService(private val markdownFunc: MarkdownFunc):BaseService<PageMapper, Page>() {

    @Cacheable(Caches.PAGE_LIST, key = "#model.hashCode")
    fun list(model:QueryModel):List<Page> = baseMapper.selectList(
        if(model.form.isEmpty())
            null
        else {
            val w = queryHelper.buildFromMap(model.form)
            if(model.fields.isNotEmpty())
                w.select(model.fields)
            w
        }
    )

    /**
     * 查询数量或者符合条件的前 20 条记录
     */
    fun query(model: QueryModel):Any =
        if(model.countOnly)
            baseMapper.selectCount(queryHelper.buildFromMap(model.form))
        else{
            list(model.form, model.pagination, model.fields)
        }

    fun buildContent(page:Page, convertMarkdown:Boolean = false): String = baseMapper.getContent(page.id).let { content->
        //如果是
        if(convertMarkdown && page.template == Page.MARKDOWN){
            return@let markdownFunc.embedImages(page.id, content)
        }

        content
    }
}

@Service
class PageLinkService(private val refresh:CacheRefresh,private val pageM:PageMapper):BaseService<PageLinkMapper, PageLink>() {

    /**
     * 当页面标题更新时，刷新关联中的页面名称
     */
    @Async
    @EventListener(PageNameUpdateEvent::class)
    fun onPageUpdate(event: PageNameUpdateEvent) {
        val page = event.page

        baseMapper.update(null, U().eq(F.PID, page.id).set(Fields.NAME.value(), page.name))
    }

    /**
     * 当页面删除时，将全部关联失效
     */
    @EventListener(PageDeleteEvent::class)
    fun onPageDelete(event: PageDeleteEvent) {
        baseMapper.update(null, U().eq(F.PID, event.id).eq(F.ACTIVE, true).set(F.ACTIVE, false))
    }

    @Cacheable(Caches.PAGE_LINK_LIST)
    fun listByUser(uid:String) = baseMapper.byUser(uid)

    /**
     * 激活某个关注
     * 如果数据已存在则设置 active = 1
     */
    fun add(link: PageLink) {
        Assert.isTrue(hasText(link.pid) && hasText(link.uid), "页面ID及用户信息不能为空")

        val oldD = baseMapper.byPageAndUser(link.pid, link.uid)
        if(oldD != null){
            if(!oldD.active){
                oldD.active = true
                baseMapper.updateById(oldD)

                logger.info("${link.uid} 重新关注页面 #${link.pid}/${oldD.name}")
            }
        }
        else {
            if(!hasText(link.name)){
                val page        = pageM.selectById(link.pid)?: throw Exception("页面#${link.pid} 不存在")
                link.name       = page.name
                link.aid        = page.aid
                link.template   = page.template
            }

            link.active = true
            link.addOn  = System.currentTimeMillis()
            baseMapper.insert(link)
            logger.info("${link.uid} 关注页面 #${link.pid}/${link.name}")
        }

        refresh.pageLink(link.pid, link.uid)
    }

    fun remove(pid:String, uid:String){
        baseMapper.update(null, U().eq(F.PID, pid).eq(F.UID, uid).set(F.ACTIVE, false))
        refresh.pageLink(pid, uid)
    }

    @Cacheable(Caches.PAGE_LINK)
    fun check(pid: String, uid:String) = count(Q().eq(F.UID, uid).eq(F.PID, pid).eq(F.ACTIVE, true)) > 0
}