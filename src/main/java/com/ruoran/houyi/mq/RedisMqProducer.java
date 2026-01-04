package com.ruoran.houyi.mq;

import com.ruoran.houyi.model.DelayMessage;
import com.ruoran.houyi.model.RedisMessageBackup;
import com.ruoran.houyi.repo.DelayMessageRepo;
import com.ruoran.houyi.repo.RedisMessageBackupRepo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 消息队列生产者
 * 使用 Redis Streams 实现消息队列功能
 * 
 * @author lh
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "redis.mq.enabled", havingValue = "true", matchIfMissing = false)
public class RedisMqProducer implements MessageProducerInterface {
    
    @Resource(name = "redisMqConfig")
    private RedisMqConfig mqConfig;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private MeterRegistry meterRegistry;
    
    @Resource
    private DelayMessageRepo delayMessageRepo;
    
    @Resource
    private RedisMessageBackupRepo messageBackupRepo;
    
    /**
     * 发送主消息（构建完成的消息）
     * 
     * @param message 消息内容
     * @param messageKey 消息 Key
     */
    public void send(String message, String messageKey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        sendToRedis(message, messageKey, mqConfig.getTopic(), false, 0);
        stopWatch.stop();
        meterRegistry.summary("houyi_push_cost", Tags.of("type", "redis"))
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
        sendToRedis(message, messageKey, mqConfig.getRetryTopic(), true, delayTimeMs);
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
     * 发送消息到 Redis
     * 
     * @param messageBody 消息内容
     * @param messageKey 消息 Key
     * @param topic Topic 名称（Stream Key）
     * @param isDelay 是否延迟消息
     * @param delayTimeMs 延迟时间（毫秒）
     */
    private void sendToRedis(String messageBody, String messageKey, String topic, 
                            boolean isDelay, long delayTimeMs) {
        try {
            log.info("发送消息到 Redis - Topic: {}, Tag: {}, Key: {}, Delay: {}ms", 
                topic, mqConfig.getTag(), messageKey, isDelay ? delayTimeMs : 0);
            
            // 提取 ShardingKey
            String shardingKey = extractShardingKey(messageBody);
            
            // 构建消息元数据
            Map<String, String> messageFields = new HashMap<>();
            messageFields.put("body", messageBody);
            messageFields.put("key", messageKey);
            messageFields.put("tag", mqConfig.getTag());
            if (shardingKey != null && !shardingKey.equals(messageKey)) {
                messageFields.put("shardingKey", shardingKey);
            }
            
            if (isDelay && delayTimeMs > 0) {
                // 延迟消息：存储到数据库
                long deliverTimestamp = System.currentTimeMillis() + delayTimeMs;
                
                DelayMessage delayMessage = new DelayMessage();
                delayMessage.setTopic(topic);
                delayMessage.setMessageBody(messageBody);
                delayMessage.setMessageKey(messageKey);
                delayMessage.setTag(mqConfig.getTag());
                delayMessage.setShardingKey(shardingKey);
                delayMessage.setDeliverTime(deliverTimestamp);
                delayMessage.setStatus(0); // 待投递
                delayMessage.setRetryCount(0);
                delayMessage.setCreateAt(System.currentTimeMillis());
                
                // 保存到数据库
                delayMessageRepo.save(delayMessage);
                
                log.debug("延迟消息已保存到数据库: key={}, deliverTime={}, id={}", 
                    messageKey, deliverTimestamp, delayMessage.getId());
                
                meterRegistry.counter("houyi_pushed_msg", 
                    Tags.of("service", "redis", "type", "delay")).increment();
            } else {
                // 普通消息：直接发送到 Redis Stream
                org.springframework.data.redis.connection.stream.RecordId messageId = 
                    stringRedisTemplate.opsForStream().add(topic, messageFields);
                
                String redisMsgId = messageId.getValue();
                
                log.debug("消息发送成功: topic={}, key={}, msgId={}", 
                    topic, messageKey, redisMsgId);
                
                // 如果启用了消息备份，保存到数据库
                if (mqConfig.isEnableMessageBackup()) {
                    try {
                        RedisMessageBackup backup = new RedisMessageBackup();
                        backup.setTopic(topic);
                        backup.setMessageBody(messageBody);
                        backup.setMessageKey(messageKey);
                        backup.setRedisMsgId(redisMsgId);
                        backup.setTag(mqConfig.getTag());
                        backup.setShardingKey(shardingKey);
                        backup.setStatus(0); // 已发送到 Redis
                        backup.setCreateAt(System.currentTimeMillis());
                        
                        messageBackupRepo.save(backup);
                        log.debug("消息已备份到数据库: key={}, backupId={}", messageKey, backup.getId());
                    } catch (Exception e) {
                        // 备份失败不影响主流程，只记录日志
                        log.warn("消息备份失败（不影响发送）: key={}, error={}", messageKey, e.getMessage());
                    }
                }
                
                meterRegistry.counter("houyi_pushed_msg", 
                    Tags.of("service", "redis", "type", "normal")).increment();
            }
            
        } catch (Exception e) {
            log.error("消息发送失败: topic={}, key={}, error={}", 
                topic, messageKey, e.getMessage(), e);
            meterRegistry.counter("houyi_pushed_msg", 
                Tags.of("service", "redis", "type", "error")).increment();
            throw new RuntimeException("Redis 消息发送失败", e);
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

