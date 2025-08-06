package com.example.customjoinmessage.platform;


import com.example.customjoinmessage.core.CustomJoinMessagePlugin;
import org.slf4j.Logger;

/**
 * 抽象平台适配器
 * 
 * 为不同平台提供统一的接口
 */
public abstract class AbstractPlatformAdapter {
    
    protected final CustomJoinMessagePlugin plugin;
    protected final Object platformInstance;
    protected final Logger logger;
    
    public AbstractPlatformAdapter(CustomJoinMessagePlugin plugin, Object platformInstance) {
        this.plugin = plugin;
        this.platformInstance = platformInstance;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 适配器启用
     */
    public abstract void onEnable();
    
    /**
     * 适配器禁用
     */
    public abstract void onDisable();
    
    /**
     * 重新加载适配器
     */
    public abstract void reload();
    
    /**
     * 注册通信通道
     */
    public abstract void registerCommunicationChannel();
    

    
    /**
     * 发送消息给玩家
     */
    public abstract void sendMessageToPlayer(Object player, String message);
    
    /**
     * 发送消息给所有玩家
     */
    public abstract void sendMessageToAll(String message);
    
    /**
     * 发送消息给服务器
     */
    public abstract void sendMessageToServer(String serverName, String message);
    
    /**
     * 获取在线玩家数量
     */
    public abstract int getOnlinePlayerCount();
    
    /**
     * 检查玩家是否在线
     */
    public abstract boolean isPlayerOnline(String playerName);
    
    /**
     * 获取适配器类型
     */
    public abstract String getAdapterType();
    
    /**
     * 获取平台版本
     */
    public abstract String getPlatformVersion();
    
    // ================================
    // 辅助方法
    // ================================
    
    /**
     * 记录调试信息
     */
    protected void debug(String message, Object... args) {
        if (plugin.getConfigManager().getPluginConfig().isDebug()) {
            logger.info("[DEBUG] " + message, args);
        }
    }
    
    /**
     * 记录详细信息
     */
    protected void verbose(String message, Object... args) {
        if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("[VERBOSE] " + message, args);
        }
    }
    
    /**
     * 安全地转换平台实例
     */
    @SuppressWarnings("unchecked")
    protected <T> T safeCast(Class<T> targetClass) {
        if (targetClass.isInstance(platformInstance)) {
            return (T) platformInstance;
        }
        throw new ClassCastException(
            "无法将 " + platformInstance.getClass().getSimpleName() + 
            " 转换为 " + targetClass.getSimpleName()
        );
    }
    
    /**
     * 检查功能是否启用
     */
    protected boolean isFeatureEnabled(String feature) {
        return plugin.isFeatureEnabled(feature);
    }
}