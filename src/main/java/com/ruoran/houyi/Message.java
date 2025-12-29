/*
 * This file is part of the zyan/wework-msgaudit.
 *
 * (c) 读心印 <aa24615@qq.com>
 *
 * This source file is subject to the MIT license that is bundled
 * with this source code in the file LICENSE.
 */

package com.ruoran.houyi;

import com.tencent.wework.Finance;
import com.tencent.wework.RSAEncrypt;
import com.ruoran.houyi.model.OriginalMsg;
import com.ruoran.houyi.mq.HouyiTcpConstructionMessageProduct;
import com.ruoran.houyi.repo.Md5IndexRepo;
import com.ruoran.houyi.repo.OriginalMsgRepo;
import com.ruoran.houyi.service.EventBus;
import com.ruoran.houyi.utils.DateUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author renlu
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Component
@Scope("prototype")
public class Message extends Thread {
    public String prikey = null;
    public String corpid = null;
    public long seqs = 0;
    public long sdk;
    public String secret;
    public String tableName;
    @Resource
    MeterRegistry meterRegistry;
    public static final List<String> typeNeedntDownload = new ArrayList<>();

    static {
        typeNeedntDownload.add("revoke");
        typeNeedntDownload.add("text");
        typeNeedntDownload.add("location");
        typeNeedntDownload.add("agree");
        typeNeedntDownload.add("disagree");
        typeNeedntDownload.add("weapp");
        typeNeedntDownload.add("card");
        typeNeedntDownload.add("todo");
        typeNeedntDownload.add("collect");
        typeNeedntDownload.add("redpacket");
        typeNeedntDownload.add("docmsg");
        typeNeedntDownload.add("markdown");
        typeNeedntDownload.add("calendar");
        typeNeedntDownload.add("news");
        typeNeedntDownload.add("external_redpacket");
        typeNeedntDownload.add("sphfeed");  // 重复项已移除
        typeNeedntDownload.add("link");
        typeNeedntDownload.add("meeting");
        typeNeedntDownload.add("voiptext");
    }

    int mode = 0;

    @Resource
    EventBus eventBus;

    @Resource
    OriginalMsgRepo originalMsgRepo;

    @Resource
    DownloadThreadKeeper downloadThreadKeeper;
    @Resource
    JedisPool jedisPool;

    @Resource
    Md5IndexRepo md5IndexRepo;


    @Resource
    HouyiTcpConstructionMessageProduct tcpProduct;

    @Value("${spring.profiles.active}")
    private String profile;

    private ExecutorService pool = Executors.newFixedThreadPool(10);
    private Map<Long,JSONObject> jsonObjectMap = new HashMap<>();

    public void init(String corpId, String secret, String privateKey) {
        if (StringUtils.isEmpty(privateKey)) {
            log.warn("未配置privateKey,无法获取会话存档;");
            return;
        }
        this.sdk = Finance.NewSdk();
        this.corpid = corpId;
        this.secret = secret;
        this.prikey = privateKey;
        int state = Finance.Init(sdk, corpId, secret);
        if (state != 0) {
            log.error("初始化（Finance.init）失败,corpId:{},secret:{}", corpId, secret);
        }

        this.setName("msg-thread-" + this.corpid);
    }

