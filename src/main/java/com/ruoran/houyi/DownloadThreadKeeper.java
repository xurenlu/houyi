package com.ruoran.houyi;

import com.ruoran.houyi.service.EventBus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import jakarta.annotation.Resource;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author renlu
 * created by renlu at 2021/7/14 10:51 上午
 */
@Data
@Slf4j
@Component
public class DownloadThreadKeeper {

    private ExecutorService executorService;
    private int maxRequests = 100;
    private static final int DEFAULT_THREAD_KEEP_ALIVE_TIME = 360;



    @Resource
    SpringContextUtils springContextUtils;

    @Resource
    EventBus eventBus;


    DefaultAsyncThreadFactory factory = new DefaultAsyncThreadFactory();

    public DefaultAsyncThreadFactory getFactory() {
        return factory;
    }

    public void init() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(8, getMaxRequests(), DEFAULT_THREAD_KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(10000),
                    factory);
        }
    }

    @Data
    public static class DefaultAsyncThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        public AtomicInteger getCounter() {
            return counter;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "download_thread_" + counter.incrementAndGet());
        }
    }

    public void execute(final String corpId, final String msgId, final String secret, long seq, JSONObject object) {
        int activeCount = ((ThreadPoolExecutor) getExecutorService()).getActiveCount();
        eventBus.setActiveThreadCount(activeCount);
        getExecutorService().execute(() -> {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try {
                String msgType = object.getString("msgtype");
                if ("mixed".equalsIgnoreCase(msgType)||"chatrecord".equalsIgnoreCase(msgType)) {
                    MixedHandler handler = new MixedHandler(corpId, secret);
                    springContextUtils.autowireBean(handler);
                    handler.handleMsgObject(msgId, seq, object);
                } else {
                    MsgHandler handler = new MsgHandler(corpId, secret);
                    springContextUtils.autowireBean(handler);
                    handler.handleMsgObject(msgId, seq, object);
                    handler.updateDownloadStatus(msgId, seq, object);
                }
            } catch (Exception e) {
                log.error("子线程异常,childTheadError,msg:{}",object, e);
            }
            stopWatch.stop();
            eventBus.setThreadExecuteTime(stopWatch.getTotalTimeSeconds());
        });
    }
}
