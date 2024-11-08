package org.appmeta.component

import com.alibaba.fastjson2.JSON
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*


@Component
class ServiceRoute {
    val logger = LoggerFactory.getLogger(javaClass)

    val restTemplate = RestTemplate().also {  }

    /**
     * 请求转发
     * @param request       原始请求
     * @param specialBody   自定义 BODY，若为空则使用原始请求体
     * @param response      响应对象
     * @param targetUrl     目标地址
     * @param extraHeaders  自定义请求头
     */
    fun redirect(request:HttpServletRequest, specialBody:ByteArray?, response:HttpServletResponse, targetUrl:String, extraHeaders: Map<String, String?>?=null):ResponseEntity<ByteArray> {
        val entity = createRequestEntity(request, specialBody, targetUrl, extraHeaders)
        return restTemplate.exchange(entity, ByteArray::class.java)
    }

    fun redirectDo(
        request: HttpServletRequest,
        response: HttpServletResponse,
        targetUrl: String, extraHeaders:
        Map<String, String?>?=null
    ): Pair<String, ResponseEntity<ByteArray>> {
        //获取参数对象

        var originBody = ""

        val multipartResolver = StandardServletMultipartResolver()

        val body = if(multipartResolver.isMultipart(request)){
            val body = LinkedMultiValueMap<String, Any>()
            if(logger.isDebugEnabled)   logger.debug("检测到转发 multipart/form-data，即将添加文件...")

            //添加文件
            multipartResolver.resolveMultipart(request).multiFileMap.entries.forEach { e->
                e.value.onEach { file->
                    body.add(e.key, object : ByteArrayResource(file.bytes) {
                        override fun getFilename() = file.originalFilename
                    })

                    if(logger.isDebugEnabled)
                        logger.debug("[multipart/form-data] 添加 ${e.key} ${file.originalFilename} (SIZE=${file.size})")
                }
            }

            val normalParams = mutableMapOf<String, Any>()
            //添加普通参数
            request.parameterNames.also { names->
                while (names.hasMoreElements()){
                    val name = names.nextElement()
                    request.getParameterValues(name).onEach { body.add(name, it) }
                    normalParams[name] = request.getParameter(name)
                }
            }
            originBody = JSON.toJSONString(normalParams)
            body
        }
        else{
            StreamUtils.copyToByteArray(request.inputStream).also {
                originBody = String(it, Charsets.UTF_8)
            }
        }

        val entity = createRequestEntity(request, body, targetUrl, extraHeaders)
        return Pair(
            originBody,
            restTemplate.exchange(entity, ByteArray::class.java)
        )
    }

    fun requestDo(targetUrl: String, params:Map<String, Any?>, extraHeaders: Map<String, String?>?) {
        TODO()
    }

    fun redirect(request:HttpServletRequest, response:HttpServletResponse, targetUrl:String, headers: Map<String, String?>?=null):ResponseEntity<ByteArray> {
        val entity = createRequestEntity(request, null, targetUrl, headers)
        return restTemplate.exchange(entity, ByteArray::class.java)
    }

    @Throws(URISyntaxException::class, IOException::class)
    private fun createRequestEntity(request: HttpServletRequest, specialBody:Any?, url: String, extraHeaders: Map<String, String?>?): RequestEntity<*> {
        val httpMethod = HttpMethod.valueOf(request.method)
        val headers = parseRequestHeader(request)
        extraHeaders?.forEach { (k, v) -> headers.add(k, v) }

        //将原始请求转换为字节数组
        val body = specialBody ?: StreamUtils.copyToByteArray(request.inputStream)
        return RequestEntity<Any>(body, headers, httpMethod, URI(url))
    }

    /**
     * 复制原始请求的 header 信息
     */
    private fun parseRequestHeader(request: HttpServletRequest): MultiValueMap<String, String?> {
        val headers = HttpHeaders()
        val headerNames: List<String> = Collections.list(request.headerNames)
        for (headerName in headerNames) {
            /**
             * 这里需要删除 content-length 头，不然传递文件时会报错：too many bytes written
             * https://github.com/spring-projects/spring-framework/issues/31902
             */
            if(!headerName.equals("content-length", false)){
                val headerValues: List<String> = Collections.list(request.getHeaders(headerName))
                for (headerValue in headerValues) {
                    headers.add(headerName, headerValue)
                }
            }
        }
        return headers
    }
}