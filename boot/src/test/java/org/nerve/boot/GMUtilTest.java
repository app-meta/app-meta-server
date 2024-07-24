package org.nerve.boot;
/*
 * @project app-meta-server
 * @file    org.nerve.boot.GMUtilTest
 * CREATE   2024年07月16日 09:19 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.nerve.boot.util.GMUtil.*;

public class GMUtilTest {

    String text = "集成显卡 2024！";

    @BeforeEach
    public void before(){
        System.out.println("明文："+text);
    }

    @Test
    public void sm2() throws Exception {
        List<String> sm2Keys = createSm2Key();

        for (int i = 0; i < sm2Keys.size(); i++) {
            System.out.println("%d -> %s".formatted(i, sm2Keys.get(i)));
        }

        String sm2Pub = sm2Keys.get(1);
        String sm2Pri = sm2Keys.get(0);
        System.out.println("SM2 公钥："+sm2Pub);
        System.out.println("SM2 私钥："+sm2Pri);
        String sm2Encoded = sm2Encrypt(text, sm2Pub);
        System.out.println("SM2 加密："+sm2Encoded);
        System.out.println("SM2 解密："+sm2Decrypt(sm2Encoded, sm2Pri, DEFAULT_ENCODING));
    }

    @Test
    public void sm3Test() throws Exception {
        System.out.println("SM3 摘要："+sm3(text));
        System.out.println("SM3 摘要（文件）："+sm3(Files.newInputStream(Paths.get("pom.xml"))));
    }

    @Test
    public void sm4() throws Exception {
        String sm4Key = createSm4Key(); //"519408db59393d216b905e9caa4d97e7";
        System.out.println(String.format("SM4 密钥：%s", sm4Key));

        String sm4Encoded = sm4Encrypt(text, sm4Key);
        System.out.println("SM4 加密："+sm4Encoded);
        System.out.println("SM4 解密："+sm4Decrypt(sm4Encoded, sm4Key));
    }

    @Test
    public void sm4Special() throws Exception {
        String sm4Key = "d1d412be2a82dab71873639add228732";
        String sm4Encoded = sm4Encrypt(text, sm4Key);
        System.out.println("SM4 加密："+sm4Encoded);
        System.out.println("SM4 解密："+sm4Decrypt(sm4Encoded, sm4Key));
    }
}
