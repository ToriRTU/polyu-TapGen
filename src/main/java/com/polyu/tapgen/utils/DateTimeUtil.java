package com.polyu.tapgen.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 */
public class DateTimeUtil {
    
    // 默认日期时间格式
    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    // ISO格式
    public static final String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    
    // 紧凑格式（用于文件名等）
    public static final String COMPACT_PATTERN = "yyyyMMddHHmmss";
    
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern(ISO_PATTERN);
    private static final DateTimeFormatter COMPACT_FORMATTER = DateTimeFormatter.ofPattern(COMPACT_PATTERN);
    
    /**
     * 获取当前时间的字符串表示（默认格式）
     */
    public static String now() {
        return format(LocalDateTime.now());
    }
    
    /**
     * 获取当前时间的字符串表示（指定格式）
     */
    public static String now(String pattern) {
        return format(LocalDateTime.now(), pattern);
    }
    
    /**
     * 格式化LocalDateTime为字符串（默认格式）
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }
    
    /**
     * 格式化LocalDateTime为字符串（指定格式）
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * 格式化LocalDateTime为ISO格式字符串
     */
    public static String formatIso(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(ISO_FORMATTER);
    }
    
    /**
     * 格式化LocalDateTime为紧凑格式字符串
     */
    public static String formatCompact(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(COMPACT_FORMATTER);
    }
    
    /**
     * 解析字符串为LocalDateTime（默认格式）
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DEFAULT_FORMATTER);
    }
    
    /**
     * 解析字符串为LocalDateTime（指定格式）
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * 解析ISO格式字符串为LocalDateTime
     */
    public static LocalDateTime parseIso(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, ISO_FORMATTER);
    }
}