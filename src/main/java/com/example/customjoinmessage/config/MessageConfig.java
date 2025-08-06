package com.example.customjoinmessage.config;

import com.example.customjoinmessage.utils.PermissionUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 消息配置类
 * 
 * 处理 messages.yml 中的消息配置
 */
public class MessageConfig {
    
    // 默认消息
    private static final String DEFAULT_JOIN_MESSAGE = "<green><yellow>{player}</yellow> 加入了服务器！</green>";
    private static final String DEFAULT_LEAVE_MESSAGE = "<red><yellow>{player}</yellow> 离开了服务器！</red>";
    private static final String DEFAULT_WELCOME_MESSAGE = "<yellow>欢迎来到服务器，{player}！</yellow>";
    private static final String DEFAULT_SWITCH_MESSAGE = "<gray>{player} 从 <yellow>{from}</yellow> 切换到 <yellow>{to}</yellow></gray>";
    
    private final Map<String, Object> messageData;
    private final Random random;
    
    public MessageConfig(Map<String, Object> messageData) {
        this.messageData = messageData != null ? messageData : new HashMap<>();
        this.random = new Random();
    }
    
    // ================================
    // 基于权限组的消息获取
    // ================================
    
    /**
     * 获取加入消息（基于权限组，支持智能回退）
     * @param permissionGroup 权限组 (动态从配置读取)
     * @param type 消息类型 (default, first-time, returning)
     */
    public String getJoinMessage(String permissionGroup, String type) {
        return getMessageWithFallback("join", permissionGroup, type, DEFAULT_JOIN_MESSAGE);
    }
    
    /**
     * 获取离开消息（基于权限组，支持智能回退）
     * @param permissionGroup 权限组 (动态从配置读取)
     * @param type 消息类型 (default)
     */
    public String getLeaveMessage(String permissionGroup, String type) {
        return getMessageWithFallback("leave", permissionGroup, type, DEFAULT_LEAVE_MESSAGE);
    }
    
    /**
     * 获取欢迎消息（基于权限组，支持智能回退）
     * @param permissionGroup 权限组 (动态从配置读取)
     * @param type 消息类型 (first-time, returning)
     */
    public String getWelcomeMessage(String permissionGroup, String type) {
        return getMessageWithFallback("welcome", permissionGroup, type, DEFAULT_WELCOME_MESSAGE);
    }
    
    /**
     * 获取服务器切换消息（基于权限组，支持智能回退）
     * @param permissionGroup 权限组 (动态从配置读取)
     * @param type 消息类型 (default)
     */
    public String getServerSwitchMessage(String permissionGroup, String type) {
        return getMessageWithFallback("server-switch", permissionGroup, type, DEFAULT_SWITCH_MESSAGE);
    }
    
    /**
     * 智能消息获取，支持回退机制
     * 
     * 回退顺序：
     * 1. 尝试获取指定权限组的消息
     * 2. 如果失败，回退到默认组的消息
     * 3. 如果还失败，返回硬编码的默认消息
     */
    private String getMessageWithFallback(String messageCategory, String permissionGroup, String type, String hardcodedDefault) {
        // 首先尝试获取指定权限组的消息
        String targetPath = "messages." + permissionGroup + "." + messageCategory + "." + type;
        String message = getRandomMessage(targetPath, null);
        
        // 如果找到消息且不为空，直接返回
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        
        // 如果不是默认组，尝试回退到默认组
        if (!PermissionUtil.DEFAULT_GROUP.equals(permissionGroup)) {
            String fallbackPath = "messages." + PermissionUtil.DEFAULT_GROUP + "." + messageCategory + "." + type;
            message = getRandomMessage(fallbackPath, null);
            
            if (message != null && !message.trim().isEmpty()) {
                return message;
            }
        }
        
        // 最后回退到硬编码默认值
        return hardcodedDefault;
    }
    
    // ================================
    // 兼容性方法（向后兼容旧的调用方式）
    // ================================
    
    /**
     * 获取加入消息（兼容性方法，使用默认权限组）
     */
    public String getJoinMessage(String type) {
        return getJoinMessage("default", type);
    }
    
