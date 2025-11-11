package com.polyu.tapgen.utils;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    // 定义ISO 8601格式的日期时间格式器
    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("UTC+8"));
    public static final DateTimeFormatter DATE_TIME_FORMATTER_DAY = DateTimeFormatter.ofPattern("MM-dd").withZone(ZoneId.of("UTC+8"));
    public static final DateTimeFormatter DATE_TIME_FORMATTER_MONTH = DateTimeFormatter.ofPattern("MM").withZone(ZoneId.of("UTC+8"));
    private static final DateTimeFormatter SDF_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateTimeFormatter SDF_UTC_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final DateTimeFormatter TIME_FORMATTER_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    public static final DateTimeFormatter TIME_FORMATTER_YEAR = DateTimeFormatter.ofPattern("yyyy");
    public static final DateTimeFormatter TIME_FORMATTER_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    // InfluxDB 2.0 要求 RFC3339 时间格式(UTC时区)
    private static final DateTimeFormatter INFLUXDB_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"));

    /**
     * 将时间戳(毫秒)转换为 InfluxDB 可识别的 RFC3339 时间格式字符串
     * @param timestamp 时间戳(毫秒)
     * @return RFC3339 格式的字符串
     */
    public static String timestampToRFC3339(long timestamp) {
        return INFLUXDB_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    /**
     * 将纳秒级时间戳+纳秒调整数转换为 RFC3339 时间格式字符串
     * @param epochSecond Unix时间戳(秒)
     * @param nanoAdjustment 纳秒调整数(0-999,999,999)
     * @return RFC3339 格式的字符串
     */
    public static String timestampToRFC3339(long epochSecond, long nanoAdjustment) {
        return INFLUXDB_FORMATTER.format(Instant.ofEpochSecond(epochSecond, nanoAdjustment));
    }

    /**
     * 生成当前时间的 RFC3339 格式字符串
     * @return 当前时间的 RFC3339 字符串表示
     */
    public static String nowToRFC3339() {
        return INFLUXDB_FORMATTER.format(Instant.now());
    }
    public static String formatTime(Instant instant, DateTimeFormatter fmt){
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        return zonedDateTime.format(fmt);
    }
    public static String getCurrentDay(){
        return formatTime(Instant.now(), TIME_FORMATTER_DAY);
    }

    /**
     * 获取本日开始时间
     * @return ISO 8601格式的日期时间字符串
     */
    public static String getTodayStart() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        return startOfDay.atZone(SYSTEM_ZONE).toInstant().toString();
    }

}