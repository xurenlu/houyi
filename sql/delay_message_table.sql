-- 延迟消息表
-- 用于存储需要延迟投递的消息

CREATE TABLE IF NOT EXISTS `delay_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `topic` VARCHAR(128) NOT NULL COMMENT '目标 Topic',
  `message_body` TEXT NOT NULL COMMENT '消息内容（JSON 格式）',
  `message_key` VARCHAR(128) DEFAULT NULL COMMENT '消息 Key',
  `tag` VARCHAR(64) DEFAULT NULL COMMENT '消息标签',
  `sharding_key` VARCHAR(128) DEFAULT NULL COMMENT '分片 Key',
  `deliver_time` BIGINT NOT NULL COMMENT '投递时间戳（毫秒）',
  `status` TINYINT DEFAULT 0 COMMENT '消息状态：0-待投递，1-已投递，2-投递失败',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `create_at` BIGINT NOT NULL COMMENT '创建时间（毫秒）',
  `deliver_at` BIGINT DEFAULT NULL COMMENT '投递时间（毫秒）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  PRIMARY KEY (`id`),
  KEY `idx_deliver_time` (`deliver_time`, `status`),
  KEY `idx_status` (`status`),
  KEY `idx_message_key` (`message_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='延迟消息表';

