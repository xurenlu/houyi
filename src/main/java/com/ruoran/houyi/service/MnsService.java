package com.ruoran.houyi.service;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.aliyun.mns.model.Message;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ms404
 */
@Slf4j
@Service
public class MnsService implements CommandLineRunner {
    private MNSClient client = null;
    @Value("${mns.accesskeyid}")
    String keyId;
    @Value("${mns.accesskeysecret}")
    String secret;
    @Value("${mns.accountendpoing}")
    String acountEndpoint;

    @Value("${mns.queueName}")
    String queueName;
    CloudQueue queue;

    @Autowired
    MeterRegistry meterRegistry;

    ExecutorService pool = new ThreadPoolExecutor(16, 64, 360, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1280),
            new DefaultAsyncThreadFactory());
    public static class DefaultAsyncThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "houyi_mns_push" + counter.incrementAndGet());
        }
    }

    @Override
    public void run(String... args) throws Exception {
        this.init();
    }

    public void init(){
        CloudAccount account = new CloudAccount(keyId,secret,acountEndpoint);
        client = account.getMNSClient();
        queue = client.getQueueRef(queueName);
    }

    public void push(String body){

        pool.execute(()->{
            try {
                Message message = new Message();
                message.setMessageBody(body);
                queue.putMessage(message);
                meterRegistry.counter("dayu_mns_real_pushed", Tags.of("mns", "-")).increment();
            }catch (Exception e){
                log.error("MNS push failed", e);
            }
        });

    }

}
