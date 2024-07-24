package org.appmeta

import com.github.jknack.handlebars.Handlebars
import org.appmeta.domain.Authable
import org.appmeta.domain.EditAuthable
import org.appmeta.domain.ServiceAuthable
import org.appmeta.tool.FileTool
import org.appmeta.tool.GZIPTool
import org.appmeta.tool.OSTool
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import java.io.File
import java.nio.file.Paths
import java.util.*


/*
 * @project app-meta-server
 * @file    org.appmeta.ToolTest
 * CREATE   2023年03月20日 11:00 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class ToolTest {

    @Test
    fun date(){
        println(Date(System.currentTimeMillis() + 50*365*24*60 * 60 * 1000L))
    }

    @Test
    fun unzip(){
        val trace = FileTool.unzip(
            File("target/demo.zip"),
            Paths.get("target", "demo"),
            FileTool.UNZIP_SKIP_ON_EXIST
        )
        trace.forEach { println(it) }
    }

    @Test
    fun gzipTool(){
        val text = "你好，我是集成显卡，代号 AB123"
        val compressed = GZIPTool.compress(text)
        println("压缩：${compressed}")
        val origin = GZIPTool.unCompress(compressed)
        println("解压：${origin}")
        println("验证：${origin == text}")

        val fromJS = "H4sIAAAAAAAAAwEqANX/5L2g5aW977yM5oiR5piv6ZuG5oiQ5pi+5Y2h77yM5Luj5Y+3IEFCMTIzlS/3mioAAAA="
        val origin2= GZIPTool.unCompress(fromJS)
        println("解压：${origin2}")
        println("验证：${origin2 == text}")
    }

    @Test
    fun authablue(){
        val UID = "admin"

        val serviceAuth = object : ServiceAuthable {
            override var serviceAuth    = "U|admin"
            override var uid            = UID
        }
        val editAuth = object : EditAuthable {
            override var editAuth       = "U|admin"
            override var uid            = UID
        }
        val authable = object : Authable {
            override var serviceAuth    = "U|admin"
            override var editAuth       = "U|admin"
            override var uid            = UID
        }

        println(serviceAuth.uid)
        println(editAuth.uid)
        println(authable.uid)
    }


    @Test
    fun exec(){
        fun callAndPrint(cmd:List<String>){
            val result = OSTool.runCommand(cmd)
            println("执行命令：${cmd.joinToString(" ")}")
            println("\tCODE=${result.first}")
            println("\tTEXT=${result.second}")
        }

        callAndPrint(listOf("node","-v"))
        callAndPrint(listOf("pm2", "jlist"))
        callAndPrint(listOf("pm2", "jlist2"))
    }

    @Test
    fun exec2(){
        fun callAndPrint(cmd:List<String>){
            val result = OSTool.runCmd(cmd)
            println("执行命令：${cmd.joinToString(" ")}")
            println("[CODE]=${result.first}")
            println("[TEXT]=${result.second}")
            println()
        }

        callAndPrint(listOf("npm","-v"))
        callAndPrint(listOf("java", "--version"))
        callAndPrint(listOf("pm2", "jlist"))
    }

    @Test
    fun template(){
//        println(
//            Handlebars()
//                .compileInline("Hello，name is {{name}}, age is {{age}}, time is {{time}}")
//                .apply(
//                    mapOf(
//                        "name" to "集成显卡",
//                        "age" to Math.random()*100,
//                        "time" to System.currentTimeMillis()
//                    )
//                )
//        )
        val template = Handlebars().compileInline("Hello，name is {{name}}, age is {{age}}, time is {{time}}")
        (0..10000).forEach {
            println(
                template.apply(
                    mapOf(
                        "name" to "集成显卡",
                        "age" to Math.random()*100,
                        "time" to System.currentTimeMillis()
                    )
                )
            )
        }
    }

    @Test
    fun restTemplate(){
        Regex("[0-9a-zA-Z_.]+").also { r->
            listOf("你哈", "abc-123", "as231_ssa", "admin.first", "ABC-").forEach { println("$it ：${r.matches(it)}") }
        }
    }
}