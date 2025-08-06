package com.example.customjoinmessage.platform.backend;

import com.example.customjoinmessage.core.CustomJoinMessagePlugin;
import com.example.customjoinmessage.core.PluginMode;
import com.example.customjoinmessage.utils.MessageFormatter;
import com.example.customjoinmessage.utils.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Spigot 平台适配器
 * 
 * 针对Spigot平台优化的消息拦截处理
 * 解决Spigot平台消息拦截失效的问题
 */
public class SpigotAdapter extends PaperAdapter {
    
    public SpigotAdapter(CustomJoinMessagePlugin plugin, Object platformInstance) {
        super(plugin, platformInstance);
    }
    
    @Override
    public void onEnable() {
        // 不调用父类方法，避免重复注册事件监听器
        // 直接注册自己的事件监听器
        Bukkit.getPluginManager().registerEvents(this, bukkitPlugin);
        
        // 注册通信通道（如果需要与代理端通信）
        try {
            registerCommunicationChannel();
        } catch (Exception e) {
            logger.error("注册通信通道失败", e);
        }
        
        if (plugin.getConfigManager().getPluginConfig().isDebug()) {
            logger.info("Spigot 后端适配器已启用 - 使用专用消息拦截");
        }
    }
    
    @Override
    public String getAdapterType() {
        return "Spigot Backend";
    }
    
