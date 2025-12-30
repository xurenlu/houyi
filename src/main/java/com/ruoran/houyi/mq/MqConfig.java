package com.ruoran.houyi.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 5.0 配置类
 * 使用新的 RocketMQ 5.0 Java SDK
 * 
 * @author lh
 */
@Configuration
@ConfigurationProperties(prefix = "rocketmq")
@Data
public class MqConfig {

    /**
     * NameServer 地址（RocketMQ 5.0 的接入点）
     * 格式：rmq-cn-xxx.cn-shanghai.rmq.aliyuncs.com:8080
     */
    private String nameSrvAddr;
    
    /**
     * 主消息队列 Topic
     */
    private String topic;
    
    /**
     * 重试队列 Topic
     */
    private String retryTopic;
    
    /**
     * 主消息队列 Producer/Consumer Group ID
     */
    private String groupId;
    
    /**
     * 重试队列 Consumer Group ID
     */
    private String retryGroupId;
    
    /**
     * 消息 Tag
     */
    private String tag;
    
    /**
     * RocketMQ 5.0 实例 ID（命名空间）
     * 格式：rmq-cn-xxx
     */
    private String namespace;
    
    /**
     * 重试延迟时间（毫秒），默认 30 秒
     */
    private long retryDelayMs = 30000;
    
    /**
     * 获取完整的接入点地址
     * 格式：rmq-cn-xxx.cn-shanghai.rmq.aliyuncs.com:8080
     */
    public String getEndpoint() {
        return nameSrvAddr;
    }
}
