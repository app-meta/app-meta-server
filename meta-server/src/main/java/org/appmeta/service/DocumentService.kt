package org.appmeta.service

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.appmeta.Caches
import org.appmeta.Document404Exception
import org.appmeta.F
import org.appmeta.domain.Document
import org.appmeta.domain.DocumentMapper
import org.appmeta.model.PageModel
import org.nerve.boot.FileStore
import org.nerve.boot.FileStore.DEFAULT
import org.nerve.boot.db.service.BaseService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import java.io.File
import java.io.InputStream
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.fileSize


/*
 * @project app-meta-server
 * @file    org.appmeta.service.DocumentService
 * CREATE   2023年02月27日 10:33 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class DocumentService(private val fileStore: FileStore) : BaseService<DocumentMapper, Document>() {

    /**
     * 存储附件
     */
    fun store(fis:InputStream, originName:String, transfer:(Document)->Unit):Document {
        val ext = FilenameUtils.getExtension(originName)

        //保存数据到 本地
        val targetPath = fileStore.buildPath("${UUID.randomUUID()}.$ext", DEFAULT)
        if (Files.notExists(targetPath.parent))
            Files.createDirectories(targetPath.parent)

        //未来考虑增加图片压缩
        FileUtils.copyToFile(fis, targetPath.toFile())

        return with(Document()) {
            size        = targetPath.fileSize()
            filename    = originName
            this.ext    = ext.uppercase()
            path        = targetPath.toString()
            addOn       = System.currentTimeMillis()

            transfer(this)

            baseMapper.insert(this)
            this
        }
    }

    /**
     * 覆盖附件
     */
    fun overwrite(model:PageModel, fis: InputStream, originName: String):Document {
        val doc = getById(model.id)?: throw Document404Exception(model.id)
        Assert.isTrue(doc.pid == model.pid, "文档#${model.id} 归属 pid 不匹配")
        Assert.isTrue(doc.ext == FilenameUtils.getExtension(originName), "文件类型不一致")

        FileUtils.copyToFile(fis, Paths.get(doc.path).toFile())
        return doc
    }

    @CacheEvict(Caches.PAGE_DOCUMENT, key="#id")
    fun delete(id:Serializable, delFile:Boolean = true):Document {
        val doc = getById(id)?: throw Document404Exception(id)
        if(delFile)
            FileUtils.deleteQuietly(File(doc.path))

        removeById(id)
        return doc
    }

    @Cacheable(Caches.PAGE_DOCUMENT)
    fun listByPage(pid:String) = list(Q().eq(F.PID, pid))
}