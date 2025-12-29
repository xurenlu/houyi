package com.ruoran.houyi;

import com.mchange.lang.ByteUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * @author renlu
 * created by renlu at 2021/7/13 9:20 下午
 */
@Slf4j
public class Md5Util {
    public static String getMd5(String str){
        String hexStr = "";
        try{
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(str.getBytes(StandardCharsets.UTF_8));
            hexStr = ByteUtils.toLowercaseHexAscii(digest);
            return hexStr;
        }catch (Exception e){
            log.error("md5 error:{}",e.getMessage(),e);
        }
        return hexStr;
    }
}
