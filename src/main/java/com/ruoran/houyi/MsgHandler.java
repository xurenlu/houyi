package com.ruoran.houyi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wework.Finance;
import com.ruoran.houyi.model.Md5Index;
import com.ruoran.houyi.model.OriginalMsg;
import com.ruoran.houyi.mq.MessageProducerAdapter;
import com.ruoran.houyi.repo.Md5IndexRepo;
import com.ruoran.houyi.repo.OriginalMsgRepo;
import com.ruoran.houyi.constants.AppConstants;
import com.ruoran.houyi.service.EventBus;
import com.ruoran.houyi.service.OssUtil;
import com.ruoran.houyi.utils.DateUtil;
import com.ruoran.houyi.utils.FileUtil;
import com.ruoran.houyi.utils.JedisUtil;
import com.ruoran.houyi.utils.RetryUtil;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

/**
 * @author renlu
 * created by renlu at 2021/6/4 11:05 上午
 */

@Slf4j
@Data
public class MsgHandler {
    static final Map<String, DownloadType> DOWNLOAD_TYPE = new HashMap<>(16);
    public String corpid;
    public long sdk;
    private String prefix = "/tmp/";
    @Resource
    Md5IndexRepo md5IndexRepo;
    @Resource
    OssThreadPool ossThreadPool;

    @Resource
    OriginalMsgRepo originalMsgRepo;

    @Resource
    DownloadThreadKeeper downloadThreadKeeper;

    @Resource
    MessageProducerAdapter messageProducerAdapter;

    @Resource
    JedisPool jedisPool;

    ObjectMapper objectMapper;

    String secret;

    @Value("${spring.profiles.active}")
    protected String profile;

    @Resource
    EventBus eventBus;

    @Resource
    OssUtil ossUtil;

    private final static Integer TRY_COUNT = AppConstants.Retry.MAX_TRY_COUNT;

    private final static Long BIG_FILE_TIME = AppConstants.Retry.BIG_FILE_TIMEOUT_MS;

    static {
        DOWNLOAD_TYPE.put("image", DownloadType.image);
        DOWNLOAD_TYPE.put("voice", DownloadType.voice);
        DOWNLOAD_TYPE.put("video", DownloadType.video);
        DOWNLOAD_TYPE.put("emotion", DownloadType.emotion);
        DOWNLOAD_TYPE.put("file", DownloadType.file);
        DOWNLOAD_TYPE.put("news", DownloadType.news);
        DOWNLOAD_TYPE.put("meeting_voice_call", DownloadType.meeting_voice_call);
        DOWNLOAD_TYPE.put("voip_doc_share", DownloadType.voip_doc_share);
        DOWNLOAD_TYPE.put("mixed", DownloadType.mixed);
        DOWNLOAD_TYPE.put("chatrecord", DownloadType.chatrecord);
    }

    public DownloadType getDownloadType(String msgType) {
        if (DOWNLOAD_TYPE.containsKey(msgType)) {
            return DOWNLOAD_TYPE.get(msgType);
        }
        return DownloadType.other;
    }


    public MsgHandler(String corpId, String secret) {

        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        this.secret = secret;
        this.sdk = Finance.NewSdk();
        this.corpid = corpId;
        int state = Finance.Init(sdk, corpId, secret);
        if (state != 0) {
            log.error("初始化（Finance.init）失败,corpId:{},secret:{}", corpId, secret);
        }
    }

    /**
     * 下载图片，音视频等常见类型
     *
     * @param wrapperName
     * @param msgTypeObject
     * @param ext
     * @param needFfmpeg
     * @throws Exception
     */
    public void handleFileDownload(
            String wrapperName,
            JSONObject msgTypeObject,
            String ext,
            boolean needFfmpeg,
            String msgId, long seq, Boolean bigFile

    ) throws Exception {
        JSONObject wrapper = msgTypeObject.getJSONObject(wrapperName);
        String md5sum = wrapper.getString("md5sum");
        String sdkFileId = wrapper.getString("sdkfileid");
        if (!sdkFileId.isEmpty()) {
            msgTypeObject.put("root_sdk_file_id", sdkFileId);
        }
        msgTypeObject.put("root_md5_sum", md5sum);
        downloadVoiceId(msgTypeObject, ext, md5sum, sdkFileId, md5sum, msgId, seq, bigFile);

    }

