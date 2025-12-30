package com.ruoran.houyi.mq;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.bean.ProducerBean;
import com.aliyun.openservices.ons.api.exception.ONSClientException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import jakarta.annotation.Resource;

/**
 * RocketMQ 5.0 TCP 生产者
 * 统一使用 TCP 协议，支持主消息队列和重试队列
 * 
 * @author lh
 */
@Slf4j
@Component
public class HouyiTcpConstructionMessageProduct {
    
    @Resource
    private ProducerBean producer;
    
    @Resource
    private MqConfig mqConfig;

    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 发送主消息（构建完成的消息）
     * 发送到 RocketMQ
     * 
     * @param message 消息内容
     * @param messageKey 消息 Key
     */
    public void send(String message, String messageKey) {
        // 发送到 RocketMQ
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        sendToRocketMQ(message, messageKey, mqConfig.getTopic(), false);
        stopWatch.stop();
        meterRegistry.summary("houyi_push_cost", Tags.of("type", "rocketmq"))
                .record(stopWatch.getTotalTimeMillis());
    }
    
    /**
     * 发送延迟消息（用于重试）
     * 
     * @param message 消息内容
     * @param messageKey 消息 Key
     * @param delayTimeMs 延迟时间（毫秒）
     */
    public void sendDelayMessage(String message, String messageKey, long delayTimeMs) {
        sendToRocketMQ(message, messageKey, mqConfig.getRetryTopic(), true, delayTimeMs);
    }
    
    /**
     * 发送延迟消息（使用默认延迟时间）
     * 
     * @param message 消息内容
     * @param messageKey 消息 Key
     */
    public void sendDelayMessage(String message, String messageKey) {
        sendDelayMessage(message, messageKey, mqConfig.getRetryDelayMs());
    }

    /**
     * 发送消息到 RocketMQ
     * 
     * @param message 消息内容
     * @param messageKey 消息 Key
     * @param topic Topic 名称
     * @param isDelay 是否延迟消息
     */
    private void sendToRocketMQ(String message, String messageKey, String topic, boolean isDelay) {
        sendToRocketMQ(message, messageKey, topic, isDelay, 0);
    }
    
    /**
     * 发送消息到 RocketMQ
     * 
     * @param message 消息内容
     * @param messageKey 消息 Key
     * @param topic Topic 名称
     * @param isDelay 是否延迟消息
     * @param delayTimeMs 延迟时间（毫秒）
     */
    private void sendToRocketMQ(String message, String messageKey, String topic, 
                                boolean isDelay, long delayTimeMs) {
        Message msg = new Message(topic, mqConfig.getTag(), message.getBytes());

        try {
            msg.setKey(messageKey);
            
            // 设置 ShardingKey（用于顺序消息）
            String shardingKey = extractShardingKey(message);
            msg.setShardingKey(shardingKey);
            
            // 设置延迟时间
            if (isDelay && delayTimeMs > 0) {
                msg.setStartDeliverTime(System.currentTimeMillis() + delayTimeMs);
            }
            
            SendResult sendResult = producer.send(msg);
            
            // 记录指标
            String messageType = isDelay ? "retry" : "normal";
            meterRegistry.counter("houyi_pushed_msg", 
                Tags.of("service", "rocketmq", "type", messageType)).increment();
            
            log.debug("消息发送成功: topic={}, key={}, msgId={}, type={}", 
                topic, messageKey, sendResult.getMessageId(), messageType);
                
        } catch (ONSClientException e) {
            log.error("消息发送失败: topic={}, key={}, error={}", topic, messageKey, e.getMessage(), e);
            throw new RuntimeException("RocketMQ 消息发送失败", e);
        }
    }
    
    /**
     * 从消息中提取 ShardingKey
     * 优先使用 from 字段，否则使用随机值
     * 
     * @param message 消息内容
     * @return ShardingKey
     */
    private String extractShardingKey(String message) {
        try {
            JSONObject obj = new JSONObject(message);
            if (obj.has("from")) {
                String from = obj.getString("from");
                meterRegistry.counter("houyi_shard_key", Tags.of("source", "from")).increment();
                return from;
            }
        } catch (Exception e) {
            log.debug("解析消息 from 字段失败，使用随机 ShardingKey", e);
        }
        
        meterRegistry.counter("houyi_shard_key", Tags.of("source", "random")).increment();
        return String.valueOf(getRandomShardingKey());
    }
    
    /**
     * 生成随机 ShardingKey（0-15）
     * 
     * @return 随机 ShardingKey
     */
    private static int getRandomShardingKey() {
        return (int) (Math.random() * 16);
    }
}
