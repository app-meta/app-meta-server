package org.appmeta.tool

import com.luciad.imageio.webp.WebPWriteParam
import org.appmeta.component.func.MarkdownConfig
import org.appmeta.component.func.MarkdownFunc
import org.junit.jupiter.api.Test
import org.nerve.boot.FileStore
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.WebpTest
 * CREATE   2023年08月08日 18:22 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class WebpTest {

    @Test
    fun read(){
        val img = ImageIO.read(File("demo/img.webp"))
        println(img.width)
        println(img.type)
    }

    @Test
    fun write(){
        ImageIO.read(File("demo/img.webp")).also { img->
            ImageIO.getImageWritersByMIMEType("image/webp").next().let { writer->
                val param = WebPWriteParam(writer.locale).also {
                    it.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    it.compressionType = it.compressionTypes[WebPWriteParam.LOSSY_COMPRESSION]
                    it.compressionQuality = 0.1f
                }
                writer.output = FileImageOutputStream(File("demo/img-compress.webp"))
                writer.write(null, IIOImage(img, null, null), param)

                println("压缩图片写入成功...")
            }
        }
    }

    @Test
    fun markdown(){
        MarkdownFunc(FileStore("attach/"), MarkdownConfig()).embedImages(
            0,
            """
# Markdown 语法
> 最近更新：集成显卡

![](attach/a.png)

截图如下：

![b.png](attach/b.jpg)
            """
        ).also {
            println(it)
        }
    }
}