    /**
     * 下载文件类型的
     *
     * @param wrapperName
     * @param msgTypeObject
     * @param ext
     * @param needFfmpeg
     * @throws Exception
     */
    public void handleFileAttachDownload(
            String wrapperName,
            JSONObject msgTypeObject,
            String ext,
            boolean needFfmpeg,
            String msgId, long seq, Boolean bigFile) throws Exception {
        JSONObject wrapper = msgTypeObject.getJSONObject(wrapperName);
        String filename = wrapper.getString("filename");
        String sdkFileId = wrapper.getString("sdkfileid");
        String md5sum = wrapper.getString("md5sum");
        msgTypeObject.put("root_md5_sum", md5sum);
        if (!sdkFileId.isEmpty()) {
            msgTypeObject.put("root_sdk_file_id", sdkFileId);
        }
        downloadVoiceId(msgTypeObject, ext, filename, sdkFileId, md5sum, msgId, seq, bigFile);
    }

    /**
     * 下载文件类型的
     *
     * @param msgTypeObject
     * @param ext
     * @param needFfmpeg
     * @throws Exception
     */
    public String handleMeetingDownload(
            JSONObject msgTypeObject,
            String ext,
            boolean needFfmpeg, String msgId, long seq, Boolean bigFile) throws Exception {
        JSONObject wrapper = msgTypeObject.getJSONObject("meeting_voice_call");
        String filename = msgTypeObject.getString("voiceid");
        String sdkFileId = wrapper.getString("sdkfileid");
        if (!sdkFileId.isEmpty()) {
            msgTypeObject.put("root_sdk_file_id", sdkFileId);
        }
        downloadVoiceId(msgTypeObject, ext, filename, sdkFileId, "", msgId, seq, bigFile);
        return filename;
    }

    private void downloadVoiceId(JSONObject msgTypeObject, String ext, String filename, String sdkFileId, String md5Sum, String msgId, long seq, Boolean bigFile) throws Exception {
        if (sdkFileId == null) {
            throw new Exception("sdkFileId not exits");
        }
        try {
            downMedia(sdkFileId, filename, ext, md5Sum, msgTypeObject, msgId, seq, bigFile);
        } catch (Exception e) {
           log.error("downMediaError,msgId:{}",msgId,e);
        }
    }

    public Optional<Md5Index> getMd5Cache(String md5) {
        return JedisUtil.execute(jedisPool, jedis -> {
            try {
                String json = jedis.get(AppConstants.RedisKey.MD5_PREFIX + md5);
                if (json == null) {
                    return null;
                }
                return objectMapper.readValue(json, Md5Index.class);
            } catch (Exception e) {
                log.error("获取MD5缓存失败, md5:{}", md5, e);
                return null;
            }
        });
    }

    public void setMd5Cache(Md5Index md5Index) {
        JedisUtil.executeVoid(jedisPool, jedis -> {
            try {
                String key = AppConstants.RedisKey.MD5_PREFIX + md5Index.getMd5();
                String value = objectMapper.writeValueAsString(md5Index);
                jedis.setex(key, AppConstants.RedisExpire.MD5_CACHE_EXPIRE, value);
            } catch (Exception e) {
                log.error("设置MD5缓存失败, md5:{}", md5Index.getMd5(), e);
            }
        });
    }

