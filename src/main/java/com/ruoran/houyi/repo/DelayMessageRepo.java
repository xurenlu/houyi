package com.ruoran.houyi.repo;

import com.ruoran.houyi.model.DelayMessage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 延迟消息 Repository
 * 
 * @author lh
 */
@Component
public interface DelayMessageRepo extends CrudRepository<DelayMessage, Long>, 
        PagingAndSortingRepository<DelayMessage, Long> {
    
    /**
     * 查询到期的待投递消息
     * 
     * @param currentTime 当前时间戳（毫秒）
     * @param limit 限制数量
     * @return 到期的消息列表
     */
    @Query("SELECT d FROM DelayMessage d WHERE d.deliverTime <= :currentTime AND d.status = 0 ORDER BY d.deliverTime ASC")
    List<DelayMessage> findExpiredMessages(@Param("currentTime") Long currentTime, 
                                          org.springframework.data.domain.Pageable pageable);
    
    /**
     * 更新消息状态为已投递
     * 
     * @param id 消息 ID
     * @param deliverAt 投递时间
     */
    @Modifying
    @Query("UPDATE DelayMessage d SET d.status = 1, d.deliverAt = :deliverAt WHERE d.id = :id")
    void markAsDelivered(@Param("id") Long id, @Param("deliverAt") Long deliverAt);
    
    /**
     * 更新消息状态为投递失败，并增加重试次数
     * 
     * @param id 消息 ID
     * @param errorMsg 错误信息
     */
    @Modifying
    @Query("UPDATE DelayMessage d SET d.status = 2, d.retryCount = d.retryCount + 1, d.errorMsg = :errorMsg WHERE d.id = :id")
    void markAsFailed(@Param("id") Long id, @Param("errorMsg") String errorMsg);
    
    /**
     * 清理已投递的旧消息（保留最近 N 天的记录）
     * 
     * @param beforeTime 清理此时间之前的记录
     */
    @Modifying
    @Query("DELETE FROM DelayMessage d WHERE d.status = 1 AND d.deliverAt < :beforeTime")
    void cleanDeliveredMessages(@Param("beforeTime") Long beforeTime);
}

