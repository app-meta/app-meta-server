package org.nerve.boot.web;
/*
 * @project boot-starter
 * @file    org.nerve.boot.web.JWTProvider
 * CREATE   2022年11月11日 18:30 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JWTProvider {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private JWTConfig config;

    private SignatureAlgorithm algorithm = SignatureAlgorithm.HS512;

    private SecretKey getSecretKey() {
        Assert.hasText(config.key, "JWT 密钥必须设置（jwt.key）");
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(config.key));
    }

    /**
     * 对用户ID、IP 进行签发
     */
    public String create(String uid, String ip) {
        return Jwts.builder()
                .setSubject(uid)
                .setAudience(ip)
                .setIssuer(config.issuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + config.expire * 1000))
                .signWith(getSecretKey(), algorithm)
                .compact();
    }

// fun verify(token:String) =
//         try{
//  val claims = Jwts.parserBuilder()
//          .requireIssuer(config.issuer)
//          .setSigningKey(secretKey) .build()
//          .parseClaimsJws(token)
//          .body
//
//  Pair(claims.subject, claims.audience)
// }catch (e:Exception){
//  logger.error("JWT 校验失败：${e.message}")
//  Pair("", "")
// }
}