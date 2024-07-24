package org.appmeta.web.system

import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.input.ReversedLinesFileReader
import org.appmeta.*
import org.appmeta.component.SystemConfig
import org.appmeta.model.FieldModel
import org.appmeta.model.KeyModel
import org.appmeta.model.SizeModel
import org.appmeta.model.SwitchModel
import org.appmeta.tool.AuthHelper
import org.appmeta.tool.FileTool
import org.appmeta.tool.OSTool
import org.nerve.boot.Const.COMMA
import org.nerve.boot.FileStore
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.DateUtil
import org.nerve.boot.web.ctrl.BasicController
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


/*
 * @project app-meta-server
 * @file    org.appmeta.web.system.SystemCtrl
 * CREATE   2023年05月23日 17:16 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

open class BasicSystemCtrl:BasicController() {
    @Resource
    protected lateinit var authHelper: AuthHelper

    fun adminAndWhiteIP(): AuthUser {
        val user = authHolder.get()
        authHelper.checkAdminAndWhiteIP(user)

        return user
    }
}

@Service
class SystemHelper {
    val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun restart() = AppMetaServer.restart()
}

@RestController
@RequestMapping("system")
class SystemCtrl(
    private val fileStore: FileStore,
    private val helper: SystemHelper, private val sysConfig:SystemConfig) : BasicSystemCtrl() {

    /**
     * 更新客户端程序包，同意保存到 attach/client 目录下
     * 对于全量包，名称为 meta-client.7z
     * 增量包则是 yyyyMMddHHmmss.7z
     *
     * @param file  文件包
     * @param increment 是否为增量包（常用于 electron-builder 更新包）
     */
    @PostMapping("update-client", name = "更新客户端程序包")
    fun updateClient(
        @RequestPart("file") file:MultipartFile,
        @RequestParam("increment", required = false, defaultValue = "false") increment:Boolean=false
    ) = resultWithData {
        adminAndWhiteIP().let { user->
            val ext = FileTool.checkExt(file.originalFilename!!, "7z", "zip")
            val path = fileStore.buildPathWithoutDate("${if(increment) DateUtil.getDateTimeSimple() else "meta-client"}.${ext}", "client")
            if(!Files.exists(path.parent))
                Files.createDirectories(path.parent)

            FileUtils.copyToFile(file.inputStream, path.toFile())
            opLog("${user.showName}在${requestIP}上传客户端程序包（增量=${increment}），保存到$path", null, Operation.IMPORT)
            "客户端程序包更新完成，下载地址为 $path"
        }
    }

    @RequestMapping("restart", name = "重启后端")
    fun restart() = result {
        val user = adminAndWhiteIP()
        opLog("${user.showName} 在 $requestIP 申请重启后端服务...", null, Operation.MODIFY)

        helper.restart()
    }

    @PostMapping("update-jar", name = "更新主程序 JAR")
    fun updateJAR(@RequestPart("file") file: MultipartFile) = result { re->
        Assert.isTrue(sysConfig.enableJar, "未开启更新 JAR 功能")

        val user = adminAndWhiteIP()
        opLog("${user.showName} 在 $requestIP 上传 ${file.originalFilename}(size=${file.size}) 以更新后端 JAR...", null, Operation.MODIFY)

//        val ext = FilenameUtils.getExtension(file.originalFilename)
//        Assert.isTrue(ext.uppercase() == "JAR", "文件格式不支持")
        FileTool.checkExt(file.originalFilename!!, "jar")

        val jarFile = Paths.get(sysConfig.jarName).also {
            if(Files.exists(it)){
                val bkFile = Paths.get("${sysConfig.jarName}.bk")
                Files.copy(it, bkFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
                logger.info("复制当前 jar 到 $bkFile")
            }
        }

        /*
        经过多次试验，发现通过 FileOutputStream 的方式可以正常写入内容

        方式一：
        FileUtils.copyToFile(file.inputStream, jarFile.toFile())

        方式二：
        Files.copy(file.inputStream, newFile, StandardCopyOption.REPLACE_EXISTING)
         */
        FileOutputStream(jarFile.toFile()).use { f->
            val fis = BufferedInputStream(file.inputStream)
            val buffer = ByteArray(10*1024*1024)
            var len: Int
            val total = file.size
            var current = 0

            while (
                run {
                    len = fis.read(buffer)
                    len
                } != -1
            ) {
                current += len
                logger.info("[COPY] %-10d / %-10d (READ %-8d)".format(current, total, len))
                f.write(buffer, 0, len)
            }
            f.flush()
            fis.close()

            /*
            写入到更新日志文件中
             */
            FileOutputStream(sysConfig.verLogOfJar, true).use { logFOS->
                BufferedWriter(OutputStreamWriter(logFOS, Charsets.UTF_8)).use { bw->
                    bw.newLine()

                    val line = "%s %-8s %s".format(DateUtil.getDateTime(), user.id, "${file.size/1024} KB")
                    bw.write(line)

                    logger.info("追加到更新日志 ${sysConfig.verLogOfJar}：$line")
                    re.data = line
                }
            }
            logger.info("${user.showName} 更新 JAR 包 ${sysConfig.jarName}")
        }
    }

    @PostMapping("log", name = "下载最新的日志文件")
    fun downloadLog(response: HttpServletResponse) {
        val user = adminAndWhiteIP()
        val logFile = Paths.get(sysConfig.logFile)
        if(Files.notExists(logFile))
            throw Exception("更新记录文件 ${sysConfig.logFile} 不存在")
        else
            downloadFile(response, logFile.toFile(), "meta-server-${DateUtil.getDateTimeSimple()}.log") {
                opLog("${user.showName} 在 $requestIP 下载运行时日志", null, Operation.EXPORT)
            }
    }

    @PostMapping("log-version", name = "查看最近的更细记录")
    fun versionLog(@RequestBody model: SizeModel) = resultWithData {
        adminAndWhiteIP()

        val p = Paths.get(sysConfig.verLogOfJar)
        if(Files.exists(p))
            ReversedLinesFileReader(p, Charsets.UTF_8).use { it.readLines(model.size) }
        else
            throw Exception("版本记录文件 ${sysConfig.verLogOfJar} 不存在")
    }
}

