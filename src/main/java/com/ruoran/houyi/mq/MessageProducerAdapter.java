package com.ruoran.houyi.mq;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息生产者适配器
 * 根据配置自动选择使用 RocketMQ 或 Redis
 * 
 * @author lh
 */
@Slf4j
@Component
public class MessageProducerAdapter {
    
    @Resource
    private RedisMqConfig redisMqConfig;
    
    @Autowired(required = false)
    private RedisMqProducer redisMqProducer;
    
    @Autowired(required = false)
    private RocketMqProducerAdapter rocketMqProducer;
    
    /**
     * 获取当前使用的消息生产者
     */
    private MessageProducerInterface getProducer() {
        if (redisMqConfig.isEnabled()) {
            return redisMqProducer;
        } else {
            return rocketMqProducer;
        }
    }
    
    /**
     * 发送主消息（构建完成的消息）
     * 
     * @param message 消息内容
     * @param messageKey 消息 Key
     */
    public void send(String message, String messageKey) {
        MessageProducerInterface producer = getProducer();
        if (producer == null) {
            log.error("消息生产者未初始化，无法发送消息");
            throw new IllegalStateException("消息生产者未初始化");
        }
        producer.send(message, messageKey);
    }
    
    /**
     * 发送延迟消息（用于重试）
     * 
     * @param message 消息内容
     * @param messageKey 消息 Key
     * @param delayTimeMs 延迟时间（毫秒）
     */
    public void sendDelayMessage(String message, String messageKey, long delayTimeMs) {
        MessageProducerInterface producer = getProducer();
        if (producer == null) {
            log.error("消息生产者未初始化，无法发送延迟消息");
            throw new IllegalStateException("消息生产者未初始化");
        }
        producer.sendDelayMessage(message, messageKey, delayTimeMs);
    }
    
    /**
     * 发送延迟消息（使用默认延迟时间）
     * 
     * @param message 消息内容
     * @param messageKey 消息 Key
     */
    public void sendDelayMessage(String message, String messageKey) {
        MessageProducerInterface producer = getProducer();
        if (producer == null) {
            log.error("消息生产者未初始化，无法发送延迟消息");
            throw new IllegalStateException("消息生产者未初始化");
        }
        producer.sendDelayMessage(message, messageKey);
    }
}

/**
 * 消息生产者接口
 */
interface MessageProducerInterface {
    void send(String message, String messageKey);
    void sendDelayMessage(String message, String messageKey, long delayTimeMs);
    void sendDelayMessage(String message, String messageKey);
}

