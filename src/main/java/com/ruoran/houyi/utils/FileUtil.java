package com.ruoran.houyi.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.regex.Pattern;

/**
 * 文件操作工具类
 *
 * @author refactored
 */
@Slf4j
public class FileUtil {

    /**
     * Linux 文件名最大长度（字节）
     * ext4 文件系统限制为 255 字节
     */
    private static final int MAX_FILENAME_BYTES = 200;  // 留一些余量给日期前缀等
    
    /**
     * 文件名中不允许的字符（Linux/Unix 特殊字符）
     */
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[/\\\\:*?\"<>|\\x00-\\x1F]");
    
    /**
     * 安全处理文件名，确保符合 Linux 文件系统规范
     * 1. 去除不安全的特殊字符
     * 2. 限制文件名长度（UTF-8 字节数）
     * 3. 处理中文文件名
     *
     * @param filename 原始文件名
     * @param md5sum   文件的 MD5 值（用于超长文件名时作为备选）
     * @return 安全的文件名
     */
    public static String sanitizeFilename(String filename, String md5sum) {
        if (filename == null || filename.isEmpty()) {
            return md5sum != null ? md5sum : "unnamed";
        }
        
        // 1. 替换不安全字符为下划线
        String safe = UNSAFE_CHARS.matcher(filename).replaceAll("_");
        
        // 2. 替换连续空格和下划线为单个
        safe = safe.replaceAll("[\\s_]+", "_");
        
        // 3. 去除首尾空格和下划线
        safe = safe.trim();
        if (safe.startsWith("_")) {
            safe = safe.substring(1);
        }
        if (safe.endsWith("_")) {
            safe = safe.substring(0, safe.length() - 1);
        }
        
        // 4. 检查 UTF-8 字节长度并截断
        byte[] bytes = safe.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length > MAX_FILENAME_BYTES) {
            // 文件名太长，使用 md5sum + 截断的原始文件名
            if (md5sum != null && !md5sum.isEmpty()) {
                // 提取文件扩展名（如果有）
                String ext = "";
                int dotIndex = safe.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < safe.length() - 1) {
                    ext = safe.substring(dotIndex);  // 包含点号
                }
                
                // 计算可用于原始文件名的字节数
                // md5sum(32) + "_" + 原始名 + 扩展名
                int extBytes = ext.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                int prefixBytes = md5sum.length() + 1;  // md5 + "_"
                int availableBytes = MAX_FILENAME_BYTES - prefixBytes - extBytes;
                
                if (availableBytes > 20) {
                    // 截断原始文件名
                    String nameWithoutExt = safe.substring(0, dotIndex > 0 ? dotIndex : safe.length());
                    String truncatedName = truncateUtf8(nameWithoutExt, availableBytes);
                    safe = md5sum + "_" + truncatedName + ext;
                } else {
                    // 空间不足，直接使用 md5sum
                    safe = md5sum + ext;
                }
            } else {
                // 没有 md5sum，直接截断
                safe = truncateUtf8(safe, MAX_FILENAME_BYTES);
            }
            log.warn("文件名过长已截断: 原始长度={}字节, 截断后={}", bytes.length, safe);
        }
        
        // 5. 如果处理后为空，使用 md5sum 或默认值
        if (safe.isEmpty()) {
            safe = md5sum != null && !md5sum.isEmpty() ? md5sum : "unnamed";
        }
        
        return safe;
    }
    
    /**
     * 按 UTF-8 字节数截断字符串，确保不会截断到多字节字符的中间
     *
     * @param str      原始字符串
     * @param maxBytes 最大字节数
     * @return 截断后的字符串
     */
    private static String truncateUtf8(String str, int maxBytes) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return str;
        }
        
        // 从后向前找到安全的截断点
        int len = maxBytes;
        while (len > 0 && (bytes[len] & 0xC0) == 0x80) {
            // 这是 UTF-8 多字节字符的后续字节，继续向前
            len--;
        }
        
        return new String(bytes, 0, len, java.nio.charset.StandardCharsets.UTF_8);
    }

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