    public void setLastSeq(long seq) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.set("last_seq_" + this.corpid, String.valueOf(seq));
        } catch (Exception e) {
            log.error("redisError,last_seq_" + this.corpid, e);
        } finally {
            if (jedis != null) {
                //这里使用的close不代表关闭连接，指的是归还资源
                jedisPool.returnResource(jedis);
            }
        }
    }

    public long getLastSeq() {
        Jedis jedis = null;
        long seq = 0;
        try {


            jedis = jedisPool.getResource();
            String lastSeqStr = jedis.get("last_seq_" + this.corpid);
            if (StringUtils.isNotEmpty(lastSeqStr)) {
                seq = Long.parseLong(lastSeqStr);
            }
        } catch (Exception e) {
            log.error("Redis操作失败", e);
        } finally {
            if (jedis != null) {
                //这里使用的close不代表关闭连接，指的是归还资源
                jedisPool.returnResource(jedis);
            }
        }
        return seq;
    }


    public boolean needDownload(JSONObject msgBody) {
        String action = msgBody.getString("action");
        if ("recall".equalsIgnoreCase(action) || "switch".equalsIgnoreCase(action)) {
            return false;
        }
        String msgType = msgBody.getString("msgtype");
        if ("mixed".equalsIgnoreCase(msgType)) {
            JSONObject mixed = msgBody.getJSONObject("mixed");
            JSONArray item = mixed.getJSONArray("item");
            for (Object single : item) {
                JSONObject object = (JSONObject) single;
                String type = object.getString("type");
                if (!typeNeedntDownload.contains(type)) {
                    return true;
                }
            }
            return false;
        }
        if ("chatrecord".equalsIgnoreCase(msgType)) {
            JSONObject mixed = msgBody.getJSONObject("chatrecord");
            JSONArray item = mixed.getJSONArray("item");
            for (Object single : item) {
                JSONObject object = (JSONObject) single;
                if (!object.has("type")) {
                    continue;
                }
                String type = object.getString("type").toLowerCase(Locale.ROOT).replaceAll("chatrecord", "");
                if (!typeNeedntDownload.contains(type)) {
                    return true;
                }
            }
            return false;
        }
        if (typeNeedntDownload.contains(msgType)) {
            return false;
        }
        return true;
    }

    public void saveMessage(String msgType, String msgId, long seq, long msgTime, JSONObject object,Boolean isFile) {

        long dateNo = Long.parseLong(DateUtil.formatYyyyMMdd(msgTime));
        OriginalMsg msg = new OriginalMsg();
        msg.setMsgId(msgId);
        msg.setContent(object.toString());
        msg.setCorpId(corpid);
        msg.setMsgType(msgType);
        msg.setDateNo(dateNo);
        msg.setSeq(seq);
        msg.setCreateAt(System.currentTimeMillis());
        //如果不是文件类型，就当场推送了
        if(!isFile){
            msg.setPushAt(System.currentTimeMillis());
        }


        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Optional<OriginalMsg> originalMsgOptional = originalMsgRepo.findFirstByCorpIdAndMsgIdAndSeq(this.corpid, msgId, seq);
        if (originalMsgOptional.isPresent()) {
            stopWatch.stop();
            meterRegistry.summary("dayu_db_save_cost", Tags.of("type","query"))
                    .record(stopWatch.getTotalTimeMillis());
            return;
        }

        originalMsgRepo.save(msg);
        stopWatch.stop();
        meterRegistry.summary("dayu_db_save_cost", Tags.of("type","update"))
                .record(stopWatch.getTotalTimeMillis());
    }


    //解密
    public String decryptData(String encryptRandomKey, String encryptMsg,String original) {
        long message = 0;
        try {
            String encryptKey = "";
            if (this.mode == 1) {
                encryptKey = RSAEncrypt.decrypt2(encryptRandomKey, this.prikey);
                this.mode = 1;
            }
            if (this.mode == 2) {
                encryptKey = RSAEncrypt.decryptByPriKey(encryptRandomKey, this.prikey);
                this.mode = 2;
            }
            if (this.mode == 0) {
                try {
                    encryptKey = RSAEncrypt.decrypt2(encryptRandomKey, this.prikey);
                    this.mode = 1;
                } catch (Exception e) {
                    log.error("解密失败：", e);
                    encryptKey = RSAEncrypt.decryptByPriKey(encryptRandomKey, this.prikey);
                    this.mode = 2;
                }
            }
            if (StringUtils.isEmpty(encryptKey)) {
                log.error("encryptRandomKey:{},encryptMsg:{},original:{}", encryptRandomKey, encryptMsg,original);
                log.error("encryptKey解密失败,{}", encryptKey);
                return "";
            }
            message = Finance.NewSlice();
            int ret = Finance.DecryptData(this.sdk, encryptKey, encryptMsg, message);
            if (ret != 0) {
                log.error("解密失败:" + ret);
                return "";
            }
            return Finance.GetContentFromSlice(message);
        } catch (Exception e) {
            log.error("解密数据失败", e);
            return "";
        } finally {
            if (message != 0) {
                Finance.FreeSlice(message);
            }
        }

    }

    public void getList() {
        if (StringUtils.isEmpty(this.prikey)) {
            log.error("未配置privateKey,无法获取会话存档;");
            return;
        }
        long slice = 0;
        try {
            long seqs = this.getLastSeq();
            slice = Finance.NewSlice();
            int ret = Finance.GetChatData(this.sdk, seqs, 500, "", "", 100, slice);
            
            if (ret != 0) {
                log.error("获取聊天数据失败, corpId:{}, ret:{}", this.corpid, ret);
                return;
            }
            
            String json = Finance.GetContentFromSlice(slice);
            processMessageData(json);
        } catch (Exception e) {
            log.error("拉取消息异常, corpId:{}", this.corpid, e);
        } finally {
            if (slice != 0) {
                Finance.FreeSlice(slice);
            }
        }
    }

    /**
     * 处理消息数据
     */
    private void processMessageData(String json) {
        JSONObject jo = new JSONObject(json);
        int errCode = jo.getInt("errcode");
        
        if (errCode == 0) {
            processSuccessResponse(jo);
        } else {
            handleErrorCode(errCode, jo.getString("errmsg"));
        }
    }

    /**
     * 处理成功响应
     */
    private void processSuccessResponse(JSONObject jo) {
        eventBus.getCorpsStatus().put(this.corpid, true);
        JSONArray chatData = jo.getJSONArray("chatdata");
        
        for (int i = 0; i < chatData.length(); i++) {
            eventBus.getTotalMsg().incrementAndGet();
            String item = chatData.get(i).toString();
            JSONObject data = new JSONObject(item);
            
            if ("ww0aad5bd009edd8e0".equals(this.corpid)) {
                batchExec(data);
            } else {
                long seq = sendMsg(data);
                setLastSeq(seq);
            }
        }
    }

    /**
     * 处理错误码
     */
    private void handleErrorCode(int errCode, String errMsg) {
        eventBus.getCorpsStatus().put(this.corpid, false);
        log.error("FinanceSdk获取会话存档失败, corpId:{}, code:{}, msg:{}", this.corpid, errCode, errMsg);
        
        switch (errCode) {
            case 301052:
                log.error("会话存档服务已过期, corpId:{}, 等待30分钟", this.corpid);
                sleepQuietly(30 * 60 * 1000);
                break;
            case 301042:
                log.error("IP不在白名单, corpId:{}, 等待30分钟", this.corpid);
                sleepQuietly(30 * 60 * 1000);
                break;
            case 41001:
                log.error("accessToken缺失, corpId:{}, 等待10分钟", this.corpid);
                sleepQuietly(10 * 60 * 1000);
                break;
            default:
                sleepQuietly(5 * 60 * 1000);
                break;
        }
    }

    /**
     * 静默休眠
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("休眠被中断, corpId:{}", this.corpid);
        }
    }

    public long sendMsg(JSONObject data) {

        String encryptRandomKey = data.getString("encrypt_random_key");
        String encryptChatMsg = data.getString("encrypt_chat_msg");
        long seq = data.getLong("seq");
        String message = this.decryptData(encryptRandomKey, encryptChatMsg,data.toString());
        JSONObject obj = new JSONObject(message);
        String msgid = obj.getString("msgid");
        String action = obj.getString("action");
        /*
         * switch 不是一条真正的消息，结构不一样，可以忽略;
         */
        if ("switch".equalsIgnoreCase(action)) {
            log.warn("switchMsg");
            return seq;
        }
        long msgTime = 0;
        try {
            msgTime = obj.getLong("msgtime");
            AtomicLong diff = eventBus.getDiffs().get(corpid);
            if (diff != null) {
                diff.set((System.currentTimeMillis() - msgTime) / 1000);
            }
        } catch (JSONException e2) {
            log.error("取不到msgTime,原始字符串:{}", message);
        }

        String msgType = obj.getString("msgtype");
        obj.put("corp_id", corpid);
        obj.put("seq", seq);
        if (needDownload(obj)) {
            saveMessage(msgType, msgid, seq, msgTime, obj,true);
//            if (msg == null) {
//                return seq;
//            }
            eventBus.getTotalDownload().incrementAndGet();
            try {
                downloadThreadKeeper.execute(this.corpid, msgid, this.secret, seq, obj);
            } catch (RejectedExecutionException e) {
                log.error("RejectedExecutionException");
                try {
                    Thread.sleep(1000 * 5 * 60);
                } catch (InterruptedException interruptedException) {
                    log.error("sleepError", interruptedException);
                }
                setLastSeq(seq);
            }
        } else {
            /**
             * 非文件类型的消息，不需要保存入库,直接推送;
             */
            saveMessage(msgType, msgid, seq, msgTime, obj,false);
            JSONObject jsonObject = new JSONObject(obj.toString());
            jsonObject.put("seq", seq);
            jsonObject.put("source", "java");
            try {
                if ("text".equalsIgnoreCase(msgType)) {
                    JSONObject textNode = obj.getJSONObject("text");
                    String content = textNode.getString("content");
                    jsonObject.put("_sign", Md5Util.getMd5(content));
                }
            }catch (Exception e){
                log.error("extract text node  error",e);
            }

            String key = "1";
            if (msgid != null) {
                key = msgid;
            }
            // 统一使用 TCP 协议发送消息
            tcpProduct.send(jsonObject.toString(), key);
        }
        return seq;
    }

    public void batchExec(JSONObject data) {
        if(!data.has("seq")) {
            return;
        }
        long msgSeq = data.getLong("seq");
        jsonObjectMap.put(msgSeq,data);
        if (jsonObjectMap.size() < 10) {
            return;
        }

        List<JSONObject> jsonObjectList = new ArrayList<>(jsonObjectMap.values());
        long tempSeq = jsonObjectList.get(9).getLong("seq");
        CountDownLatch countDownLatch = new CountDownLatch(10);
        AtomicInteger failCount = new AtomicInteger();
        for (JSONObject item : jsonObjectList) {
            if(item.has("seq")){
                long seq = item.getLong("seq");
                if(seq > tempSeq){
                    tempSeq = seq;
                }
            }

            pool.execute(() -> {
                try {
                    sendMsg(item);
                } catch (Exception e) {
                    log.error("batchSendMsgError,", e);
                    failCount.getAndIncrement();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        try {
            countDownLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("batchSendMsgError,awaitError, ", e);
        }
        if(failCount.get()  == 0 && tempSeq > 0){
            setLastSeq(tempSeq);
        }
        jsonObjectMap.clear();

    }

    @Override
    public void run() {
        do {
            getList();
            if (StringUtils.isEmpty(this.getPrikey())) {
                log.error("私钥为空，不再空转");
                break;
            }
            // 控制拉取频率，避免过快
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.error("线程休眠被中断", e);
            }
        } while (true);
    }
}