    /**
     * Spigot专用的玩家加入事件处理
     * 使用直接替换策略（参考ShorterJoinQuitMessages的成功经验）
     */
    @Override
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 直接替换系统消息为自定义消息（而不是设置为null）
        if (plugin.getConfigManager().getPluginConfig().isInterceptJoinMessages()) {
            String customMessage = getCustomJoinMessage(player);
            if (customMessage != null && !customMessage.trim().isEmpty()) {
                event.setJoinMessage(customMessage);
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("Spigot已替换 {} 的加入消息: {}", player.getName(), customMessage);
                }
            } else {
                // 如果没有自定义消息，则禁用系统消息
                event.setJoinMessage(null);
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("Spigot已禁用 {} 的系统加入消息", player.getName());
                }
            }
        }
        
        // 处理欢迎消息和其他逻辑
        handleSpigotPlayerJoinLogic(player);
    }
    
    /**
     * Spigot专用的玩家离开事件处理
     * 使用直接替换策略（参考ShorterJoinQuitMessages的成功经验）
     */
    @Override
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 直接替换系统消息为自定义消息
        if (plugin.getConfigManager().getPluginConfig().isInterceptLeaveMessages()) {
            String customMessage = getCustomLeaveMessage(player);
            if (customMessage != null && !customMessage.trim().isEmpty()) {
                event.setQuitMessage(customMessage);
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("Spigot已替换 {} 的离开消息: {}", player.getName(), customMessage);
                }
            } else {
                // 如果没有自定义消息，则禁用系统消息
                event.setQuitMessage(null);
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("Spigot已禁用 {} 的系统离开消息", player.getName());
                }
            }
        }
        
        // 处理欢迎消息和其他逻辑
        handleSpigotPlayerLeaveLogic(player);
    }
    
    /**
     * 获取自定义加入消息
     */
    private String getCustomJoinMessage(Player player) {
        try {
            // 根据运行模式决定是否生成自定义消息
            if (plugin.getPluginMode() == PluginMode.BACKEND_STANDALONE) {
                String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
                
                // 判断消息类型
                boolean isFirstTime = isFirstTimeJoin(player);
                boolean isReturning = !isFirstTime && shouldShowReturningMessage(player);
                
                String messageType;
                if (isFirstTime) {
                    messageType = "first-time";
                } else if (isReturning) {
                    messageType = "returning";
                } else {
                    messageType = "default";
                }
                
                String template = plugin.getConfigManager().getMessageConfig()
                    .getJoinMessage(permissionGroup, messageType);
                
                if (template != null && !template.trim().isEmpty()) {
                    return formatMessage(template, player);
                }
            }
            // 从属模式不生成消息，由代理端处理
            return null;
        } catch (Exception e) {
            logger.error("获取自定义加入消息失败: {}", player.getName(), e);
            return null;
        }
    }
    
    /**
     * 获取自定义离开消息
     */
    private String getCustomLeaveMessage(Player player) {
        try {
            // 根据运行模式决定是否生成自定义消息
            if (plugin.getPluginMode() == PluginMode.BACKEND_STANDALONE) {
                String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
                String template = plugin.getConfigManager().getMessageConfig()
                    .getLeaveMessage(permissionGroup, "default");
                
                if (template != null && !template.trim().isEmpty()) {
                    return formatMessage(template, player);
                }
            }
            // 从属模式不生成消息，由代理端处理
            return null;
        } catch (Exception e) {
            logger.error("获取自定义离开消息失败: {}", player.getName(), e);
            return null;
        }
    }
    
    /**
     * 处理Spigot玩家加入的其他逻辑（欢迎消息等）
     */
    private void handleSpigotPlayerJoinLogic(Player player) {
        try {
            // 根据运行模式决定后续处理
            if (plugin.getPluginMode() == PluginMode.BACKEND_STANDALONE) {
                // 独立模式：发送欢迎消息
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("Spigot独立模式 - 处理欢迎消息");
                }
                
                // 发送欢迎消息
                boolean isFirstTime = isFirstTimeJoin(player);
                boolean isReturning = !isFirstTime && shouldShowReturningMessage(player);
                
                if (isFirstTime && plugin.getConfigManager().getPluginConfig().isFirstTimeWelcomeEnabled()) {
                    sendWelcomeMessage(player, "first-time");
                } else if (isReturning && plugin.getConfigManager().getPluginConfig().isReturningWelcomeEnabled()) {
                    sendWelcomeMessage(player, "returning");
                }
                
            } else if (plugin.getPluginMode() == PluginMode.BACKEND_SLAVE) {
                // 从属模式：通知代理端处理
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("Spigot从属模式 - 通知代理端处理玩家加入");
                }
                notifyProxyPlayerJoin(player);
            }
        } catch (Exception e) {
            logger.error("处理Spigot玩家加入逻辑时发生错误: {}", player.getName(), e);
        }
    }
    
    /**
     * 处理Spigot玩家离开的其他逻辑
     */
    private void handleSpigotPlayerLeaveLogic(Player player) {
        try {
            if (plugin.getPluginMode() == PluginMode.BACKEND_SLAVE) {
                // 从属模式：通知代理端处理
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("Spigot从属模式 - 通知代理端处理玩家离开");
                }
                notifyProxyPlayerLeave(player);
            }
            // 独立模式的离开消息已经通过setQuitMessage直接设置
        } catch (Exception e) {
            logger.error("处理Spigot玩家离开逻辑时发生错误: {}", player.getName(), e);
        }
    }
    
    /**
     * 格式化消息
     */
    private String formatMessage(String template, Player player) {
        String message = template.replace("{player}", player.getName())
                               .replace("{server}", bukkitPlugin.getServer().getName());
        
        // 应用颜色代码转换
        return MessageFormatter.translateColorCodes(message);
    }
    
    /**
     * 通知代理端玩家加入
     */
    private void notifyProxyPlayerJoin(Player player) {
        sendPluginMessage("PLAYER_JOIN", player);
    }
    
    /**
     * 通知代理端玩家离开
     */
    private void notifyProxyPlayerLeave(Player player) {
        sendPluginMessage("PLAYER_LEAVE", player);
    }
    
    /**
     * 发送插件消息到代理端
     */
    private void sendPluginMessage(String action, Player player) {
        try {
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(b);
            
            out.writeUTF(action);
            out.writeUTF(player.getName());
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(bukkitPlugin.getServer().getName());
            
            player.sendPluginMessage(bukkitPlugin, "customjoinmessage:sync", b.toByteArray());
        } catch (Exception e) {
            logger.error("发送插件消息失败: {}", action, e);
        }
    }
}