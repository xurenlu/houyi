package com.ruoran.houyi.mq;

import com.aliyun.openservices.ons.api.bean.ConsumerBean;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 消费者配置
 * 创建重试队列的 ConsumerBean
 * 注意：subscriptionTable 由 HouyiTcpRetryConsumer 在 @PostConstruct 中设置
 * 所以这里不设置 initMethod，让 Bean 延迟启动
 * 
 * @author houyi
 */
@Slf4j
@Configuration
public class RocketMqConsumerConfig {
    
    @Resource
    private MqConfig mqConfig;
    
    @Resource
    private AliyunConfig aliyunConfig;
    
    /**
     * 创建重试队列消费者 Bean
     * 不设置 initMethod，让 HouyiTcpRetryConsumer 在设置完 subscriptionTable 后手动启动
     * 
     * @return ConsumerBean
     */
    @Bean(name = "retryConsumer", destroyMethod = "shutdown")
    public ConsumerBean buildRetryConsumer() {
        log.info("创建 RocketMQ 重试消费者 Bean: topic={}, groupId={}", 
            mqConfig.getRetryTopic(), mqConfig.getRetryGroupId());
        
        ConsumerBean consumerBean = new ConsumerBean();
        // 使用包含 Group ID 的配置
        consumerBean.setProperties(mqConfig.getRetryConsumerProperties(aliyunConfig));
        
        return consumerBean;
    }
}

