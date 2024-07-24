package org.appmeta.component.func

import com.luciad.imageio.webp.WebPWriteParam
import net.coobird.thumbnailator.Thumbnails
import org.appmeta.component.PageContentUpdateEvent
import org.appmeta.domain.Page
import org.nerve.boot.FileStore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.io.path.*


/*
 * @project app-meta-server
 * @file    org.appmeta.component.func.MarkdownFunc
 * CREATE   2023年08月09日 11:07 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Configuration
@ConfigurationProperties(prefix = "page.markdown")
class MarkdownConfig {
    var maxWidth        = 900       //图片宽度超出此值则进行压缩
    var quality         = 0.1F      //转换为 webp 时质量阈值
    var resizeQuality   = 0.8f      //裁剪图片的质量阈值
    var exts            = listOf("jpg","jpeg","bmp","png")
    var dir             = "markdown"
}

@Component
class MarkdownFunc(
    private val fileStore: FileStore,
    private val config: MarkdownConfig) {

    @Value("\${server.servlet.context-path}")
    private val contextPath = ""

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 转换为 Base64 编码
     */
    private fun base64(bytes:ByteArray) = "![](data:image/webp;base64,${Base64.getEncoder().encodeToString(bytes)})"

    private fun txtFile(id: Long) = fileStore.buildPathWithoutDate("${id}.txt", config.dir)

    /**
     *
     * @param id    文档唯一编号
     * @param text  markdown 源文本
     */
    fun embedImages(id:Long, text:String):String = txtFile(id).let { file->
        if(file.exists()) return@let Files.readString(file)

        Regex("!\\[.*?\\]\\((.*?)\\)")
            .replace(text) { match->
                val fileUrl = match.groupValues.last().let {
                    if(it.startsWith(contextPath))
                        it.replaceFirst(contextPath, "")
                    else
                        it
                }
                //暂不支持互联网资源
                if(fileUrl.startsWith("http"))  return@replace match.value

                val imgPath = Paths.get(".", fileUrl)
                val ext = imgPath.extension.lowercase()
                logger.info("${imgPath.toAbsolutePath() }  ${imgPath.isRegularFile()}")

                if(imgPath.exists() && imgPath.isRegularFile()){
                    if(config.exts.contains(ext)){
                        var img = ImageIO.read(imgPath.toFile()).let {
                            if(it.width > config.maxWidth){
                                if(logger.isDebugEnabled)   logger.debug("图片 $imgPath 宽度超出阈值 ${config.maxWidth} 即将裁剪...")

                                //对图片进行缩放，如需水印可以调用 watermark 方法
                                Thumbnails.of(it)
                                    .width(config.maxWidth)
                                    .outputQuality(config.resizeQuality)
                                    .asBufferedImage()
                            }
                            else
                                it
                        }

                        val out = ByteArrayOutputStream()
                        val mout = MemoryCacheImageOutputStream(out)
                        ImageIO.getImageWritersByMIMEType("image/webp").next().let { writer->
                            writer.output = mout

                            writer.write(
                                null,
                                IIOImage(img, null, null),
                                WebPWriteParam(writer.locale).also {
                                    it.compressionMode = ImageWriteParam.MODE_EXPLICIT
                                    it.compressionType = it.compressionTypes[WebPWriteParam.LOSSY_COMPRESSION]
                                    it.compressionQuality = config.quality
                                }
                            )
                            if(logger.isDebugEnabled)   logger.debug("图片 $imgPath 转 webp 完成...")
                        }
                        mout.flush()
                        base64(out.toByteArray())
                    }
                    //对于 webp 格式不作缩放处理直接编码
                    else if(ext == "webp"){
                        base64(Files.readAllBytes(imgPath))
                    }
                    else{
                        if(logger.isDebugEnabled)   logger.debug("图片 $imgPath 不是支持的格式...")
                        match.value
                    }
                }
                else {
                    logger.error("图片 $imgPath 不存在或不是一个有效文件...")

                    match.value
                }
            }
            .also {
                file.parent.also { p->
                    if(!p.exists())
                        Files.createDirectories(p)
                }
                Files.writeString(file, it, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                logger.info("缓存 $file 写入成功（SIZE = ${file.fileSize()} B）")
            }
    }

    @Async
    @EventListener(PageContentUpdateEvent::class)
    fun onPageUpdate(event: PageContentUpdateEvent) {
        event.page.also {
            if(it.template == Page.MARKDOWN){
                logger.info("检测到 #${it.id} 的内容变更，即将删除其缓存文件（若存在）...")
                txtFile(it.id).deleteIfExists()
            }
        }
    }
}