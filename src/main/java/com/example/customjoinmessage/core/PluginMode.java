package com.example.customjoinmessage.core;

/**
 * 插件运行模式
 */
public enum PluginMode {
    
    /**
     * 后端独立模式
     * - 完整功能运行
     * - 拦截系统消息 + 发送自定义消息
     * - 适用于：单服务器或没有代理的环境
     */
    BACKEND_STANDALONE("后端独立模式", "完整功能，自主处理所有消息"),
    
    /**
     * 代理主控模式  
     * - 管理全局玩家状态
     * - 发送跨服务器消息（加入/离开/切换）
     * - 与后端插件协调工作
     * - 适用于：Velocity/BungeeCord代理服务器
     */
    PROXY_MASTER("代理主控模式", "管理全局状态，发送跨服务器消息"),
    
    /**
     * 后端从属模式
     * - 仅拦截系统默认消息
     * - 不发送自定义消息（由代理端处理）
     * - 与代理端插件协调工作
     * - 适用于：连接到代理的后端服务器
     */
    BACKEND_SLAVE("后端从属模式", "仅拦截系统消息，配合代理端工作"),
    
    /**
     * 禁用模式
     * - 插件不执行任何功能
     * - 用于调试或临时禁用
     */
    DISABLED("禁用模式", "插件功能已禁用");
    
    private final String displayName;
    private final String description;
    
    PluginMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 是否需要拦截系统消息
     */
    public boolean shouldInterceptSystemMessages() {
        return this == BACKEND_STANDALONE || this == BACKEND_SLAVE;
    }
    
    /**
     * 是否需要发送自定义消息
     */
    public boolean shouldSendCustomMessages() {
        return this == BACKEND_STANDALONE || this == PROXY_MASTER;
    }
    
    /**
     * 是否需要跨服务器通信
     */
    public boolean needsCrossServerCommunication() {
        return this == PROXY_MASTER || this == BACKEND_SLAVE;
    }
    
    /**
     * 是否为主动模式（主要功能提供者）
     */
    public boolean isActiveMode() {
        return this == BACKEND_STANDALONE || this == PROXY_MASTER;
    }
    
    /**
     * 是否为从属模式（辅助功能提供者）
     */
    public boolean isSlaveMode() {
        return this == BACKEND_SLAVE;
    }
    
    /**
     * 智能决定插件运行模式（基于Velocity转发检测）
     * 
     * @deprecated 此方法已废弃，现在使用基于消息广播的动态检测
     */
    @Deprecated
    public static PluginMode determineMode(PlatformDetector.PlatformType platformType, 
                                         String configMode,
                                         boolean velocityForwardingEnabled) {
        
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("CustomJoinMessage");
        logger.warn("调用了废弃的determineMode方法，请使用新的动态检测机制");
        
        // 简化逻辑，只基于平台类型和配置
        if (!"auto".equalsIgnoreCase(configMode)) {
            switch (configMode.toLowerCase()) {
                case "standalone": return BACKEND_STANDALONE;
                case "proxy": return PROXY_MASTER;
                case "backend": return BACKEND_SLAVE;
                case "disabled": return DISABLED;
            }
        }
        
        return platformType.isProxy() ? PROXY_MASTER : BACKEND_STANDALONE;
    }
    
    /**
     * 获取模式切换建议
     */
    public String getModeSwitchAdvice(PlatformDetector.PlatformType platform) {
        switch (this) {
            case BACKEND_STANDALONE:
                if (platform.isProxy()) {
                    return "建议切换到 PROXY_MASTER 模式";
                }
                return "当前模式适合单服务器环境";
                
            case PROXY_MASTER:
                if (!platform.isProxy()) {
                    return "建议切换到 BACKEND_STANDALONE 或 BACKEND_SLAVE 模式";
                }
                return "当前模式适合代理服务器环境";
                
            case BACKEND_SLAVE:
                if (platform.isProxy()) {
                    return "建议切换到 PROXY_MASTER 模式";
                }
                return "当前模式适合与代理服务器配合使用";
                
            case DISABLED:
                return "插件功能已禁用，可以启用其他模式";
                
            default:
                return "未知模式状态";
        }
    }
}