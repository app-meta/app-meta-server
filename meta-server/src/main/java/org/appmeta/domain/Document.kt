package org.appmeta.domain

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Update
import org.nerve.boot.annotation.CN
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.domain.IDLong
import java.io.Serializable


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Attachment
 * CREATE   2022年12月14日 19:37 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@CN("图片资源")
open class Image : IDLong() {
    var filename    = ""
    var size        = 0L
    var ext         = ""
    var path        = ""
    var addOn       = 0L
    var uid         = ""

    fun of(user:AuthUser?) {
        if(user != null)    uid = user.id
    }
}

@CN("文档（附件）")
class Document:Image(), WithPage {
    override var aid= ""
    override var pid= ""
    var summary     = ""
    var download    = 0
}

@Mapper
interface ImageMapper:BaseMapper<Image>
@Mapper
interface DocumentMapper:BaseMapper<Document> {

    @Update("UPDATE document SET download=download+1 WHERE id=#{0}")
    fun afterDownload(id:Serializable)
}