package org.nerve.boot.util;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * CREATE ON 2022年03月29日 17:28
 * 与之对应的 Node.js/Javascript 加解密方式
 const NodeRSA = require("node-rsa")

 const encryptionScheme = "pkcs1"

 module.exports = {
     encrypt (data, publicKey) {
         const pubKey = new NodeRSA(publicKey, 'pkcs8-public')
         //设置与后端一致的加密方式 pkcs1
         pubKey.setOptions({ encryptionScheme })
         return pubKey.encrypt(Buffer.from(data), 'base64')
     },

     decrypt (data, privateKey){
         const priKey = new NodeRSA(privateKey, 'pkcs8-private')
         priKey.setOptions({ encryptionScheme })
         return priKey.decrypt(Buffer.from(data, 'base64'), 'utf8')
     },

     sign (data, privateKey){
        const priKey = new NodeRSA(privateKey, 'pkcs8-private')
        return priKey.sign(Buffer.from(data)).toString('base64')
     },

     verify (data, signature, publicKey){
         const pubKey = new NodeRSA(publicKey, 'pkcs8-public')
         return pubKey.verify(data, Buffer.from(signature, 'base64'))
     }
 }

 */
public final class RSAProvider {

    private final static String RSA = "RSA";
    private final static String SHA256WithRSA = "SHA256WithRSA";
    private final static int MAX = 117;

    private String privateKey;
    private String publicKey;

    /**
     * 创建 RSA 工具类，自动生成公私钥（字符串格式）
     * @throws Exception
     */
    public RSAProvider() throws Exception {
        //自动生成密钥
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
        generator.initialize(1024, new SecureRandom());
        KeyPair keyPair = generator.genKeyPair();

        Base64.Encoder encoder = Base64.getEncoder();
        initKey(
                encoder.encodeToString(keyPair.getPublic().getEncoded()),
                encoder.encodeToString(keyPair.getPrivate().getEncoded())
        );
    }

    /**
     * 使用特定公私钥初始化工具类
     * @param pubKey
     * @param priKey
     */
    public RSAProvider(String pubKey, String priKey){
        initKey(pubKey, priKey);
    }

    private void initKey(String pubKey, String priKey){
        this.privateKey = priKey;
        this.publicKey = pubKey;
    }

    private PrivateKey buildPriKey() throws Exception {
        if(privateKey == null || privateKey.isEmpty())  throw new RuntimeException("PRIVATE KEY is empty!");

        return KeyFactory.getInstance(RSA)
                .generatePrivate(
                        new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey))
                );
    }

    private PublicKey buildPubKey() throws Exception {
        if(publicKey == null || publicKey.isEmpty())  throw new RuntimeException("PUBLIC KEY is empty!");

        return KeyFactory.getInstance(RSA)
                .generatePublic(
                        new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))
                );
    }

    /**
     * 使用私钥签名
     * @param content
     * @return
     */
    public String sign(String content) throws Exception {
        Signature signature = Signature.getInstance(SHA256WithRSA);
        signature.initSign(buildPriKey());
        signature.update(content.getBytes());
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * 公钥验签
     * @param message
     * @param signed
     * @return
     * @throws Exception
     */
    public boolean verifySign(String message, String signed) throws Exception {
        Signature signature = Signature.getInstance(SHA256WithRSA);
        signature.initVerify(buildPubKey());
        signature.update(message.getBytes());
        return signature.verify(Base64.getDecoder().decode(signed));
    }


    /**
     * 使用公钥加密
     * @param content 任意长度的字符
     * @return
     */
    public String encrypt(String content) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.ENCRYPT_MODE, buildPubKey());
        //此方法只能加密长度小于 117 bytes
//        return Base64.getEncoder().encodeToString(cipher.doFinal(content.getBytes()));

        byte[] bytes = content.getBytes();
        ByteArrayOutputStream bops = new ByteArrayOutputStream();
        int offLen = 0;

        while (bytes.length - offLen > 0){
            bops.write(cipher.doFinal(bytes, offLen, Math.min(bytes.length - offLen, MAX)));
            offLen += MAX;
        }
        bops.close();
        return Base64.getEncoder().encodeToString(bops.toByteArray());
    }

    /**
     * 使用私钥解密
     * @param data 任意长度的密文（分段解密）
     * @return
     * @throws Exception
     */
    public String decrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.DECRYPT_MODE, buildPriKey());
        // 此方法报错：Data must not be longer than 128 bytes
//        return new String(cipher.doFinal(Base64.getDecoder().decode(data)));

        byte[] bytes =  Base64.getDecoder().decode(data);
        ByteArrayOutputStream bops = new ByteArrayOutputStream();
        int offLen = 0;

        while (bytes.length - offLen > 0){
            bops.write(cipher.doFinal(bytes, offLen, Math.min(bytes.length - offLen, 128)));
            offLen += 128;
        }
        bops.close();
        return bops.toString();
    }

    public String getPrivateKey() {
        return privateKey;
    }
    public String getPublicKey() {
        return publicKey;
    }
}
