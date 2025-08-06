package com.example.customjoinmessage.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 平台检测器
 * 自动检测当前运行的服务器核心类型
 */
public class PlatformDetector {
    
    private static final Logger logger = LoggerFactory.getLogger("CustomJoinMessage");
    private static PlatformType detectedPlatform = null;
    
    public enum PlatformType {
        // 代理端
        VELOCITY("Velocity", true),
        BUNGEECORD("BungeeCord", true),
        
        // 后端服务器
        PAPER("Paper", false),
        FOLIA("Folia", false),
        SPIGOT("Spigot", false),
        CRAFTBUKKIT("CraftBukkit", false),
        
        // 未知
        UNKNOWN("Unknown", false);
        
        private final String displayName;
        private final boolean isProxy;
        
        PlatformType(String displayName, boolean isProxy) {
            this.displayName = displayName;
            this.isProxy = isProxy;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isProxy() {
            return isProxy;
        }
        
        public boolean isBackend() {
            return !isProxy;
        }
    }
    
    /**
     * 检测是否启用了Velocity/BungeeCord转发
     * 注意：此方法已废弃，现在由用户手动配置运行模式
     * 
     * @deprecated 现在由用户在配置文件中手动设置运行模式
     */
    @Deprecated
    public static boolean isVelocityForwardingEnabled() {
        // 废弃的文件检测逻辑，现在始终返回 false
        // 运行模式由用户在配置文件中手动设置
            return false;
    }
    
    /**
     * 检测当前平台类型
     */
    public static PlatformType detectPlatform() {
        if (detectedPlatform != null) {
            return detectedPlatform;
        }
        
        // 检测 Velocity
        if (isClassPresent("com.velocitypowered.api.proxy.ProxyServer")) {
            detectedPlatform = PlatformType.VELOCITY;
            return detectedPlatform;
        }
        
        // 检测 BungeeCord
        if (isClassPresent("net.md_5.bungee.api.ProxyServer")) {
            detectedPlatform = PlatformType.BUNGEECORD;
            return detectedPlatform;
        }
        
        // 检测 Folia (Paper的分支，需要先检测)
        if (isClassPresent("io.papermc.paper.threadedregions.RegionizedServer")) {
            detectedPlatform = PlatformType.FOLIA;
            return detectedPlatform;
        }
        
        // 检测 Paper
        if (isClassPresent("io.papermc.paper.event.player.PlayerItemCooldownEvent") ||
            isClassPresent("com.destroystokyo.paper.PaperConfig")) {
            detectedPlatform = PlatformType.PAPER;
            return detectedPlatform;
        }
        
        // 检测 Spigot
        if (isClassPresent("org.spigotmc.SpigotConfig") ||
            isClassPresent("net.md_5.bungee.api.chat.BaseComponent")) {
            detectedPlatform = PlatformType.SPIGOT;
            return detectedPlatform;
        }
        
        // 检测 CraftBukkit
        if (isClassPresent("org.bukkit.Bukkit")) {
            detectedPlatform = PlatformType.CRAFTBUKKIT;
            return detectedPlatform;
        }
        
        // 未知平台
        detectedPlatform = PlatformType.UNKNOWN;
        logger.warn("无法检测到已知的服务器平台类型");
        return detectedPlatform;
    }
    
    /**
     * 检查指定的类是否存在
     */
    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 是否为代理服务器
     */
    public static boolean isProxyServer() {
        return detectPlatform().isProxy();
    }
    
    /**
     * 是否为后端服务器
     */
    public static boolean isBackendServer() {
        return detectPlatform().isBackend();
    }
    
    /**
     * 获取服务器版本信息
     */
    public static String getServerVersion() {
        try {
            PlatformType platform = detectPlatform();
            
            switch (platform) {
                case VELOCITY:
                    return getVelocityVersion();
                case BUNGEECORD:
                    return getBungeeCordVersion();
                case PAPER:
                case FOLIA:
                case SPIGOT:
                case CRAFTBUKKIT:
                    return getBukkitVersion();
                default:
                    return "Unknown";
            }
        } catch (Exception e) {
            logger.warn("获取服务器版本时出错: {}", e.getMessage());
            return "Unknown";
        }
    }
    
    /**
     * 获取 Velocity 版本
     */
    private static String getVelocityVersion() {
        try {
            // 尝试通过反射获取Velocity版本
            Class.forName("com.velocitypowered.proxy.VelocityServer");
            return "Velocity (版本检测需要运行时)";
        } catch (Exception e) {
            return "Velocity (未知版本)";
        }
    }
    
    /**
     * 获取 BungeeCord 版本
     */
    private static String getBungeeCordVersion() {
        try {
            Class.forName("net.md_5.bungee.api.ProxyServer");
            return "BungeeCord (版本检测需要运行时)";
        } catch (Exception e) {
            return "BungeeCord (未知版本)";
        }
    }
    
    /**
     * 获取 Bukkit 系列版本
     */
    private static String getBukkitVersion() {
        try {
            Class.forName("org.bukkit.Bukkit");
            // 在运行时可以调用 Bukkit.getVersion()
            return "Bukkit系列 (版本检测需要运行时)";
        } catch (Exception e) {
            return "Bukkit系列 (未知版本)";
        }
    }
    
    /**
     * 获取平台能力信息
     */
    public static PlatformCapabilities getCapabilities() {
        PlatformType platform = detectPlatform();
        
        return new PlatformCapabilities(
            platform.isProxy(),
            platform.isBackend(),
            platform != PlatformType.UNKNOWN,
            supportsPluginMessaging(platform),
            supportsModernEvents(platform)
        );
    }
    
    /**
     * 检查是否支持插件消息
     */
    private static boolean supportsPluginMessaging(PlatformType platform) {
        return platform != PlatformType.UNKNOWN;
    }
    
    /**
     * 检查是否支持现代事件系统
     */
    private static boolean supportsModernEvents(PlatformType platform) {
        return platform == PlatformType.VELOCITY || 
               platform == PlatformType.PAPER || 
               platform == PlatformType.FOLIA;
    }
    
    /**
     * 平台能力信息
     */
    public static class PlatformCapabilities {
        private final boolean isProxy;
        private final boolean isBackend;
        private final boolean isSupported;
        private final boolean supportsPluginMessaging;
        private final boolean supportsModernEvents;
        
        public PlatformCapabilities(boolean isProxy, boolean isBackend, boolean isSupported, 
                                  boolean supportsPluginMessaging, boolean supportsModernEvents) {
            this.isProxy = isProxy;
            this.isBackend = isBackend;
            this.isSupported = isSupported;
            this.supportsPluginMessaging = supportsPluginMessaging;
            this.supportsModernEvents = supportsModernEvents;
        }
        
        // Getters
        public boolean isProxy() { return isProxy; }
        public boolean isBackend() { return isBackend; }
        public boolean isSupported() { return isSupported; }
        public boolean supportsPluginMessaging() { return supportsPluginMessaging; }
        public boolean supportsModernEvents() { return supportsModernEvents; }
    }
}