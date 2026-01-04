-- Redis 消息备份表
-- 用于备份发送到 Redis Stream 的消息，防止数据丢失
CREATE TABLE IF NOT EXISTS `redis_message_backup` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `topic` VARCHAR(128) NOT NULL,
    `message_body` TEXT NOT NULL,
    `message_key` VARCHAR(128),
    `redis_msg_id` VARCHAR(128),
    `tag` VARCHAR(64),
    `sharding_key` VARCHAR(128),
    `status` TINYINT DEFAULT 0, -- 0: sent to redis, 1: acknowledged, 2: failed
    `create_at` BIGINT NOT NULL,
    `ack_at` BIGINT,
    INDEX `idx_message_key` (`message_key`),
    INDEX `idx_topic_status` (`topic`, `status`),
    INDEX `idx_create_at` (`create_at`),
    INDEX `idx_redis_msg_id` (`redis_msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

