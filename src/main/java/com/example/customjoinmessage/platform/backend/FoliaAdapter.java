package com.example.customjoinmessage.platform.backend;

import com.example.customjoinmessage.core.CustomJoinMessagePlugin;
import org.bukkit.entity.Player;

/**
 * Folia 平台适配器
 * 
 * 继承Paper适配器，并添加Folia特定的支持
 * Folia使用区域化调度器，需要特殊处理
 */
public class FoliaAdapter extends PaperAdapter {
    
    public FoliaAdapter(CustomJoinMessagePlugin plugin, Object platformInstance) {
        super(plugin, platformInstance);
    }
    
    @Override
    public void onEnable() {
        // 静默启用
        
        // Folia需要特殊的调度器处理
        // 静默 - Folia使用区域化调度器
        
        // 调用父类方法
        super.onEnable();
        
        // 静默启用完成
    }
    
    @Override
    public String getAdapterType() {
        return "Folia Backend";
    }
    
    /**
     * Folia专用的欢迎消息发送（重写父类方法）
     */
    @Override
    protected void sendWelcomeMessage(Player player) {
        sendWelcomeMessageDirectFolia(player, false);
    }
    
    /**
     * Folia专用的直接发送欢迎消息（避免重复检测）
     */
    private void sendWelcomeMessageDirectFolia(Player player, boolean isFirstTimeAlreadyChecked) {
        try {
            // 延迟发送欢迎消息（后端独立模式只支持首次加入）
            int delay = plugin.getConfigManager().getPluginConfig().getFirstTimeWelcomeDelay();
            
            // Folia使用玩家特定的调度器
            player.getScheduler().runDelayed(bukkitPlugin, (task) -> {
                // 确定欢迎消息类型（避免重复首次检测）
                String messageType;
                if (isFirstTimeAlreadyChecked) {
                    // 已经确认是首次加入，直接使用first-time
                    messageType = "first-time";
                    if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("Folia欢迎 - 使用已确认的首次加入类型: first-time");
            }
                } else {
                    // 需要重新检测
                    messageType = determineWelcomeMessageType(player);
                    if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("Folia欢迎 - 重新检测消息类型: {}", messageType);
            }
                }
                
                // 获取欢迎消息
                String welcomeTemplate = plugin.getConfigManager().getMessageConfig().getWelcomeMessage(messageType);
                if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                    logger.info("Folia模板 - 使用模板: {}", welcomeTemplate);
                }
                
                // 格式化消息
                String formattedWelcome = plugin.getConfigManager().getMessageConfig()
                    .formatMessage(welcomeTemplate, player.getName());
                // 玩家看到的消息
            logger.info("{}", formattedWelcome);
                
                // 发送给玩家
                sendMessageToPlayer(player, formattedWelcome);
                // 静默发送完成
                
            }, null, delay / 50); // 转换为ticks，Folia以ticks为单位
            
        } catch (Exception e) {
            logger.error("发送欢迎消息失败", e);
        }
    }
    
    /**
     * 重写父类方法以支持Folia的直接发送
     */
    public void sendWelcomeMessageDirectFoliaOverride(Player player, boolean isFirstTimeAlreadyChecked) {
        sendWelcomeMessageDirectFolia(player, isFirstTimeAlreadyChecked);
    }
}