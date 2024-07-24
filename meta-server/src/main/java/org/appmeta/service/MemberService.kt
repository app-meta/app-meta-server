package org.appmeta.service

import org.appmeta.F
import org.appmeta.domain.Member
import org.appmeta.domain.MemberMapper
import org.appmeta.tool.JWTTool
import org.nerve.boot.Const.COMMA
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.util.AESProvider
import org.springframework.stereotype.Service
import org.springframework.util.AntPathMatcher
import org.springframework.util.Assert
import org.springframework.util.StringUtils.hasText


/*
 * @project app-meta-server
 * @file    org.appmeta.service.MemberService
 * CREATE   2023年04月14日 09:55 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class MemberService(private val jwtTool: JWTTool) : BaseService<MemberMapper, Member>() {

    val matcher = AntPathMatcher()

    fun createToken(ip:String, text:String) = getById(ip).let { member->
        if(logger.isDebugEnabled)   logger.debug("来自 $ip 的密文 $text （授权会员信息：IP=${member.id} EXPIRE=${member.expire} IDS=${member.ids}）")
        if(member == null) throw Exception("$ip 不是授权的终端（请联系平台管理员授权）")

        AESProvider().decrypt(text, member.secret).let { origin->
            if(!hasText(origin))    throw Exception("数据解密失败")
            if(logger.isDebugEnabled) logger.info("解密为 $origin")

            val tmp = origin.split("-")
            Assert.isTrue(tmp.size == 2 && hasText(tmp[0]) && hasText(tmp[1]), "登录参数格式有误")
            val time = tmp[1].toLong()
            Assert.isTrue(System.currentTimeMillis() - time <= 180 * 1000, "该密文信息已过期")

            val uid = tmp[0]
            //判断是否为允许登录的ID
            member.ids.split(COMMA).firstOrNull { matcher.match(it.trim(), uid) }?: throw Exception("$uid 不允许通过此终端登录")

            logger.info("$uid 在终端 $ip 授权登录，有效期 ${member.expire} 分钟")
            jwtTool.create(uid, ip, member.expire)
        }
    }

    fun add(bean: Member) {
        Assert.hasText(bean.id, "终端IP必须填写")
        Assert.isTrue(bean.expire <= 1440, "有限期不能超过 24 小时")

        if(count(Q().eq(F.ID, bean.id))<=0){
            bean.addOn = System.currentTimeMillis()
            baseMapper.insert(bean)
        }
        else
            baseMapper.updateById(bean)
    }
}