package com.ruoran.houyi.sync;

import com.ruoran.houyi.DownloadThreadKeeper;
import com.ruoran.houyi.model.CorpInfo;
import com.ruoran.houyi.model.OriginalMsg;
import com.ruoran.houyi.mq.HouyiTcpConstructionMessageProduct;
import com.ruoran.houyi.repo.CorplistRepo;
import com.ruoran.houyi.repo.OriginalMsgRepo;
import com.ruoran.houyi.service.EventBus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author lh
 */
@Component
@Data
@Slf4j
public class ReloadNotPushMsg {

    @Resource
    private OriginalMsgRepo originalMsgRepo;

    @Value("${spring.profiles.active}")
    protected String profile;

    @Resource
    private CorplistRepo corplistRepo;

    private Map<String,String> secretMap = new HashMap<>();

    @Resource
    HouyiTcpConstructionMessageProduct tcpProducer;

    @Resource
    DownloadThreadKeeper downloadThreadKeeper;

    @Resource
    EventBus eventBus;

    private String getSecret(String wxCorpId){
        if(secretMap.containsKey(wxCorpId)){
            return secretMap.get(wxCorpId);
        }else{
            Optional<CorpInfo> optionalCorpInfo =  corplistRepo.findFirstByCorpid(wxCorpId);
            if(optionalCorpInfo.isPresent()){
                secretMap.put(wxCorpId,optionalCorpInfo.get().getSecret());
                return optionalCorpInfo.get().getSecret();
            }
        }
        return null;
    }

    @Scheduled(fixedRate = 1000 * 60*10)
    public void run() {
        String tag = getProfile();
        if (tag == null || tag.isEmpty()) {
            tag = "dev";
        }
        List<OriginalMsg> msgList = originalMsgRepo.findNotPushMessage();
        if (!CollectionUtils.isEmpty(msgList)) {
            for (OriginalMsg msg : msgList) {
                String secret = getSecret(msg.getCorpId());
                if(StringUtils.isEmpty(secret)){
                    if(msg.getPushAt() == null){
                        msg.setPushAt(-1L);
                    }else{
                        msg.setPushAt( msg.getPushAt()-1);
                    }
                    originalMsgRepo.save(msg);
                    continue;
                }
                JSONObject jsonObject = new JSONObject(msg.getContent());
                jsonObject.put("seq", msg.getSeq());
                jsonObject.put("secret",secret);
                jsonObject.put("push_at",msg.getPushAt());
                try {
                    tcpProducer.sendDelayMessage(jsonObject.toString(), msg.getMsgId());
                    if(msg.getPushAt() == null){
                        msg.setPushAt(-1L);
                    }else{
                        msg.setPushAt( msg.getPushAt()-1);
                    }
                    originalMsgRepo.save(msg);
                } catch (Exception e) {
                    log.error("ReloadNotPushMsgError,msg:{}", jsonObject);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 1000 * 60*10)
    public void bigFile() {
        String tag = getProfile();
        if (tag == null || tag.isEmpty()) {
            tag = "dev";
        }
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) downloadThreadKeeper.getExecutorService();
        int maxPoolSize = executorService.getMaximumPoolSize();
        // 多大文件下载也没有意义
        if(eventBus.getActiveThreadCount() < maxPoolSize*0.1) {
            List<OriginalMsg> msgList = originalMsgRepo.findBigFileNotPushMessage();
            if (!CollectionUtils.isEmpty(msgList)) {
                for (OriginalMsg msg : msgList) {
                    log.error("tryDownBigFile , msgid:{}",msg.getMsgId());
                    String secret = getSecret(msg.getCorpId());
                    if (StringUtils.isEmpty(secret)) {
                        continue;
                    }
                    JSONObject jsonObject = new JSONObject(msg.getContent());
                    jsonObject.put("seq", msg.getSeq());
                    jsonObject.put("secret", secret);
                    jsonObject.put("big_file",true);
                    jsonObject.put("push_at",msg.getPushAt());
                    try {
                        tcpProducer.sendDelayMessage(jsonObject.toString(), msg.getMsgId());
                        msg.setPushAt( msg.getPushAt()-1);
                        originalMsgRepo.save(msg);
                    } catch (Exception e) {
                        log.error("ReloadBigFileNotPushMsgError,msg:{}", jsonObject);
                    }
                }
            }
        }
    }
}
