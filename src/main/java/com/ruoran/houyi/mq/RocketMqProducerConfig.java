package com.ruoran.houyi.mq;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 5.0 生产者配置
 * 使用 Remoting SDK（兼容 4.x 和 5.x）
 * 
 * @author houyi
 */
@Slf4j
@Configuration
public class RocketMqProducerConfig {
    
    @Resource
    private MqConfig mqConfig;
    
    @Resource
    private AliyunConfig aliyunConfig;
    
    private DefaultMQProducer producer;
    
    @Bean
    public DefaultMQProducer buildProducer() throws MQClientException {
        log.info("========================================");
        log.info("初始化 RocketMQ 5.0 Producer");
        log.info("Endpoint: {}", mqConfig.getEndpoint());
        log.info("Namespace: {}", mqConfig.getNamespace());
        log.info("Group ID: {}", mqConfig.getGroupId());
        log.info("AccessKey: {}", aliyunConfig.getKey());
        log.info("========================================");
        
        // 创建 ACL RPCHook
        RPCHook rpcHook = new AclClientRPCHook(
            new SessionCredentials(aliyunConfig.getKey(), aliyunConfig.getSecret())
        );
        
        // 创建 Producer（参照 ffcrm 项目的配置顺序）
        producer = new DefaultMQProducer(mqConfig.getGroupId(), rpcHook);
        
        // 1. 设置 Producer Group（显式设置，确保生效）
        producer.setProducerGroup(mqConfig.getGroupId());
        
        // 2. 设置接入方式为阿里云
        producer.setAccessChannel(org.apache.rocketmq.client.AccessChannel.CLOUD);
        
        // 3. 开启消息轨迹
        producer.setEnableTrace(true);
        
        // 4. 设置 NameServer 地址
        producer.setNamesrvAddr(mqConfig.getEndpoint());
        
        // 5. RocketMQ 5.0 Serverless 不需要设置 Namespace
        // Namespace 已经包含在 NameServer 地址（实例 ID）中
        // 注释掉，避免 Topic 名称变成 "namespace%topic" 格式
        // producer.setNamespaceV2(mqConfig.getNamespace());
        
        // 其他配置
        producer.setSendMsgTimeout(3000);
        
        // 启动 Producer
        producer.start();
        
        log.info("========================================");
        log.info("RocketMQ 5.0 Producer 启动成功！");
        log.info("Producer Group: {}", producer.getProducerGroup());
        log.info("Namespace: {}", producer.getNamespace());
        log.info("NameServer: {}", producer.getNamesrvAddr());
        
        // 测试 Topic 路由
        try {
            log.info("尝试获取 Topic 路由信息...");
            org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl impl = 
                (org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl) producer.getDefaultMQProducerImpl();
            org.apache.rocketmq.common.message.MessageQueue[] queues = 
                impl.getTopicPublishInfoTable().get(mqConfig.getTopic()) != null ? 
                impl.getTopicPublishInfoTable().get(mqConfig.getTopic()).getMessageQueueList().toArray(new org.apache.rocketmq.common.message.MessageQueue[0]) : null;
            if (queues != null && queues.length > 0) {
                log.info("Topic {} 路由信息: {} 个队列", mqConfig.getTopic(), queues.length);
            } else {
                log.warn("警告：Topic {} 的路由信息为空！", mqConfig.getTopic());
            }
        } catch (Exception e) {
            log.warn("无法获取 Topic 路由信息: {}", e.getMessage());
        }
        
        log.info("========================================");
        
        return producer;
    }
    
    @PreDestroy
    public void destroy() {
        if (producer != null) {
            try {
                log.info("关闭 RocketMQ Producer");
                producer.shutdown();
            } catch (Exception e) {
                log.error("关闭 RocketMQ Producer 失败", e);
            }
        }
    }
}
