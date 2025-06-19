package org.appmeta.module

import jakarta.servlet.http.HttpServletResponse
import org.appmeta.S
import org.appmeta.model.FieldModel
import org.nerve.boot.Const
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.MD5Util
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


/*
 * @project app-meta-server
 * @file    org.appmeta.module.ShortUrl
 * CREATE   2023年11月02日 13:40 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

interface ShortUrlProvier {
    fun get(uuid:String):String?

    fun exit(uuid: String):Boolean

    fun add(uuid: String, url:String)
}

//@CN("短链接")
//@TableName
//class ShortUrl:StringEntity {
//    var url     = ""
//
//    constructor()
//    constructor(id:String, url:String){
//        setId(id)
//        this.url = url
//    }
//}
//
//@Mapper
//interface ShortUrlMapper:BaseMapper<ShortUrl>
//
//@Component
//class ShortUrlDbProvider(private val mapper: ShortUrlMapper):ShortUrlProvier {
//
//    override fun get(uuid: String) = mapper.selectById(uuid)?.url
//
//    override fun exit(uuid: String) = mapper.exists(QueryWrapper<ShortUrl>().eq(F.ID, uuid))
//
//    override fun add(uuid: String, url: String) {
//        mapper.insert(ShortUrl(uuid, url))
//    }
//}

@Component
class ShortUrlFileProvider:ShortUrlProvier {
    /**
     * 默认使用同目录下的 routes 作为数据存储目录
     */
    private val dataPath = Paths.get("shorturl.txt")
    private val map = mutableMapOf<String, String>()

    private fun loadFromFile() = Files.readAllLines(dataPath).forEach{ line->
        val t = StringUtils.split(line, Const.SPACE)?: return@forEach
        map[t[0]] = t[1]
    }

    override fun get(uuid: String): String? {
        if(map.isEmpty())   loadFromFile()
        return map[uuid]
    }

    override fun exit(uuid: String) = map.containsKey(uuid)

    override fun add(uuid: String, url: String) {
        map[uuid] = url

        Files.write(
            dataPath,
            map.map { "${it.key} ${it.value}" },
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }
}

@Order(Int.MAX_VALUE)
@RestController
@RequestMapping("s")
class ShortUrlCtrl(private val settingS:SettingService,private val provier: ShortUrlProvier):BasicController() {
    @Value("\${server.servlet.context-path}")
    private val contextPath = ""

    @PostMapping("create", name = "生成短链接")
    fun create(@RequestBody model:FieldModel) = resultWithData {
        Assert.isTrue(model.key.startsWith("/"), "长链接必须以 / 开头")

        var uuid = if(model.id is String) (model.id as String).trim() else EMPTY
        if(!StringUtils.hasText(uuid))
            uuid = MD5Util.encode(model.key).substring(8, 16)

        if(provier.exit(uuid)){
            val url = provier.get(uuid)
            if(url != model.key)
                throw Exception("短链接 $uuid 已存在：$url")
        }
        else {
            provier.add(uuid, model.key)
            logger.info("创建短链接 $uuid >> ${model.key}")
        }

        uuid
    }

    @GetMapping("{uuid}", name = "短链接自动跳转")
    fun shortUrl(@PathVariable uuid:String, response: HttpServletResponse) {
        val url = provier.get(uuid)?: throw Exception("短链接不存在")

        logger.info("短链接跳转 $uuid >> $url")
        settingS.value(S.SYS_HOST).also { host->
            val redirect = "${host?: EMPTY}${contextPath}${url}"
            if(logger.isDebugEnabled)   logger.debug("SYS_HOST=$host，短链接 $uuid 跳转完整地址 $redirect")
            response.sendRedirect(redirect)
        }
    }
}