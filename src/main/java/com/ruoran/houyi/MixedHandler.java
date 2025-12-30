package com.ruoran.houyi;

import com.ruoran.houyi.model.OriginalMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * @author renlu
 * created by renlu at 2021/7/22 7:15 下午
 */

@Slf4j
public class MixedHandler extends MsgHandler {

    public MixedHandler(String corpId, String secret) {
        super(corpId, secret);
    }
    @Override
    public void handleMsgObject(String msgId,long seq,JSONObject object) throws Exception {
        String msgType = "";
        try{
            msgType = object.getString("msgtype");
        }catch(JSONException e){
            log.error("意外错误:找不到 msgtype:{}", object);
            return;
        }
        if(!"mixed".equalsIgnoreCase(msgType) &&!"chatrecord".equalsIgnoreCase(msgType)){
            log.error("MixedHandler只处理Mixed和ChatRecord类型的消息,现在是:{}",msgType);
            return;
        }
        JSONObject msgTypeObject = object.getJSONObject(normalizeMsgType(msgType));
        if(msgTypeObject==null){
            throw new Exception("msgTypeObject is null");
        }
        handleMixed(msgId,seq,object,msgType);
    }
    public void handleMixed(String msgId,long seq,JSONObject object,String msgType) throws Exception {
        JSONObject mixed = object.getJSONObject(msgType);
        JSONArray item = mixed.getJSONArray("item");
        List<JSONObject> results = new ArrayList<>();
        for(Object single:item){
            JSONObject itemObject = (JSONObject)single;
            if(!itemObject.has("content")){
                continue;
            }
            String content = itemObject.getString("content");
            JSONObject obj  =new JSONObject();
            JSONObject contentJson  =new JSONObject(content);
            obj.put("content",new JSONObject(content));
            String itemType = itemObject.getString("type");
            if(!itemObject.has("type")){
                results.add(obj);
                continue;
            }
            if(itemType!=null && !StringUtils.isEmpty(itemType)){
                itemType = itemType.toLowerCase(Locale.ROOT).replaceAll("chatrecord","");
            }
            obj.put("type",itemType);
            if("text".equalsIgnoreCase(itemType)){
                results.add(obj);
                continue;
            }
            String sdkFileId ="";
            try {
                sdkFileId = contentJson.getString("sdkfileid");
            }catch (Exception e){
                results.add(obj);
                continue;
            }
            String md5sum = contentJson.getString("md5sum");
            if(Message.typeNeedntDownload.contains(itemType)){
                results.add(obj);
                continue;
            }
            String mediaPath = "";
            if(!StringUtils.isEmpty(md5sum)){
                mediaPath = md5sum;
            }else{
                mediaPath  = Md5Util.getMd5(sdkFileId);
            }
            String itemExt = "";
            switch (itemType){
                case "image":
                    itemExt=".jpg";
                    break;
                case "emotion":
                    int type=0;
                    try {
                        type = contentJson.getInt("type");
                    }catch (Exception ignore){
                    }
                    if(type==1){
                        itemExt = ".gif";
                    }else{
                        itemExt = ".jpg";
                    }
                    break;
                case "voice":
                    itemExt=".amr";
                    break;
                case "video":
                    itemExt = ".mp4";
                    break;
                case "text":
                default:
                    break;
            }
            object.put("seq",seq);
            obj.put("seq",seq);
            Boolean bigFile = false;
            if(object.has("big_file")){
                bigFile = object.getBoolean("big_file");
            }
            if (!simpleDownMedia(sdkFileId, mediaPath,itemExt, md5sum, obj, object, msgId,seq,bigFile)) {
                return;
            }
            results.add(obj);
        }
        eventBus.getMixedTypeRootCounter().incrementAndGet();
        JSONArray array = new JSONArray(results);
        mixed.put("item",array);
        object.put(msgType,mixed);
        /*
         * 更新数据库里的这条信息;
         */
        Optional<OriginalMsg> originalMsgOptional = originalMsgRepo.findFirstByCorpIdAndMsgIdAndSeq(this.corpid,msgId,seq);
        if(!originalMsgOptional.isPresent()){
            log.error("mixed/chatRecord类型的消息,msgID:{} 不存在",msgId);
        }else {
            OriginalMsg originalMsg = originalMsgOptional.get();
            originalMsg.setContent(object.toString());
            originalMsg.setPushAt(System.currentTimeMillis());
            object.put("seq",seq);
            object.put("source","java");
            String key = msgId;
            if(StringUtils.isEmpty(key)){
                key = UUID.randomUUID().toString();
            }
            // 使用消息生产者适配器发送消息（自动选择 RocketMQ 或 Redis）
            messageProducerAdapter.send(object.toString(), key);
            originalMsgRepo.save(originalMsg);
        }
    }
}