@RestController
@RequestMapping("system/cache")
class CacheCtrl(private val cacheManager:CaffeineCacheManager):BasicSystemCtrl() {

    @RequestMapping("list", name = "缓存列表")
    fun list() = resultWithData {
        cacheManager.cacheNames.map { name->
            mapOf(
                F.NAME      to name,
                "size"      to (cacheManager.getCache(name) as CaffeineCache).nativeCache.estimatedSize()
            )
        }
    }

    @RequestMapping("clean", name = "清空指定缓存")
    fun clean(@RequestBody model:FieldModel) = resultWithData {
        model.key.split(COMMA).map { it.trim() }.onEach { n->
            cacheManager.getCache(n)?.let {
                val id = model.id.toString()
                if(StringUtils.hasText(id))
                    it.evict(id)
                else
                    it.clear()

                logger.info("清除 NAME=${n} 的缓存（ID=${model.id}）...")
            }
        }
    }
}

@RestController
@RequestMapping("system/os")
class OSFuncCtrl:BasicSystemCtrl() {

    @Value("\${server.port}")
    private var mainPort:String = "8080"

    @PostMapping("port", name = "检测端口是否被占用")
    fun checkPort(@RequestBody model: SwitchModel) = resultWithData {
        val user    = adminAndWhiteIP()

        val pid     = OSTool.findPIDByPort(model.key)
        val hasPid  = pid.isNotBlank()
        if(model.enable){
            if(model.key == mainPort)   throw Exception("端口 ${model.key} 为主应用，不可操作")

            if(hasPid){
                OSTool.killProcess(pid).also {
                    "${user.showName} 关闭端口 ${model.key} 的关联进程#${pid} ...".also { msg->
                        logger.info(msg)
                        opLog(msg, null, Operation.MODIFY)
                    }
                }
            }
            else{
                throw Exception("端口 ${model.key} 未被占用")
            }
        }
        //只是检测
        else{
            logger.info("${user.showName} 检测端口 ${model.key} 进程，PID=$pid")
            hasPid
        }

    }
}