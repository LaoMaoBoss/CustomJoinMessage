package com.example.customjoinmessage.config;

import com.example.customjoinmessage.utils.TimeUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件主配置类
 * 
 * 处理 config.yml 中的配置项
 */
public class PluginConfig {
    
    // 默认配置值
    private static final String DEFAULT_MODE = "backend";
    private static final boolean DEFAULT_DEBUG = false;
    
    // 硬编码的通信通道（用户无需配置）
    private static final String COMMUNICATION_CHANNEL = "customjoinmessage:sync";
    
    private final Map<String, Object> configData;
    
    public PluginConfig(Map<String, Object> configData) {
        this.configData = configData != null ? configData : new HashMap<>();
    }
    
    // ================================
    // 插件基础设置
    // ================================
    
    /**
     * 获取运行模式
     */
    public String getMode() {
        String mode = getString("plugin.mode", DEFAULT_MODE);
        // 只在真正需要调试时输出
        return mode;
    }
    
    /**
     * 是否启用调试模式
     */
    public boolean isDebug() {
        return getBoolean("plugin.debug", DEFAULT_DEBUG);
    }
    
    /**
     * 是否启用详细日志（现在统一使用调试模式配置）
     */
    public boolean isVerboseLogging() {
        return isDebug();
    }
    
    /**
     * 获取通信通道（硬编码，用户无需配置）
     */
    public String getChannel() {
        return COMMUNICATION_CHANNEL;
    }
    
    // ================================
    // 功能开关
    // ================================
    
    /**
     * 检查功能是否启用
     */
    public boolean isFeatureEnabled(String feature) {
        return getBoolean("features." + feature + ".enabled", false);
    }
    
    /**
     * 首次加入欢迎消息是否启用
     */
    public boolean isFirstTimeWelcomeEnabled() {
        return getBoolean("features.welcome-message.first-time-enabled", true);
    }
    
    /**
     * 获取首次加入欢迎消息延迟
     */
    public int getFirstTimeWelcomeDelay() {
        return getInt("features.welcome-message.first-time-delay", 500);
    }
    
    /**
     * 回归玩家欢迎消息是否启用
     */
    public boolean isReturningWelcomeEnabled() {
        return getBoolean("features.welcome-message.returning-enabled", true);
    }
    
    /**
     * 回归玩家阈值（秒）
     * 支持友好时间格式，如 "1d", "2h30m", "1d12h30m"
     */
    public long getReturningThreshold() {
        Object value = getValue("features.welcome-message.returning-threshold");
        
        if (value instanceof String) {
            // 新格式：字符串时间格式，如 "1d", "2h30m"
            String timeString = (String) value;
            long seconds = TimeUtil.parseTimeToSeconds(timeString);
            return seconds > 0 ? seconds : 86400; // 如果解析失败，使用默认24小时
        } else if (value instanceof Number) {
            // 旧格式：数字秒数（向后兼容）
            return ((Number) value).longValue();
        } else {
            // 默认24小时
            return 86400;
        }
    }
    
    /**
     * 服务器切换消息是否启用
     */
    public boolean isServerSwitchMessageEnabled() {
        return isFeatureEnabled("server-switch-message");
    }
    
    /**
     * 是否启用服务器切换消息
     */
    public boolean isServerSwitchEnabled() {
        return getBoolean("features.server-switch-message.enabled", true);
    }
    
    /**
     * 切换消息是否向所有玩家显示
     */
    public boolean isServerSwitchShowToAll() {
        return getBoolean("features.server-switch-message.show-to-all", false);
    }
    
    /**
     * 自定义加入格式是否启用
     */
    public boolean isCustomJoinFormatEnabled() {
        return isFeatureEnabled("custom-join-format");
    }
    
    /**
     * 自定义离开格式是否启用
     */
    public boolean isCustomLeaveFormatEnabled() {
        return isFeatureEnabled("custom-leave-format");
    }
    
    /**
     * 首次加入是否启用
     */
    public boolean isFirstJoinEnabled() {
        return isFeatureEnabled("first-join");
    }
    
    /**
     * 首次加入是否使用不同消息
     */
    public boolean isFirstJoinDifferentMessage() {
        return getBoolean("features.first-join.different-message", true);
    }
    
    // ================================
    // 消息拦截设置
    // ================================
    
    /**
     * 是否拦截加入消息
     */
    public boolean isInterceptJoinMessages() {
        return getBoolean("interception.intercept-join-messages", true);
    }
    
    /**
     * 是否拦截离开消息
     */
    public boolean isInterceptLeaveMessages() {
        return getBoolean("interception.intercept-leave-messages", true);
    }
    
    // ================================
    // 服务器别名配置（仅代理端使用）
    // ================================
    
    /**
     * 获取服务器别名
     * 
     * @param serverName 原始服务器名称
     * @return 服务器别名，如果没有配置别名则返回原始名称
     */
    public String getServerAlias(String serverName) {
        if (serverName == null) {
            return "未知服务器";
        }
        
        String alias = getString("server-aliases." + serverName, null);
        return alias != null ? alias : serverName;
    }
    
    /**
     * 检查是否配置了服务器别名
     * 
     * @param serverName 服务器名称
     * @return 如果配置了别名返回true，否则返回false
     */
    public boolean hasServerAlias(String serverName) {
        return getValue("server-aliases." + serverName) != null;
    }

    
    // ================================
    // 辅助方法
    // ================================
    
    /**
     * 获取字符串值
     */
    private String getString(String path, String defaultValue) {
        Object value = getValue(path);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 获取布尔值
     */
    private boolean getBoolean(String path, boolean defaultValue) {
        Object value = getValue(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * 获取整数值
     */
    private int getInt(String path, int defaultValue) {
        Object value = getValue(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * 根据路径获取值（公共方法，供其他工具类使用）
     */
    public Object getValue(String path) {
        String[] keys = path.split("\\.");
        Object current = configData;
        
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null;
            }
        }
        
        return current;
    }
}