package com.ruoran.houyi.config;

import com.ruoran.houyi.DownloadThreadKeeper;
import com.ruoran.houyi.Start;
import com.ruoran.houyi.model.CorpInfo;
import com.ruoran.houyi.model.OriginalMsg;
import com.ruoran.houyi.pojo.CorpInfoData;
import com.ruoran.houyi.pojo.YapiResult;
import com.ruoran.houyi.repo.CorplistRepo;
import com.ruoran.houyi.repo.OriginalMsgRepo;
import com.ruoran.houyi.service.CorpInfoApi;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * @author renlu
 * created by renlu at 2021/7/15 5:39 下午
 */
@RestController
@Slf4j
public class DashController {
    @Resource
    CorpInfoApi corpInfoApi;
    @Resource
    CorplistRepo corplistRepo;
    @Resource
    OriginalMsgRepo originalMsgRepo;

    @Resource
    DownloadThreadKeeper downloadThreadKeeper;

    @Resource
    Start start;

    @GetMapping("/api/open/pull/corp")
    public Object pullNewCorp(String corpId) {
        Optional<CorpInfo> corpInfoOptional = corplistRepo.findFirstByCorpid(corpId);
        CorpInfo corpInfo = corpInfoOptional.orElseGet(CorpInfo::new);
        try {
            YapiResult result = corpInfoApi.getCorpInfo(corpId);
            saveCorp(corpId, corpInfo, result, corplistRepo);
        } catch (Exception e) {
            return e.getMessage();
        }

        return corpInfo;
    }

    public static void saveCorp(String corpId, CorpInfo corpInfo, YapiResult result, CorplistRepo corplistRepo) {
        if (result.getCode() == 200) {
            CorpInfoData data = result.getData();
            corpInfo.setCorpname(data.getCorpName());
            corpInfo.setCorpid(data.getCorpId());
            corpInfo.setPrikey(data.getMessageKey());
            corpInfo.setSecret(data.getMessageSecret());
            corpInfo.setCorpid(corpId);
            corpInfo.setStatus(1);
            try {
                corplistRepo.save(corpInfo);
            }catch (Exception e2){
                log.error("can't save corpId:{},secret:{},privKey:{}",corpId,data.getMessageSecret(),data.getMessageKey(),e2);
            }
        }
    }

    @GetMapping("/api/open/trigger/corp")
    public Object triggerCorpId(String corpId){
        try {
            start.triggerCorpId(corpId);
        }catch (Exception e){
            return e.getMessage();
        }
        return corpId;
    }

    @GetMapping("/api/open/single/msg")
    public Object downSingle(String corpId,String msgId,long seq){
        Optional<CorpInfo> corpInfoOptional = corplistRepo.findFirstByCorpid(corpId);
        if(!corpInfoOptional.isPresent()){
            return "corpInfo not exists for corpId:"+corpId;
        }
        Optional<OriginalMsg> msgOptional = originalMsgRepo.findFirstByCorpIdAndMsgIdAndSeq(corpId,msgId,seq);
        if(!msgOptional.isPresent()){
            return "msg not exists:"+msgId ;
        }
        log.warn("msg:{}",msgOptional.get().getContent());
        JSONObject object = new JSONObject(msgOptional.get().getContent());
        downloadThreadKeeper.execute(corpId,msgId,corpInfoOptional.get().getSecret(),seq, object);
        return object;
    }

    @GetMapping("/api/open/status")
    public Object status() throws Exception {
        Map<String,Boolean> status=  start.getEventBus().getCorpsStatus();
        List<CorpInfo> succs  = new ArrayList<>();
        List<CorpInfo> fails  = new ArrayList<>();
        for(Map.Entry<String,Boolean> entry : status.entrySet()){
            String corpId = entry.getKey();
            if( entry.getValue()){
                succs.add(getCorpInfo(corpId));
            }else{
                fails.add(getCorpInfo(corpId));
            }
        }
        Map<String,List<CorpInfo>>  result = new HashMap<>();
        result.put("succ",succs);
        result.put("fail",fails);
        return result;
    }
    public CorpInfo getCorpInfo(String corpId) throws Exception {
        Optional<CorpInfo> optional = corplistRepo.findFirstByCorpid(corpId);
        if(optional.isPresent()){
            return optional.get();
        }else{
            throw new Exception("unkown corpId:"+corpId);
        }
    }
}
