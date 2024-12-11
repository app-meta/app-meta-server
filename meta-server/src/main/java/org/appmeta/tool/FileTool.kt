package org.appmeta.tool

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.input.ReversedLinesFileReader
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.FileTool
 * CREATE   2023年02月10日 15:46 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

object FileTool {

    /**
     * 向文件指定位置中插入内容
     */
    fun injectText(file: File, position:Long, text:String) {
        RandomAccessFile(file, "rw").use { random->
            random.seek(position)

            val builder = StringBuilder()
            val bs = ByteArray(1024)

            var len: Int
            while (random.read(bs).also { len = it } != -1) {
                builder.append(String(bs, 0, len))
            }

            random.seek(position)
            random.write(text.toByteArray())
            random.write(builder.toString().toByteArray())
        }
    }


    const val UNZIP_OVERWRITE       = 0     //覆盖旧文件
    const val UNZIP_SKIP_ON_EXIST   = 1     //如果目标文件存在则跳过

    /**
     * 将压缩包解压到指定目录
     * 注意：需手动删除源目录
     */
    fun unzip(zipFile:File, target: Path, type:Int = UNZIP_OVERWRITE):List<String> {
        if(!Files.exists(target))
            Files.createDirectories(target)
        else {
            if(!Files.isDirectory(target))
                throw RuntimeException("$target 必须是目录")
        }

//        ZipInputStream(FileInputStream(zipFile)).use { zipIs->
        ArchiveStreamFactory().createArchiveInputStream<ArchiveInputStream<ZipArchiveEntry>>(
            BufferedInputStream(FileInputStream(zipFile))
        ).use { zipIs->

            val trace = mutableListOf<String>()
            val targetFolder = target.toFile()

            var entry: ArchiveEntry? = zipIs.nextEntry
            while(entry!=null){
                val file = File(targetFolder, entry.name)
                if(!file.parentFile.exists())   file.parentFile.mkdirs()

                if(type == UNZIP_SKIP_ON_EXIST && file.exists()){
                    trace.add("[ SKIP] $file")
                }
                else{
                    trace.add("[WRITE] $file")

                    if(entry.isDirectory)
                        file.mkdir()
                    else{
                        val fileOut = FileOutputStream(file)
                        fileOut.write(zipIs.readBytes())
                        fileOut.close()
                    }
                }

                entry = zipIs.nextEntry
            }

            return trace
        }
    }

    /**
     * 读取文件前N行或者最后N行
     */
    fun readLines(p:Path, reversed:Boolean=true, lineSize:Int=10, charset: Charset =Charsets.UTF_8) =
        if(reversed)
            ReversedLinesFileReader(p, charset).use { it.readLines(lineSize) }
        else
            RandomAccessFile(p.toFile(), "r").use { r->
                val lines = mutableListOf<String>()
                var count = 0
                while (count < lineSize){
                    val line = r.readLine()
                    if(line != null)
                        lines.add(line)
                    count ++
                }
                lines
            }

    /**
     * 检查文件后缀名是否符合要求
     */
    fun checkExt(filename:String, vararg exts:String):String =
        FilenameUtils.getExtension(filename).also {ext->
            if(!exts.any { it.equals(ext, true) })
                throw Exception("文件格式[$ext]不支持，请使用${exts}")
        }
}
