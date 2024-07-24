package org.nerve.boot.util;
/*
 * @project app-meta-server
 * @file    org.nerve.boot.util.GMUtils
 * CREATE   2024年07月11日 17:38 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 基于 Bouncy Castle 封装的 GM 加解密
 *
 * 参考 https://github.com/dromara/hutool/blob/v5-master/hutool-crypto/src/main/java/cn/hutool/crypto/SmUtil.java
 */

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class GMUtil {

    public static String DEFAULT_ENCODING = "utf-8";

    private static String SM4 = "SM4";
    private static String SM3 = "SM3";
    /**
     * 加密算法/分组加密模式/分组填充方式
     * PKCS5Padding-以8个字节为一组进行分组加密
     * 定义分组加密模式使用：PKCS5Padding
     */
    private static String SM4_TRANSFORM = "SM4/ECB/PKCS5Padding"; // "SM4/CBC/PKCS7Padding";
    private static String PROVIDER = "BC";
    private static String SPEC = "sm2p256v1";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String createSm4Key() throws Exception {
        // 生成 SM4 密钥
        KeyGenerator keyGen = KeyGenerator.getInstance(SM4, PROVIDER);
        keyGen.init(128, new SecureRandom()); // SM4 使用 128 位密钥
        SecretKey secretKey = keyGen.generateKey();
        return bytesToHex(secretKey.getEncoded());
    }

    public static String sm4Encrypt(String content, String key) throws Exception{
        return sm4Encrypt(content, key, DEFAULT_ENCODING);
    }

    /**
     *
     * @param content
     * @param hexKey
     * @param charsetName
     * @return
     * @throws Exception
     */
    public static String sm4Encrypt(String content, String hexKey, String charsetName) throws Exception {
        Cipher cipher = Cipher.getInstance(SM4_TRANSFORM, PROVIDER);

        byte[] keyBytes = Hex.decode(hexKey);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, SM4));
        byte[] ciphertext = cipher.doFinal(content.getBytes(charsetName));
        return bytesToHex(ciphertext);
    }

    public static String sm4Decrypt(String encryptText, String hexKey) throws Exception {
        return sm4Decrypt(encryptText, hexKey, DEFAULT_ENCODING);
    }

    /**
     *
     * @param encryptText
     * @param hexKey
     * @param charsetName
     * @return
     * @throws Exception
     */
    public static String sm4Decrypt(String encryptText, String hexKey, String charsetName) throws Exception {
        Cipher cipher = Cipher.getInstance(SM4_TRANSFORM, PROVIDER);

        byte[] keyBytes = Hex.decode(hexKey);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, SM4));
        byte[] ciphertext = cipher.doFinal(Hex.decode(encryptText));
        return new String(ciphertext, charsetName);
    }

    /**
     * 生成文本内容的信息摘要
     * @param content 文本
     * @return 返回长度为 64 的 hex 文本
     */
    public static String sm3(String content) throws Exception {
        // 获取 SM3 MessageDigest 实例
        MessageDigest digest = MessageDigest.getInstance(SM3, PROVIDER);

        digest.update(content.getBytes());
        return bytesToHex(digest.digest());
    }


    public static String sm3(InputStream is) throws Exception {
        // 获取 SM3 MessageDigest 实例
        MessageDigest digest = MessageDigest.getInstance(SM3, PROVIDER);

        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = is.read(buffer)) > 0) {
            digest.update(buffer, 0, numRead);
        }
        is.close();

        return bytesToHex(digest.digest());
    }

    /**
     * 第一个元素为私钥、第二个元素为公钥、第三个元素为 30 开头的私钥、第四个元素为 30 开头的公钥
     *
     * @return SM2 密码对
     * @throws Exception
     */
    public static List<String> createSm2Key() throws Exception {
        List<String> keys = new ArrayList<>(2);
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC",  PROVIDER);
        keyPairGen.initialize(new ECGenParameterSpec(SPEC), new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();

        BCECPublicKey pubKey = (BCECPublicKey) keyPair.getPublic();
        BCECPrivateKey priKey = (BCECPrivateKey) keyPair.getPrivate();

        //私钥（16进制字符串，头部不带00长度共64）
        keys.add(bytesToHex(priKey.getS().toByteArray()));
        //获取公钥（16进制字符串，头部带04长度共130）
        keys.add(bytesToHex(((BCECPublicKey)keyPair.getPublic()).getQ().getEncoded(false)));

        /*
        同时添加 30 开头的密钥
        在X.509证书中，公钥信息是按照ASN.1编码规则进行编码的，而这种编码可能会在公钥前面加上一些额外的信息，使得整个序列看起来以“30”开头。
        “30”在十六进制中对应于ASN.1的SEQUENCE标签，表明这是一个复合数据类型，包含了多个元素，如算法标识符和公钥本身。
         */
        keys.add(bytesToHex(priKey.getEncoded()));
        keys.add(bytesToHex(pubKey.getEncoded()));

        return keys;
    }

    public static String sm2Encrypt(String content, String hexPubKey) throws Exception {
        return sm2Encrypt(content, hexPubKey, DEFAULT_ENCODING);
    }

    /**
     * SM2 加密
     * @param content       待加密文本
     * @param hexPubKey     公钥串（HEX编码）
     * @param charsetName   字符集
     * @return              HEX编码的加密结果
     * @throws Exception
     */
    public static String sm2Encrypt(String content, String hexPubKey, String charsetName) throws Exception{
        SM2Engine engine = new SM2Engine(new SM3Digest(), SM2Engine.Mode.C1C3C2);

        CipherParameters pubKeyParams;

        if(hexPubKey.length() == 130 && hexPubKey.startsWith("04")){
            // 获取一条SM2曲线参数
            X9ECParameters sm2ECParameters = GMNamedCurves.getByName(SPEC);
            // 构造ECC算法参数，曲线方程、椭圆曲线G点、大整数N
            ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());
            //提取公钥点
            ECPoint pukPoint = sm2ECParameters.getCurve().decodePoint(Hex.decode(hexPubKey));
            // 公钥前面的02或者03表示是压缩公钥，04表示未压缩公钥, 04的时候，可以去掉前面的04
            pubKeyParams = new ECPublicKeyParameters(pukPoint, domainParameters);
        }
        else
            pubKeyParams = PublicKeyFactory.createKey(Hex.decode(hexPubKey));

        engine.init(true, new ParametersWithRandom(pubKeyParams));

        byte[] bytes = content.getBytes(charsetName);
        return bytesToHex(engine.processBlock(bytes, 0, bytes.length));
    }

    public static String sm2Decrypt(String encryptText, String hexPriKey)throws Exception {
        return sm2Decrypt(encryptText, hexPriKey, DEFAULT_ENCODING);
    }

    /**
     * SM2 解密
     * @param encryptText
     * @param hexPriKey
     * @param charsetName
     * @return
     * @throws Exception
     */
    public static String sm2Decrypt(String encryptText, String hexPriKey, String charsetName) throws Exception {
        SM2Engine engine = new SM2Engine(new SM3Digest(), SM2Engine.Mode.C1C3C2);

        CipherParameters priKeyParams;
        if(hexPriKey.length() == 64 || (hexPriKey.length() == 66 && hexPriKey.startsWith("00"))){
            //获取一条SM2曲线参数
            X9ECParameters sm2ECParameters = GMNamedCurves.getByName(SPEC);
            //构造domain参数
            ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());

            BigInteger privateKeyD = new BigInteger(hexPriKey, 16);
            priKeyParams = new ECPrivateKeyParameters(privateKeyD, domainParameters);
        }
        else
            priKeyParams = PrivateKeyFactory.createKey(Hex.decode(hexPriKey));

        engine.init(false, priKeyParams);

        byte[] bytes = Hex.decode(encryptText);
        return new String(engine.processBlock(bytes, 0, bytes.length), charsetName);
    }

    public static String hexToBase64(String hex){
        return bytesToBase64(Hex.decode(hex));
    }

    public static String base64ToHex(String base64) throws UnsupportedEncodingException {
        return bytesToHex(Base64.getDecoder().decode(base64));
    }

    /**
     * 将字节数组转换为十六进制字符串的方法
     * @param bytes 字节数组
     * @return
     */
    private static String bytesToHex(byte[] bytes) throws UnsupportedEncodingException {
        return new String(Hex.encode(bytes), DEFAULT_ENCODING);
    }

    private static String bytesToBase64(byte[] bytes){
        return Base64.getEncoder().encodeToString(bytes);
    }
}
