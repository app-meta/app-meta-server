package org.appmeta.web

import org.appmeta.F
import org.appmeta.service.MemberService
import org.nerve.boot.util.AESProvider
import org.nerve.boot.util.RSAProvider
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

/**
 * 外展服务
 * 用于和其他应用进行数据交互
 */
@RestController
@RequestMapping("outreach")
class OutreachCtrl(private val memberS:MemberService):BasicController() {

    /**
     * 授权终端登录流程：
     *      构建参数：{UID}-{13位时间戳}
     *      使用密钥加密（方式为 AES/CBC/NoPadding，补全到16倍数）
     *      对密文进行 BASE64 编码后，以 text 参数名发送到后端
     *      得到 token
     */
    @RequestMapping("create-token", name = "创建JWT Token")
    fun createToken(text:String) = memberS.createToken(requestIP, String(Base64.getDecoder().decode(text)))

    @GetMapping("create-aes-key", name = "创建 AES 密钥")
    fun createAESKey() = AESProvider().creatKey()

    @PostMapping("create-rsa-key", name = "创建 RSA 密钥对")
    fun createRSAKey() = resultWithData {
        val rsa = RSAProvider()
        mapOf(
            F.PUB_KEY to rsa.publicKey,
            F.PRI_KEY to rsa.privateKey
        )
    }
}