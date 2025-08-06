package com.example.customjoinmessage.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息格式化工具类
 * 
 * 提供高级的消息格式化和占位符替换功能
 */
public class MessageFormatter {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 格式化消息
     * 
     * @param template 消息模板
     * @param placeholders 占位符映射
     * @return 格式化后的消息
     */
    public static String format(String template, Map<String, String> placeholders) {
        if (template == null) {
            return "";
        }
        
        String result = template;
        
        // 添加系统占位符
        Map<String, String> allPlaceholders = new HashMap<>(placeholders);
        addSystemPlaceholders(allPlaceholders);
        
        // 替换占位符
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = allPlaceholders.getOrDefault(placeholder, matcher.group(0));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        
        return buffer.toString();
    }
    
    /**
     * 格式化玩家加入消息
     * 
     * @param template 消息模板
     * @param playerName 玩家名称
     * @return 格式化后的消息
     */
    public static String formatJoinMessage(String template, String playerName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        return format(template, placeholders);
    }
    
    /**
     * 格式化玩家离开消息
     * 
     * @param template 消息模板
     * @param playerName 玩家名称
     * @return 格式化后的消息
     */
    public static String formatLeaveMessage(String template, String playerName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        return format(template, placeholders);
    }
    
    /**
     * 格式化服务器切换消息
     * 
     * @param template 消息模板
     * @param playerName 玩家名称
     * @param fromServer 来源服务器
     * @param toServer 目标服务器
     * @return 格式化后的消息
     */
    public static String formatSwitchMessage(String template, String playerName, 
                                           String fromServer, String toServer) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        placeholders.put("from", fromServer);
        placeholders.put("to", toServer);
        placeholders.put("prev", fromServer); // 兼容性
        placeholders.put("cur", toServer); // 兼容性
        return format(template, placeholders);
    }
    
    /**
     * 添加系统占位符
     */
    private static void addSystemPlaceholders(Map<String, String> placeholders) {
        LocalDateTime now = LocalDateTime.now();
        
        // 时间占位符
        placeholders.put("time", TIME_FORMATTER.format(now));
        placeholders.put("date", DATE_FORMATTER.format(now));
        placeholders.put("datetime", DATETIME_FORMATTER.format(now));
        
        // 系统信息占位符
        placeholders.put("java_version", System.getProperty("java.version"));
        placeholders.put("os_name", System.getProperty("os.name"));
        placeholders.put("os_version", System.getProperty("os.version"));
        
        // 运行时信息
        Runtime runtime = Runtime.getRuntime();
        placeholders.put("max_memory", formatBytes(runtime.maxMemory()));
        placeholders.put("total_memory", formatBytes(runtime.totalMemory()));
        placeholders.put("free_memory", formatBytes(runtime.freeMemory()));
        placeholders.put("used_memory", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
    }
    
    /**
     * 格式化字节数
     */
    private static String formatBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
    
    /**
     * 转义特殊字符
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }
    
    /**
     * 反转义特殊字符
     */
    public static String unescape(String text) {
        if (text == null) {
            return "";
        }
        
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'");
    }
    
    /**
     * 截断文本
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 居中文本
     */
    public static String center(String text, int width) {
        if (text == null || text.length() >= width) {
            return text;
        }
        
        int padding = (width - text.length()) / 2;
        StringBuilder result = new StringBuilder();
        
        // 左填充
        for (int i = 0; i < padding; i++) {
            result.append(" ");
        }
        
        result.append(text);
        
        // 右填充
        while (result.length() < width) {
            result.append(" ");
        }
        
        return result.toString();
    }
    
    /**
     * 转换颜色代码
     * 支持MiniMessage格式(<green>)和传统&格式(&a)
     * 
     * @param message 原始消息
     * @return 转换后的消息
     */
    public static String translateColorCodes(String message) {
        if (message == null) {
            return null;
        }
        
        String result = message;
        
        // 先转换MiniMessage格式到&格式
        result = convertMiniMessageToLegacy(result);
        
        // 然后转换&格式到Bukkit颜色代码
        result = org.bukkit.ChatColor.translateAlternateColorCodes('&', result);
        
        return result;
    }
    
    /**
     * 只转换&格式颜色代码（用于已经是&格式的消息）
     * 
     * @param message 包含&颜色代码的消息
     * @return 转换后的消息
     */
    public static String translateLegacyColorCodes(String message) {
        if (message == null) {
            return null;
        }
        
        // 直接转换&格式到Bukkit颜色代码
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 将MiniMessage格式转换为传统&格式
     * 
     * @param message MiniMessage格式的消息
     * @return 转换后的&格式消息
     */
    private static String convertMiniMessageToLegacy(String message) {
        if (message == null) {
            return null;
        }
        
        // MiniMessage颜色映射到&代码
        String result = message
            .replace("<black>", "&0")
            .replace("<dark_blue>", "&1")
            .replace("<dark_green>", "&2")
            .replace("<dark_aqua>", "&3")
            .replace("<dark_red>", "&4")
            .replace("<dark_purple>", "&5")
            .replace("<gold>", "&6")
            .replace("<gray>", "&7")
            .replace("<grey>", "&7")
            .replace("<dark_gray>", "&8")
            .replace("<dark_grey>", "&8")
            .replace("<blue>", "&9")
            .replace("<green>", "&a")
            .replace("<aqua>", "&b")
            .replace("<red>", "&c")
            .replace("<light_purple>", "&d")
            .replace("<yellow>", "&e")
            .replace("<white>", "&f")
            
            // 格式化代码
            .replace("<bold>", "&l")
            .replace("<italic>", "&o")
            .replace("<underlined>", "&n")
            .replace("<underline>", "&n")
            .replace("<strikethrough>", "&m")
            .replace("<obfuscated>", "&k")
            .replace("<magic>", "&k")
            .replace("<reset>", "&r")
            
            // 结束标签（简单忽略，因为&格式不支持）
            .replaceAll("</[^>]*>", "");
        
        return result;
    }
}