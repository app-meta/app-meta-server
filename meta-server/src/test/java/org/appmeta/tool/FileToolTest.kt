package org.appmeta.tool

import org.junit.jupiter.api.Test
import java.nio.file.Paths


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.FileToolTest
 * CREATE   2023年06月21日 10:03 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class FileToolTest {

    @Test
    fun readLines(){
        val p = Paths.get("pom.xml")
        println(FileTool.readLines(p))
        println(FileTool.readLines(p, false))
    }
}