package org.appmeta.tool

import org.appmeta.web.ws.FileTail
import org.junit.jupiter.api.Test
import java.nio.file.Paths


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.FileTailTest
 * CREATE   2024年03月14日 14:03 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class FileTailTest {

    @Test
    fun tail(){
        FileTail(
            Paths.get("attach/log.txt"),
            { text-> println("监听到文件更新：${text}") }
        ).also { t->
            Thread.sleep(60*1000)
            t.stop()
        }
    }
}