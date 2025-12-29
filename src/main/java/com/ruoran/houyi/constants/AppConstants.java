package com.ruoran.houyi.constants;

/**
 * 应用常量类
 *
 * @author refactored
 */
public class AppConstants {

    private AppConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 重试相关常量
     */
    public static class Retry {
        public static final int MAX_TRY_COUNT = 16;
        public static final long BIG_FILE_TIMEOUT_MS = 5 * 60 * 1000L; // 5分钟
    }

    /**
     * Redis key 前缀
     */
    public static class RedisKey {
        public static final String MD5_PREFIX = "md5sum-";
        public static final String LAST_SEQ_PREFIX = "last_seq_";
        public static final String NO_MD5SUM_FILE_PREFIX = "no_md5sum_file_";
    }

    /**
     * Redis 过期时间（秒）
     */
    public static class RedisExpire {
        public static final int MD5_CACHE_EXPIRE = 60 * 60 * 24 * 3; // 3天
        public static final int DOWNLOADING_FLAG_EXPIRE = 60 * 5; // 5分钟
        public static final int NO_MD5SUM_FILE_EXPIRE = 7200; // 2小时
    }

    /**
     * 文件扩展名
     */
    public static class FileExt {
        public static final String JPG = ".jpg";
        public static final String PNG = ".png";
        public static final String GIF = ".gif";
        public static final String AMR = ".amr";
        public static final String MP3 = ".mp3";
        public static final String MP4 = ".mp4";
    }

    /**
     * 特殊消息类型阈值
     */
    public static class MessageThreshold {
        public static final int HIGH_FREQUENCY_FILE_TIMES = 100;
    }
}

