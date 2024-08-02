package org.appmeta.web

import com.luciad.imageio.webp.WebPWriteParam
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.appmeta.S
import org.appmeta.domain.Image
import org.appmeta.domain.ImageMapper
import org.nerve.boot.Const
import org.nerve.boot.FileStore
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.util.Assert
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.util.*
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.min


/*
 * @project app-meta-server
 * @file    org.appmeta.web.AttachmentCtrl
 * CREATE   2022年12月19日 10:25 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("attachment")
class AttachmentCtrl(
    private val imgM: ImageMapper,
    private val fileStore: FileStore,
    private val settingService: SettingService) : BasicController() {

    private val IMAGE = "image"

    @RequestMapping("upload-image", name = "上传文档图片")
    fun uploadImage(@RequestParam("file") file: MultipartFile) = resultWithData {
        logger.debug("上传文件 ${file.originalFilename} (SIZE=${file.size})")
        val user = authHolder.get()
        //判断后缀
        val suffix = settingService.value(S.PICTURE_SUFFIX)
        val fileExt = FilenameUtils.getExtension(file.originalFilename)
        Assert.isTrue(
            suffix.split(Const.COMMA).indexOfFirst { it.equals(fileExt, true) } >= 0,
            "仅允许上传${suffix}类型的图片"
        )

        val fileID = UUID.randomUUID().toString()
        //保存数据到本地
        val targetPath = fileStore.buildPath("${fileID}.$fileExt", IMAGE)
        if (Files.notExists(targetPath.parent))
            Files.createDirectories(targetPath.parent)

        var finalUrl = targetPath.toString()

        /*
        add on 2024-08-02
        增加文档图片的裁剪、转 WebP
         */
        if(settingService.booleanValue(S.PICTURE_COMPRESS, true)){
            val maxWidth = settingService.intValue(S.PICTURE_WIDTH, 960)
            val imgBuffer = ImageIO.read(file.inputStream).let { img->
                //判断是否需要裁剪图片
                if(maxWidth > 0 && img.width > maxWidth){
                    if(logger.isDebugEnabled)   logger.debug("图片 ${file.originalFilename} 宽度超出阈值 $maxWidth 即将裁剪...")

                    Thumbnails.of(img)
                        .width(maxWidth)
                        .outputQuality(min(settingService.intValue(S.PICTURE_COMPRESS_Q, 80)/100f, 1.0f))
                        .asBufferedImage()
                }
                else
                    img
            }
            //判断是否需要转换为 WebP
            if(settingService.booleanValue(S.PICTURE_WEBP, true)){
                if(logger.isDebugEnabled)   logger.debug("即将将图片 ${file.originalFilename} 转换为 webp 格式...")

                val webpFile = targetPath.parent.resolve("$fileID.webp").toFile()
                ImageIO.getImageWritersByMIMEType("image/webp").next().let { writer->
                    val param = WebPWriteParam(writer.locale).also {
                        it.compressionMode = ImageWriteParam.MODE_EXPLICIT
                        it.compressionType = it.compressionTypes[WebPWriteParam.LOSSY_COMPRESSION]
                        it.compressionQuality = min(settingService.intValue(S.PICTURE_WEBP_Q, 80)/100f, 1.0f)
                    }
                    writer.output = FileImageOutputStream(webpFile)
                    writer.write(null, IIOImage(imgBuffer, null, null), param)
                }
                finalUrl = webpFile.toString()
            }
            else
                ImageIO.write(imgBuffer, fileExt, targetPath.toFile())
        }
        else
            FileUtils.copyToFile(file.inputStream, targetPath.toFile())

        with(Image()) {
            of(user)

            size = file.size
            filename = file.originalFilename!!
            ext = fileExt.uppercase()
            path = finalUrl
            addOn = System.currentTimeMillis()

            imgM.insert(this)

            opLog("上传图片资源 $filename >> $path", this, Operation.IMPORT)
            path
        }
    }
}