package com.ruoran.houyi.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 消息队列配置类
 * 
 * @author lh
 */
@Configuration
@ConfigurationProperties(prefix = "redis.mq")
@Data
public class RedisMqConfig {

    /**
     * 是否启用 Redis 消息队列（替代 RocketMQ）
     */
    private boolean enabled = false;
    
    /**
     * 主消息队列 Stream Key
     */
    private String topic = "mq:topic:main";
    
    /**
     * 重试队列 Stream Key
     */
    private String retryTopic = "mq:topic:retry";
    
    /**
     * 延迟消息 Sorted Set Key
     */
    private String delayQueueKey = "mq:delay:queue";
    
    /**
     * 消费者组名称
     */
    private String consumerGroup = "houyi-consumer-group";
    
    /**
     * 重试消费者组名称
     */
    private String retryConsumerGroup = "houyi-retry-consumer-group";
    
    /**
     * 消息 Tag（用于过滤）
     */
    private String tag = "default";
    
    /**
     * 重试延迟时间（毫秒），默认 30 秒
     */
    private long retryDelayMs = 30000;
    
    /**
     * 消费者批处理大小
     */
    private int batchSize = 10;
    
    /**
     * 消费者轮询间隔（毫秒）
     */
    private long pollIntervalMs = 1000;
    
    /**
     * 延迟消息扫描间隔（毫秒）
     */
    private long delayScanIntervalMs = 1000;
}

