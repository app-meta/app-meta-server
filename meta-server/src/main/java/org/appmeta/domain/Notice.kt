package org.appmeta.domain

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.appmeta.ALL
import org.appmeta.Caches
import org.nerve.boot.annotation.CN
import org.nerve.boot.domain.IDLong
import org.nerve.boot.util.DateUtil
import org.springframework.cache.annotation.Cacheable


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Noticing
 * CREATE   2023年03月22日 13:08 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@CN("公告")
@TableName("notice")
class Notice : LongUserBean, ServiceAuthable {
    companion object {
        const val NOTICE    = "notice"      //占位显示
        const val DIALOG    = "dialog"      //对话框展示
    }

    var mode    = NOTICE
    override var serviceAuth: String    = ALL
    var fromDate= ""
    var toDate  = ""
    var launch  = 0
    var addOn   = 0L

    constructor()
    constructor(newId:Long) {
        id      = newId
    }
}

@CN("公告阅读记录")
@TableName("notice_line")
class NoticeLine : IDLong {
    var uid     = ""
    var oid     = 0L
    var doneOn  = 0L
    var addOn   = 0L

    constructor()
    constructor(n:Notice){
        oid     = n.id
        addOn   = System.currentTimeMillis()
    }
}

@Mapper
interface NoticeMapper:BaseMapper<Notice> {

    @Cacheable(Caches.NOTICE_LIST)
    @Select("SELECT * FROM notice WHERE fromDate <= #{0} AND toDate >= #{0} ORDER BY id DESC")
    fun loadValid(day:String = DateUtil.getDate()):List<Notice>
}
@Mapper
interface NoticeLineMapper:BaseMapper<NoticeLine>