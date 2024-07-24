package org.nerve.boot.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES工具类，兼容python交互
 */
public class AESProvider {
    private String key = "rNvU4jW8CednizmR";    //默认密钥

    private final static String CBC = "AES/CBC/NoPadding";
    public final static String AES = "AES";
    private final static String ENCODING = "utf-8";

    public AESProvider(){}

    public AESProvider(String key){
        this.key = key;
    }

    /**
     * 加密数据
     * @param data
     * @return
     */
    public String encrypt(String data){
        return encrypt(data, key);
    }

    /**
     * 使用特定的密钥加密数据
     * @param data
     * @param _key
     * @return
     */
    public String encrypt(String data,String _key){
        try{
            byte[] _keyBytes = (_key==null)?key.getBytes():_key.getBytes();
            Cipher cipher = Cipher.getInstance(CBC);
            int blockSize = cipher.getBlockSize();
            byte[] dataBytes = data.getBytes(ENCODING);
            int plaintextLength = dataBytes.length;
            if(plaintextLength % blockSize != 0)
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(_keyBytes, AES), new IvParameterSpec(_keyBytes));
            return Base64.encodeBase64String(cipher.doFinal(plaintext));
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解密
     * @param encryptData
     * @return
     */
    public String decrypt(String encryptData){
        return decrypt(encryptData, key);
    }

    /**
     * 使用特定的密钥解密数据
     * @param encryptData
     * @param _key
     * @return
     */
    public String decrypt(String encryptData,String _key){
        try {
            byte[] _keyBytes = (_key==null)?key.getBytes():_key.getBytes();
            byte[] encrypted1 = Base64.decodeBase64(encryptData);
            Cipher cipher = Cipher.getInstance(CBC);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(_keyBytes, AES), new IvParameterSpec(_keyBytes));
            byte[] original = cipher.doFinal(encrypted1);

            return new String(original, ENCODING).trim();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成16位的密钥
     * @return
     */
    public String creatKey(){
        return RandomStringUtils.randomAlphanumeric(16);
    }

    public String getKey(){
        return this.key;
    }
}
