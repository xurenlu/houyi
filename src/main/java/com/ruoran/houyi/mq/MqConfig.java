package com.ruoran.houyi.mq;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * RocketMQ 5.0 统一配置类
 * 使用 TCP 协议，支持主消息队列和重试队列
 * 
 * @author lh
 */
@Configuration
@ConfigurationProperties(prefix = "rocketmq")
@Data
public class MqConfig {

    /**
     * NameServer 地址
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
     * RocketMQ 5.0 命名空间（可选，Serverless 版需要）
     */
    private String namespace;
    
    /**
     * 公网接入点（可选）
     */
    private String publicEndpoint;
    
    /**
     * 重试延迟时间（毫秒），默认 30 秒
     */
    private long retryDelayMs = 30000;
    
    /**
     * 获取 RocketMQ 配置属性
     * 
     * @param aliyunConfig 阿里云配置
     * @return Properties
     */
    public Properties getMqProperties(AliyunConfig aliyunConfig) {
        return getMqPropertie(aliyunConfig);
    }
    
    /**
     * 获取 RocketMQ 配置属性（保留旧方法名用于兼容）
     * 
     * @param aliyunConfig 阿里云配置
     * @return Properties
     */
    @Deprecated
    public Properties getMqPropertie(AliyunConfig aliyunConfig) {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.AccessKey, aliyunConfig.getKey());
        properties.setProperty(PropertyKeyConst.SecretKey, aliyunConfig.getSecret());
        properties.setProperty(PropertyKeyConst.NAMESRV_ADDR, this.nameSrvAddr);
        
        // RocketMQ 5.0 命名空间支持（如果 SDK 支持）
        if (this.namespace != null && !this.namespace.isEmpty()) {
            try {
                properties.setProperty("INSTANCE_ID", this.namespace);
            } catch (Exception e) {
                // 忽略不支持的属性
            }
        }
        
        return properties;
    }
    
    /**
     * 获取重试消费者的配置属性（包含 Group ID）
     * 
     * @param aliyunConfig 阿里云配置
     * @return Properties
     */
    public Properties getRetryConsumerProperties(AliyunConfig aliyunConfig) {
        Properties properties = getMqProperties(aliyunConfig);
        properties.setProperty(PropertyKeyConst.GROUP_ID, this.retryGroupId);
        return properties;
    }
}
