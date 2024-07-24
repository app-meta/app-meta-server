package org.appmeta.web.ws

import jakarta.annotation.Resource
import org.appmeta.F
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.web.auth.AuthConfig
import org.nerve.boot.web.auth.UserLoader
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeInterceptor
import java.lang.Exception


/*
 * @project app-meta-server
 * @file    org.appmeta.web.ws.SocketConfig
 * CREATE   2024年03月14日 08:52 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Component
class WsInterceptor : HandshakeInterceptor {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Resource
    lateinit var userLoader: UserLoader
    @Resource
    lateinit var authConfig: AuthConfig

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        if(logger.isDebugEnabled){
            logger.debug("WS 握手开始：${request.uri} 客户端=${request.remoteAddress}")
            request.headers.forEach { name, v -> logger.debug("[HEADER] $name = $v") }
        }

        val user =  userLoader.from(request.headers.getFirst(authConfig.tokenName))

        if(user == null){
            logger.error("${request.remoteAddress} 尝试匿名连接 ${request.uri}，已拦截")
            return false
        }

        attributes[F.USER]      = user
        attributes[F.PARAMS]    = request.headers.getFirst(F.PARAMS)?: EMPTY
        // 返回 true 才能建立连接
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }
}

@Configuration
@EnableWebSocket
class SocketConfig : WebSocketConfigurer {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Resource
    lateinit var interceptor: WsInterceptor
    @Resource
    lateinit var fileTailHandler:FileTailWsHandler

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(fileTailHandler, "/ws/file-tail").addInterceptors(interceptor)

        if(logger.isDebugEnabled)   logger.debug("配置 WebSocket 完成，通用拦截器 ${interceptor}...")
    }
}