    /**
     * 获取离开消息（兼容性方法，使用默认权限组）
     */
    public String getLeaveMessage(String type) {
        return getLeaveMessage("default", type);
    }
    
    /**
     * 获取欢迎消息（兼容性方法，使用默认权限组）
     */
    public String getWelcomeMessage(String type) {
        return getWelcomeMessage("default", type);
    }
    
    /**
     * 获取VIP离开消息（兼容性方法）
     */
    public String getVipLeaveMessage() {
        return getLeaveMessage("vip", "default");
    }
    
    /**
     * 获取管理员离开消息（兼容性方法）
     */
    public String getAdminLeaveMessage() {
        return getLeaveMessage("admin", "default");
    }
    
    /**
     * 获取首次欢迎消息
     */
    public String getFirstTimeWelcomeMessage() {
        return getWelcomeMessage("first-time");
    }
    
    /**
     * 获取回归玩家欢迎消息
     */
    public String getReturningWelcomeMessage() {
        return getWelcomeMessage("returning");
    }
    
    /**
     * 获取服务器切换消息
     */
    public String getServerSwitchMessage(String type) {
        return getRandomMessage("messages.server-switch." + type, DEFAULT_SWITCH_MESSAGE);
    }
    
    /**
     * 获取默认切换消息
     */
    public String getServerSwitchMessage() {
        return getServerSwitchMessage("default");
    }
    
    // ================================
    // 特殊情况消息
    // ================================
    
    /**
     * 获取网络问题消息
     */
    public String getNetworkMessage(String type) {
        return getString("special.network." + type, "{player} 因网络问题离开了服务器");
    }
    
    /**
     * 获取踢出消息
     */
    public String getKickMessage(String type) {
        return getString("special.kick." + type, "{player} 被踢出了服务器");
    }
    
    // ================================
    // 权限组消息
    // ================================
    
    /**
     * 是否启用权限组消息
     */
    public boolean isPermissionGroupsEnabled() {
        return getBoolean("permission-groups.enabled", false);
    }
    
    /**
     * 获取权限组消息
     */
    public String getPermissionGroupMessage(String group, String messageType) {
        return getString("permission-groups.groups." + group + "." + messageType, null);
    }
    
    // ================================
    // 消息格式化
    // ================================
    
    /**
     * 格式化消息（替换占位符）
     */
    public String formatMessage(String template, String playerName) {
        return formatMessage(template, playerName, null, null);
    }
    
    /**
     * 格式化服务器切换消息
     */
    public String formatSwitchMessage(String template, String playerName, String fromServer, String toServer) {
        return formatMessage(template, playerName, fromServer, toServer);
    }
    
    /**
     * 通用消息格式化
     */
    public String formatMessage(String template, String playerName, String fromServer, String toServer) {
        if (template == null) {
            return "";
        }
        
        String formatted = template;
        
        // 基础占位符
        if (playerName != null) {
            formatted = formatted.replace("{player}", playerName);
        }
        if (fromServer != null) {
            formatted = formatted.replace("{from}", fromServer);
            formatted = formatted.replace("{prev}", fromServer); // 兼容性
        }
        if (toServer != null) {
            formatted = formatted.replace("{to}", toServer);
            formatted = formatted.replace("{cur}", toServer); // 兼容性
        }
        
        // 时间占位符
        formatted = formatted.replace("{time}", getCurrentTime());
        formatted = formatted.replace("{date}", getCurrentDate());
        
        // 🔥 新增：服务器状态占位符（跨平台兼容）
        formatted = replaceServerPlaceholders(formatted);
        
        // TODO: 添加更多占位符支持  
        // - {uuid}
        // - {world}
        // - {ip}
        // 等等
        
        return formatted;
    }
    
