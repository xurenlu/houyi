package com.ruoran.houyi.mq;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 5.0 gRPC SDK 生产者
 * 支持发送 FIFO 顺序消息和延迟消息
 * 
 * @author lh
 */
@Slf4j
@Component
@ConditionalOnBean(Producer.class)
public class HouyiTcpConstructionMessageProduct implements MessageProducerInterface {
    
    @Resource
    private Producer producer;
    
    @Resource
    private ClientServiceProvider clientServiceProvider;
    
    @Resource
    private MqConfig mqConfig;

    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 发送主消息（构建完成的消息）
     * 发送到 RocketMQ 主队列（FIFO 顺序消息）
     * 
     * @param messageBody 消息内容
     * @param messageKey 消息 Key
     */
    @Override
    public void send(String messageBody, String messageKey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        sendFifoMessage(messageBody, messageKey, mqConfig.getTopic());
        stopWatch.stop();
        meterRegistry.summary("houyi_push_cost", Tags.of("type", "rocketmq"))
                .record(stopWatch.getTotalTimeMillis());
    }
    
    /**
     * 发送延迟消息（用于重试）
     * 
     * @param messageBody 消息内容
     * @param messageKey 消息 Key
     * @param delayTimeMs 延迟时间（毫秒）
     */
    @Override
    public void sendDelayMessage(String messageBody, String messageKey, long delayTimeMs) {
        sendDelayMessageToTopic(messageBody, messageKey, mqConfig.getRetryTopic(), delayTimeMs);
    }
    
    /**
     * 发送延迟消息（使用默认延迟时间）
     * 
     * @param messageBody 消息内容
     * @param messageKey 消息 Key
     */
    @Override
    public void sendDelayMessage(String messageBody, String messageKey) {
        sendDelayMessage(messageBody, messageKey, mqConfig.getRetryDelayMs());
    }

    /**
     * 发送 FIFO 顺序消息
     * 
     * @param messageBody 消息内容
     * @param messageKey 消息 Key
     * @param topic Topic 名称
     */
    private void sendFifoMessage(String messageBody, String messageKey, String topic) {
        try {
            // 提取 MessageGroup（用于 FIFO 消息分区）
            String messageGroup = extractShardingKey(messageBody);
            
            log.info("发送 FIFO 消息 - Topic: {}, Tag: {}, Key: {}, MessageGroup: {}", 
                topic, mqConfig.getTag(), messageKey, messageGroup);
            
            // 构建 FIFO 消息（必须设置 MessageGroup）
            Message message = clientServiceProvider.newMessageBuilder()
                .setTopic(topic)
                .setTag(mqConfig.getTag())
                .setKeys(messageKey)
                .setMessageGroup(messageGroup)  // FIFO 消息必须设置 MessageGroup
                .setBody(messageBody.getBytes(StandardCharsets.UTF_8))
                .build();
            
            // 发送消息
            SendReceipt sendReceipt = producer.send(message);
            
            // 记录指标
            meterRegistry.counter("houyi_pushed_msg", 
                Tags.of("service", "rocketmq", "type", "fifo")).increment();
            
            log.debug("FIFO 消息发送成功: topic={}, key={}, msgId={}", 
                topic, messageKey, sendReceipt.getMessageId());
                
        } catch (Exception e) {
            log.error("FIFO 消息发送失败: topic={}, key={}, error={}", topic, messageKey, e.getMessage(), e);
            throw new RuntimeException("RocketMQ FIFO 消息发送失败", e);
        }
    }
    
    /**
     * 发送延迟消息
     * 
     * @param messageBody 消息内容
     * @param messageKey 消息 Key
     * @param topic Topic 名称
     * @param delayTimeMs 延迟时间（毫秒）
     */
    private void sendDelayMessageToTopic(String messageBody, String messageKey, String topic, long delayTimeMs) {
        try {
            log.info("发送延迟消息 - Topic: {}, Tag: {}, Key: {}, DelayMs: {}", 
                topic, mqConfig.getTag(), messageKey, delayTimeMs);
            
            // 计算投递时间
            long deliveryTimestamp = System.currentTimeMillis() + delayTimeMs;
            
            // 构建延迟消息
            Message message = clientServiceProvider.newMessageBuilder()
                .setTopic(topic)
                .setTag(mqConfig.getTag())
                .setKeys(messageKey)
                .setDeliveryTimestamp(deliveryTimestamp)  // 延迟消息设置投递时间
                .setBody(messageBody.getBytes(StandardCharsets.UTF_8))
                .build();
            
            // 发送消息
            SendReceipt sendReceipt = producer.send(message);
            
            // 记录指标
            meterRegistry.counter("houyi_pushed_msg", 
                Tags.of("service", "rocketmq", "type", "delay")).increment();
            
            log.debug("延迟消息发送成功: topic={}, key={}, msgId={}, deliverAt={}", 
                topic, messageKey, sendReceipt.getMessageId(), deliveryTimestamp);
                
        } catch (Exception e) {
            log.error("延迟消息发送失败: topic={}, key={}, error={}", topic, messageKey, e.getMessage(), e);
            throw new RuntimeException("RocketMQ 延迟消息发送失败", e);
        }
    }
    
    /**
     * 从消息中提取 ShardingKey（用作 MessageGroup）
     * 优先使用 from 字段，否则使用随机值
     * 
     * @param message 消息内容
     * @return ShardingKey / MessageGroup
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
        return "group-" + getRandomShardingKey();
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
