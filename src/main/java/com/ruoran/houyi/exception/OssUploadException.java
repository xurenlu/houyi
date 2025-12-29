package com.ruoran.houyi.exception;

/**
 * OSS 上传异常
 *
 * @author refactored
 */
public class OssUploadException extends RuntimeException {
    
    private final String localPath;
    private final String ossPath;

    public OssUploadException(String message, String localPath, String ossPath) {
        super(message);
        this.localPath = localPath;
        this.ossPath = ossPath;
    }

    public OssUploadException(String message, String localPath, String ossPath, Throwable cause) {
        super(message, cause);
        this.localPath = localPath;
        this.ossPath = ossPath;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getOssPath() {
        return ossPath;
    }
}

