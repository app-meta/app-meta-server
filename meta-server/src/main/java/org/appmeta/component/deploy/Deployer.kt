package org.appmeta.component.deploy

import org.appmeta.domain.Terminal
import java.io.File

interface Deployer {

    fun name():String

    /**
     * 检测运行环境是否完备
     */
    fun checkRequirement():Boolean = true

    /**
     * 更新配置
     */
    fun refreshConfig(id: String, terminal: Terminal)

    fun deploy(id:String, codeFile: File, terminal: Terminal)

    /**
     * 重启或者运行某个应用
     */
    fun restart(id:String)

    /**
     * 停止应用
     */
    fun stop(id:String):Boolean

    /**
     * 移除应用
     */
    fun remove(id:String)

    /**
     * 查看指定应用的详细信息
     */
//    fun detail(id:String):Map<String, Any>

    fun overview():List<TerminalProcess>
}