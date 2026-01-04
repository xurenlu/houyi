package com.ruoran.houyi.downloader;

import com.ruoran.houyi.Audio;
import com.ruoran.houyi.constants.AppConstants;
import com.ruoran.houyi.model.Md5Index;
import com.ruoran.houyi.service.EventBus;
import com.ruoran.houyi.service.OssUtil;
import com.ruoran.houyi.utils.DateUtil;
import com.ruoran.houyi.utils.FileUtil;
import com.tencent.wework.Finance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 媒体文件下载器
 * 封装文件下载的核心逻辑，消除重复代码
 *
 * @author refactored
 */
@Slf4j
@Data
public class MediaDownloader {

    private final long sdk;
    private final String corpid;
    private final String prefix;
    private final EventBus eventBus;
    private final OssUtil ossUtil;

    /**
     * 下载上下文
     */
    @Data
    public static class DownloadContext {
        private String sdkFileId;
        private String mediaPath;
        private String ext;
        private String md5sum;
        private String msgId;
        private long seq;
        private Boolean bigFile;
        private JSONObject object;
        private long startDownTime;
        private String localPath;
        private String ossTargetPath;
        private String dateStr;
    }

    /**
     * 下载媒体文件
     *
     * @param context 下载上下文
     * @return 下载是否成功
     * @throws Exception 下载异常
     */
    public boolean downloadMedia(DownloadContext context) throws Exception {
        prepareDownload(context);
        
        String indexBuff = "";
        while (true) {
            long mediaData = Finance.NewMediaData();
            int ret = Finance.GetMediaData(this.sdk, indexBuff, context.getSdkFileId(), "", "", 60, mediaData);

            if (ret != 0) {
                Finance.FreeMediaData(mediaData);
                return false; // 调用方处理重试
            }

            // 写入文件
            writeMediaData(context.getLocalPath(), mediaData);

            if (Finance.IsMediaDataFinish(mediaData) == 1) {
                return processDownloadedFile(context, mediaData);
            } else {
                indexBuff = Finance.GetOutIndexBuf(mediaData);
                Finance.FreeMediaData(mediaData);
                
                // 检查大文件超时
                if (!context.getBigFile() && isDownloadTimeout(context)) {
                    FileUtil.safeDelete(context.getLocalPath());
                    return false;
                }
            }
        }
    }

    /**
     * 准备下载
     */
    private void prepareDownload(DownloadContext context) {
        String dateStr = DateUtil.nowYyyyMmDdHh();
        context.setDateStr(dateStr);
        // 安全处理文件名，防止文件名过长或包含特殊字符
        String safeMediaPath = FileUtil.sanitizeFilename(context.getMediaPath(), context.getMd5sum());
        context.setMediaPath(safeMediaPath);  // 更新为安全的路径
        context.setLocalPath(prefix + dateStr + "_" + safeMediaPath + context.getExt());
        context.setOssTargetPath("mochat2/" + dateStr.replace("_", "/") + "/" + safeMediaPath + context.getExt());
        context.setStartDownTime(System.currentTimeMillis());
        
        FileUtil.safeDelete(context.getLocalPath());
    }

    /**
     * 写入媒体数据到文件
     */
    private void writeMediaData(String localPath, long mediaData) throws Exception {
        try (FileOutputStream outputStream = new FileOutputStream(localPath, true)) {
            outputStream.write(Finance.GetData(mediaData));
            outputStream.flush();
        }
    }

    /**
     * 处理下载完成的文件
     */
    private boolean processDownloadedFile(DownloadContext context, long mediaData) {
        try {
            eventBus.getTotalRealDount().incrementAndGet();
            Finance.FreeMediaData(mediaData);

            // MD5 校验
            if (!validateMd5(context)) {
                return false;
            }

            // AMR 转 MP3
            if (AppConstants.FileExt.AMR.equals(context.getExt())) {
                convertAmrToMp3(context);
            }

            // 上传到 OSS
            return uploadToOss(context);
        } catch (Exception e) {
            log.error("处理下载文件失败, msgId:{}", context.getMsgId(), e);
            return false;
        } finally {
            FileUtil.safeDelete(context.getLocalPath());
        }
    }

    /**
     * MD5 校验
     */
    private boolean validateMd5(DownloadContext context) {
        String md5sum = context.getMd5sum();
        if (StringUtils.isEmpty(md5sum) || AppConstants.FileExt.GIF.equalsIgnoreCase(context.getExt())) {
            return true;
        }

        String calculatedMd5 = calculateFileMd5(new File(context.getLocalPath()));
        if (!md5sum.equalsIgnoreCase(calculatedMd5)) {
            log.error("MD5校验失败, msgId:{}, 期望:{}, 实际:{}", 
                context.getMsgId(), md5sum, calculatedMd5);
            return false;
        }
        
        log.debug("MD5校验成功, msgId:{}", context.getMsgId());
        return true;
    }

    /**
     * AMR 转 MP3
     */
    private void convertAmrToMp3(DownloadContext context) {
        String localPathMp3 = prefix + context.getDateStr() + "_" + context.getMediaPath() + AppConstants.FileExt.MP3;
        Audio.toMp3(context.getLocalPath(), localPathMp3);
        FileUtil.safeDelete(context.getLocalPath());
        
        context.setLocalPath(localPathMp3);
        context.setOssTargetPath("mochat2/" + context.getDateStr().replace("_", "/") + "/" 
            + context.getMediaPath() + AppConstants.FileExt.MP3);
    }

    /**
     * 上传到 OSS
     */
    private boolean uploadToOss(DownloadContext context) {
        Boolean result = ossUtil.upload(context.getLocalPath(), context.getOssTargetPath());
        if (result) {
            context.getObject().put("ossPath", context.getOssTargetPath());
            log.info("OSS上传成功, msgId:{}, path:{}", context.getMsgId(), context.getOssTargetPath());
            return true;
        } else {
            log.error("OSS上传失败, msgId:{}", context.getMsgId());
            return false;
        }
    }

    /**
     * 检查下载是否超时
     */
    private boolean isDownloadTimeout(DownloadContext context) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - context.getStartDownTime() > AppConstants.Retry.BIG_FILE_TIMEOUT_MS) {
            log.error("下载超时, msgId:{}, 耗时:{}ms", 
                context.getMsgId(), currentTime - context.getStartDownTime());
            return true;
        }
        return false;
    }

    /**
     * 计算文件 MD5
     */
    private String calculateFileMd5(File file) {
        // 使用 MsgHandler 中的静态方法
        return com.ruoran.houyi.MsgHandler.getFileMd5(file);
    }

    /**
     * 保存 MD5 索引
     */
    public void saveMd5Index(String md5sum, String filePath, String ossPath) {
        if (StringUtils.isEmpty(md5sum) || StringUtils.isEmpty(ossPath)) {
            return;
        }

        Md5Index md5Index = new Md5Index();
        md5Index.setMd5(md5sum);
        md5Index.setFilePath(filePath);
        md5Index.setOssPath(ossPath);
        md5Index.setOssAt(System.currentTimeMillis());
        
        // 注意：这里需要调用方传入 md5IndexRepo 来保存
        log.debug("MD5索引已准备, md5:{}", md5sum);
    }
}

