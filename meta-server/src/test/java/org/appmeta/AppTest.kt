package org.appmeta

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter.Feature.*
import org.nerve.boot.domain.AuthUser
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AppTest {
	protected val logger = LoggerFactory.getLogger(javaClass)

	protected val UID	= "admin"
	protected val UNAME	= "测试管理员"
	protected val AID 	= "TEST"
	protected val AID_DEMO	= "demo"

	protected fun getUser(uid:String = UID): AuthUser {
		val user = AuthUser()
		user.id = uid
		user.roles = listOf()
		user.name = UNAME
		user.ip	= "127.0.0.1"

		return user
	}

	fun json(obj:Any?, pretty:Boolean=false) {
		if(pretty)
			println(JSON.toJSONString(obj, FieldBased, WriteMapNullValue, PrettyFormat))
		else
			println(JSON.toJSONString(obj, FieldBased, WriteMapNullValue))
	}
}
