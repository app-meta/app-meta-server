package org.appmeta.deploy

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths


/*
 * @project app-meta-server
 * @file    org.appmeta.deploy.DeployTest
 * CREATE   2024年07月09日 18:07 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class DeployTest {

    @Test
    fun updateArgs(){
        Files.readString(Paths.get("../terminal/FKZX_CLSCQ/terminal.config.js"), Charsets.UTF_8).also { text->
            println(text)
            Regex("args:\"(.*?)\"").find(text)?.also { m->
                println(m)
                println(m.range)
                val old = m.groupValues.last()
                println(old)
                println(text.substring(0, m.range.first))
                println(text.substring(m.range.last))
//                text.repl
                println(text.replaceRange(m.range, "args:\"---------------\""))
                println(text.replaceFirst(old, "-----------------new args--------------------"))
            }

        }
    }
}