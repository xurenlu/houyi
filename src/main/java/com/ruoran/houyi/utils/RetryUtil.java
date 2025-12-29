package com.ruoran.houyi.utils;

import com.ruoran.houyi.mq.HouyiTcpConstructionMessageProduct;
import com.ruoran.houyi.service.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 重试逻辑工具类
 * 使用 RocketMQ 5.0 TCP 协议发送重试消息
 *
 * @author refactored
 */
@Slf4j
public class RetryUtil {

    /**
     * 发送重试消息到 RocketMQ（TCP 协议）
     *
     * @param wholeRootObject 完整的消息对象
     * @param producer        TCP 生产者
     * @param eventBus        事件总线
     * @param secret          密钥
     * @param profile         环境配置（已废弃，保留用于兼容）
     * @param maxTryCount     最大重试次数
     */
    public static void sendRetryMessage(JSONObject wholeRootObject, 
                                       HouyiTcpConstructionMessageProduct producer,
                                       EventBus eventBus, String secret, 
                                       String profile, int maxTryCount) {
        try {
            wholeRootObject.put("secret", secret);
            
            AtomicLong retryCounter = eventBus.getRocketRetryCounter();
            if (retryCounter != null) {
                retryCounter.incrementAndGet();
            }
            
            wholeRootObject.put("rocketRetry", "1");
            int tryCount = wholeRootObject.has("tryCount") ? wholeRootObject.getInt("tryCount") : 0;
            
            if (tryCount < maxTryCount) {
                wholeRootObject.put("tryCount", tryCount + 1);
                
                String msgId = wholeRootObject.optString("msgid", "unknown");
                
                // 使用 TCP 延迟消息发送到重试队列
                producer.sendDelayMessage(wholeRootObject.toString(), msgId);
                
                log.info("重试消息已发送: msgId={}, tryCount={}/{}", 
                    msgId, tryCount + 1, maxTryCount);
            } else {
                log.warn("重试次数已达上限，放弃重试: msgId={}, tryCount={}", 
                    wholeRootObject.optString("msgid", "unknown"), tryCount);
            }
        } catch (Exception e) {
            log.error("发送重试消息失败", e);
        }
    }

    /**
     * 判断是否是网络错误，需要重试
     *
     * @param ret 返回码
     * @return 是否需要重试
     */
    public static boolean isNetworkError(int ret) {
        return ret == 10001 || ret == 10002 || ret == 10003 || ret == 10009 || ret == 10011;
    }
}

