package com.ruoran.houyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大禹配置属性
 * 
 * @author refactored
 */
@Data
@Component
@ConfigurationProperties(prefix = "dayu")
public class HouyiProperties {

    /**
     * 阿里云配置
     */
    private AliyunConfig aliyun = new AliyunConfig();

    /**
     * RocketMQ配置
     */
    private RocketMqConfig rocketmq = new RocketMqConfig();

    /**
     * 下载配置
     */
    private DownloadConfig download = new DownloadConfig();

    /**
     * 阿里云配置
     */
    @Data
    public static class AliyunConfig {
        private String key;
        private String secret;
        private String bucket;
        private String ossEndpoint;
        private String ossAccessKey;
        private String ossAccessSecret;
    }

    /**
     * RocketMQ配置
     */
    @Data
    public static class RocketMqConfig {
        private String endpoint;
        private String instanceId;
        private String topic;
        private String tag;
        private String accessKeyId;
        private String accessKeySecret;
    }

    /**
     * 下载配置
     */
    @Data
    public static class DownloadConfig {
        private String tempPath = "/tmp/";
        private int maxRetryCount = 16;
        private long bigFileTimeoutMs = 5 * 60 * 1000L;
    }
}

