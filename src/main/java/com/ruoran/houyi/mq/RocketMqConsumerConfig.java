package com.ruoran.houyi.mq;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 5.0 消费者配置
 * 使用 Remoting SDK（兼容 4.x 和 5.x）
 * 
 * @author houyi
 */
@Slf4j
@Configuration
public class RocketMqConsumerConfig {
    
    @Resource
    private MqConfig mqConfig;
    
    @Resource
    private AliyunConfig aliyunConfig;
    
    @Value("${spring.profiles.active:dev}")
    private String env;
    
    private DefaultMQPushConsumer retryConsumer;
    private DefaultMQPushConsumer mainConsumer;
    
    /**
     * 主队列 Consumer（Dummy Consumer，仅用于建立订阅关系）
     */
    @Bean
    public DefaultMQPushConsumer buildMainConsumer() throws MQClientException {
        log.info("初始化 RocketMQ 5.0 主队列 Consumer: endpoint={}, namespace={}, groupId={}, topic={}", 
            mqConfig.getEndpoint(), mqConfig.getNamespace(), mqConfig.getGroupId(), mqConfig.getTopic());
        
        // 创建 ACL RPCHook
        RPCHook rpcHook = new AclClientRPCHook(
            new SessionCredentials(aliyunConfig.getKey(), aliyunConfig.getSecret())
        );
        
        // 创建 Consumer
        mainConsumer = new DefaultMQPushConsumer(mqConfig.getGroupId(), rpcHook);
        
        // 1. 设置 Consumer Group
        mainConsumer.setConsumerGroup(mqConfig.getGroupId());
        
        // 2. 设置接入方式为阿里云
        mainConsumer.setAccessChannel(org.apache.rocketmq.client.AccessChannel.CLOUD);
        
        // 3. 开启消息轨迹
        mainConsumer.setEnableTrace(true);
        
        // 4. 设置 NameServer 地址
        mainConsumer.setNamesrvAddr(mqConfig.getEndpoint());
        
        // 5. RocketMQ 5.0 Serverless 不需要设置 Namespace
        // Namespace 已经包含在 NameServer 地址（实例 ID）中
        // mainConsumer.setNamespaceV2(mqConfig.getNamespace());
        
        // 6. 订阅 Topic 和 Tag
        mainConsumer.subscribe(mqConfig.getTopic(), mqConfig.getTag());
        
        // 设置消费模式为集群消费
        mainConsumer.setMessageModel(org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel.CLUSTERING);
        
        // 设置消费线程数
        mainConsumer.setConsumeThreadMin(1);
        mainConsumer.setConsumeThreadMax(4);
        
        log.info("RocketMQ 5.0 主队列 Consumer 初始化成功（未启动）");
        
        return mainConsumer;
    }
    
    /**
     * 重试队列 Consumer
     */
    @Bean
    public DefaultMQPushConsumer buildRetryConsumer() throws MQClientException {
        // 仅在生产环境创建消费者
        if ("dev".equalsIgnoreCase(env)) {
            log.info("开发环境，跳过重试消费者创建");
            return null;
        }
        
        log.info("初始化 RocketMQ 5.0 重试消费者: endpoint={}, namespace={}, groupId={}, topic={}", 
            mqConfig.getEndpoint(), mqConfig.getNamespace(), mqConfig.getRetryGroupId(), mqConfig.getRetryTopic());
        
        // 创建 ACL RPCHook
        RPCHook rpcHook = new AclClientRPCHook(
            new SessionCredentials(aliyunConfig.getKey(), aliyunConfig.getSecret())
        );
        
        // 创建 Consumer（参照 ffcrm 项目的配置顺序）
        retryConsumer = new DefaultMQPushConsumer(mqConfig.getRetryGroupId(), rpcHook);
        
        // 1. 设置 Consumer Group（显式设置，确保生效）
        retryConsumer.setConsumerGroup(mqConfig.getRetryGroupId());
        
        // 2. 设置接入方式为阿里云
        retryConsumer.setAccessChannel(org.apache.rocketmq.client.AccessChannel.CLOUD);
        
        // 3. 开启消息轨迹
        retryConsumer.setEnableTrace(true);
        
        // 4. 设置 NameServer 地址
        retryConsumer.setNamesrvAddr(mqConfig.getEndpoint());
        
        // 5. RocketMQ 5.0 Serverless 不需要设置 Namespace
        // Namespace 已经包含在 NameServer 地址（实例 ID）中
        // 注释掉，避免 Topic 名称变成 "namespace%topic" 格式
        // retryConsumer.setNamespaceV2(mqConfig.getNamespace());
        
        // 6. 订阅 Topic 和 Tag
        retryConsumer.subscribe(mqConfig.getRetryTopic(), mqConfig.getTag());
        
        // 设置消费模式为集群消费
        retryConsumer.setMessageModel(org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel.CLUSTERING);
        
        // 设置消费线程数
        retryConsumer.setConsumeThreadMin(1);
        retryConsumer.setConsumeThreadMax(4);
        
        log.info("RocketMQ 5.0 重试消费者初始化成功（未启动）");
        
        return retryConsumer;
    }
    
    @PreDestroy
    public void destroy() {
        if (mainConsumer != null) {
            try {
                log.info("关闭 RocketMQ 主队列 Consumer");
                mainConsumer.shutdown();
            } catch (Exception e) {
                log.error("关闭 RocketMQ 主队列 Consumer 失败", e);
            }
        }
        
        if (retryConsumer != null) {
            try {
                log.info("关闭 RocketMQ 重试消费者");
                retryConsumer.shutdown();
            } catch (Exception e) {
                log.error("关闭 RocketMQ 重试消费者失败", e);
            }
        }
    }
}
