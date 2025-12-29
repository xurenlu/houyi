/*
 * This file is part of the zyan/wework-msgaudit.
 *
 * (c) 读心印 <aa24615@qq.com>
 *
 * This source file is subject to the MIT license that is bundled
 * with this source code in the file LICENSE.
 */

package com.tencent.wework;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA 加密工具类
 * 
 * @author renlu
 * @author refactored
 */
@Slf4j
public class RSAEncrypt {
//    public static void main (String[] args) throws Exception {
//        Map<String, Object> keyMap = genKeyPair();
//        String message = "df723820";
//        System.out.println("随机生成的公钥为:" + keyMap.get(0));
//        System.out.println("随机生成的私钥为:" + keyMap.get(1));
//        String messageEn = encrypt(message,keyMap.get(0).toString());
//        System.out.println(message + "\t加密后的字符串为:" + messageEn);
//        String messageDe = decrypt(messageEn,keyMap.get(1).toString());
//        System.out.println("还原后的字符串为:" + messageDe);
//    }

    /**
     * 随机生成密钥对
     * @throws NoSuchAlgorithmException
     */
    public static Map<String, Object> genKeyPair() throws NoSuchAlgorithmException {
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // 初始化密钥对生成器，密钥大小为96-1024位
        keyPairGen.initialize(1024,new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        java.security.interfaces.RSAPrivateKey privateKey = (java.security.interfaces.RSAPrivateKey) keyPair.getPrivate();   // 得到私钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();  // 得到公钥
        String publicKeyString = new String(Base64.encodeBase64(publicKey.getEncoded()));
        // 得到私钥字符串
        String privateKeyString = new String(Base64.encodeBase64((privateKey.getEncoded())));
        // 将公钥和私钥保存到Map
        Map<String, Object> keyMap = new HashMap<>(2);  //用于封装随机产生的公钥与私钥
        keyMap.put("0", publicKeyString);  //0表示公钥
        keyMap.put("1", privateKeyString);  //1表示私钥
        return keyMap;
    }
    /**
     * RSA公钥加密
     *
     * @param str
     *            加密字符串
     * @param publicKey
     *            公钥
     * @return 密文
     * @throws Exception
     *             加密过程中的异常信息
     */
    public static String encrypt( String str, String publicKey ) throws Exception{
        //base64编码的公钥
        byte[] decoded = Base64.decodeBase64(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        //RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        String outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
        return outStr;
    }

    /**
     * RSA私钥解密
     *
     * @param str
     *            加密字符串
     * @param privateKey
     *            私钥
     * @return 铭文
     * @throws Exception
     *             解密过程中的异常信息
     */
    public static String decrypt(String str, String privateKey) throws Exception{
        //64位解码加密后的字符串
        byte[] inputByte = Base64.decodeBase64(str.getBytes("UTF-8"));
        //base64编码的私钥
        byte[] decoded = Base64.decodeBase64(privateKey);
        java.security.interfaces.RSAPrivateKey priKey = (java.security.interfaces.RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        //RSA解密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        String outStr = new String(cipher.doFinal(inputByte));
        return outStr;
    }


    /**
     * 使用 BouncyCastle 解析 RSA 私钥并解密
     * 
     * @param encrypt_random_key 加密的随机密钥
     * @param privKeyPem PEM 格式的私钥
     * @return 解密后的字符串
     * @throws Exception 解密过程中的异常
     */
    public static String decrypt2(String encrypt_random_key, String privKeyPem) throws Exception {
        String privKeyPemNew = privKeyPem.replaceAll("\\n", "")
                .replace("\n","")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----","")
                .replaceAll("-----END PRIVATE KEY-----","");

        byte[] bytes = java.util.Base64.getDecoder().decode(privKeyPemNew);

        // 使用 BouncyCastle 解析 RSA 私钥
        ASN1Sequence sequence = ASN1Sequence.getInstance(bytes);
        RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(sequence);
        
        BigInteger modulus = rsaPrivateKey.getModulus();
        BigInteger publicExp = rsaPrivateKey.getPublicExponent();
        BigInteger privateExp = rsaPrivateKey.getPrivateExponent();
        BigInteger prime1 = rsaPrivateKey.getPrime1();
        BigInteger prime2 = rsaPrivateKey.getPrime2();
        BigInteger exp1 = rsaPrivateKey.getExponent1();
        BigInteger exp2 = rsaPrivateKey.getExponent2();
        BigInteger crtCoef = rsaPrivateKey.getCoefficient();

        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(
            modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef
        );
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        
        // 64位解码加密后的字符串
        byte[] inputByte = Base64.decodeBase64(encrypt_random_key.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new String(cipher.doFinal(inputByte));
    }
    
    public static String decryptByPriKey(String ciphertext, String privateKeyStr) {
        privateKeyStr = privateKeyStr.replaceAll("-----BEGIN PRIVATE KEY-----", "");
        privateKeyStr = privateKeyStr.replaceAll("-----END PRIVATE KEY-----", "");
        privateKeyStr = privateKeyStr.replaceAll("-----BEGIN RSA PRIVATE KEY-----", "");
        privateKeyStr = privateKeyStr.replaceAll("-----END RSA PRIVATE KEY-----", "");
        privateKeyStr = privateKeyStr.replaceAll("\\s+", "");
        try {
            byte[] privateBytes = Base64.decodeBase64(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            int inputLen = Base64.decodeBase64(ciphertext).length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;
            int MAX_DECRYPT_BLOCK = 128;
            byte[] encryptedData = Base64.decodeBase64(ciphertext);
            // 对数据分段解密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                    cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_DECRYPT_BLOCK;
            }
            byte[] decryptedData = out.toByteArray();
            out.close();
            return new String(decryptedData);
        } catch (Exception e) {
            log.error("RSA 解密失败", e);
        }
        return null;
    }
}
