package com.ruoran.houyi.mq;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.SessionCredentialsProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 5.0 gRPC SDK 生产者配置
 * 
 * @author houyi
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "redis.mq.enabled", havingValue = "false", matchIfMissing = true)
public class RocketMqProducerConfig {
    
    @Resource
    private MqConfig mqConfig;
    
    private Producer producer;
    
    @Bean
    public ClientServiceProvider clientServiceProvider() {
        return ClientServiceProvider.loadService();
    }
    
    @Bean
    public ClientConfiguration clientConfiguration() {
        log.info("========================================");
        log.info("初始化 RocketMQ 5.0 gRPC SDK 配置");
        log.info("Endpoint: {}", mqConfig.getEndpoint());
        log.info("Username: {}", mqConfig.getUsername() != null ? 
            mqConfig.getUsername().substring(0, Math.min(8, mqConfig.getUsername().length())) + "***" : "未设置");
        log.info("========================================");
        
        SessionCredentialsProvider credentialsProvider = 
            new StaticSessionCredentialsProvider(mqConfig.getUsername(), mqConfig.getPassword());
        
        return ClientConfiguration.newBuilder()
            .setEndpoints(mqConfig.getEndpoint())
            .setCredentialProvider(credentialsProvider)
            .build();
    }
    
    @Bean(name = "producer")
    public Producer producer(ClientServiceProvider provider, ClientConfiguration clientConfiguration) 
            throws ClientException {
        log.info("========================================");
        log.info("初始化 RocketMQ 5.0 gRPC Producer");
        log.info("Topic: {}", mqConfig.getTopic());
        log.info("Retry Topic: {}", mqConfig.getRetryTopic());
        log.info("========================================");
        
        producer = provider.newProducerBuilder()
            .setClientConfiguration(clientConfiguration)
            .setTopics(mqConfig.getTopic(), mqConfig.getRetryTopic())
            .build();
        
        log.info("RocketMQ 5.0 gRPC Producer 创建成功！");
        
        return producer;
    }
    
    @PreDestroy
    public void destroy() {
        if (producer != null) {
            try {
                log.info("关闭 RocketMQ gRPC Producer");
                producer.close();
            } catch (Exception e) {
                log.error("关闭 RocketMQ gRPC Producer 失败", e);
            }
        }
    }
}
