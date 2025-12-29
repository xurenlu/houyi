package com.ruoran.houyi.service;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author renlu
 * created by renlu at 2021/7/15 6:34 下午
 */
@Service
@Data
public class EventBus {
    AtomicLong totalMsg = new AtomicLong(0);
    AtomicLong totalDownload  = new AtomicLong(0);
    AtomicLong totalRealDount = new AtomicLong(0);
    AtomicLong md5CacheHits = new AtomicLong(0);
    AtomicLong md5CacheWrites = new AtomicLong(0);
    AtomicLong rocketRetryCounter = new AtomicLong(0);
    AtomicLong rocketRetrySucc = new AtomicLong(0);
    volatile Integer activeThreadCount = 0;
    volatile Double threadExecuteTime = 0.0d;
    AtomicLong mixedTypeItemCounter = new AtomicLong(0);
    AtomicLong mixedTypeRootCounter = new AtomicLong(0);

    ConcurrentHashMap<String,AtomicLong> diffs = new ConcurrentHashMap<>(16);
    ConcurrentHashMap<String,Boolean> corpsStatus = new ConcurrentHashMap<>(16);

}
