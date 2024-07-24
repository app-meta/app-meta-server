package org.appmeta.domain

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.nerve.boot.annotation.CN


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Outreach
 * CREATE   2023年04月14日 09:23 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 提供快捷登录的终端
 */

@CN("会员终端")
@TableName("member")
class Member : SummaryBean {
    companion object {
        const val CLI		= "cli"		//命令行终端
        const val WORKER    = "worker"  //远程工作者
        const val OTHER 	= "other"	//其他
    }

    var ids     = ""        //允许登录的ID，多个用英文逗号隔开
    var category= CLI       //
    var mode    = 0         //模式，0=默认，1=轮询（目前仅针对 Worker，即客户端轮询查询任务，用于网络无法从服务端到客户端的场景）

    var secret  = ""        //AES 密钥
    var pubKey  = ""
    var priKey  = ""

    var expire  = 60        //令牌有限期，默认是 60 分钟
    var addOn   = 0L

    constructor()
    constructor(ip:String) {
        id  = ip
    }
}

@Mapper
interface MemberMapper:BaseMapper<Member> {

    @Select("SELECT * FROM member WHERE id=#{0} OR uuid=#{0} LIMIT 1")
    fun loadByIdOrUuid(id:String):Member?
}