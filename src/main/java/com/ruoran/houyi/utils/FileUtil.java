package com.ruoran.houyi.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 文件操作工具类
 *
 * @author refactored
 */
@Slf4j
public class FileUtil {

    /**
     * 安全删除文件，忽略异常
     *
     * @param filePath 文件路径
     */
    public static void safeDelete(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        safeDelete(new File(filePath));
    }

    /**
     * 安全删除文件，忽略异常
     *
     * @param file 文件对象
     */
    public static void safeDelete(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        try {
            if (file.delete()) {
                log.debug("文件删除成功: {}", file.getAbsolutePath());
            } else {
                log.warn("文件删除失败: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("删除文件时发生异常: {}", file.getAbsolutePath(), e);
        }
    }
}

