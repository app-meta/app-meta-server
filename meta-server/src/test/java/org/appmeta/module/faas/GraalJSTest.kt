package org.appmeta.module.faas

import org.junit.jupiter.api.Test


/*
 * @project app-meta-server
 * @file    org.appmeta.module.faas.GraalJSTest
 * CREATE   2024年01月03日 18:20 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class GraalJSTest {

    @Test
    fun result(){
        // 数组返回时，toString 会显示 (2)[1,2]
        val regexList = Regex("(^\\([0-9]+\\))\\[.*]$")
        listOf("[{i: 0}, {i: 1}]", "(3)[1,2,3]").forEach {
            regexList.find(it)?.also { f->
                println("匹配到啦！")
                println(f.value)
                println(f.groups.last())
                println(it.replaceFirst(f.groupValues.last(), ""))
            }
            println("$it = ${regexList.matches(it)}")
        }
    }
}