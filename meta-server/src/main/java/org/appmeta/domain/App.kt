package org.appmeta.domain

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import jakarta.validation.constraints.NotBlank
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update
import org.appmeta.Caches
import org.appmeta.H
import org.nerve.boot.annotation.CN
import org.nerve.boot.db.StringEntity
import org.springframework.cache.annotation.Cacheable
import java.io.Serializable


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.App
 * CREATE   2022年12月05日 17:44 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@CN("应用")
class App : UserBean, Launchable {
	companion object {
		const val FAST		= 0		//快应用
		const val EXTRA 	= 2		//外联应用
	}

	var active 		= false
	var offline 	= false			//是否下线
	var abbr 		= ""
	var category	= FAST
	var author 		= ""
	var addOn 		= 0L

	override var launch 	= 0
	var mark 		= 0
	var thumb 		= 0

	constructor()
	constructor(id:String) {
		setId(id)
	}
}

@CN("应用属性")
@TableName("app_property")
class AppProperty : StringEntity() {
	/*
	窗口设置
	 */
	var winFrame	= true
	var winMax 		= false
	var winWidth	= 920
	var winHeight 	= 480

	var native 		= false		//是否需要在原生环境下运行，勾选后，如果在纯 WEB 环境下会报错


	fun bind(app: App) {
		id = app.id
	}
}

@CN("应用版本")
@TableName("app_version")
class AppVersion : AppWithUser {
	var pid 	= ""
	@NotBlank(message = "版本号 Y.M.D 不能为空")
	var version = ""
	var summary = ""
	var path 	= ""
	var size 	= 0L
	var addOn 	= 0L

	constructor()
	constructor(aid:String) {
		this.aid 	= aid
		this.addOn 	= System.currentTimeMillis()
	}
}

@CN("应用角色")
@TableName("app_role")
class AppRole {
	var aid		= ""
	var uuid 	= ""
	var name 	= ""
	var auth 	= ""
	var ip		= ""
	var summary = ""
	var addOn 	= 0L

	fun authList() 	= H.splitToList(auth)
	fun ipList() 	= H.splitToList(ip)
}

@CN("应用角色关联")
@TableName("app_role_link")
class AppRoleLink {
	var aid 	= ""
	var uid 	= ""
	var role 	= ""

	fun roleList() 	= H.splitToList(role)
}


@CN("应用关联")
@TableName("app_link")
class AppLink:AppWithUser {
	companion object {
		const val MARK = 0
		const val LIKE = 1
	}

	var type 	= MARK

	constructor()
	constructor(aid:String, uid:String, type:Int = MARK){
		this.aid		= aid
		this.uid		= uid
		this.type		= type
	}
}

@CN("应用日志")
@TableName("app_log")
class AppLog:AppWithUser {
	constructor()
	constructor(aid: String, uid:String, msg:String, channel:String){
		this.aid		= aid
		this.uid		= uid
		this.msg		= msg
		this.channel	= channel
		this.addOn		= System.currentTimeMillis()
	}

	var msg 	= ""
	var channel = ""
	var addOn 	= 0L
}


@Mapper
interface AppMapper:BaseMapper<App> {

	/**
	 * 字符串替换
	 *
	 * 默认情况下，使用#{}格式的语法会导致MyBatis创建预处理语句属性并以它为背景设置安全的值（比如?）。这样做很安全，很迅速也是首选做法，
	 * 有时你只是想直接在SQL语句中插入一个不改变的字符串。比如，像ORDER BY，你可以这样来使用：
	 *
	 * ORDER BY ${columnName}
	 *
	 * 这里MyBatis不会修改或转义字符串。
	 *
	 * 重要：接受从用户输出的内容并提供给语句中不变的字符串，这样做是不安全的。这会导致潜在的SQL注入攻击，因此你不应该允许用户输入这些字段，
	 * 或者通常自行转义并检查。
	 *
	 * 总的来说，就是order by 传参的时候要用$符号，用#不生效（尽管你在控制台看到的生成语句是正确的，但是结果就是不对！
	 */
	@Select("SELECT * FROM app WHERE active=1 AND offline=0 ORDER BY \${field} DESC LIMIT #{size}")
	fun loadOrderBy(@Param("field") field:String,@Param("size") size:Int=10):List<App>

	@Cacheable(Caches.APP)
	@Select("SELECT * FROM app WHERE id=#{0}")
	fun withCache(id: Serializable):App?

	@Update("UPDATE app SET launch=launch+#{size} WHERE id=#{id}")
	fun updateLaunch(@Param("id") id: Serializable, @Param("size") size:Int)
}

@Mapper
interface AppVersionMapper:BaseMapper<AppVersion> {
	@Select("SELECT * FROM app_version WHERE  aid=#{aid} AND pid=#{pid} ORDER BY id DESC LIMIT #{size}")
	fun latestBy(@Param("aid") aid:String, @Param("pid") pid:Serializable, @Param("size") size:Int=10):List<AppVersion>
}

@Mapper
interface AppPropertyMapper:BaseMapper<AppProperty>

/**
 * 由于采用了复合主键，xxxById 接口将无法正常使用
 */
@Mapper
interface AppRoleMapper:BaseMapper<AppRole> {
	@Select("SELECT * FROM app_role WHERE aid=#{aid} AND uuid=#{uuid} LIMIT 1")
	fun load(@Param("aid") aid:String, @Param("uuid") uuid:String):AppRole?
}

@Mapper
interface AppRoleLinkMapper:BaseMapper<AppRoleLink> {
	@Select("SELECT * FROM app_role_link WHERE aid=#{aid} AND uid=#{uid} LIMIT 1")
	fun load(@Param("aid") aid:String, @Param("uid") uid:String):AppRoleLink?
}

@Mapper
interface AppLinkMapper:BaseMapper<AppLink>

@Mapper
interface AppLogMapper:BaseMapper<AppLog>