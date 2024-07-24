package org.appmeta.tool

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.apache.commons.lang3.StringUtils
import org.appmeta.S
import org.nerve.boot.module.setting.SettingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import java.util.*
import javax.crypto.SecretKey


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.JWTTool
 * CREATE   2022年12月19日 16:00 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Component
class JWTTool(private val settingS:SettingService) {
    private val logger      = LoggerFactory.getLogger(javaClass)

    private val algorithm   = SignatureAlgorithm.HS512
    private val ISSUER      = "APP-META"

    private fun getSecretKey(): SecretKey {
        val key = settingS.value(S.AUTH_JWT_KEY)
        Assert.hasText(key, "请联系管理员设置令牌密钥 AUTH_JWT_KEY")
        /**
         * 创建密钥：
         *  val secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512)
         *  val keyString = Encoders.BASE64.encode(secretKey.encoded)
         */
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(key))
    }

    fun create(uid: String, ip: String) = create(uid, ip, settingS.intValue(S.AUTH_JWT_EXPIRE, 120))

    fun create(uid:String, ip:String, expire:Int) = Jwts.builder()
            .setSubject(uid)
            .setAudience(ip)
            .setIssuer(ISSUER)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expire * 60 * 1000L))
            .signWith(getSecretKey(), algorithm)
            .compact()

    /**
     *
     */
    fun verify(token: String) =
        try {
            val claims = Jwts.parserBuilder()
                .requireIssuer(ISSUER)
                .setSigningKey(getSecretKey()).build()
                .parseClaimsJws(token)
                .body

            Pair(claims.subject, claims.audience)
        } catch (e: Exception) {
            logger.error("JWT 校验失败：${e.message}")
            Pair("", "")
        }

    fun verifyAndCheckIp(token: String, ip:String): Boolean {
        val data = verify(token)
        return StringUtils.isNoneEmpty(data.first, data.second) && data.second == ip
    }
}