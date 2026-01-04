package com.ruoran.houyi.mq;

import com.ruoran.houyi.DownloadThreadKeeper;
import com.ruoran.houyi.repo.OriginalMsgRepo;
import com.ruoran.houyi.repo.RedisMessageBackupRepo;
import com.ruoran.houyi.model.OriginalMsg;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 消息队列消费者
 * 消费重试队列中的消息，重新下载失败的文件
 * 
 * @author lh
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "redis.mq.enabled", havingValue = "true", matchIfMissing = false)
public class RedisMqConsumer {
    
    @Resource(name = "redisMqConfig")
    private RedisMqConfig mqConfig;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private DownloadThreadKeeper downloadThreadKeeper;
    
    @Resource
    private OriginalMsgRepo originalMsgRepo;
    
    @Resource
    private RedisMessageBackupRepo messageBackupRepo;
    
    @Resource
    private MeterRegistry meterRegistry;
    
    @Value("${spring.profiles.active:dev}")
    private String env;
    
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    @PostConstruct
    public void init() {
        // 仅在生产环境启用重试消费者
        if ("dev".equalsIgnoreCase(env)) {
            log.info("开发环境，跳过 Redis 重试消费者初始化");
            return;
        }
        
        if (!mqConfig.isEnabled()) {
            log.info("Redis MQ 未启用，跳过消费者初始化");
            return;
        }
        
        try {
            log.info("启动 Redis 重试消费者: topic={}, groupId={}, tag={}", 
                mqConfig.getRetryTopic(), mqConfig.getRetryConsumerGroup(), mqConfig.getTag());
            
            // 创建消费者组（如果不存在）
            createConsumerGroupIfNotExists(mqConfig.getRetryTopic(), mqConfig.getRetryConsumerGroup());
            
            // 创建 Stream 消息监听容器
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                    .builder()
                    .pollTimeout(Duration.ofMillis(mqConfig.getPollIntervalMs()))
                    .build();
            
            listenerContainer = StreamMessageListenerContainer.create(
                stringRedisTemplate.getConnectionFactory(),
                options
            );
            
            // 注册消息监听器
            listenerContainer.receive(
                Consumer.from(mqConfig.getRetryConsumerGroup(), "consumer-1"),
                StreamOffset.create(mqConfig.getRetryTopic(), ReadOffset.lastConsumed()),
                new org.springframework.data.redis.stream.StreamListener<String, MapRecord<String, String, String>>() {
                    @Override
                    public void onMessage(MapRecord<String, String, String> record) {
                        try {
                            Map<String, String> valueMap = record.getValue();
                            String body = valueMap.get("body");
                            String key = valueMap.get("key");
                            String tag = valueMap.get("tag");
                            
                            // 检查 Tag 过滤
                            if (!mqConfig.getTag().equals(tag) && !"*".equals(mqConfig.getTag())) {
                                log.debug("消息 Tag 不匹配，跳过: tag={}, expected={}", tag, mqConfig.getTag());
                                return;
                            }
                            
                            String redisMsgId = record.getId().getValue();
                            if (processMessage(body, key, redisMsgId)) {
                                // 消息处理成功，ACK
                                stringRedisTemplate.opsForStream().acknowledge(
                                    mqConfig.getRetryTopic(),
                                    mqConfig.getRetryConsumerGroup(),
                                    record.getId()
                                );
                                
                                // 如果启用了消息备份，标记为已确认
                                if (mqConfig.isEnableMessageBackup()) {
                                    try {
                                        messageBackupRepo.markAsAcknowledged(redisMsgId, System.currentTimeMillis());
                                    } catch (Exception e) {
                                        log.warn("标记消息备份为已确认失败: msgId={}, error={}", redisMsgId, e.getMessage());
                                    }
                                }
                            } else {
                                // 消息处理失败，不 ACK，稍后重试
                                log.warn("消息处理失败，稍后重试: key={}", key);
                            }
                        } catch (Exception e) {
                            log.error("处理消息异常: {}", e.getMessage(), e);
                        }
                    }
                }
            );
            
            // 启动监听容器
            listenerContainer.start();
            running.set(true);
            
            log.info("Redis 重试消费者启动成功");
        } catch (Exception e) {
            log.error("Redis 重试消费者启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Redis retry consumer", e);
        }
    }
    
