package org.appmeta.component

import org.appmeta.URL_ALL
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.socket.server.standard.ServerEndpointExporter
import java.util.concurrent.TimeUnit


/*
 * @project app-meta-server
 * @file    org.appmeta.component.MvcConfig
 * CREATE   2023年02月07日 17:08 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Configuration
class MvcConfig(private val config: AppConfig) : WebMvcConfigurer {
    val logger = LoggerFactory.getLogger(javaClass)

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        /*
        增加静态资源的处理

        在 classpath 下目录结构
            www/                    app-meta 前端打包后的资源文件
            www-app/                小程序发布后的资源
                xxxx/               小程序（目录名为 ID）
                    index.html      小程序入口文件（默认为 index.html），访问路径 {应用context}/www/xxxx/index.html
         */
        val mainHandler = registry.addResourceHandler(URL_ALL).addResourceLocations("${config.resProtocol}:${config.resPath}/")
        val h5Handler   = registry.addResourceHandler("/${config.resAppContext}/**").addResourceLocations("${config.resProtocol}:${config.resAppPath}/")

        if(config.resCacheTime >= 0L){
            logger.info("[应有资源] 配置协商缓存，有效期=${config.resCacheTime}分钟")
            CacheControl.maxAge(config.resCacheTime, TimeUnit.MINUTES).also {
                mainHandler.setCacheControl(it)
                h5Handler.setCacheControl(it)
            }
        }

        logger.info("[应用资源] 资源协议=${config.resProtocol} 前端资源目录=${config.resPath} 应用部署目录=${config.resAppPath} 应用访问CONTEXT=${config.resAppContext}")
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        //默认跳转到首页
        registry.addViewController("").setViewName(config.home)
    }

    /**
     * 默认开启 CORS
     */
//    @Bean
//    @ConditionalOnProperty(value = ["app.cors"], havingValue = "true", matchIfMissing = false)
//    fun corsFilter() = CorsFilter(
//        UrlBasedCorsConfigurationSource().also {
//            logger.info("[CORS] 开启支持跨域请求 Access-Control-Allow-Origin=${ANY}")
//
//            it.registerCorsConfiguration(
//                URL_ALL,
//
//                CorsConfiguration().also { c->
//                    c.addAllowedOrigin(ANY)
//                    c.addAllowedHeader(ANY)
//                    c.addAllowedMethod(ANY)
//                    // 设置为 true 时无法正常 CORS
//                    c.allowCredentials = false
//                }
//            )
//        }
//    )

    /*
    由于启用了自定义 Filter ，此方法配置的 CORS 不生效
     */
//    override fun addCorsMappings(registry: CorsRegistry) {
//        logger.info("[CORS] 支持跨域请求 Access-Control-Allow-Origin=${ANY}")
//        registry.addMapping(URL_ALL)
//            .allowedOriginPatterns(ANY)
//            .allowCredentials(true)
//            .allowedMethods(ANY)
//            .allowedHeaders(ANY)
//    }
}