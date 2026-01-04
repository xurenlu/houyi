package com.ruoran.houyi.mq;

import com.ruoran.houyi.DownloadThreadKeeper;
import com.ruoran.houyi.repo.OriginalMsgRepo;
import com.ruoran.houyi.model.OriginalMsg;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RocketMQ 5.0 gRPC SDK 重试消费者
 * 消费重试队列中的延迟消息，重新下载失败的文件
 * 使用 SimpleConsumer 进行消息拉取
 * 
 * @author houyi
 */
@Slf4j
@Component
@ConditionalOnBean(SimpleConsumer.class)
public class HouyiTcpRetryConsumer {

    @Resource
    private SimpleConsumer retrySimpleConsumer;
    
    @Resource
    private MqConfig mqConfig;
    
    @Resource
    private DownloadThreadKeeper downloadThreadKeeper;
    
    @Resource
    private OriginalMsgRepo originalMsgRepo;
    
    @Resource
    private MeterRegistry meterRegistry;
    
    @Value("${spring.profiles.active:dev}")
    private String env;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService consumerExecutor;

    @PostConstruct
    public void init() {
        // 仅在生产环境启用重试消费者
        if ("dev".equalsIgnoreCase(env)) {
            log.info("开发环境，跳过重试消费者初始化");
            return;
        }
        
        if (retrySimpleConsumer == null) {
            log.warn("重试消费者未创建，跳过初始化");
            return;
        }
        
        log.info("启动 RocketMQ 5.0 gRPC 重试消费者: topic={}, groupId={}, tag={}", 
            mqConfig.getRetryTopic(), mqConfig.getRetryGroupId(), mqConfig.getTag());
        
        running.set(true);
        consumerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rocketmq-retry-consumer");
            t.setDaemon(true);
            return t;
        });
        
        consumerExecutor.submit(this::pollMessages);
        
        log.info("RocketMQ 5.0 gRPC 重试消费者启动成功");
    }
    
    /**
     * 轮询消息
     */
    private void pollMessages() {
        while (running.get()) {
            try {
                // 拉取消息，最多等待 30 秒
                List<MessageView> messages = retrySimpleConsumer.receive(32, Duration.ofSeconds(30));
                
                if (messages.isEmpty()) {
                    continue;
                }
                
                log.debug("收到 {} 条重试消息", messages.size());
                
                for (MessageView message : messages) {
                    try {
                        if (processMessage(message)) {
                            // 消息处理成功，确认消息
                            retrySimpleConsumer.ack(message);
                        } else {
                            // 消息处理失败，稍后重试（不 ack，让消息重新投递）
                            log.warn("消息处理失败，等待重新投递: msgId={}", message.getMessageId());
                        }
                    } catch (Exception e) {
                        log.error("处理重试消息异常: msgId={}, error={}", 
                            message.getMessageId(), e.getMessage(), e);
                    }
                }
                
            } catch (Exception e) {
                if (running.get()) {
                    log.error("拉取消息异常: {}", e.getMessage(), e);
                    // 短暂休眠后继续
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.info("重试消费者轮询线程已停止");
    }
    
    /**
     * 处理单条消息
     * 
     * @param message 消息
     * @return true 表示成功，false 表示需要重试
     */
    private boolean processMessage(MessageView message) {
        String msgBody = StandardCharsets.UTF_8.decode(message.getBody()).toString();
        String msgId = message.getMessageId().toString();
        
        try {
            log.info("收到重试消息: msgId={}, topic={}", msgId, message.getTopic());
            
            JSONObject object = new JSONObject(msgBody);
            
            // 验证必需字段
            if (!validateMessage(object)) {
                log.warn("重试消息缺少必需字段，跳过: {}", msgBody);
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
            log.error("处理重试消息失败: msgId={}, error={}", msgId, e.getMessage(), e);
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
        running.set(false);
        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
            log.info("重试消费者执行器已关闭");
        }
    }
}
