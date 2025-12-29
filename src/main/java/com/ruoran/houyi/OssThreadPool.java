package com.ruoran.houyi;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.UploadFileRequest;
import com.ruoran.houyi.repo.OriginalMsgRepo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import javax.imageio.IIOException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author renlu
 * created by renlu at 2021/7/15 2:21 下午
 */
@Service
@Data
@Slf4j
public class OssThreadPool implements InitializingBean {

    private ExecutorService executorService;
    private int maxRequests = 60;
    private static final int DEFAULT_THREAD_KEEP_ALIVE_TIME = 360;

    @Resource
    OriginalMsgRepo originalMsgRepo;

    @Resource
    SpringContextUtils springContextUtils;

    @Value("${aliyun.bucket}")
    String bucket;

    @Resource
    OSS oss;

    OssThreadPool.DefaultAsyncThreadFactory factory = new OssThreadPool.DefaultAsyncThreadFactory();
    @Override
    public void afterPropertiesSet() throws Exception {
        this.init();
    }

    public void init(){
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(4, getMaxRequests(), DEFAULT_THREAD_KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(10000),
                    factory);
        }
    }

    public static class DefaultAsyncThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        public AtomicInteger getCounter(){
            return counter;
        }

        @Override
        public Thread newThread( Runnable runnable) {
            return new Thread(runnable, "oss_thread_" + counter.incrementAndGet());
        }
    }

    public Future<Boolean> execute(String localPath,String targetPath){
        return getExecutorService().submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                ObjectMetadata meta = new ObjectMetadata();
                meta.setObjectAcl(CannedAccessControlList.Private);
                UploadFileRequest uploadFileRequest = new UploadFileRequest(bucket,targetPath);
                uploadFileRequest.setUploadFile(localPath);
                uploadFileRequest.setTaskNum(5);
                uploadFileRequest.setPartSize(1024 * 1024);
                uploadFileRequest.setEnableCheckpoint(true);
                uploadFileRequest.setCheckpointFile(Md5Util.getMd5(localPath));
                uploadFileRequest.setObjectMetadata(meta);
                try {
                    oss.uploadFile(uploadFileRequest);
                    File file = new File(localPath);
                    if(file.delete()){
                        log.info(localPath+" 删除成功");
                    }
                }catch (Throwable throwable){
                    log.error("OSS上传任务执行失败", throwable);
                    throw new Exception("上传失败");
                }
                return Boolean.TRUE;
            }
        });
    }


}
