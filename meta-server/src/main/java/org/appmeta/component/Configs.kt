package org.appmeta.component

import org.appmeta.tool.FileTool
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/*
 * @project app-meta-server
 * @file    org.appmeta.component.Configs
 * CREATE   2023年02月07日 18:40 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class DashboardConfig {
    var daySpan         = 30                //统计流量的时间跨度（天）
    var top             = 8                 //用户等统计前N
}

@Configuration
@ConfigurationProperties(prefix = "app")
class AppConfig {
    var home            = "index.html"
    var name            = "APP-META-SERVER"

    var resProtocol     = "file"            //资源协议，默认 file，可选 classpath
    var resPath         = "www"
    var resAppPath      = "www-app"
    var resHistoryPath  = "www-history"     //前端资源历史版本目录
    var resAppContext   = "www"

    var resZipFile      = "static.zip"      //前端资源打包后的文件名称
    var resZipCheck     = true              //解压前端资源是否进行校验
    var resZipKeep      = false             //前端资源是否在解压后保留源文件
    var resZipLimit     = 20                //最多保留版本包数量
    var resCacheTime    = -1L                //前端资源缓存时间，单位分钟
    var appIdRegex      = "^[A-Za-z0-9_]{3,20}\$"
    var appLaunchWindow = 20                //应用运行次数统计的时间窗口，单位分钟，即在该时间内不会重复计算

    var authCheckIP     = true              //是否在 CAS 回调中判断IP一致性（在某些场景下需要设置为 false）
    var authCacheExpire = 5                 //授权信息缓存时长（单位分钟），默认 5

    var terminalPath    = "terminal"        //后端服务部署的目录
    var terminalStart   = "terminal.config.js"
    var terminalConfig  = "config.json"     //默认的配置文件
    var terminalZipOver = true              //压缩包解压时，是否覆盖旧文件

    var headerChannel   = "CHANNEL"

    var dbmKey          = "WhlGdNfs4pwd138e"//数据库管理模块密钥
    var dbmLifetime     = 2 * 60L           //数据库管理模块 DataSource 存活时长，单位分钟
    var dbmAuthExpire   = 10 * 60           //数据库管理模块权限缓存，单位秒

    var dashboard       = DashboardConfig()
}

@Configuration
@ConfigurationProperties(prefix = "app.sys")
class SystemConfig{
    var adminId         = "admin"           //初始化的管理员ID
    var adminName       = "超级管理员"

    var enableJar       = false             //是否开启 JAR 更新功能
    var jarName         = "meta-server-1.0.jar"
    var logFile         = "logs/spring.log"

    var verLogOfJar     = "version-jar.txt"
}
