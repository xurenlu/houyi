package com.ruoran.houyi.model;

import lombok.Data;
import jakarta.persistence.*;

/**
 * Redis 消息备份实体类
 * 用于备份发送到 Redis Stream 的消息，防止数据丢失
 * 
 * @author lh
 */
@Data
@Entity
@Table(name = "redis_message_backup", indexes = {
    @Index(name = "idx_message_key", columnList = "messageKey"),
    @Index(name = "idx_topic_status", columnList = "topic,status"),
    @Index(name = "idx_create_at", columnList = "createAt")
})
public class RedisMessageBackup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Redis Stream Topic
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
     * Redis Stream 消息 ID
     */
    @Column(name = "redis_msg_id", columnDefinition = "varchar(128)")
    private String redisMsgId;
    
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
     * 消息状态：0-已发送到 Redis，1-已确认消费，2-发送失败
     */
    @Column(name = "status", columnDefinition = "TINYINT DEFAULT 0")
    private Integer status = 0;
    
    /**
     * 创建时间
     */
    @Column(name = "create_at", columnDefinition = "BIGINT NOT NULL")
    private Long createAt;
    
    /**
     * 确认时间（消费确认时更新）
     */
    @Column(name = "ack_at", columnDefinition = "BIGINT")
    private Long ackAt;
    
    @PrePersist
    public void prePersist() {
        if (createAt == null) {
            createAt = System.currentTimeMillis();
        }
        if (status == null) {
            status = 0;
        }
    }
}

