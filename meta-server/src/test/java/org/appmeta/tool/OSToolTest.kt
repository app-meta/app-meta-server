package org.appmeta.tool

import org.junit.jupiter.api.Test


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.OSToolTest
 * CREATE   2024年07月08日 17:48 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class OSToolTest {

    @Test
    fun findPIDWithPort(){
        //"3000", "4000",
        listOf("8000").forEach {
            println("端口 $it 占用PID=${OSTool.findPIDByPort(it)}")
        }
    }

    @Test
    fun killPID(){
        val port = "8000"
        OSTool.findPIDByPort(port).also { pid->
            println("端口 $port 占用 PID = $pid")

            if(pid.isNotEmpty()){
                println(OSTool.killProcess(pid))
                println("再次监测端口 $port = ${OSTool.findPIDByPort(port)}")
            }
            else
                println("端口 $port 未使用...")
        }
    }
}