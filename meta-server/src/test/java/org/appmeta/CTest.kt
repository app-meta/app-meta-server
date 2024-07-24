package org.appmeta

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import org.appmeta.component.AppConfig
import org.appmeta.component.SystemConfig
import org.junit.jupiter.api.Test
import org.nerve.boot.util.AESProvider
import org.nerve.boot.util.DateUtil
import java.util.*


/*
 * @project app-meta-server
 * @file    org.appmeta.CTest
 * CREATE   2022年12月08日 08:58 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class CTest {
    val config = AppConfig()

    @Test
    fun buildVersion(){
        println(H.buildVersion())

        listOf("AB","ABC123","AB_2","AB2341231214354542121313214343", "AB-123").onEach {
            println(it+">"+Regex(config.appIdRegex).matches(it))
        }
    }

    @Test
    fun matchMDImage(){
        Regex("!\\[.*?\\]\\((.*?)\\)").let { reg->
            val text = reg.replace("""
                # Markdown 语法
                > 最近更新：集成显卡
                
                ![](http://cdn.aliyun.com/a.png)
                
                截图如下：
                
                ![b.png](http://cdn.aliyun.com/b.png)
            """.trimIndent()) { result->
                val url = result.groupValues.last()
                "![](data:image/webp:base64,${url})"
            }

            println(text)
        }
    }

    @Test
    fun days(){
        println(System.currentTimeMillis() - 30*24*60*60*1000)
        println(System.currentTimeMillis())
        println(Date().let{ d->(-10 ..  0).map { DateUtil.formatDate(DateUtil.addDays(d, it)) }})
    }

    @Test
    fun createJwtKey(){
        val secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512)
        val keyString = Encoders.BASE64.encode(secretKey.encoded)

        println(keyString)
    }

    @Test
    fun createAESKey(){
        val provider = AESProvider()
        println(provider.creatKey())

        println("密钥：${provider.key}")
        val msg = provider.encrypt("ABC 集成显卡")
        println("密文：$msg")
        println("明文：${provider.decrypt(msg)}")
    }

    @Test
    fun tes(){
        println(AESProvider().decrypt("ebOdm2ZzhQIVvKvwG8bAJw==", config.dbmKey))
    }
}