package com.ruoran.houyi.mq;

import com.ruoran.houyi.DownloadThreadKeeper;
import com.ruoran.houyi.repo.OriginalMsgRepo;
import com.ruoran.houyi.model.OriginalMsg;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

/**
 * RocketMQ 5.0 重试消费者
 * 消费重试队列中的消息，重新下载失败的文件
 * 使用 Remoting SDK（兼容 4.x 和 5.x）
 * 
 * @author houyi
 */
@Slf4j
@Component
public class HouyiTcpRetryConsumer {

    @Resource(name = "buildRetryConsumer")
    private DefaultMQPushConsumer retryConsumer;
    
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

    @PostConstruct
    public void init() {
        // 仅在生产环境启用重试消费者
        if ("dev".equalsIgnoreCase(env)) {
            log.info("开发环境，跳过重试消费者初始化");
            return;
        }
        
        if (retryConsumer == null) {
            log.warn("重试消费者未创建，跳过初始化");
            return;
        }
        
        try {
            log.info("启动 RocketMQ 5.0 重试消费者: topic={}, groupId={}, tag={}", 
                mqConfig.getRetryTopic(), mqConfig.getRetryGroupId(), mqConfig.getTag());
            
            // 注册消息监听器
            retryConsumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, 
                                                                ConsumeConcurrentlyContext context) {
                    for (MessageExt msg : msgs) {
                        try {
                            if (processMessage(msg)) {
                                // 消息处理成功
                                continue;
                            } else {
                                // 消息处理失败，稍后重试
                                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                            }
                        } catch (Exception e) {
                            log.error("处理重试消息异常: msgId={}, error={}", 
                                msg.getMsgId(), e.getMessage(), e);
                            // 返回 RECONSUME_LATER，让 RocketMQ 稍后重新投递
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });
            
            // 启动消费者
            retryConsumer.start();
            
            log.info("RocketMQ 5.0 重试消费者启动成功");
        } catch (Exception e) {
            log.error("RocketMQ 5.0 重试消费者启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize RocketMQ retry consumer", e);
        }
    }
    
    /**
     * 处理单条消息
     * 
     * @param message 消息
     * @return true 表示成功，false 表示需要重试
     */
    private boolean processMessage(MessageExt message) {
        String msgBody = new String(message.getBody(), StandardCharsets.UTF_8);
        String msgId = message.getMsgId();
        
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
}
