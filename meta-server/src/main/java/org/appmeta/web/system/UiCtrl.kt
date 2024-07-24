package org.appmeta.web.system

import org.apache.commons.io.FileUtils
import org.appmeta.component.AppConfig
import org.nerve.boot.util.MD5Util.encode
import org.nerve.boot.util.Timing
import org.springframework.util.FileSystemUtils
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream


@RestController
@RequestMapping("system/ui")
class UiCtrl(private val config: AppConfig) : BasicSystemCtrl(){

    /**
     * 上传前端压缩文件
     * @param   file
     */
    @RequestMapping("upload")
    fun uploadMain(@RequestPart("file") file:MultipartFile) = result { re->
        val user = adminAndWhiteIP()

        val timing = Timing()
        val zipFile = getStaticFile()

        FileUtils.copyToFile(file.inputStream, zipFile)

        logger.info("${user.showName} 上传前端资源，保存到：{} （原始文件名={}）", zipFile.absolutePath, file.originalFilename)

        re.data = unzipDo(config.resZipKeep)
        re.message = "处理完成，耗时 ${timing.toSecondStr()} 秒！"
    }

    @RequestMapping("unzip")
    fun unzip(keep:Boolean=true):String {
        return unzipDo(keep)
    }

    private val FAIL = "文件检验不通过"
    private val SIGN = "SIGN"

    /**
     * 返回 SIGN 文件中的清单列表
     */
    fun checkZipFile(file:File): List<String> {
        val zipFile = ZipFile(file)

        val signEntry = zipFile.getEntry(SIGN) ?: throw Exception(FAIL)

        val zis = zipFile.getInputStream(signEntry)
        val sign = StreamUtils.copyToString(zis, StandardCharsets.UTF_8)
        if(sign.trim().isEmpty())   throw Exception(FAIL)

        val c = Calendar.getInstance()
        var fileMd5 = encode("${c[Calendar.YEAR]}-${c[Calendar.MONTH]+1}-${c[Calendar.DAY_OF_MONTH]}")

        val signList = sign.split("\n").map { it.trim().split(" ") }
        signList.forEach { v->
            val entry = zipFile.getEntry(v[0])
            val code = if(entry == null) encode(v[0]) else encode(zipFile.getInputStream(entry))
            fileMd5 = encode(code + fileMd5)
            if(fileMd5 != v[1])
                throw Exception("${FAIL}:${v[0]}")
        }

        zipFile.close()
        return signList.map { it[0] }
    }

    /**
     * 更新前端资源
     * 1. 将前端打包后的 static.zip 上传到 jar 同目录下
     * 2. 在浏览器中调用此接口接口
     *
     * 处理流程：
     * 1. 判断 static.zip 是否存在，若不存在则中断操作
     * 2. 解压压缩包到 lib/public 下
     * 3. 返回处理结果
     */
    fun unzipDo(keep:Boolean=true): String{
        logger.info("即将开始解压前端资源(保留原文件=$keep})...")
        val zipFile = getStaticFile()
        val joiner = StringJoiner("<br>")

        if(zipFile.exists() && zipFile.isFile){
            val signFiles =  if(config.resZipCheck) checkZipFile(zipFile) else emptyList()

            val targetPath = with(signFiles) {
                var dir = config.resPath
                // 如果 @@ 开头则解压到 www 目录
                if(signFiles.isNotEmpty() && signFiles.first().startsWith("@@")){
                    dir = "./${config.resAppPath}/${signFiles.first().substring(2)}"
                }
                logger.info("解压到 {}", dir)
                Paths.get(dir)
            }

            val zipIs = ZipInputStream(FileInputStream(zipFile))
            FileSystemUtils.deleteRecursively(targetPath)

            val targetFolder = targetPath.toFile()

            var entry: ZipEntry? = zipIs.nextEntry
            while(entry!=null){
                val file = File(targetFolder, entry.name)
                if(!file.parentFile.exists())   file.parentFile.mkdirs()

                val msg = "inflating:\t$file"
                if(logger.isDebugEnabled)   logger.debug(msg)
                joiner.add(msg)

                if(entry.isDirectory)
                    file.mkdir()
                else{
                    val fileOut = FileOutputStream(file)
                    fileOut.write(zipIs.readBytes())
                    fileOut.close()
                }

                zipIs.closeEntry()
                entry = zipIs.nextEntry
            }
            zipIs.close()

            joiner.add("Done!")
            logger.info("前端资源解压完成!")

            FileSystemUtils.deleteRecursively(targetPath.resolve(SIGN))

            if(!keep){
                FileSystemUtils.deleteRecursively(zipFile)
                joiner.add("Origin file deleted...")
            }
        }else
            joiner.add("资源包 ${config.resZipFile} 不存在或者不是有效的文件!")

        return joiner.toString()
    }

    private fun getStaticFile() = Paths.get(config.resZipFile).toFile()
}