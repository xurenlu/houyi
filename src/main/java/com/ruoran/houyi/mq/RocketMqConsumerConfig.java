package com.ruoran.houyi.mq;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Collections;

/**
 * RocketMQ 5.0 gRPC SDK 消费者配置
 * 使用 SimpleConsumer 进行消息拉取
 * 
 * @author houyi
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "redis.mq.enabled", havingValue = "false", matchIfMissing = true)
public class RocketMqConsumerConfig {
    
    @Resource
    private MqConfig mqConfig;
    
    @Value("${spring.profiles.active:dev}")
    private String env;
    
    private SimpleConsumer retryConsumer;
    
    /**
     * 重试队列 SimpleConsumer
     * 用于消费延迟消息队列中的重试消息
     */
    @Bean
    public SimpleConsumer retrySimpleConsumer(ClientServiceProvider provider, ClientConfiguration clientConfiguration) 
            throws ClientException {
        // 仅在生产环境创建消费者
        if ("dev".equalsIgnoreCase(env)) {
            log.info("开发环境，跳过重试消费者创建");
            return null;
        }
        
        log.info("========================================");
        log.info("初始化 RocketMQ 5.0 gRPC 重试消费者");
        log.info("Topic: {}", mqConfig.getRetryTopic());
        log.info("Group ID: {}", mqConfig.getRetryGroupId());
        log.info("Tag: {}", mqConfig.getTag());
        log.info("========================================");
        
        // 创建过滤表达式
        FilterExpression filterExpression = new FilterExpression(mqConfig.getTag(), FilterExpressionType.TAG);
        
        // 创建 SimpleConsumer
        retryConsumer = provider.newSimpleConsumerBuilder()
            .setClientConfiguration(clientConfiguration)
            .setConsumerGroup(mqConfig.getRetryGroupId())
            .setSubscriptionExpressions(Collections.singletonMap(mqConfig.getRetryTopic(), filterExpression))
            .setAwaitDuration(Duration.ofSeconds(30))
            .build();
        
        log.info("RocketMQ 5.0 gRPC 重试消费者创建成功！");
        
        return retryConsumer;
    }
    
    @PreDestroy
    public void destroy() {
        if (retryConsumer != null) {
            try {
                log.info("关闭 RocketMQ gRPC 重试消费者");
                retryConsumer.close();
            } catch (Exception e) {
                log.error("关闭 RocketMQ gRPC 重试消费者失败", e);
            }
        }
    }
}
