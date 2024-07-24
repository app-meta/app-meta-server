package org.appmeta.web

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

        //保存数据到 本地
        val targetPath = fileStore.buildPath("${UUID.randomUUID()}.$fileExt", IMAGE)
        if (Files.notExists(targetPath.parent))
            Files.createDirectories(targetPath.parent)

        //未来考虑增加图片压缩
        FileUtils.copyToFile(file.inputStream, targetPath.toFile())

        with(Image()) {
            of(user)

            size = file.size
            filename = file.originalFilename!!
            ext = fileExt.uppercase()
            path = targetPath.toString()
            addOn = System.currentTimeMillis()

            imgM.insert(this)

            opLog("上传图片资源 $filename >> $path", this, Operation.IMPORT)
            path
        }
    }
}