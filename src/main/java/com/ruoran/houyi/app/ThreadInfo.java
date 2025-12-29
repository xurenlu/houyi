package com.ruoran.houyi.app;

import com.ruoran.houyi.DownloadThreadKeeper;
import com.ruoran.houyi.OssThreadPool;
import com.ruoran.houyi.Start;
import com.ruoran.houyi.model.CorpInfo;
import com.ruoran.houyi.repo.CorplistRepo;
import com.ruoran.houyi.service.EventBus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author renlu
 * created by renlu at 2021/7/15 6:09 下午
 */

@Component
@Slf4j
public class ThreadInfo implements MeterBinder {

    @Resource
    DownloadThreadKeeper downloadThreadKeeper;

    @Resource
    OssThreadPool ossThreadPool;

    @Resource
    Start start;

    @Resource
    EventBus eventBus;

    @Resource
    CorplistRepo corplistRepo;

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        Gauge.builder("dayu.workers.downloadThread", this::getDownloadThreadCount).baseUnit("个")
                .description("下载线程数").register(meterRegistry);
        Gauge.builder("dayu.workers.ossThread", this::getOssThreadCount).baseUnit("个")
                .description("oss上传线程数").register(meterRegistry);
        Gauge.builder("dayu.workers.messageThread",this::getMessageThread).baseUnit("个").description("消息进程数").register(meterRegistry);
        Gauge.builder("dayu.counter.totalMsg",this::getTotalMsg).baseUnit("条").description("收到的消息数").register(meterRegistry);
        Gauge.builder("dayu.counter.totalDownload",this::getTotalDownload).baseUnit("个").description("总下载文件数").register(meterRegistry);
        Gauge.builder("dayu.counter.rocketRetryCounter",this::getRocketRetryCounter).baseUnit("个")
                .description("通过 rocket 重试的次数").register(meterRegistry);
        Gauge.builder("dayu.counter.activeThreadCount",this::getActiveThreadCount).baseUnit("个")
                .description("下载活动线程数").register(meterRegistry);
        Gauge.builder("dayu.counter.threadExecuteTime",this::getThreadExecuteTime).baseUnit("秒")
                .description("下载任务执行时间").register(meterRegistry);
        Gauge.builder("dayu.counter.rocketRetrySuccCounter",this::getRocketSucc).baseUnit("个").description("从 rocket 拿出来重试并成功的次数").register(meterRegistry);


        List<CorpInfo> corpInfos = corplistRepo.findAll();
        for(CorpInfo info:corpInfos){
            eventBus.getDiffs().put(info.getCorpid(),new AtomicLong(1));
        }
        for(CorpInfo corpInfo :corpInfos){
            String corpId = corpInfo.getCorpid();
            log.error("time diff for corpId:{}",corpId);
            Gauge.builder("dayu.timediff."+corpId,()-> eventBus.getDiffs().get(corpId)).baseUnit("ms").description("当前消息与系统时间之差").register(meterRegistry);
        }
        Gauge.builder("dayu.counter.succCorpsCount",this::getSuccCorps).baseUnit("个").description("当前成功拉取消息的企业微信主体个数").register(meterRegistry);
        Gauge.builder("dayu.counter.failCorpsCount",this::getFailCorps).baseUnit("个").description("当前无法成功拉取消息的企业微信主体个数").register(meterRegistry);
        Gauge.builder("dayu.counter.totalDownloadSucc",()-> eventBus.getTotalRealDount()) .baseUnit("个").description("真实下载成功的文件数").register(meterRegistry);
        Gauge.builder("dayu.counter.mixedItemDownloadSucc",()-> eventBus.getMixedTypeItemCounter()).baseUnit("个").description("mixed单体下载成功数").register(meterRegistry);
        Gauge.builder("dayu.counter.mixedMsgDownloadSucc",()-> eventBus.getMixedTypeRootCounter()).baseUnit("个").description("mixed消息下载成功数").register(meterRegistry);
    }



    public Number getSuccCorps(){
        int count=0;
        for(Boolean bool :eventBus.getCorpsStatus().values()){
            if(bool){
                count++;
            }
       }
        return count;
    }

    public Number getFailCorps(){
        int count=0;
        for(Boolean bool :eventBus.getCorpsStatus().values()){
            if(!bool){
                count++;
            }
        }
        return count;
    }
    public Number getRocketSucc(){
        return  eventBus.getRocketRetrySucc();
    }
    public Number getTotalMsg(){
        return eventBus.getTotalMsg();
    }

    public Number getTotalDownload(){
        return eventBus.getTotalDownload();
    }
    public Number getDownloadThreadCount(){
         DownloadThreadKeeper.DefaultAsyncThreadFactory factory = downloadThreadKeeper.getFactory();
         return factory.getCounter();
    }

    public Number getOssThreadCount(){
        OssThreadPool.DefaultAsyncThreadFactory factory = ossThreadPool.getFactory();
        return factory.getCounter();
    }
    public Number getMessageThread(){
        return start.getCopySet().size();
    }
    public Number getRocketRetryCounter(){
        return eventBus.getRocketRetryCounter().get();
    }
    public Number getActiveThreadCount(){
        return eventBus.getActiveThreadCount();
    }
    public Number getThreadExecuteTime(){
        return eventBus.getThreadExecuteTime();
    }
}
