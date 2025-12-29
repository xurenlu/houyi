package com.ruoran.houyi.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.UploadFileRequest;
import com.ruoran.houyi.Md5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.File;

/**
 * @author lh
 */
@Component
@Slf4j
public class OssUtil {

    @Value("${aliyun.bucket}")
    String bucket;

    @Resource
    OSS oss;

    public Boolean upload(String localPath,String targetPath){
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
            log.error("上传oss文件失败：",throwable);
            return false;
        }
        return true;
    }
}
