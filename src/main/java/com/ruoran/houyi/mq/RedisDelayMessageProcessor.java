package com.ruoran.houyi.mq;

import com.ruoran.houyi.model.DelayMessage;
import com.ruoran.houyi.repo.DelayMessageRepo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 延迟消息处理器
 * 定期扫描延迟队列（Sorted Set），将到期的消息投递到目标 Stream
 * 
 * @author lh
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "redis.mq.enabled", havingValue = "true", matchIfMissing = false)
public class RedisDelayMessageProcessor {
    
    @Resource(name = "redisMqConfig")
    private RedisMqConfig mqConfig;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private DelayMessageRepo delayMessageRepo;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    @PostConstruct
    public void init() {
        if (!mqConfig.isEnabled()) {
            log.info("Redis MQ 未启用，跳过延迟消息处理器初始化");
            return;
        }
        
        running.set(true);
        log.info("Redis 延迟消息处理器已启动");
    }
    
    /**
     * 定期扫描数据库延迟消息表，将到期的消息投递到目标 Stream
     * 每 1 秒执行一次（可通过配置调整）
     */
    @Scheduled(fixedDelayString = "${redis.mq.delay-scan-interval-ms:1000}")
    @Transactional
    public void processDelayMessages() {
        if (!running.get() || !mqConfig.isEnabled()) {
            return;
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            
            // 从数据库查询到期的消息（每次处理一批，避免一次性加载太多）
            Pageable pageable = PageRequest.of(0, mqConfig.getBatchSize());
            List<DelayMessage> expiredMessages = delayMessageRepo.findExpiredMessages(currentTime, pageable);
            
            if (expiredMessages == null || expiredMessages.isEmpty()) {
                return;
            }
            
            log.debug("发现 {} 条延迟消息到期", expiredMessages.size());
            
            for (DelayMessage delayMessage : expiredMessages) {
                try {
                    String topic = delayMessage.getTopic();
                    String body = delayMessage.getMessageBody();
                    String key = delayMessage.getMessageKey();
                    String tag = delayMessage.getTag() != null ? delayMessage.getTag() : mqConfig.getTag();
                    
                    // 构建消息字段
                    Map<String, String> messageFields = new HashMap<>();
                    messageFields.put("body", body);
                    messageFields.put("key", key != null ? key : "");
                    messageFields.put("tag", tag);
                    if (delayMessage.getShardingKey() != null) {
                        messageFields.put("shardingKey", delayMessage.getShardingKey());
                    }
                    
                    // 投递到目标 Stream
                    stringRedisTemplate.opsForStream().add(topic, messageFields);
                    
                    // 更新数据库状态为已投递
                    delayMessageRepo.markAsDelivered(delayMessage.getId(), System.currentTimeMillis());
                    
                    log.debug("延迟消息已投递: topic={}, key={}, id={}, deliverTime={}", 
                        topic, key, delayMessage.getId(), delayMessage.getDeliverTime());
                    
                } catch (Exception e) {
                    log.error("处理延迟消息失败: id={}, error={}", 
                        delayMessage.getId(), e.getMessage(), e);
                    
                    // 更新状态为投递失败
                    try {
                        delayMessageRepo.markAsFailed(delayMessage.getId(), 
                            e.getMessage() != null ? e.getMessage() : "未知错误");
                    } catch (Exception ex) {
                        log.error("更新延迟消息状态失败: id={}", delayMessage.getId(), ex);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("扫描延迟消息表失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定期清理已投递的旧消息（保留最近 7 天的记录）
     * 每天执行一次
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2 点执行
    @Transactional
    public void cleanDeliveredMessages() {
        if (!running.get() || !mqConfig.isEnabled()) {
            return;
        }
        
        try {
            // 清理 7 天前的已投递消息
            long beforeTime = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            delayMessageRepo.cleanDeliveredMessages(beforeTime);
            log.info("清理已投递的延迟消息: 清理时间点 {}", beforeTime);
        } catch (Exception e) {
            log.error("清理延迟消息失败: {}", e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        running.set(false);
        log.info("Redis 延迟消息处理器已停止");
    }
}

