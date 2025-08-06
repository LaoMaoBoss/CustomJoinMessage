package com.example.customjoinmessage.utils;

import com.example.customjoinmessage.config.PluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能权限工具类
 * 
 * 支持动态权限组检测，从配置文件中读取权限组定义
 * 支持用户自定义权限组名称和权限节点
 */
public class PermissionUtil {
    
    private static final Logger logger = LoggerFactory.getLogger("CustomJoinMessage");
    
    // 默认权限组（兜底保证）
    public static final String DEFAULT_GROUP = "default";
    
    // 缓存配置信息，避免重复解析
    private static volatile Map<String, Integer> priorityMappings = new ConcurrentHashMap<>();
    private static volatile boolean configLoaded = false;
    
    // 权限节点前缀（固定格式）
    private static final String PERMISSION_PREFIX = "customjoinmessage.";
    
    /**
     * 初始化权限配置（从配置文件加载）
     */
    public static void initializePermissions(PluginConfig pluginConfig) {
        try {
            // 清空旧缓存
            priorityMappings.clear();
            
            // 从配置文件读取权限组信息
            loadPermissionGroups(pluginConfig);
            
            configLoaded = true;
            
            if (logger.isDebugEnabled()) {
                logger.debug("智能权限检测已初始化，共加载 {} 个权限组: {}", priorityMappings.size(), priorityMappings);
            }
            
        } catch (Exception e) {
            logger.error("初始化权限配置失败，将使用默认配置: {}", e.getMessage(), e);
            loadDefaultPermissions();
        }
    }
    
    /**
     * 从配置文件加载权限组信息
     */
    private static void loadPermissionGroups(PluginConfig pluginConfig) {
        // 加载优先级映射
        Map<String, Object> priorities = getConfigMap(pluginConfig, "permission-groups.priority");
        for (Map.Entry<String, Object> entry : priorities.entrySet()) {
            String groupName = entry.getKey();
            int priority = Integer.parseInt(entry.getValue().toString());
            priorityMappings.put(groupName, priority);
        }
        
        // 确保默认组存在
        if (!priorityMappings.containsKey(DEFAULT_GROUP)) {
            priorityMappings.put(DEFAULT_GROUP, 0);
        }
    }
    
    /**
     * 获取配置文件中的 Map 数据
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getConfigMap(PluginConfig pluginConfig, String path) {
        try {
            Object value = pluginConfig.getValue(path);
            if (value instanceof Map) {
                return (Map<String, Object>) value;
            }
        } catch (Exception e) {
            logger.warn("读取配置路径 {} 失败: {}", path, e.getMessage());
        }
        return new HashMap<>();
    }
    
    /**
     * 加载默认权限配置（兜底机制）
     */
    private static void loadDefaultPermissions() {
        priorityMappings.clear();
        
        // 默认配置
        priorityMappings.put("default", 0);
        priorityMappings.put("vip", 10);
        
        configLoaded = true;
    }
    
    /**
     * 获取玩家的权限组名称（Velocity环境）
     * 
     * @param player Velocity Player对象
     * @return 权限组名称，根据配置动态确定
     */
    public static String getPlayerPermissionGroup(com.velocitypowered.api.proxy.Player player) {
        if (!configLoaded) {
            logger.warn("权限配置未初始化，使用默认组");
            return DEFAULT_GROUP;
        }
        
        if (player == null) {
            return DEFAULT_GROUP;
        }
        
        return findBestPermissionGroup(permission -> player.hasPermission(permission));
    }
    
    /**
     * 获取玩家的权限组名称（Bukkit环境）
     * 
     * @param player Bukkit Player对象
     * @return 权限组名称，根据配置动态确定
     */
    public static String getPlayerPermissionGroup(org.bukkit.entity.Player player) {
        if (!configLoaded) {
            logger.warn("权限配置未初始化，使用默认组");
            return DEFAULT_GROUP;
        }
        
        if (player == null) {
            return DEFAULT_GROUP;
        }
        
        return findBestPermissionGroup(permission -> player.hasPermission(permission));
    }
    
    /**
     * 获取玩家的权限组名称（BungeeCord环境）
     * 
     * @param player BungeeCord ProxiedPlayer对象
     * @return 权限组名称，根据配置动态确定
     */
    public static String getPlayerPermissionGroup(net.md_5.bungee.api.connection.ProxiedPlayer player) {
        if (!configLoaded) {
            logger.warn("权限配置未初始化，使用默认组");
            return DEFAULT_GROUP;
        }
        
        if (player == null) {
            return DEFAULT_GROUP;
        }
        
        return findBestPermissionGroup(permission -> player.hasPermission(permission));
    }
    
    /**
     * 找到玩家最佳权限组（根据优先级）
     * 使用固定权限格式：customjoinmessage.权限组名
     */
    private static String findBestPermissionGroup(PermissionChecker checker) {
        String bestGroup = DEFAULT_GROUP;
        int highestPriority = priorityMappings.getOrDefault(DEFAULT_GROUP, 0);
        
        // 遍历所有权限组，找到优先级最高且玩家拥有权限的组
        for (Map.Entry<String, Integer> entry : priorityMappings.entrySet()) {
            String groupName = entry.getKey();
            int priority = entry.getValue();
            
            // 默认组无需权限检查，其他组需要检查权限
            boolean hasPermission;
            if (DEFAULT_GROUP.equals(groupName)) {
                hasPermission = true; // 默认组所有玩家都有
            } else {
                String permission = PERMISSION_PREFIX + groupName;
                hasPermission = checker.hasPermission(permission);
            }
            
            if (hasPermission && priority > highestPriority) {
                bestGroup = groupName;
                highestPriority = priority;
            }
        }
        
        return bestGroup;
    }
    
    /**
     * 获取所有可用的权限组
     */
    public static Set<String> getAvailableGroups() {
        return new HashSet<>(priorityMappings.keySet());
    }
    
    /**
     * 检查权限组是否存在
     */
    public static boolean isValidGroup(String groupName) {
        return priorityMappings.containsKey(groupName);
    }
    
    /**
     * 获取权限组的优先级
     */
    public static int getGroupPriority(String groupName) {
        return priorityMappings.getOrDefault(groupName, 0);
    }
    
    /**
     * 重新加载权限配置
     */
    public static void reloadPermissions(PluginConfig pluginConfig) {
        logger.info("重新加载权限配置...");
        initializePermissions(pluginConfig);
    }
    
    /**
     * 权限检查器接口（用于函数式编程）
     */
    @FunctionalInterface
    private interface PermissionChecker {
        boolean hasPermission(String permission);
    }
}