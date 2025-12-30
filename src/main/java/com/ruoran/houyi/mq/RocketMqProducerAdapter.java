package com.ruoran.houyi.mq;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 生产者适配器
 * 让 RocketMQ 生产者实现 MessageProducerInterface 接口
 * 
 * @author lh
 */
@Slf4j
@Component
public class RocketMqProducerAdapter implements MessageProducerInterface {
    
    @Resource
    private HouyiTcpConstructionMessageProduct rocketMqProducer;
    
    @Override
    public void send(String message, String messageKey) {
        rocketMqProducer.send(message, messageKey);
    }
    
    @Override
    public void sendDelayMessage(String message, String messageKey, long delayTimeMs) {
        rocketMqProducer.sendDelayMessage(message, messageKey, delayTimeMs);
    }
    
    @Override
    public void sendDelayMessage(String message, String messageKey) {
        rocketMqProducer.sendDelayMessage(message, messageKey);
    }
}

