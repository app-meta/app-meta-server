package org.appmeta.module.dbm

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.appmeta.Caches
import org.appmeta.F
import org.appmeta.component.AppConfig
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.util.AESProvider
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import java.io.Serializable


/*
 * @project app-meta-server
 * @file    org.appmeta.module.dbm.Service
 * CREATE   2023年05月22日 11:24 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class DatabaseSourceService(
    private val authM:DatabaseAuthMapper,
    private val config:AppConfig) : BaseService<DatabaseSourceMapper, DatabaseSource>() {

    /**
     * 不宜使用 Serializable 作为 Key
     */
    @Cacheable(Caches.DBM_SOURCE)
    fun withCache(id:Long): DatabaseSource? {
        if(logger.isDebugEnabled)   logger.debug("从数据库中读取 DatabaseSource#$id 的信息...")
        return getById(id)
    }

    fun listWithoutPwd() = list().onEach { it.pwd = "" }

    @CacheEvict(Caches.DBM_SOURCE, key = "#source.id")
    fun insertOrUpdate(source: DatabaseSource) {
        Assert.hasText(source.host, "数据源地址不能为空")
        Assert.hasText(source.username, "数据源用户名不能为空")
        Assert.isTrue(source.port > 0, "数据源端口格式不正确")
        Assert.hasText(source.type, "数据源类型不能为空")

        if(source.pwd.isNotEmpty())
            source.pwd = AESProvider().encrypt(source.pwd, config.dbmKey)

        if(source.using()){
            updateById(source)
            logger.info("更新数据源 $source")
        }
        else{
            save(source)
            logger.info("新增数据源 $source")
        }
    }

    @CacheEvict(Caches.DBM_SOURCE)
    fun remove(id: Serializable): DatabaseSource {
        val source = getById(id)?: throw Exception("数据源#$id 不存在")

        val authCount = authM.selectCount(QueryWrapper<DatabaseAuth>().eq(F.SOURCE_ID, id))
        if(authCount>0)
            throw Exception("数据源#$id [${source.type.uppercase()}@${source.host}] 存在 $authCount 则权限分配，暂不能删除")

        removeById(id)
        return source
    }
}
