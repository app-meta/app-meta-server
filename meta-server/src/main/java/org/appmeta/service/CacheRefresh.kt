package org.appmeta.service

import org.appmeta.Caches
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Component

@Component
class CacheRefresh {
    private val  logger = LoggerFactory.getLogger(javaClass)

    @CacheEvict(Caches.AUTH_USER, allEntries = true)
    fun authUser(id:String) {}

    @CacheEvict(Caches.APP)
    fun app(id:String){}

    @Caching(
        evict = [
            CacheEvict(Caches.PAGE_LIST, allEntries = true),
            CacheEvict(Caches.PAGE_LINK),
            CacheEvict(Caches.PAGE_LINK_LIST, key = "#uid")
        ]
    )
    fun pageLink(pid:String, uid:String){}

    @CacheEvict(Caches.PAGE_LINK_LIST)
    fun pageLinkOfUser(uid: String){}

    @CacheEvict(Caches.PAGE_LIST, allEntries = true)
    fun pageList() {}

    @CacheEvict(Caches.NOTICE_LIST, allEntries = true)
    fun noticeList() {}

    @CacheEvict(Caches.PAGE_SERVER)
    fun pageServer(aid:String) {}

    @CacheEvict(Caches.PAGE_DOCUMENT)
    fun pageDocumentList(id:String) {
        if(logger.isDebugEnabled) logger.debug("移除页面 #$id 的附件清单缓存...")
    }

    @Caching(
        evict = [
            CacheEvict(Caches.API),
            CacheEvict(Caches.API_DETAIL)
        ]
    )
    fun api(id:Long) {
        if(logger.isDebugEnabled) logger.debug("移除开放接口 #$id 的缓存...")
    }
}