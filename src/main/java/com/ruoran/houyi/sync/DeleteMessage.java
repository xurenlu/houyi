package com.ruoran.houyi.sync;

import com.ruoran.houyi.repo.OriginalMsgRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

/**
 * @author lh
 */
@Component
@Slf4j
public class DeleteMessage {
    @Resource
    private OriginalMsgRepo originalMsgRepo;

    /**
     * 每天凌晨5点删除前一天的消息
     */
    @Scheduled(cron="* * 4-8 * * ?")
    @Transactional(rollbackOn = Exception.class)
    public void delete(){
        originalMsgRepo.deleteOldMsgs();
    }
}
