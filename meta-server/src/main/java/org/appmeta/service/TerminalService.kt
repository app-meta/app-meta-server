package org.appmeta.service

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO
import org.appmeta.Caches
import org.appmeta.F
import org.appmeta.component.AppConfig
import org.appmeta.domain.*
import org.appmeta.model.TerminalLogModel
import org.appmeta.model.TerminalLogOverview
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Pagination
import org.nerve.boot.db.service.QueryHelper
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils.hasText
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.exists


/*
 * @project app-meta-server
 * @file    org.appmeta.service.TerminalService
 * CREATE   2023年04月06日 10:41 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 应用后端服务
 */

@Service
class TerminalService(
    private val config:AppConfig,
    private val detailM:TerminalLogDetailMapper,
    private val logM:TerminalLogMapper, private val pageM:PageMapper) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val queryHelper = QueryHelper<TerminalLog>()

    /**
     * 判断应用是否能使用对应的端口，不能重复
     */
    fun checkPortUsable(aid:String, port:Int): Boolean {
        val terminals = pageM.selectObjs<String>(
                QueryWrapper<Page>().eq(F.TEMPLATE, Page.SERVER).ne(F.AID, aid).select(F.CONTENT)
            )
            .map { JSON.parseObject(it.toString(), Terminal::class.java) }

        return terminals.firstOrNull { it.mode == Terminal.INSIDE && it.port == port } == null
    }

    @Cacheable(Caches.PAGE_SERVER)
    fun load(aid: String): Terminal {
        val page = loadServer(aid, true)
        return JSON.parseObject(page.content, Terminal::class.java).also {  it.pid = "${page.id}" }
    }

    fun loadServer(aid: String, withContent:Boolean = false): Page {
        val q = QueryWrapper<Page>().eq(F.AID, aid).eq(F.TEMPLATE, Page.SERVER)
        if(withContent)
            q.select(F.CONTENT, F.ID)

        return pageM.selectOne(q)?: throw Exception("应用[${aid}]未开通后端服务或者未初始化")
    }

    fun logList(params:Map<String, Any>, pagination: Pagination, aid:String=EMPTY, pid:String= EMPTY): List<TerminalLog> {
        val p = PageDTO<TerminalLog>(
            pagination.page.toLong(),
            pagination.pageSize.toLong()
        )
        val q = queryHelper.buildFromMap(params)
        if(hasText(aid))    q.eq(F.AID, aid)
        if(hasText(pid)){
            // 查询 FaaS 记录
            q.eq(F.HOST, pid).eq(F.CODE, 0)
        }
        q.orderByDesc(F.ID)

        logM.selectPage(p, q)
        pagination.total = p.total
        return p.records
    }

    /**
     * 统计当日的数据
     */
    @Cacheable(Caches.PAGE_TERMINAL)
    fun logOverview(aid: String): TerminalLogOverview {
        var total   = 0L
        var today   = 0L
        var used    = 0L
        var error   = 0L

        val time    = with(Calendar.getInstance()) {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)

            timeInMillis
        }
        logM.streamByAid(aid) { c->
            val log = c.resultObject
            total   ++
            used += log.used
            if(log.code != 200) error ++
            if(log.addOn >= time) today ++
        }

        return TerminalLogOverview(aid, total, today, used, error)
    }

    fun loadLog(id: Long) = logM.selectById(id)

    fun loadLogDetail(id: Long) = detailM.selectById(id)

    fun loadLast(model:TerminalLogModel):TerminalLog? = logM.selectOne(
        QueryWrapper<TerminalLog>().also { q->
            q.eq(hasText(model.aid),    F.AID, model.aid)
            q.eq(hasText(model.uid),    F.UID, model.uid)
            q.eq(hasText(model.path),   F.URL, model.path)
            q.eq(hasText(model.ip),     F.IP, model.ip)
            q.eq(hasText(model.channel),F.CHANNEL, model.channel)

            q.orderByDesc(F.ID)
            q.last("LIMIT 1")
        }
    )

    fun resolvePath(aid:String, dir:String): Path {
        val p = if(hasText(dir)) dir else ""
        val root = Paths.get(config.terminalPath, aid)
        val path = root.resolve(p)

        if(!path.startsWith(root))    throw Exception("非法路径 $p")

        if(!path.exists())  throw Exception("应用#${aid}不存在文件/目录：$path")

        return path
    }
}