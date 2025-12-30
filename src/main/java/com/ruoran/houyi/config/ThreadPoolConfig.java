package com.ruoran.houyi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 统一线程池配置
 * 
 * @author refactored
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Value("${thread-pool.download.core-size:8}")
    private int downloadCoreSize;

    @Value("${thread-pool.download.max-size:100}")
    private int downloadMaxSize;

    @Value("${thread-pool.download.queue-capacity:10000}")
    private int downloadQueueCapacity;

    @Value("${thread-pool.oss.core-size:4}")
    private int ossCoreSize;

    @Value("${thread-pool.oss.max-size:60}")
    private int ossMaxSize;

    @Value("${thread-pool.oss.queue-capacity:10000}")
    private int ossQueueCapacity;

    /**
     * 下载线程池
     */
    @Bean("downloadExecutor")
    public ThreadPoolTaskExecutor downloadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(downloadCoreSize);
        executor.setMaxPoolSize(downloadMaxSize);
        executor.setQueueCapacity(downloadQueueCapacity);
        executor.setKeepAliveSeconds(360);
        executor.setThreadNamePrefix("download-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("下载线程池初始化完成: core={}, max={}, queue={}", 
            downloadCoreSize, downloadMaxSize, downloadQueueCapacity);
        return executor;
    }

    /**
     * OSS上传线程池
     */
    @Bean("ossExecutor")
    public ThreadPoolTaskExecutor ossExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(ossCoreSize);
        executor.setMaxPoolSize(ossMaxSize);
        executor.setQueueCapacity(ossQueueCapacity);
        executor.setKeepAliveSeconds(360);
        executor.setThreadNamePrefix("oss-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("OSS线程池初始化完成: core={}, max={}, queue={}", 
            ossCoreSize, ossMaxSize, ossQueueCapacity);
        return executor;
    }
}

