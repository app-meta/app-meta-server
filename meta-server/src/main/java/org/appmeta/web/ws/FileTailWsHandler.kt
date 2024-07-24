package org.appmeta.web.ws

import com.alibaba.fastjson2.JSON
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import org.apache.commons.lang3.exception.ExceptionUtils
import org.appmeta.ANY
import org.appmeta.F
import org.appmeta.domain.PageMapper
import org.appmeta.service.TerminalService
import org.appmeta.tool.AuthHelper
import org.nerve.boot.Const
import org.nerve.boot.domain.AuthUser
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name


/*
 * @project app-meta-server
 * @file    org.appmeta.web.ws.FileTailWsHandler
 * CREATE   2024年03月14日 08:57 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class FileTail(
    val path:Path,
    val handler: Consumer<String>,
    val charset: Charset=Charsets.UTF_8,
    delay:Long=1000
): FileAlterationListenerAdaptor() {

    private val watcher = FileSystems.getDefault().newWatchService()

    private val MODE = "r"
    private var reader = RandomAccessFile(path.toFile(), MODE)
    private var position= reader.length()

    // 使用 JDK 自带的 WatchService ，发现不能正常读取文件追加的内容
    private var monitor: FileAlterationMonitor = FileAlterationMonitor(delay)

    init {
        // 初始化监视器，只检测同名的文件
        FileAlterationObserver(path.parent.toFile()) { f: File -> f.name == path.name }.also { observer->
            observer.addListener(this)
            monitor.addObserver(observer)

            monitor.start()
        }
    }

    override fun onFileChange(file: File) {
        reader.seek(position)

        //开启读取
        val bytes = mutableListOf<Byte>()
        val tmp = ByteArray(1024)
        var readSize: Int

        while ((reader.read(tmp).also { readSize = it }) != -1) {
            for (i in 0..< readSize){
                bytes.add(tmp[i])
            }
        }

        position += bytes.size

        handler.accept(String(bytes.toByteArray(), charset))
    }

    fun stop() {
        reader.close()
        monitor.stop()
    }
}

@Component
class FileTailWsHandler(
    private val terminalS:TerminalService,
    private val authHelper: AuthHelper, private val pageM:PageMapper) : TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val EXTS = arrayOf("TXT", "LOG", "MD", "CSV")

        val monitors = mutableMapOf<String, FileTail>()
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val user = session.attributes[F.USER] as AuthUser
        val params = session.attributes[F.PARAMS] as String
        logger.info("${user.showName} 请求跟踪文件 $params")

        try{
            val json = JSON.parseObject(params)

            val textFile = json.getString(F.AID).let { aid ->
                val p = if (aid == ANY) {
                    // 查看主应用日志
                    authHelper.checkAdminAndWhiteIP(user)
                    Paths.get("logs/spring.log")
                }else{
                    val page = terminalS.loadServer(aid)
                    //判断是否具备 aid 的权限
                    if(!authHelper.checkService(page, user))
                        throw Exception(HttpStatus.UNAUTHORIZED.name)

                    terminalS.resolvePath(aid, json.getString(F.PATH))
                }
                // 判断文件是否存在
                if(!(p.exists() && p.isRegularFile()))
                    throw Exception("目标不存在或者不是标准文件")
                FilenameUtils.getExtension(p.name).also { ext->
                    if(!EXTS.contains(ext.uppercase()))
                        throw Exception("不支持 $ext 后缀的文件")
                }

                p
            }

            // 加入队列
            monitors[session.id] = FileTail(
                textFile,
                { text -> session.sendMessage(TextMessage(text)) },
                Charset.forName(json.getOrDefault("charset", "UTF-8").toString())
            )
        }catch (e:Exception){
            logger.error("处理客户端消息失败", e)
            session.sendMessage(TextMessage("服务器出错：${ExceptionUtils.getMessage(e)}"))
            session.close(CloseStatus.SERVER_ERROR)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info("客户端（${session.id}）${session.remoteAddress} 断开连接...")

        monitors.remove(session.id)?.stop()
    }
}