    public static String getFileMd5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte[] buffer = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            log.error("计算文件MD5失败, file:{}", file.getAbsolutePath(), e);
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        String res = bigInt.toString(16);
        while (res.length() < 32) {
            res = "0" + res;
        }
        return res;
    }


    public boolean tryMd5Sum(String md5sum, JSONObject object) {
        if (!md5sum.isEmpty()) {

            Optional<Md5Index> redisMd5Optional = getMd5Cache(md5sum);
            if (redisMd5Optional.isPresent()) {
                object.put("file_path", redisMd5Optional.get().getFilePath());
                object.put("ossPath", redisMd5Optional.get().getOssPath());
                return true;
            }
            Optional<Md5Index> md5IndexOptional = md5IndexRepo.findFirstByMd5(md5sum);
            if (md5IndexOptional.isPresent()) {
                object.put("file_path", md5IndexOptional.get().getFilePath());
                object.put("ossPath", md5IndexOptional.get().getOssPath());
                Md5Index md5Index = md5IndexOptional.get();
                /**
                 * 大于阈值算是一个高频的文件，就不更新了，节约服务器资源
                 */
                if (md5Index.getTimes() < AppConstants.MessageThreshold.HIGH_FREQUENCY_FILE_TIMES) {
                    md5Index.setTimes(md5Index.getTimes() + 1);
                    md5IndexRepo.save(md5Index);
                }
                setMd5Cache(md5Index);
                return true;
            }
        }
        return false;
    }

    public Boolean isDownIng(String md5sum) {
        return JedisUtil.execute(jedisPool, jedis -> {
            String flag = jedis.get(md5sum);
            if (StringUtils.isEmpty(flag)) {
                jedis.setex(md5sum, AppConstants.RedisExpire.DOWNLOADING_FLAG_EXPIRE, "exist");
                return false;
            } else {
                return true;
            }
        }).orElse(false);
    }

    /**
     * 在进行mixed类型的消息里的附件下载时，如果下载失败，则整条丢进rocketMQ重试队列 。
     *
     * @param sdkFileId
     * @param mediaPath
     * @param ext
     * @param md5sum
     * @param object
     * @param wholeRootObject
     * @param msgId
     * @return boolean 下载成功就返回true，否则返回false
     * @throws Exception
     */
    public boolean simpleDownMedia(String sdkFileId, String mediaPath, String ext,
                                   @NonNull String md5sum, JSONObject object, JSONObject wholeRootObject, String msgId, long seq, Boolean bigFile) throws Exception {
        if (StringUtils.isEmpty(md5sum)) {
            md5sum = Md5Util.getMd5(sdkFileId);
        }
        if (tryMd5Sum(md5sum, object)) {
            if (object.has("ossPath") && StringUtils.isNotEmpty(object.getString("ossPath"))) {
                return true;
            }
        }

//        if(isDownIng(md5sum)){
//            return false;
//        }
        String indexBuff = "";
        String dateStr = DateUtil.nowYyyyMmDdHh();
        String localPath = this.getPrefix() + dateStr + "_" + mediaPath + "" + ext;
        String ossTargetPath = "mochat2/" + dateStr.replace("_", "/") + "/" + mediaPath + "" + ext;


        if(StringUtils.isEmpty(md5sum)){
            String form = "";
            if(wholeRootObject.has("form")){
                form = wholeRootObject.getString(form);
            }
            if(object.has("form")){
                form = object.getString(form);
            }
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                String path = jedis.get("no_md5sum_file_" +form+"_"+localPath);
                if(StringUtils.isNotEmpty(path)){
                    object.put("ossPath", path);
                    // filePath 不要了
                    return true;
                }

            } catch (Exception e) {
                log.error("Redis操作失败", e);
            } finally {
                if (jedis != null) {
                    jedisPool.returnResource(jedis);
                }
            }
        }

        long startDownTime = System.currentTimeMillis();
        FileUtil.safeDelete(localPath);
        while (true) {
            long mediaData = Finance.NewMediaData();
            int ret = Finance.GetMediaData(this.sdk, indexBuff, sdkFileId, "", "", 60, mediaData);

            if (RetryUtil.isNetworkError(ret)) {
                /**
                 * 网络有波动，先发进 rocket,一分钟后重试
                 */
                RetryUtil.sendRetryMessage(wholeRootObject, messageProducerAdapter, 
                    getEventBus(), this.getSecret(), getProfile(), TRY_COUNT);
                return false;
            }

            if (ret != 0) {
                log.error("获取失败下载句柄失败,corpID:{},msgId:{},sdkFileId:{},returnValue:{}", this.corpid, msgId, sdkFileId, ret);
                object.put("down_fail_at", System.currentTimeMillis());
                RetryUtil.sendRetryMessage(wholeRootObject, messageProducerAdapter, 
                    getEventBus(), this.getSecret(), getProfile(), TRY_COUNT);
                return false;
            }
            FileOutputStream outputStream = new FileOutputStream(localPath, true);
            outputStream.write(Finance.GetData(mediaData));
            outputStream.flush();
            outputStream.close();
            if (Finance.IsMediaDataFinish(mediaData) == 1) {
                try {
                    eventBus.getTotalRealDount().incrementAndGet();
                    eventBus.getMixedTypeItemCounter().incrementAndGet();
                    Finance.FreeMediaData(mediaData);
                    if (md5sum.length() > 0 && !(".gif".equalsIgnoreCase(ext))) {
                        object.put("file_path", this.getPrefix() + mediaPath + "" + ext);
                        String md5 = getFileMd5(new File(localPath));
                        if (!md5sum.equalsIgnoreCase(md5)) {
                            log.error("下载文件失败，md5校验失败,文件:{},消息md5sum:{},计算md5:{}", this.getPrefix() + mediaPath + "" + ext, md5sum, md5);
                        } else {
                            log.info("md5sum 校验成功");
                        }
                    }
                    if (".amr".equals(ext)) {
                        String localPathMp3 = this.getPrefix() + dateStr + "_" + mediaPath + "" + ".mp3";
                        Audio.toMp3(localPath, localPathMp3);
                        // 删除.amr中间文件
                        FileUtil.safeDelete(localPath);
                        localPath = localPathMp3;
                        ossTargetPath =   "mochat2/" + dateStr.replace("_", "/") + "/" + mediaPath +".mp3";
                    }
                    Boolean result = ossUtil.upload(localPath, ossTargetPath);
                    if (result) {
                        object.put("ossPath", ossTargetPath);
                    } else {
                        log.error("uploadOSSError,msgId:{}", msgId);
                    }

                    if (result) {
                        object.put("ossPath", ossTargetPath);
                        if(StringUtils.isEmpty(md5sum)){
                            String form = "";
                            if(wholeRootObject.has("form")){
                                form = wholeRootObject.getString(form);
                            }
                            if(object.has("form")){
                                form = object.getString(form);
                            }
                            Jedis jedis = null;
                            try {
                                jedis = jedisPool.getResource();
                                jedis.setex("no_md5sum_file_" +form+"_"+localPath,7200,ossTargetPath);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (jedis != null) {
                                    jedisPool.returnResource(jedis);
                                }
                            }
                        }
                    } else {
                        log.error("uploadOSSError,msgId:{}", msgId);
                    }
                    return true;
                } catch (Exception e) {
                    log.error("downFileError,msgId:{},", msgId, e);
                } finally {
                    if (object.has("ossPath")) {
                        eventBus.getRocketRetrySucc().incrementAndGet();
                    }
                    if (!object.has("ossPath")) {
                        RetryUtil.sendRetryMessage(wholeRootObject, messageProducerAdapter, 
                            getEventBus(), this.getSecret(), getProfile(), TRY_COUNT);
                    }
                    FileUtil.safeDelete(localPath);
                }
            } else {
                indexBuff = Finance.GetOutIndexBuf(mediaData);
                Finance.FreeMediaData(mediaData);
                if (!bigFile) {
                    long DownIngTime = System.currentTimeMillis();
                    if (DownIngTime - startDownTime > BIG_FILE_TIME) {
                        log.error("DownOutOfTimeError,{},{},{},{}", msgId, startDownTime, DownIngTime, seq);
                        Optional<OriginalMsg> optionalOriginalMsg = originalMsgRepo.findFirstByCorpIdAndMsgIdAndSeq(corpid, msgId, seq);
                        if (optionalOriginalMsg.isPresent()) {
                            OriginalMsg originalMsg = optionalOriginalMsg.get();
                            log.error("BigFile,{}", originalMsg.getId());
                            originalMsg.setPushAt(-999L);
                            originalMsgRepo.save(originalMsg);
                            // 删除大文件
                            File file = new File(localPath);
                            if (file.exists()) {
                                file.delete();
                            }
                            return false;
                        }
                    }
                }
            }
        }

    }

    public void downMedia(String sdkFileId, String mediaPath, String ext,
                          @NonNull String md5sum, JSONObject object, String msgId, long seq, Boolean bigFile) throws Exception {

        if (tryMd5Sum(md5sum, object)) {
            if (object.has("ossPath") && StringUtils.isNotEmpty(object.getString("ossPath"))) {
                return;
            }
        }
        if(StringUtils.isEmpty(md5sum)){
            log.warn("md5sumNullError , msgId:{}",msgId);
        }
        if(StringUtils.isNotEmpty(md5sum) && isDownIng(md5sum)){
            if(object.has("push_at")){
                if(object.getLong("push_at")< -5 && object.getLong("push_at")> -100){
                    // 下载次数达5次后直接突破文件不能并行下载的限制 // 除了大文件
                    log.error("MsgOverDown , msgId：{},{}",msgId,object);
                }else{
                    return;
                }
            }else{
                return ;
            }
        }
        String indexbuf = "";
        String dateStr = DateUtil.nowYyyyMmDdHh();
        String localPath = this.getPrefix() + dateStr + "_" + mediaPath + "" + ext;
        String ossTargetPath = "mochat2/" + dateStr.replace("_", "/") + "/" + mediaPath + "" + ext;
        long startDownTime = System.currentTimeMillis();
        FileUtil.safeDelete(localPath);
        if(StringUtils.isEmpty(md5sum)){
            String form = "";
            if(object.has("form")){
                form = object.getString(form);
            }
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                String path = jedis.get("no_md5sum_file_" +form+"_"+localPath);
                if(StringUtils.isNotEmpty(path)){
                    object.put("ossPath", path);
                    return;
                    // filePath 不要了
                }

            } catch (Exception e) {
                log.error("Redis操作失败", e);
            } finally {
                if (jedis != null) {
                    jedisPool.returnResource(jedis);
                }
            }
        }
        while (true) {
            long mediaData = Finance.NewMediaData();
            int ret = Finance.GetMediaData(this.sdk, indexbuf, sdkFileId, "", "", 60, mediaData);

            if (ret == 10001 || ret == 10002 || ret == 10003 || ret == 10009 || ret == 10011) {
                /**
                 * 网络有波动，先发进 rocket,一分钟后重试;
                 */
                log.error("下载失败，丢进 rocketMq 10秒后下载,downFileError,msgId:{},tryMsg:{}",msgId,object);
                String tag = getProfile();
                if (tag == null || tag.isEmpty()) {
                    tag = "dev";
                }
                try {
                    object.put("secret", this.getSecret());
                    getEventBus().getRocketRetryCounter().incrementAndGet();
                    object.put("rocketRetry", "1");
                    if (object.has("tryCount")) {
                        int tryCount = object.getInt("tryCount");
                        if (tryCount < TRY_COUNT) {
                            object.put("tryCount", tryCount + 1);
                            messageProducerAdapter.sendDelayMessage(object.toString(), msgId);
                        }
                    } else {
                        object.put("tryCount", 1);
                        messageProducerAdapter.sendDelayMessage(object.toString(), msgId);
                    }
                } catch (Exception e) {
                    log.error("尝试用 rocketMq记录下载错误的，失败了");
                }
                return;
            }
            if (ret != 0) {
                Optional<OriginalMsg> originalMsgOptional = originalMsgRepo.findFirstByCorpIdAndMsgIdAndSeq(this.corpid, msgId, seq);
                if(originalMsgOptional.isPresent()) {
                    // 有可能修复线程发了消息消费成功了
                    if(StringUtils.isEmpty(originalMsgOptional.get().getOssPath())) {
                        log.error("获取失败下载句柄失败,corpId:{},msgId:{},sdkFileId:{},returnValue:{}", getCorpid(), msgId, sdkFileId, ret);
                        object.put("down_fail_at", System.currentTimeMillis());
                        try {
                            String tag = getProfile();
                            if (tag == null || tag.isEmpty()) {
                                tag = "dev";
                            }
                            object.put("secret", this.getSecret());
                            log.error("下载失败，丢进 rocketMq 10秒后下载,downFileError,msgId:{},tryMsg:{}", msgId, object);
                            getEventBus().getRocketRetryCounter().incrementAndGet();
                            object.put("rocketRetry", "1");
                            if (object.has("tryCount")) {
                                int tryCount = object.getInt("tryCount");
                                if (tryCount < TRY_COUNT) {
                                    object.put("tryCount", tryCount + 1);
                                    messageProducerAdapter.sendDelayMessage(object.toString(), tag);
                                }
                            } else {
                                object.put("tryCount", 1);
                                messageProducerAdapter.sendDelayMessage(object.toString(), tag);
                            }
                        } catch (Exception e) {
                            log.error("尝试用 rocketMq记录下载错误的，失败了");
                        }
                    }
                }
                return;
            }

            FileOutputStream outputStream = new FileOutputStream(localPath, true);
            outputStream.write(Finance.GetData(mediaData));
            outputStream.flush();
            outputStream.close();
            if (Finance.IsMediaDataFinish(mediaData) == 1) {
                try {
                    eventBus.getTotalRealDount().incrementAndGet();
                    Finance.FreeMediaData(mediaData);
                    object.put("file_path", this.getPrefix() + mediaPath + "" + ext);
                    if (md5sum.length() > 0 && !(".gif".equalsIgnoreCase(ext))) {
                        String md5 = getFileMd5(new File(localPath));
                        if (!md5sum.equalsIgnoreCase(md5)) {
                            log.error("md5Error,msgId:{}", msgId);
                            log.error("下载文件失败，md5校验失败,文件:{},消息md5sum:{},计算md5:{}", this.getPrefix() + mediaPath + "" + ext, md5sum, md5);
                            //     throw new Exception("下载文件失败，md5校验失败");
                        } else {
                            log.info("md5sum 校验成功");
                        }

                    }
                    if (".amr".equals(ext)) {
                        String localPathMp3 = this.getPrefix() + dateStr + "_" + mediaPath + "" + ".mp3";
                        Audio.toMp3(localPath, localPathMp3);
                        try {
                            File file = new File(localPath);
                            if (file.exists()) {
                                file.delete();
                            }
                        }catch (Exception e){
                            log.error("ClearFileError:",e);
                        }
                        localPath = localPathMp3;
                        ossTargetPath =   "mochat2/" + dateStr.replace("_", "/") + "/" + mediaPath +".mp3";
                    }
                    Boolean result = ossUtil.upload(localPath, ossTargetPath);
                    if (result) {
                        object.put("ossPath", ossTargetPath);
                        if(StringUtils.isEmpty(md5sum)){
                            String form = "";
                            if(object.has("form")){
                                form = object.getString(form);
                            }
                            Jedis jedis = null;
                            try {
                                jedis = jedisPool.getResource();
                                jedis.setex("no_md5sum_file_" +form+"_"+localPath,7200,ossTargetPath);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (jedis != null) {
                                    jedisPool.returnResource(jedis);
                                }
                            }
                        }
                    } else {
                        log.error("uploadOSSError,msgId:{}", msgId);
                    }

                } catch (Exception e) {
                    log.error("downFileError,msgId:{},", msgId, e);

                } finally {
                    if (object.has("ossPath")) {
                        eventBus.getRocketRetrySucc().incrementAndGet();
                    }
                    if (!object.has("ossPath")) {
                        String tag = getProfile();
                        if (tag == null || tag.isEmpty()) {
                            tag = "dev";
                        }
                        Optional<OriginalMsg> originalMsgOptional = originalMsgRepo.findFirstByCorpIdAndMsgIdAndSeq(this.corpid, msgId, seq);
                        if(originalMsgOptional.isPresent()){
                            // 有可能修复线程发了消息消费成功了
                            if(StringUtils.isEmpty(originalMsgOptional.get().getOssPath())){
                                object.put("secret", this.getSecret());
                                log.error("下载失败，丢进 rocketMq 10秒后下载,downFileError,msgId:{},tryMsg:{}",msgId,object);
                                getEventBus().getRocketRetryCounter().incrementAndGet();
                                object.put("rocketRetry", "1");
                                if (object.has("tryCount")) {
                                    int tryCount = object.getInt("tryCount");
                                    if (tryCount < TRY_COUNT) {
                                        object.put("tryCount", tryCount + 1);
                                        messageProducerAdapter.sendDelayMessage(object.toString(), tag);
                                    }
                                } else {
                                    object.put("tryCount", 1);
                                    messageProducerAdapter.sendDelayMessage(object.toString(), tag);
                                }
                            }
                        }
                    }
                    FileUtil.safeDelete(localPath);
                    break;
                }
            } else {
                indexbuf = Finance.GetOutIndexBuf(mediaData);
                Finance.FreeMediaData(mediaData);
                if (!bigFile) {
                    long DownIngTime = System.currentTimeMillis();
                    if (DownIngTime - startDownTime > BIG_FILE_TIME) {
                        log.error("DownOutOfTimeError,{},{},{},{}", msgId, startDownTime, DownIngTime, seq);
                        Optional<OriginalMsg> optionalOriginalMsg = originalMsgRepo.findFirstByCorpIdAndMsgIdAndSeq(corpid, msgId, seq);
                        if (optionalOriginalMsg.isPresent()) {
                            OriginalMsg originalMsg = optionalOriginalMsg.get();
                            log.error("BigFile,{}", originalMsg.getId());
                            originalMsg.setPushAt(-999L);
                            originalMsgRepo.save(originalMsg);
                            File file = new File(localPath);
                            if (file.exists()) {
                                file.delete();
                            }
                            return;
                        }
                    }
                }
            }

        }
    }


    public String normalizeMsgType(String msgType) {
        if ("docmsg".equalsIgnoreCase(msgType)) {
            return "doc";
        }
        if ("markdown".equalsIgnoreCase(msgType)) {
            return "info";
        }
        if ("news".equalsIgnoreCase(msgType)) {
            return "info";
        }
        if("qydiskfile".equalsIgnoreCase(msgType)){
            return "info";
        }
        if ("external_redpacket".equalsIgnoreCase(msgType)) {
            return "redpacket";
        }
        if("voiptext".equalsIgnoreCase(msgType)){
            return "info";
        }
        return msgType;
    }

    public void updateDownloadStatus(String msgId, long seq, JSONObject object) {
        String type = object.getString("msgtype");
        if ("mixed".equalsIgnoreCase(type)) {
            /**
             * mixed类型的比较特殊 ，不在这里更新;
             */
            return;
        }
        Optional<OriginalMsg> originalMsgOptional = originalMsgRepo.findFirstByCorpIdAndMsgIdAndSeq(this.corpid, msgId, seq);
        if (originalMsgOptional.isPresent()) {
            OriginalMsg originalMsg = originalMsgOptional.get();
            try {
                String rootMd5Sum = object.getString("root_md5_sum");
                originalMsg.setMd5Sum(rootMd5Sum);
            } catch (JSONException ignore) {
            }
            try {
                long downFailAt = object.getLong("down_fail_at");
                originalMsg.setDownFailAt(downFailAt);
            } catch (JSONException e) {
                originalMsg.setDownFinishAt(System.currentTimeMillis());
            }
            try {
                String rootSdkFileId = object.getString("root_sdk_file_id");
                originalMsg.setSdkfileid(rootSdkFileId);
            } catch (JSONException ignore) {

            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                String filePath = "";
                String rootMd5Sum = "";
                String ossPath = "";
                if (object.has("file_path")) {
                    filePath = object.getString("file_path");
                    originalMsg.setFilePath(filePath);
                }
                try {
                    if (object.has("root_md5_sum")) {
                        rootMd5Sum = object.getString("root_md5_sum");
                        originalMsg.setMd5Sum(rootMd5Sum);
                    }
                    if (object.has("ossPath")) {
                        ossPath = object.getString("ossPath");
                        originalMsg.setOssPath(ossPath);
                    }


                    Optional<Md5Index> indexOptional = md5IndexRepo.findFirstByMd5(rootMd5Sum);
                    if (!indexOptional.isPresent()) {
                        Md5Index md5Index = new Md5Index();
                        md5Index.setFilePath(filePath);
                        md5Index.setMd5(rootMd5Sum);
                        try {
                            md5Index.setOssPath(ossPath);
                            md5Index.setOssAt(System.currentTimeMillis());
                        } catch (JSONException e3) {
                            //
                        }
                        if(StringUtils.isNotEmpty(ossPath)) {
                            md5IndexRepo.save(md5Index);
                            setMd5Cache(md5Index);
                        }
                    }
                } catch (JSONException e2) {

                }
            } catch (JSONException ignore) {

            } catch (Exception e) {
                e.printStackTrace();
            }
            // todo 判断下聊天数据是否下载完了附件发消息到mq
            if (StringUtils.isNotEmpty(originalMsg.getOssPath())) {
                JSONObject jsonObject = new JSONObject(originalMsg.getContent());
                jsonObject.put("seq", seq);
                jsonObject.put("ossPath", originalMsg.getOssPath());
                jsonObject.put("source", "java");
                try{
                    jsonObject.put("_sign",Md5Util.getMd5(originalMsg.getOssPath()));
                }catch (Exception e){
                    log.error("GetOssPathError:",e);
                }
                String key = msgId;
                if (StringUtils.isEmpty(key)) {
                    key = UUID.randomUUID().toString();
                }
                // 统一使用 TCP 协议发送消息
                {
                    messageProducerAdapter.send(jsonObject.toString(), key);
                }
                originalMsg.setPushAt(System.currentTimeMillis());
            }
            originalMsgRepo.save(originalMsg);
        }

    }


    public void handleMsgObject(String msgId, long seq, JSONObject object) throws Exception {
        String msgType = "";
        try {
            msgType = object.getString("msgtype");
        } catch (JSONException e) {
            log.error("意外错误:找不到 msgtype:{}", object.toString());
            return;
        }
        Boolean bigFile = false;
        if (object.has("big_file")) {
            bigFile = object.getBoolean("big_file");
        }
        JSONObject msgTypeObject = object.getJSONObject(normalizeMsgType(msgType));
        if (msgTypeObject == null) {
            throw new Exception("msgTypeObject is null");
        }
        DownloadType downloadType = getDownloadType(msgType);
        String ext = "";
        switch (downloadType) {
            case image:
                ext = ".jpg";
                handleFileDownload("image", object, ext, false, msgId, seq, bigFile);
                break;
            case voice:
                ext = ".amr";
                handleFileDownload("voice", object, ext, true, msgId, seq, bigFile);
                break;
            case video:
                ext = ".mp4";
                handleFileDownload("video", object, ext, false, msgId, seq, bigFile);
                break;
            case emotion:
                int type;
                try {
                    type = msgTypeObject.getInt("type");
                } catch (Exception e) {
                    throw new Exception("when msgType=emotion,type is required");
                }
                if (type == 1) {
                    ext = ".gif";
                } else {
                    ext = ".png";
                }
                handleFileDownload("emotion", object, ext, false, msgId, seq, bigFile);
                break;
            case file:
                handleFileAttachDownload("file", object, ext, false, msgId, seq, bigFile);
                break;
            case voip_doc_share:
                handleFileAttachDownload("voip_doc_share", object, ext, false, msgId, seq, bigFile);
                break;
            case meeting_voice_call:
                ext = ".mp3";
                handleMeetingDownload(object, ext, true, msgId, seq, bigFile);
                break;
            case mixed:
                throw new Exception("mixed类型的应由MixedHandler处理");
            case chatrecord:
                throw new Exception("chatrecord类型的应用ChatRecordHandler处理");
            default:
                break;
        }
    }


}
