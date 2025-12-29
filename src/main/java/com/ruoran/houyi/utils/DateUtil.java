package com.ruoran.houyi.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 日期工具类
 * 使用线程安全的 DateTimeFormatter 替代 SimpleDateFormat
 *
 * @author refactored
 */
public class DateUtil {

    /**
     * 时区：Asia/Shanghai
     */
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    /**
     * 格式：yyyyMMdd
     */
    private static final DateTimeFormatter FORMATTER_YYYYMMDD = 
        DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZONE_ID);

    /**
     * 格式：yyyy_MM_dd_HH
     */
    private static final DateTimeFormatter FORMATTER_YYYY_MM_DD_HH = 
        DateTimeFormatter.ofPattern("yyyy_MM_dd_HH").withZone(ZONE_ID);

    /**
     * 格式化日期为 yyyyMMdd
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatYyyyMMdd(Date date) {
        if (date == null) {
            return null;
        }
        return FORMATTER_YYYYMMDD.format(date.toInstant());
    }

    /**
     * 格式化日期为 yyyy_MM_dd_HH
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatYyyyMmDdHh(Date date) {
        if (date == null) {
            return null;
        }
        return FORMATTER_YYYY_MM_DD_HH.format(date.toInstant());
    }

    /**
     * 格式化时间戳为 yyyyMMdd
     *
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的字符串
     */
    public static String formatYyyyMMdd(long timestamp) {
        return FORMATTER_YYYYMMDD.format(Instant.ofEpochMilli(timestamp));
    }

    /**
     * 格式化时间戳为 yyyy_MM_dd_HH
     *
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的字符串
     */
    public static String formatYyyyMmDdHh(long timestamp) {
        return FORMATTER_YYYY_MM_DD_HH.format(Instant.ofEpochMilli(timestamp));
    }

    /**
     * 获取当前时间的 yyyyMMdd 格式
     *
     * @return 格式化后的字符串
     */
    public static String nowYyyyMMdd() {
        return FORMATTER_YYYYMMDD.format(LocalDateTime.now(ZONE_ID));
    }

    /**
     * 获取当前时间的 yyyy_MM_dd_HH 格式
     *
     * @return 格式化后的字符串
     */
    public static String nowYyyyMmDdHh() {
        return FORMATTER_YYYY_MM_DD_HH.format(LocalDateTime.now(ZONE_ID));
    }
}

