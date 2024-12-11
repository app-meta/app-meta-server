package org.appmeta.web.page

import com.alibaba.fastjson2.JSON
import jakarta.servlet.http.HttpServletResponse
import jakarta.websocket.*
import jakarta.websocket.CloseReason.CloseCodes
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.appmeta.F
import org.appmeta.H
import org.appmeta.Role
import org.appmeta.component.AppConfig
import org.appmeta.component.deploy.Deployer
import org.appmeta.domain.*
import org.appmeta.model.*
import org.appmeta.module.dbm.DatabaseSource.Companion.SQLITE
import org.appmeta.module.dbm.DatabaseSourceService
import org.appmeta.service.TerminalService
import org.appmeta.tool.AuthHelper
import org.appmeta.tool.FileTool
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.FileStore
import org.nerve.boot.Result
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.util.AESProvider
import org.nerve.boot.util.DateUtil
import org.nerve.boot.web.auth.AuthHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.socket.WebSocketSession
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.*

@RestController
@RequestMapping("page/terminal")
class SupportTerminalCtrl (
    private val config: AppConfig,
    private val versionM:AppVersionMapper,
    private val fileStore: FileStore,
    private val deployer: Deployer,
    private val dbSourceS:DatabaseSourceService,
    private val service: TerminalService, private val logM:TerminalLogMapper):BasicPageCtrl() {

    @PostMapping("overview", name = "后端服务运行状态")
    fun overview(@RequestBody model: IdStringModel) = _checkEditResult(service.load(model.id).pid) { _, _->
        deployer.overview().find { it.name == model.id }
    }

    @PostMapping("restart", name = "重启后端服务")
    fun restart(@RequestBody model: IdStringModel) = _checkEditResult(service.load(model.id).pid) { _, _->
        deployer.restart(model.id)
    }

    @PostMapping("stop", name = "停止后端服务")
    fun stop(@RequestBody model: IdStringModel) = _checkEditResult(service.load(model.id).pid) { _, _->
        deployer.stop(model.id)
    }

    private fun _prepareLogDetail(log: TerminalLog) =  _checkServiceAuth(service.load(log.aid).pid) { _, _ ->
        TerminalDetailResult(
            log,
            service.loadLogDetail(log.id)
        )
    }

    @PostMapping("trace/{id}", name = "查看特定ID的请求详情")
    fun logOne(@PathVariable id:Long) = resultWithData {
        val log = service.loadLog(id)?: throw Exception("请求转发记录 #$id 不存在")

        _prepareLogDetail(log)
    }

    @PostMapping("trace/last", name = "查看最新一条请求")
    fun logLast(@RequestBody model: TerminalLogModel) = resultWithData {
        val log = service.loadLast(model)?: return@resultWithData null

        _prepareLogDetail(log)
    }

    @RequestMapping("trace-{aid}", name = "按应用查询后端服务记录")
    fun logList(@RequestBody model: QueryModel, @PathVariable aid:String) = _checkEditAuth(service.load(aid).pid) {_, _ ->
        Result().also {
            println(JSON.toJSONString(model))
            var pid = EMPTY
            if(model.form.containsKey(F.PID)){
                pid = model.form[F.PID] as String
                model.form[F.PID] as String
                model.form.remove(F.PID)
            }
            it.data = service.logList(model.form, model.pagination, aid, pid)
            it.total= model.pagination.total
        }
    }

    @PostMapping("trace-overview", name = "单应用后端服务总览")
    fun logOverview(@RequestBody model:IdStringModel) = resultWithData { service.logOverview(model.id) }

    @PostMapping("refresh-config", name = "刷新后端服务配置信息")
    fun refreshConfig(@RequestBody model: IdStringModel) = service.load(model.id).let { terminal->
        _checkEditResult(terminal.pid) { _, _->
            deployer.refreshConfig(model.id, terminal)
        }
    }

    val VALID_EXTS = listOf("js", "jar", "zip")

    @PostMapping("deploy", name = "部署应用服务")
    fun deploy(@RequestPart("file") file: MultipartFile, version: AppVersion) = result {
        val user = authHolder.get()
        Assert.isTrue(H.hasAnyRole(user, Role.DEPLOYER, Role.ADMIN), "仅限 ${Role.DEPLOYER} 角色或者管理员进行部署操作")

        val page = pageM.selectById(version.pid)
        if(!authHelper.checkEdit(page, user))
            throw Exception("您不具备编辑该页面/功能的权限")


        val ext = FilenameUtils.getExtension(file.originalFilename)
        Assert.isTrue(VALID_EXTS.contains(ext), "仅支持 $VALID_EXTS 格式（当前为 $ext）")

        val terminal = service.load(page.aid)

        deployer.checkRequirement()

        //"TERMINAL-${DateUtil.getDateTimeSimple()}.${ext}"
        //保留原始文件名
        val path = fileStore.buildPathWithoutDate("${page.aid}/${file.originalFilename}", FileStore.TEMP)
        if(!Files.exists(path.parent))
            Files.createDirectories(path.parent)
        val codeFile = path.toFile()

        FileUtils.copyToFile(file.inputStream, codeFile)

        if(terminal.useSource && terminal.dbSource>0){
            //读取数据源
            dbSourceS.withCache(terminal.dbSource)?.also {source->
                logger.info("应用#${page.aid}关联数据源#${source.id}|${source.name}，自动填充连接信息")
                terminal.dbPort = source.port
                terminal.dbUser = source.username
                terminal.dbName = source.db
                terminal.dbPwd  = AESProvider().decrypt(source.pwd, config.dbmKey)

                //对于 sqlite 数据库，只能限定应用目录下的文件
                if(source.type == SQLITE){
                    terminal.dbHost = dbSourceS.buildSqlitePathForApp(source.host, page.aid)
                }
                else{
                    terminal.dbHost = source.host
                }
            }
        }

        deployer.deploy(page.aid, codeFile, terminal)

        version.uid  = user.showName
        version.path = codeFile.path
        version.size = codeFile.length()
        if(!StringUtils.hasText(version.version))
            version.version = H.buildVersion()
        version.addOn = System.currentTimeMillis()

        versionM.insert(version)

        updateDateById(page.id)
    }

    /**
     * 构建目录或者文件的信息
     */
    private fun _buildFileItem(f:Path) = f.isDirectory().let { isDir->
        mapOf(
            F.NAME  to f.name,
            F.TYPE  to if(isDir) 0 else 1,
            "size"  to if(isDir) 0 else f.fileSize(),
            F.TIME  to f.getLastModifiedTime().toString()
        )
    }

    /**
     * 参数：
     *  id      应用id
     *  key     路径，默认为 /
     *          如果目标是文件夹，则显示其结构：
     *              name    名称
     *              type    0 = 目录，1=文件
     *              size    文件大小（byte）
     *              time    最后修改时间
     *          如果是文件，则读取前 10 行内容
     *
     *  value   操作类型，download 则为下载，否则为显示内容
     */
    @PostMapping("file", name = "显示部署目录结构或下载文件")
    fun directoryOrDownload(@RequestBody model:FieldModel, response: HttpServletResponse):Unit =
        _checkServiceAuth(service.load(model.id as String).pid) { page, user ->
            val path = service.resolvePath(page.aid, model.key)

            logger.info("${user.showName} 尝试访问 应用#${page.aid} 的文件 ${model.key}")
            val isFile = path.isRegularFile()

            when (model.value) {
                "download" -> {
                    if(!isFile) throw Exception("应用#${page.aid}下 $path 是一个目录，不支持下载")

                    downloadFile(response, path.toFile(), path.name) {
                        opLog("${user.showName} 在 $requestIP 下载应用#${page.aid} 的文件 $path", null, Operation.EXPORT)
                    }
                }
                //add on 2023-11-07
                "delete" -> {
                    val delResult = Files.deleteIfExists(path)
                    opLog("${user.showName} 在 $requestIP 删除应用#${page.aid} 的文件 $path", null, Operation.DELETE)
                    initResponse(response)
                    write(response, JSON.toJSONString(Result.ok(delResult)))
                }
                else -> {
                    initResponse(response)
                    write(response, JSON.toJSONString(
                        Result().setData(
                            mapOf(
                                "file"      to _buildFileItem(path),
                                F.CONTENT   to if(isFile) FileTool.readLines(path, false) else {
                                    Files.list(path).map { _buildFileItem(it) }.toList()
                                }
                            )
                        )
                    ))
                }
            }
        }
}