    /**
     * 🔥 跨平台替换服务器占位符
     */
    private String replaceServerPlaceholders(String formatted) {
        try {
            // 尝试检测当前环境并获取服务器信息
            if (isBukkitEnvironment()) {
                // Bukkit环境：使用反射避免直接依赖
                Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
                Object server = bukkitClass.getMethod("getServer").invoke(null);
                
                // 获取在线玩家数
                Object onlinePlayers = server.getClass().getMethod("getOnlinePlayers").invoke(server);
                int onlineCount = ((java.util.Collection<?>) onlinePlayers).size();
                
                // 获取最大玩家数
                int maxPlayers = (Integer) server.getClass().getMethod("getMaxPlayers").invoke(server);
                
                // 获取服务器名称
                String serverName = (String) server.getClass().getMethod("getName").invoke(server);
                
                formatted = formatted.replace("{online_count}", String.valueOf(onlineCount));
                formatted = formatted.replace("{max_players}", String.valueOf(maxPlayers));
                formatted = formatted.replace("{server}", serverName);
                
            } else if (isVelocityEnvironment()) {
                // Velocity环境：暂时使用占位符，后续可以通过适配器传递数据
                formatted = formatted.replace("{online_count}", "?");
                formatted = formatted.replace("{max_players}", "?");
                formatted = formatted.replace("{server}", "Velocity");
                
            } else {
                // 未知环境：使用默认值
                formatted = formatted.replace("{online_count}", "?");
                formatted = formatted.replace("{max_players}", "?");
                formatted = formatted.replace("{server}", "Unknown");
            }
        } catch (Exception e) {
            // 任何错误都使用默认值
            formatted = formatted.replace("{online_count}", "?");
            formatted = formatted.replace("{max_players}", "?");
            formatted = formatted.replace("{server}", "Error");
        }
        
        return formatted;
    }
    
    /**
     * 检查是否在Bukkit环境中
     */
    private boolean isBukkitEnvironment() {
        try {
            Class.forName("org.bukkit.Bukkit");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 检查是否在Velocity环境中
     */
    private boolean isVelocityEnvironment() {
        try {
            Class.forName("com.velocitypowered.api.proxy.ProxyServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 获取时间格式
     */
    public String getTimeFormat() {
        return getString("formatting.time-format", "HH:mm:ss");
    }
    
    /**
     * 获取日期格式
     */
    public String getDateFormat() {
        return getString("formatting.date-format", "yyyy-MM-dd");
    }
    
    /**
     * 是否显示前缀后缀
     */
    public boolean isShowPrefixSuffix() {
        return getBoolean("formatting.player-name.show-prefix-suffix", true);
    }
    
    /**
     * 获取玩家名称最大长度
     */
    public int getPlayerNameMaxLength() {
        return getInt("formatting.player-name.max-length", 16);
    }
    
    /**
     * 是否使用友好服务器名称
     */
    public boolean isUseFriendlyNames() {
        return getBoolean("formatting.server-name.use-friendly-names", true);
    }
    
    /**
     * 获取服务器名称映射
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getServerNameMapping() {
        Object value = getValue("formatting.server-name.name-mapping");
        if (value instanceof Map) {
            return (Map<String, String>) value;
        }
        return new HashMap<>();
    }
    
    // ================================
    // 多语言支持
    // ================================
    
    /**
     * 获取默认语言
     */
    public String getDefaultLanguage() {
        return getString("localization.default-language", "zh_CN");
    }
    
    /**
     * 获取支持的语言列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getSupportedLanguages() {
        Object value = getValue("localization.supported-languages");
        if (value instanceof List) {
            return (List<String>) value;
        }
        return List.of("zh_CN", "en_US");
    }
    
    /**
     * 获取语言检测方式
     */
    public String getDetectionMethod() {
        return getString("localization.detection-method", "client");
    }
    
    // ================================
    // 辅助方法
    // ================================
    
    /**
     * 获取随机消息（支持消息列表）
     */
    private String getRandomMessage(String path, String defaultValue) {
        Object value = getValue(path);
        
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> messages = (List<String>) value;
            if (!messages.isEmpty()) {
                return messages.get(random.nextInt(messages.size()));
            }
        } else if (value instanceof String) {
            return (String) value;
        }
        
        return defaultValue;
    }
    
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
     * 根据路径获取值
     */
    private Object getValue(String path) {
        String[] keys = path.split("\\.");
        Object current = messageData;
        
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        // 简单实现，后续可以使用 DateTimeFormatter
        return java.time.LocalTime.now().toString();
    }
    
    /**
     * 获取当前日期
     */
    private String getCurrentDate() {
        // 简单实现，后续可以使用 DateTimeFormatter
        return java.time.LocalDate.now().toString();
    }
}