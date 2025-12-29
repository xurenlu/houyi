package com.ruoran.houyi.exception;

/**
 * 下载异常
 *
 * @author refactored
 */
public class DownloadException extends RuntimeException {
    
    private final String msgId;
    private final int errorCode;

    public DownloadException(String message, String msgId) {
        super(message);
        this.msgId = msgId;
        this.errorCode = -1;
    }

    public DownloadException(String message, String msgId, int errorCode) {
        super(message);
        this.msgId = msgId;
        this.errorCode = errorCode;
    }

    public DownloadException(String message, String msgId, Throwable cause) {
        super(message, cause);
        this.msgId = msgId;
        this.errorCode = -1;
    }

    public String getMsgId() {
        return msgId;
    }

    public int getErrorCode() {
        return errorCode;
    }
}