//@Component
//@ServerEndpoint("/page/terminal/ws")
class TerminalWebsocket {
    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var user:AuthUser
    lateinit var session: Session
    lateinit var file:String

    companion object {
        lateinit var authHelper: AuthHelper
        lateinit var authHolder: AuthHolder
        lateinit var appM:AppMapper

        private val clients = ConcurrentHashMap<String, WebSocketSession>()
    }

    @Autowired
    fun setAuthHelper(authHelper: AuthHelper){
        TerminalWebsocket.authHelper = authHelper
    }

    @Autowired
    fun setAuthHolder(authHolder: AuthHolder){
        TerminalWebsocket.authHolder = authHolder
    }

    @OnOpen
    fun onOpen(session:Session){
        this.user = authHolder.get()
        this.session = session

        logger.info("创建连接：${session.id} class=${hashCode()} user=${user.showName} ${appM}")
        session.basicRemote.sendText("你好，${DateUtil.getDateTime()}")
    }

    @OnClose
    fun onClose(){
        logger.info("关闭连接：${session.id} class=${hashCode()}")
    }

    @OnMessage
    fun onText(text:String){
        if(logger.isDebugEnabled)   logger.debug("收到客户端消息 $text")

        try{
            JSON.parseObject(text).let { json->
                val path = json.getString(F.PATH)
                val aid = json.getString(F.AID)
            }
        }catch (e:Exception){
            logger.error("处理客户端消息失败", e)
            session.close(CloseReason(CloseCodes.CANNOT_ACCEPT, e.message))
        }
    }

//    override fun afterConnectionEstablished(session: WebSocketSession) {
//        logger.info("创建连接：${session.id}")
//
//        session.sendMessage(TextMessage("${DateUtil.getDateTime()}"))
//    }
//
//    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
//        logger.info("WS 连接断开：${session.id}")
//
//        clients.remove(session.id)
//    }
}