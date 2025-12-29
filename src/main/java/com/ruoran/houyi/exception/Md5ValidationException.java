package com.ruoran.houyi.exception;

/**
 * MD5 校验异常
 *
 * @author refactored
 */
public class Md5ValidationException extends DownloadException {
    
    private final String expectedMd5;
    private final String actualMd5;

    public Md5ValidationException(String msgId, String expectedMd5, String actualMd5) {
        super(String.format("MD5校验失败, 期望:%s, 实际:%s", expectedMd5, actualMd5), msgId);
        this.expectedMd5 = expectedMd5;
        this.actualMd5 = actualMd5;
    }

    public String getExpectedMd5() {
        return expectedMd5;
    }

    public String getActualMd5() {
        return actualMd5;
    }
}

