package org.appmeta.module.dbm

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.appmeta.ANY
import org.appmeta.F
import org.appmeta.S
import org.appmeta.component.AppConfig
import org.nerve.boot.Const.COMMA
import org.nerve.boot.cache.CacheManage
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.AESProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils.hasText
import java.io.Serializable
import java.sql.Connection
import java.sql.ResultSet

@Service
class DatabaseService(
    private val logM:DatabaseLogMapper,
    private val config: AppConfig,
    private val authM:DatabaseAuthMapper,
    private val sourceS:DatabaseSourceService, private val settingS:SettingService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val LIMIT = "LIMIT"

    private val dataSources = mutableMapOf<Serializable, HikariDataSource>()

    private fun _createDataSource(id:Long): HikariDataSource {
        val source = sourceS.withCache(id)?: throw Exception("数据源#$id 不存在，请先定义")

        val ds = HikariDataSource(HikariConfig().also {
            it.username = source.username

            if(source.pwd.isNotEmpty())
                it.password = AESProvider().decrypt(source.pwd, config.dbmKey)

            it.jdbcUrl = "jdbc:${source.type}://${source.host}:${source.port}"
//            it.driverClassName = "com.mysql.cj.jdbc.Driver"
        })
        ds.maxLifetime = config.dbmLifetime * 60 * 1000
        dataSources[id] = ds

        logger.info("创建数据源实例 $ds")
        return ds
    }

    private fun <R> withConn(model: DbmModel, worker:(Connection)-> R):R {
        val dataSource = if(dataSources.containsKey(model.sourceId)){
            dataSources[model.sourceId]?.let {
                if(it.isRunning) it else _createDataSource(model.sourceId)
            }
        }
        else
            _createDataSource(model.sourceId)

        return dataSource!!.connection.use {
            if(hasText(model.db)){
                if(logger.isDebugEnabled)   logger.debug("自动切换到数据库 ${model.db}")
                it.createStatement().execute("USE `${model.db}`;")
            }
            return@use worker(it)
        }
    }

    private fun queryForList(conn:Connection, sql:String, ps:Collection<Any?> = emptyList()) = conn.prepareStatement(sql).use {
        ps.forEachIndexed { i, v ->
            when (v) {
                is String ->    it.setString(i+1, v)
                is Long ->      it.setLong(i+1, v)
                is Int ->       it.setInt(i+1, v)
                is Boolean ->   it.setBoolean(i+1, v)
            }
        }
        if(logger.isDebugEnabled)   logger.debug("执行SQL > $sql")
        it.execute()

        return@use if(it.resultSet == null) listOf("${it.updateCount} row(s) effected!") else _readResultSet(it.resultSet)
    }

    private fun queryForBatch(conn: Connection, sql:String) = conn.createStatement().use { se->
        // 按照分号+换行进行分割
        sql.split(";\n").onEach { se.addBatch(it) }
        se.executeBatch()

        return@use se.updateCount
    }

    private fun _readResultSet(result: ResultSet, withTitle:Boolean = true):List<Any> {
        val list = mutableListOf<Any>()
        val meta = result.metaData
        val cols = meta.columnCount
        if(withTitle && cols > 1)
            list.add((0 until cols).map { i-> meta.getColumnName(i+1) })

        while (result.next()){
            val row = (0 until cols).map { i->
                // val type = meta.getColumnType(i+1)
                result.getObject(i+1)
            }
            list.add(if(row.size == 1) row.first() else row)
        }

        return list
    }

    fun listOfDataBase(model: DbmModel) = withConn(model) { queryForList(it, "SHOW DATABASES")}

    fun listOfTable(model: DbmModel) = withConn(model) { queryForList(it, "SHOW TABLES") }

    /**
     * 列出指定表的列属性，结果如下
     *
     * Field, Type, Null, Key, Default, Extra
     */
    fun tableDetail(model: DbmModel) = withConn(model) { queryForList(it, "DESC `${model.table}`") }

    fun runSQL(model: DbmModel):Any {
        if(model.batch && !settingS.booleanValue(S.DBM_BATCH, true))
            throw Exception("未开启批量执行 SQL")

        return withConn(model) {
            if(model.batch)
                queryForBatch(it, model.sql!!)
            else
                queryForList(it, model.sql!!)
        }
    }

    fun read(model: DbmModel) = withConn(model) {
        val size = settingS.intValue(S.DBM_LIMIT, 200)

        val condition = if(hasText(model.condition)) {
            if(model.condition!!.uppercase().contains(LIMIT))
                model.condition
            else
                "${model.condition} $LIMIT $size"
        } else {
            "1=1 $LIMIT ${settingS.intValue(S.DBM_LIMIT, 200)}"
        }

        val sql = "SELECT ${if(hasText(model.columns)) model.columns else ANY} FROM ${model.table} WHERE $condition"

        queryForList(it, sql)
    }

    fun updateRow(model: DbmModel) = model.obj?.let { ps->
        withConn(model){ con->
            queryForList(
                con,
                "UPDATE ${model.table} SET ${ps.keys.joinToString(COMMA) { "${it}=?" }} WHERE ${model.condition} LIMIT 1",
                ps.values
            )
        }
    }

    fun delRow(model:DbmModel) = withConn(model) {
        queryForList(it, "DELETE FROM ${model.table} WHERE ${model.condition} LIMIT 1")
    }

    @Async
    fun saveLog(log: DatabaseLog) {
        log.addOn = System.currentTimeMillis()

        logM.insert(log)
    }

    /**
     * 判断用户是否具备指定的操作权限
     */
    fun check(user: AuthUser, sourceId:Long, action:String) = CacheManage.get(
        "DBM-${user.id}-$sourceId-$action",
        {
            authM
                .selectList(QueryWrapper<DatabaseAuth>().eq(F.UID, user.id).eq(F.SOURCE_ID, sourceId))
                .any { it.allow == ANY || it.allow.split(COMMA).contains(action) }
                .also {
                    logger.info("判断 ${user.showName} 对 #$sourceId 的 $action 操作权限=$it")
                }
        },
        config.dbmAuthExpire
    )
}