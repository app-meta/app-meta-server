package org.appmeta.tool

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.PakoTool
 * CREATE   2023年03月20日 10:28 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 适用于需要压缩、解压字符串（量大、想要节约流量）的场景
 * 客户端解压示例（使用 pako 库）
 *
import { gzip, ungzip } from 'pako'

const CHUNK_SZ = 0x8000;
const Uint8ToString = u8a => {
    var c = []
    for (var i = 0; i < u8a.length; i += CHUNK_SZ) {
        c.push(String.fromCharCode.apply(null, u8a.subarray(i, i + CHUNK_SZ)))
    }
    return btoa(c)
}
const StringToUint8 = b64encoded=>new Uint8Array(atob(b64encoded).split("").map(c=>c.charCodeAt(0)))

export const compress = text=> Uint8ToString(gzip(typeof(text)==='string'? text : JSON.stringify(text)))

export const unCompress = text=> ungzip(StringToUint8(text), {to: 'string'})
 */

object GZIPTool {
    /**
     * 对字符串进行压缩
     * 返回 BASE64 格式
     */
    fun compress(text:String?): String {
        if(text.isNullOrEmpty())  return ""

        ByteArrayOutputStream().use { out->
            val gzip = GZIPOutputStream(out)
            gzip.write(text.toByteArray())
            gzip.close()

            return String(Base64.getEncoder().encode(out.toByteArray()))
        }
    }

    /**
     * 对 BASE64 编码的内容进行解压缩
     * 返回纯文本
     */
    fun unCompress(text: String):String {
        ByteArrayOutputStream().use { out->
            ByteArrayInputStream( Base64.getDecoder().decode(text)).use { bin->
                val gzip    = GZIPInputStream(bin)
                val bs      = ByteArray(256)

                var n: Int
                // 将未压缩数据读入字节数组
                while (gzip.read(bs).also { n = it } >= 0) {
                    out.write(bs, 0, n)
                }
            }
            return out.toString()
        }
    }
}
