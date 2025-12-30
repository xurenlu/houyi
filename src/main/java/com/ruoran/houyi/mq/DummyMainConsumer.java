package com.ruoran.houyi.mq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dummy Consumer for wechat-archive-msg Topic
 * 用于建立订阅关系，让 Producer 能够获取路由信息
 * 实际消息由外部系统消费
 * 
 * @author houyi
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rocketmq.dummy-consumer.enabled", havingValue = "true", matchIfMissing = false)
public class DummyMainConsumer {

    @Resource(name = "buildMainConsumer")
    private DefaultMQPushConsumer mainConsumer;

    @Value("${spring.profiles.active:dev}")
    private String env;

    @PostConstruct
    public void init() {
        try {
            log.info("========================================");
            log.info("启动 Dummy Consumer（仅用于建立订阅关系）");
            log.info("========================================");

            // 注册消息监听器（不做任何处理，直接返回成功）
            mainConsumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, 
                                                                ConsumeConcurrentlyContext context) {
                    // 不做任何处理，直接返回成功
                    // 实际消息由外部系统消费
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });

            // 启动 Consumer
            mainConsumer.start();
            log.info("Dummy Consumer 启动成功！");

        } catch (Exception e) {
            log.error("Dummy Consumer 启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Dummy Consumer", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (mainConsumer != null) {
            mainConsumer.shutdown();
            log.info("Dummy Consumer 已关闭");
        }
    }
}

