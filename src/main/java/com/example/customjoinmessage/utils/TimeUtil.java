package com.example.customjoinmessage.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间工具类
 * 
 * 支持解析友好的时间格式，如: 1d30h5m, 2h, 30m, 7d等
 */
public class TimeUtil {
    
    // 时间单位模式：支持 d(天), h(小时), m(分钟), s(秒)
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([dhms])");
    
    // 时间单位对应的秒数
    private static final long SECONDS_PER_DAY = 24 * 60 * 60;     // 86400秒
    private static final long SECONDS_PER_HOUR = 60 * 60;        // 3600秒
    private static final long SECONDS_PER_MINUTE = 60;           // 60秒
    
    /**
     * 解析时间字符串为秒数
     * 
     * 支持格式：
     * - 1d (1天)
     * - 2h (2小时)
     * - 30m (30分钟)
     * - 45s (45秒)
     * - 1d2h30m (1天2小时30分钟)
     * - 7d12h (7天12小时)
     * 
     * @param timeString 时间字符串
     * @return 对应的秒数，解析失败返回0
     */
    public static long parseTimeToSeconds(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return 0;
        }
        
        // 移除空格并转换为小写
        String cleanTime = timeString.replaceAll("\\s+", "").toLowerCase();
        
        // 如果是纯数字，视为秒数（向后兼容）
        if (cleanTime.matches("\\d+")) {
            try {
                return Long.parseLong(cleanTime);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        long totalSeconds = 0;
        Matcher matcher = TIME_PATTERN.matcher(cleanTime);
        
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "d": // 天
                    totalSeconds += value * SECONDS_PER_DAY;
                    break;
                case "h": // 小时
                    totalSeconds += value * SECONDS_PER_HOUR;
                    break;
                case "m": // 分钟
                    totalSeconds += value * SECONDS_PER_MINUTE;
                    break;
                case "s": // 秒
                    totalSeconds += value;
                    break;
                default:
                    // 忽略未知单位
                    break;
            }
        }
        
        return totalSeconds;
    }
    
    /**
     * 解析时间字符串为毫秒数
     * 
     * @param timeString 时间字符串
     * @return 对应的毫秒数，解析失败返回0
     */
    public static long parseTimeToMillis(String timeString) {
        return parseTimeToSeconds(timeString) * 1000;
    }
    
    /**
     * 将秒数转换为友好的时间字符串
     * 
     * @param seconds 秒数
     * @return 友好的时间字符串，如 "1d2h30m" 或 "2h30m" 或 "45m"
     */
    public static String formatSecondsToTime(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        
        StringBuilder result = new StringBuilder();
        
        // 计算天数
        long days = seconds / SECONDS_PER_DAY;
        if (days > 0) {
            result.append(days).append("d");
            seconds %= SECONDS_PER_DAY;
        }
        
        // 计算小时数
        long hours = seconds / SECONDS_PER_HOUR;
        if (hours > 0) {
            result.append(hours).append("h");
            seconds %= SECONDS_PER_HOUR;
        }
        
        // 计算分钟数
        long minutes = seconds / SECONDS_PER_MINUTE;
        if (minutes > 0) {
            result.append(minutes).append("m");
            seconds %= SECONDS_PER_MINUTE;
        }
        
        // 计算剩余秒数（只在没有其他单位时显示）
        if (seconds > 0 && result.length() == 0) {
            result.append(seconds).append("s");
        }
        
        return result.length() > 0 ? result.toString() : "0s";
    }
    
    /**
     * 验证时间字符串格式是否正确
     * 
     * @param timeString 时间字符串
     * @return 是否为有效格式
     */
    public static boolean isValidTimeFormat(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return false;
        }
        
        String cleanTime = timeString.replaceAll("\\s+", "").toLowerCase();
        
        // 纯数字也是有效的（向后兼容）
        if (cleanTime.matches("\\d+")) {
            return true;
        }
        
        // 检查是否完全匹配时间格式
        return cleanTime.matches("^(\\d+[dhms])+$");
    }
    
    /**
     * 获取时间单位的示例说明
     * 
     * @return 时间格式说明
     */
    public static String getTimeFormatHelp() {
        return "时间格式支持: d(天), h(小时), m(分钟), s(秒)\n" +
               "示例: 1d (1天), 2h30m (2小时30分钟), 1d12h30m (1天12小时30分钟)";
    }
}