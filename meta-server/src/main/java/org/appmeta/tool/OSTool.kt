package org.appmeta.tool

import org.appmeta.IS_WINDOW
import org.nerve.boot.Const.EMPTY
import java.io.File
import java.util.concurrent.TimeUnit


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.OSTool
 * CREATE   2023年03月30日 17:08 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

object OSTool {

    /**
     * 根据给定端口，查询占用的程序 PID
     *
     * windows：
     *  $ netstat -ano | findstr ":3000"
     *  TCP    192.168.110.209:3322   103.212.12.52:3000     ESTABLISHED     97396
     *
     * linex:
     *  $ netstat -ano | grep ":3000"
     *
     */
    fun findPIDByPort(port:String):String {
        val lines = listOf("netstat", "-ano")   //, "|", if(IS_WINDOW) "findstr" else "grep", "\":${port}\""

        return runCmd(lines, File("."), 0).let { result ->
            if(result.first == 0){
                result.second!!.let { lines->
                    val line = lines.split("\n").find { l-> l.contains(":${port}") }

                    line?.trim()?.split(Regex("\\s+"))?.last() ?: EMPTY
                }
            }
            else
                EMPTY
        }
    }

    /**
     * 停止进程
     */
    fun killProcess(pid:String):Boolean {
        val lines = if(IS_WINDOW) listOf("taskkill", "/F", "/PID", pid) else listOf("kill", "-9", pid)
        runCmd(lines).let { r->
            if(r.first == 0)    return true
            throw Exception(r.second)
        }
    }

    /**
     * 调用系统命令行中的命令.以List<String>的方式输入命令的各个参数.
     * 默认在当前目录中执行,超时时间为60秒
     *
     * 建议使用 runCmd 方法
     */
    fun runCommand(
        cmd: List<String>,
        workingDir: File = File("."),
        timeoutAmount: Long = 1L,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): Pair<Int, String?> = runCatching {
        val cmds = mutableListOf<String>()
        if(System.getProperty("os.name").uppercase().contains("WINDOW"))
            cmds.addAll(listOf("cmd", "/c"))
        else
            cmds.addAll(listOf("sh", "-c"))

        cmds.addAll(cmd)
        val process = ProcessBuilder(cmd)
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()

        // jdk17之后这样写
        val text = process.also {  it.waitFor(timeoutAmount, timeUnit) }.inputReader().readText()

        Pair(process.exitValue(), text)
    }.onFailure { Pair(-1, it.message) }.getOrThrow()


    /**
     * 调用系统命令
     * 默认在当前目录中执行,超时时间为30秒
     *
     * 返回结果 Pair<进程返回code，文本输出>
     *     code =-1 时为执行报错
     */
    fun runCmd(
        cmd: List<String>,
        workDir: File = File("."),
        timeoutAmount: Long = 20L,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ):Pair<Int, String?> =
        try {
            val pb = ProcessBuilder(cmd)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()

            // jdk17之后这样写
            val text = pb.also {  it.waitFor(timeoutAmount, timeUnit) }.inputReader().readText()

            Pair(pb.exitValue(), text.trim())
        } catch (e: Exception) {
            Pair(-1, e.message)
        }
}
