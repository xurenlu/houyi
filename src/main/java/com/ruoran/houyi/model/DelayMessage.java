package com.ruoran.houyi.model;

import lombok.Data;
import jakarta.persistence.*;

/**
 * 延迟消息实体类
 * 用于存储需要延迟投递的消息
 * 
 * @author lh
 */
@Data
@Entity
@Table(name = "delay_message", indexes = {
    @Index(name = "idx_deliver_time", columnList = "deliverTime,status"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_message_key", columnList = "messageKey")
})
public class DelayMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    /**
     * 目标 Topic
     */
    @Column(name = "topic", columnDefinition = "varchar(128) NOT NULL")
    private String topic;
    
    /**
     * 消息内容（JSON 格式）
     */
    @Column(name = "message_body", columnDefinition = "TEXT NOT NULL")
    private String messageBody;
    
    /**
     * 消息 Key
     */
    @Column(name = "message_key", columnDefinition = "varchar(128)")
    private String messageKey;
    
    /**
     * 消息标签
     */
    @Column(name = "tag", columnDefinition = "varchar(64)")
    private String tag;
    
    /**
     * 分片 Key（可选）
     */
    @Column(name = "sharding_key", columnDefinition = "varchar(128)")
    private String shardingKey;
    
    /**
     * 投递时间戳（毫秒）
     */
    @Column(name = "deliver_time", columnDefinition = "BIGINT NOT NULL")
    private Long deliverTime;
    
    /**
     * 消息状态：0-待投递，1-已投递，2-投递失败
     */
    @Column(name = "status", columnDefinition = "TINYINT DEFAULT 0")
    private Integer status = 0;
    
    /**
     * 重试次数
     */
    @Column(name = "retry_count", columnDefinition = "INT DEFAULT 0")
    private Integer retryCount = 0;
    
    /**
     * 创建时间
     */
    @Column(name = "create_at", columnDefinition = "BIGINT NOT NULL")
    private Long createAt;
    
    /**
     * 投递时间
     */
    @Column(name = "deliver_at", columnDefinition = "BIGINT")
    private Long deliverAt;
    
    /**
     * 错误信息
     */
    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;
    
    @PrePersist
    public void prePersist() {
        if (createAt == null) {
            createAt = System.currentTimeMillis();
        }
        if (status == null) {
            status = 0;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
}

