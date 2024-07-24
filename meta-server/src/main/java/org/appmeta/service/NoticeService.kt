package org.appmeta.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import org.appmeta.F
import org.appmeta.S
import org.appmeta.domain.Notice
import org.appmeta.domain.NoticeLine
import org.appmeta.domain.NoticeLineMapper
import org.appmeta.domain.NoticeMapper
import org.appmeta.tool.AuthHelper
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.setting.SettingService
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.StringUtils.hasText


/*
 * @project app-meta-server
 * @file    org.appmeta.service.NoticeService
 * CREATE   2023年03月22日 14:46 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class NoticeService(
    private val settingS:SettingService,
    private val cacheR:CacheRefresh,
    private val authHelper: AuthHelper, private val lineM: NoticeLineMapper) : BaseService<NoticeMapper, Notice>() {

    fun create(notice: Notice){
        Assert.isTrue(hasText(notice.name) && hasText(notice.summary), "公告标题及内容不能为空")

        notice.addOn = System.currentTimeMillis()

        if(notice.using())
            baseMapper.updateById(notice)
        else
            baseMapper.insert(notice)

        cacheR.noticeList()
    }

    fun findByUser(user:AuthUser):Notice? {
        val notices = baseMapper.loadValid()
        if(notices.isEmpty())   return null

        if(settingS.booleanValue(S.SYS_NOTICE_LINE, false)){
            // 获取用户已经阅读过的公告合集
            val readList= lineM.selectList(
                QueryWrapper<NoticeLine>()
                    .eq(F.UID, user.id)
                    .gt(F.DONE_ON, 0)
                    .`in`(F.OID, notices.map { it.id })
            ).map { it.id }
            // 筛选公告
            val notice = notices.firstOrNull { !readList.contains(it.id) && authHelper.checkService(it, user) }?: return null

            if(lineM.selectCount(QueryWrapper<NoticeLine>().eq(F.UID, user.id).eq(F.OID, notice.id)) <= 0){
                with(NoticeLine(notice)){
                    uid = user.id
                    try{ lineM.insert(this) }catch (e:Exception){}
                }
            }
            return notice
        }
        else {
            return notices.firstOrNull { authHelper.checkService(it, user) }
        }
    }

    fun loadLines(id:Long) = lineM.selectList(QueryWrapper<NoticeLine>().eq(F.OID, id))

    fun afterRead(id: Long, uid:String) {
        if(!settingS.booleanValue(S.SYS_NOTICE_LINE, false))    return

        lineM.update(
            null,
            UpdateWrapper<NoticeLine>().eq(F.OID, id).eq(F.UID, uid).set(F.DONE_ON, System.currentTimeMillis())
        )
        baseMapper.update(null, U().eq(F.ID, id).setSql("${F.LAUNCH} = ${F.LAUNCH}+1"))
    }
}