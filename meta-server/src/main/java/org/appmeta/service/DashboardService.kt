package org.appmeta.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.sun.management.OperatingSystemMXBean
import org.appmeta.Caches
import org.appmeta.Channels
import org.appmeta.F
import org.appmeta.F.DEPART
import org.appmeta.F.ID
import org.appmeta.F.LABEL
import org.appmeta.F.LAUNCH
import org.appmeta.F.NAME
import org.appmeta.F.TEMPLATE
import org.appmeta.F.UID
import org.appmeta.F.VALUE
import org.appmeta.component.AppConfig
import org.appmeta.domain.*
import org.appmeta.model.OverviewResultModel
import org.appmeta.module.dbm.DatabaseSourceMapper
import org.nerve.boot.util.DateUtil
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.lang.management.ManagementFactory
import java.util.*


/*
 * @project app-meta-server
 * @file    org.appmeta.service.DashboardService
 * CREATE   2023年03月30日 11:54 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class DashboardService(
    private val accountS:AccountService,
    private val terminalLogM:TerminalLogMapper,
    private val memberM:MemberMapper,
    private val dbSourceM:DatabaseSourceMapper,
    private val pageM:PageMapper,
    private val pageLinkM:PageLinkMapper,
    private val documentM:DocumentMapper,
    private val launchMapper: PageLaunchMapper,
    private val dataM:DataMapper,
    private val config:AppConfig,
    private val appM: AppMapper) {

    private fun buildAmountItem(label:String, value:Number, suffix:String="个") = mapOf(
        LABEL     to label,
        VALUE     to value,
        "suffix"    to suffix
    )

    @Cacheable(Caches.SYS_OVERVIEW)
    fun overview():OverviewResultModel {
        val memHeap     = ManagementFactory.getMemoryMXBean().heapMemoryUsage
        val memNonHeap  = ManagementFactory.getMemoryMXBean().nonHeapMemoryUsage
        val sys         = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

        val MB          = 1024 * 1024
        return OverviewResultModel(
            listOf(
                buildAmountItem("应用数", appM.selectCount(null)),
                buildAmountItem("页面 / 功能", pageM.selectCount(null)),
                buildAmountItem("关注", pageLinkM.selectCount(QueryWrapper<PageLink>().eq(F.ACTIVE, true)), "则"),
                buildAmountItem("数据量", dataM.selectCount(null), "条"),
                buildAmountItem("文档 / 附件", documentM.selectCount(null), "份"),
                buildAmountItem("终端会员", memberM.selectCount(null)),
                buildAmountItem("数据源", dbSourceM.selectCount(null)),
                buildAmountItem("服务响应", terminalLogM.selectCount(null), "次")
            ),
            pageM
                .selectMaps(QueryWrapper<Page>().select("$TEMPLATE as $LABEL", "count(*) as $VALUE").groupBy(TEMPLATE))
                .associate { Pair(it[LABEL]!!, it[VALUE]) },
            pageM.selectList(QueryWrapper<Page>().select(NAME, LAUNCH).orderByDesc(LAUNCH).last("LIMIT 10"))
                .associate { Pair(it.name, it.launch) },
           mapOf(
               "started"    to DateUtil.formatDate(Date(ManagementFactory.getRuntimeMXBean().startTime), "MM-dd HH:mm"),
               "memory"     to (memHeap.used + memNonHeap.used) / MB,
               "memoryMax"  to (memHeap.max + memNonHeap.max) / MB,
               "threads"    to ManagementFactory.getThreadMXBean().threadCount,
               "os"         to "${System.getProperty("os.name")}/${System.getProperty("os.version")}",
               "osCpu"      to "${sys.arch}/${sys.availableProcessors}核",
               "osMem"      to sys.totalMemorySize / MB,
               "osMemFree"  to sys.freeMemorySize / MB,
               "jdk"        to System.getProperty("java.version")
           )
        )
    }

    /**
     * 对指定的应用进行总览统计
     * 1、计算最近 15 天的流量趋势（分不同终端类型）
     * 2、统计最多使用的前 5 个页面/用户
     */
    @Cacheable(Caches.APP_OVERVIEW)
    fun ofApp(aid:String): Map<String, Any> {
        val cfg = config.dashboard

        val time = System.currentTimeMillis() - cfg.daySpan * 24 * 60 * 60 * 1000L
        val days = Date().let{ d->(-cfg.daySpan+1 ..  0).map { DateUtil.formatDate(DateUtil.addDays(d, it)) }}
        val buildQ = {
            QueryWrapper<PageLaunch>().also { q->
                if(StringUtils.hasText(aid))    q.eq(F.AID, aid)
            }
        }
        val groupBy:(String)->QueryWrapper<PageLaunch> = { key ->
            buildQ()
                .select("$key as $ID", "count(*) as $VALUE")
                .groupBy(key)
                .orderByDesc(VALUE)
                .last("LIMIT ${cfg.top}")
        }

        return mapOf(
            "date"      to DateUtil.getDateTime(),
            "days"      to days,
            "data"      to launchMapper.selectList(
                            buildQ().ge(F.ADD_ON, time)
                        ).let { launchs->
                            val dayDatas = launchs.groupBy { DateUtil.formatDate(Date(it.addOn)) }
                            println(days)
                            mutableListOf<Map<String, Any>>().also { list->
                                listOf(Channels.BROWSER, Channels.CLIENT, Channels.MOBILE, Channels.CLI).map { name->
                                    list.add(
                                        mapOf(
                                            NAME  to name,
                                            "data"  to days.map {
                                                if(dayDatas.containsKey(it)) dayDatas[it]!!.filter { l->l.channel==name }.size else 0
                                            }
                                        )
                                    )
                                }
                                list.add(0, mapOf(
                                    NAME  to "ALL",
                                    "data"  to days.map { if(dayDatas.containsKey(it)) dayDatas[it]!!.size else 0  }
                                ))
                            }
                        },
            "topUser"   to launchMapper.selectMaps(groupBy(UID)).map { v->
                val id = v[ID] as String
                v[ID] = "${accountS.getNameById(id)}(${id})"
                v
            },
            "topDepart" to launchMapper.selectMaps(groupBy(DEPART)),
            "topPage"   to pageM.selectMaps(
                QueryWrapper<Page>().also { q->
                    if(StringUtils.hasText(aid))    q.eq(F.AID, aid)
                    q.select("$NAME as $ID", "$LAUNCH as $VALUE")
                    .orderByDesc(LAUNCH)
                    .last("LIMIT ${cfg.top}")
                }
            )
        )
    }
}