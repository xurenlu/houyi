package com.ruoran.houyi.mq;

import com.ruoran.houyi.repo.RedisMessageBackupRepo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis 消息备份清理器
 * 定期清理已确认的旧消息备份
 * 
 * @author lh
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "redis.mq.enabled", havingValue = "true", matchIfMissing = false)
public class RedisMessageBackupCleaner {
    
    @Resource
    private RedisMessageBackupRepo messageBackupRepo;
    
    /**
     * 定期清理已确认的旧消息备份
     * 每天凌晨 3 点执行，清理 7 天前的已确认消息
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨 3 点执行
    @Transactional
    public void cleanOldBackups() {
        try {
            // 清理 7 天前的已确认消息
            long beforeTime = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            messageBackupRepo.cleanAcknowledgedMessages(beforeTime);
            log.info("清理已确认的 Redis 消息备份: 删除了 7 天前的记录");
        } catch (Exception e) {
            log.error("清理 Redis 消息备份失败: {}", e.getMessage(), e);
        }
    }
}

