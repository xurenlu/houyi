package com.ruoran.houyi.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 5.0 配置类
 * 使用 RocketMQ 5.0 gRPC SDK
 * 
 * @author lh
 */
@Configuration
@ConfigurationProperties(prefix = "rocketmq")
@Data
public class MqConfig {

    /**
     * 接入点地址（RocketMQ 5.0 gRPC 端点）
     * 格式：rmq-cn-xxx.cn-shanghai.rmq.aliyuncs.com:8080
     */
    private String endpoint;
    
    /**
     * 实例用户名（从 RocketMQ 实例详情页面获取）
     */
    private String username;
    
    /**
     * 实例密码（从 RocketMQ 实例详情页面获取）
     */
    private String password;
    
    /**
     * 主消息队列 Topic（FIFO 顺序消息类型）
     */
    private String topic;
    
    /**
     * 重试队列 Topic（延迟消息类型）
     */
    private String retryTopic;
    
    /**
     * 主消息队列 Consumer Group ID
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
     * 重试延迟时间（毫秒），默认 30 秒
     */
    private long retryDelayMs = 30000;
}
