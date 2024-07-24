package org.appmeta.component.deploy

class TerminalProcess {
    var pid         = 0         //进程ID
    var name        = ""
    var mem         = 0L        //内存，b
    var cpu         = 0.0F      //cpu 占用
    var version     = ""        //应用版本
    var vm          = ""
    var vmVersion   = ""        //容器/虚拟机版本
    var uptime      = 0L        //启动时间戳
    var status      = ""
    var addOn       = 0L        //统计时间

    constructor(){
        addOn       = System.currentTimeMillis()
    }
}