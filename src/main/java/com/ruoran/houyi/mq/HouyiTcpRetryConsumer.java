package com.ruoran.houyi.mq;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.bean.ConsumerBean;
import com.aliyun.openservices.ons.api.bean.Subscription;
import com.ruoran.houyi.DownloadThreadKeeper;
import com.ruoran.houyi.repo.OriginalMsgRepo;
import com.ruoran.houyi.model.OriginalMsg;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

/**
 * RocketMQ 5.0 TCP 重试消费者
 * 消费重试队列中的消息，重新下载失败的文件
 * 
 * @author houyi
 */
@Slf4j
@Component
public class HouyiTcpRetryConsumer {

    @Resource
    private ConsumerBean retryConsumer;
    
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
        
        try {
            log.info("初始化 RocketMQ TCP 重试消费者: topic={}, groupId={}, tag={}", 
                mqConfig.getRetryTopic(), mqConfig.getRetryGroupId(), mqConfig.getTag());
            
            // 订阅重试队列
            Map<Subscription, MessageListener> subscriptionTable = new HashMap<>();
            Subscription subscription = new Subscription();
            subscription.setTopic(mqConfig.getRetryTopic());
            subscription.setExpression(mqConfig.getTag());
            
            subscriptionTable.put(subscription, new RetryMessageListener());
            retryConsumer.setSubscriptionTable(subscriptionTable);
            
            // 手动启动消费者
            retryConsumer.start();
            
            log.info("RocketMQ TCP 重试消费者初始化完成");
        } catch (Exception e) {
            log.error("RocketMQ TCP 重试消费者初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize RocketMQ retry consumer", e);
        }
    }
    
    /**
     * 重试消息监听器
     */
    private class RetryMessageListener implements MessageListener {
        
        @Override
        public Action consume(Message message, ConsumeContext context) {
            String msgBody = new String(message.getBody());
            String msgId = message.getKey();
            
            try {
                log.info("收到重试消息: msgId={}, topic={}", msgId, message.getTopic());
                
                JSONObject object = new JSONObject(msgBody);
                
                // 验证必需字段
                if (!validateMessage(object)) {
                    log.warn("重试消息缺少必需字段，跳过: {}", msgBody);
                    meterRegistry.counter("houyi_retry_msg", 
                        Tags.of("result", "invalid")).increment();
                    return Action.CommitMessage;
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
                    return Action.CommitMessage;
                }
                
                // 提交到下载线程池重新下载
                try {
                    downloadThreadKeeper.execute(corpId, originalMsgId, secret, seq, object);
                    meterRegistry.counter("houyi_retry_msg", 
                        Tags.of("result", "resubmit")).increment();
                    log.info("重试消息已提交到下载线程池: corpId={}, msgId={}, seq={}", 
                        corpId, originalMsgId, seq);
                    return Action.CommitMessage;
                    
                } catch (RejectedExecutionException e) {
                    log.error("下载线程池已满，稍后重试: corpId={}, msgId={}, seq={}", 
                        corpId, originalMsgId, seq);
                    meterRegistry.counter("houyi_retry_msg", 
                        Tags.of("result", "thread_pool_full")).increment();
                    // 返回 ReconsumeLater，让 RocketMQ 稍后重新投递
                    return Action.ReconsumeLater;
                }
                
            } catch (Exception e) {
                log.error("处理重试消息失败: msgId={}, error={}", msgId, e.getMessage(), e);
                meterRegistry.counter("houyi_retry_msg", 
                    Tags.of("result", "error")).increment();
                // 返回 ReconsumeLater，让 RocketMQ 稍后重新投递
                return Action.ReconsumeLater;
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
}


