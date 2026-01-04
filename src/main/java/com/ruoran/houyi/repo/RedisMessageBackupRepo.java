package com.ruoran.houyi.repo;

import com.ruoran.houyi.model.RedisMessageBackup;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Redis 消息备份 Repository
 *
 * @author lh
 */
@Component
public interface RedisMessageBackupRepo extends CrudRepository<RedisMessageBackup, Long>,
        PagingAndSortingRepository<RedisMessageBackup, Long> {

    /**
     * 根据消息 Key 查找备份
     */
    Optional<RedisMessageBackup> findByMessageKey(String messageKey);

    /**
     * 根据 Redis 消息 ID 查找备份
     */
    Optional<RedisMessageBackup> findByRedisMsgId(String redisMsgId);

    /**
     * 标记消息为已确认
     */
    @Modifying
    @Query("UPDATE RedisMessageBackup r SET r.status = 1, r.ackAt = :ackAt WHERE r.redisMsgId = :redisMsgId")
    void markAsAcknowledged(@Param("redisMsgId") String redisMsgId, @Param("ackAt") Long ackAt);

    /**
     * 标记消息为发送失败
     */
    @Modifying
    @Query("UPDATE RedisMessageBackup r SET r.status = 2 WHERE r.id = :id")
    void markAsFailed(@Param("id") Long id);

    /**
     * 清理已确认的旧消息（保留最近 N 天）
     */
    @Modifying
    @Query("DELETE FROM RedisMessageBackup r WHERE r.status = 1 AND r.ackAt < :beforeTime")
    void cleanAcknowledgedMessages(@Param("beforeTime") Long beforeTime);

    /**
     * 查找发送失败的消息（用于重试）
     */
    @Query("SELECT r FROM RedisMessageBackup r WHERE r.status = 2 ORDER BY r.createAt ASC")
    List<RedisMessageBackup> findFailedMessages();
}