    /**
     * 创建消费者组（如果不存在）
     */
    private void createConsumerGroupIfNotExists(String streamKey, String groupName) {
        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, groupName);
            log.info("创建消费者组: stream={}, group={}", streamKey, groupName);
        } catch (Exception e) {
            // 消费者组已存在，忽略错误
            log.debug("消费者组已存在或创建失败: {}", e.getMessage());
        }
    }
    
    /**
     * 处理单条消息
     * 
     * @param messageBody 消息体
     * @param messageKey 消息 Key
     * @param messageId 消息 ID
     * @return true 表示成功，false 表示需要重试
     */
    private boolean processMessage(String messageBody, String messageKey, String messageId) {
        try {
            log.info("收到重试消息: msgId={}, key={}", messageId, messageKey);
            
            JSONObject object = new JSONObject(messageBody);
            
            // 验证必需字段
            if (!validateMessage(object)) {
                log.warn("重试消息缺少必需字段，跳过: {}", messageBody);
                meterRegistry.counter("houyi_retry_msg", 
                    Tags.of("result", "invalid")).increment();
                return true; // 无效消息，不再重试
            }
            
            String corpId = object.getString("corp_id");
            String originalMsgId = object.getString("msgid");
            String secret = object.getString("secret");
            long seq = object.getLong("seq");
            int tryCount = object.optInt("tryCount", 0);
            
            log.info("处理重试消息: corpId={}, msgId={}, seq={}, tryCount={}", 
                corpId, originalMsgId, seq, tryCount);
            
            // 检查是否已经成功下载
            Optional<OriginalMsg> originalMsgOpt = 
                originalMsgRepo.findFirstByCorpIdAndMsgIdAndSeq(corpId, originalMsgId, seq);
                
            if (originalMsgOpt.isPresent() && 
                StringUtils.isNotEmpty(originalMsgOpt.get().getOssPath())) {
                log.info("消息已成功下载，跳过重试: corpId={}, msgId={}, seq={}, ossPath={}", 
                    corpId, originalMsgId, seq, originalMsgOpt.get().getOssPath());
                meterRegistry.counter("houyi_retry_msg", 
                    Tags.of("result", "already_success")).increment();
                return true; // 已成功，不再重试
            }
            
            // 提交到下载线程池重新下载
            try {
                downloadThreadKeeper.execute(corpId, originalMsgId, secret, seq, object);
                meterRegistry.counter("houyi_retry_msg", 
                    Tags.of("result", "resubmit")).increment();
                log.info("重试消息已提交到下载线程池: corpId={}, msgId={}, seq={}", 
                    corpId, originalMsgId, seq);
                return true; // 提交成功
                
            } catch (RejectedExecutionException e) {
                log.error("下载线程池已满，稍后重试: corpId={}, msgId={}, seq={}", 
                    corpId, originalMsgId, seq);
                meterRegistry.counter("houyi_retry_msg", 
                    Tags.of("result", "thread_pool_full")).increment();
                return false; // 线程池满，需要重试
            }
            
        } catch (Exception e) {
            log.error("处理重试消息失败: key={}, error={}", messageKey, e.getMessage(), e);
            meterRegistry.counter("houyi_retry_msg", 
                Tags.of("result", "error")).increment();
            return false; // 处理失败，需要重试
        }
    }
    
    /**
     * 验证消息必需字段
     * 
     * @param object 消息 JSON 对象
     * @return 是否有效
     */
    private boolean validateMessage(JSONObject object) {
        return object.has("corp_id") 
            && object.has("msgid") 
            && object.has("secret")
            && object.has("seq");
    }
    
    @PreDestroy
    public void destroy() {
        if (listenerContainer != null && running.get()) {
            try {
                log.info("关闭 Redis 重试消费者");
                listenerContainer.stop();
                running.set(false);
            } catch (Exception e) {
                log.error("关闭 Redis 重试消费者失败", e);
            }
        }
    }
}

