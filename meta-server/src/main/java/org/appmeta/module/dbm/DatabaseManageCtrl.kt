package org.appmeta.module.dbm

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import io.jsonwebtoken.lang.Assert
import org.apache.commons.lang3.exception.ExceptionUtils
import org.appmeta.F
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.model.DataModel.Companion.CREATE
import org.appmeta.model.DataModel.Companion.DELETE
import org.appmeta.model.DataModel.Companion.READ
import org.appmeta.model.DataModel.Companion.UPDATE
import org.appmeta.model.IdModel
import org.appmeta.model.QueryModel
import org.nerve.boot.Result
import org.nerve.boot.cache.CacheManage
import org.nerve.boot.db.service.QueryHelper
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.util.Timing
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder
import java.util.*

class UseSQLException(msg:String="该功能暂未支持，请使用 SQL 方式"):RuntimeException(msg)

@RestController
@RequestMapping("dbm/source")
class DatabaseSourceCtrl(
    private val logMapper: DatabaseLogMapper,
    private val authMapper: DatabaseAuthMapper,
    private val service: DatabaseSourceService,
    private val mapper: DatabaseSourceMapper):BasicController() {

    @RequestMapping("detail", name = "数据源详细")
    fun detail(@RequestBody model:IdModel) = resultWithData { service.withCache(model.id) }

    @RequestMapping("list", name = "数据源清单")
    fun list() = resultWithData {
        val user = authHolder.get()

        service.listWithoutPwd().let { sources->
            if(H.hasAnyRole(user, Role.ADMIN, Role.DBM_ADMIN))
                sources
            else
                authMapper.selectObjs<Int>(QueryWrapper<DatabaseAuth>().eq(F.UID, user.id).select(F.SOURCE_ID))
                    .let { ids->
                        if(ids.isEmpty())
                            emptyList<DatabaseSource>()
                        else
                            sources.filter { ids.contains(it.id.toInt()) }
                    }
        }
    }

//    @RequestMapping("mine", name = "查看授权的数据源")
//    fun mine() = resultWithData {
//        val user = authHolder.get()
//        val ids = authMapper.selectObjs(QueryWrapper<DatabaseAuth>().eq(F.UID, user.id).select(F.SOURCE_ID))
//        println(ids)
//
//        if(ids.isEmpty())
//            emptyList<DatabaseSource>()
//        else{
//            service.listWithoutPwd().filter { ids.contains(it.id.toInt()) }
//        }
//    }

    @RequestMapping("add", name = "创建数据源")
    fun create(@RequestBody source: DatabaseSource) = result {
        service.insertOrUpdate(source)
    }

    @RequestMapping("delete", name = "删除数据源")
    fun delete(@RequestBody model:IdModel) = result {
        val source = service.remove(model.id)
        opLog("删除数据源 $source", source, Operation.DELETE)
    }

    @PostMapping("log", name = "查看数据源操作日志")
    fun log(@RequestBody model: QueryModel) = result {
        val p = Page.of<DatabaseLog>(model.pagination.page.toLong(), model.pagination.pageSize.toLong())
        logMapper.selectPage(p, QueryHelper<DatabaseLog>().buildFromMap(model.form))

        it.data = p.records
        it.total= p.total
    }
}

@RestController
@RequestMapping("dbm/auth")
class DatabaseAuthCtrl(private val mapper: DatabaseAuthMapper):BasicController() {

    //清空缓存
    private fun _cleanCache(uid:String) = CacheManage.clearWithPrefix("DBM-${uid}")

    @RequestMapping("list", name = "数据源权限列表")
    fun list() = resultWithData { mapper.selectList(null) }

    @RequestMapping("edit", name = "数据源权限维护")
    fun edit(@RequestBody auth: DatabaseAuth) = result {
        Assert.hasText(auth.uid, "用户ID不能为空")
        Assert.isTrue(auth.sourceId>0, "未关联数据源")
        Assert.hasText(auth.allow, "权限值未分配")


        if (auth.using())
            mapper.updateById(auth)
        else
            mapper.insert(auth)

        _cleanCache(auth.uid)
    }

    @RequestMapping("delete", name = "数据源权限删除")
    fun delete(@RequestBody model:IdModel) = result {
        mapper.selectById(model.id)?.let {
            mapper.deleteById(model.id)
            _cleanCache(it.uid)

            opLog("删除数据源权限 ID=${model.id}", it, Operation.DELETE)
        }
    }
}


@RestController
@RequestMapping("dbm")
class DatabaseManageCtrl(
    private val sourceS:DatabaseSourceService,
    private val service: DatabaseService) :BasicController() {

    @RequestMapping
    fun deal(@RequestBody model: DbmModel): Result {
        val log = DatabaseLog()
        val timing = Timing()
        try{
            val source = sourceS.withCache(model.sourceId)?: throw Exception("无效数据源 sourceId=${model.sourceId}")
            log.of(source)

            val user = authHolder.get()
            log.uid = user.id

            if(StringUtils.hasText(model.sql)){
                model.sql = URLDecoder.decode(String(Base64.getDecoder().decode(model.sql)), Charsets.UTF_8)
                log.ps = model.sql
            }
            else if(StringUtils.hasText(model.condition))
                log.ps = "${model.condition} ${if(model.obj != null) JSON.toJSONString(model.obj) else ""}"

            if(logger.isDebugEnabled)   logger.debug("${user.showName} 执行数据库操作：$model")

            if(!service.check(user, model.sourceId, model.action))
                throw Exception("未授权对数据源 #${source.id}/${source.name} 进行${model.action}操作")

            val data =  when (model.action) {
                CREATE          -> throw UseSQLException()
                UPDATE, DELETE  -> {
                    Assert.hasText(model.condition, "筛选条件不能为空")
                    Assert.isTrue(org.apache.commons.lang3.StringUtils.isNoneEmpty(model.db, model.table), "必须指定目标数据表")

                    if(model.action== UPDATE)
                        service.updateRow(model)
                    else
                        service.delRow(model)
                }
                READ            -> {
                    Assert.hasText(model.table, "请指定数据表（table）")
                    service.read(model)
                }
                "",DbmModel.SQL ->{
                    if(!StringUtils.hasText(model.sql)) throw Exception("参数 sql 必须填写")
                    service.runSQL(model)
                }
                else    -> throw Exception("无效的 ACTION，请阅读文档")
            }

            return Result.ok(data)
        }catch (e:Exception) {
            log.summary = ExceptionUtils.getMessage(e)
            logger.error("执行数据源操作失败：${log.summary}")

            return Result.fail(e)
        }finally {
            log.used = timing.toMillSecond()
            if(log.sourceId<=0) log.sourceId = model.sourceId
            log.action  = model.action
            log.target  = arrayOf(model.db, model.table).filter { it.isNotEmpty() }.joinToString("/")

            service.saveLog(log)
        }
    }

    @RequestMapping("items")
    fun contentList(@RequestBody model: DbmModel) = resultWithData {
        if(StringUtils.hasText(model.db)){
            if(model.table.isEmpty())
                service.listOfTable(model)
            else{
                if(logger.isDebugEnabled)   logger.debug("查看 #${model.sourceId} 数据表 `${model.db}`.${model.table} 的结构...")
                service.tableDetail(model)
            }
        }
        else
            service.listOfDataBase(model)
    }
}