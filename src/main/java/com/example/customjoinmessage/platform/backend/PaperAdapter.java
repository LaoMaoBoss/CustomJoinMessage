package com.example.customjoinmessage.platform.backend;

import com.example.customjoinmessage.core.CustomJoinMessagePlugin;
import com.example.customjoinmessage.core.PluginMode;
import com.example.customjoinmessage.platform.AbstractPlatformAdapter;
import com.example.customjoinmessage.utils.MessageFormatter;
import com.example.customjoinmessage.utils.PermissionUtil;
import com.example.customjoinmessage.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Paper 平台适配器
 * 
 * 负责处理 Paper/Bukkit 服务器的玩家加入/离开事件
 * 支持系统消息拦截和自定义消息发送
 */
public class PaperAdapter extends AbstractPlatformAdapter implements Listener, PluginMessageListener {
    
    protected final JavaPlugin bukkitPlugin;
    
    public PaperAdapter(CustomJoinMessagePlugin plugin, Object platformInstance) {
        super(plugin, platformInstance);
        this.bukkitPlugin = (JavaPlugin) platformInstance;
    }
    
    @Override
    public void onEnable() {
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, bukkitPlugin);
        
        // 注册通信通道（如果需要与代理端通信）
        registerCommunicationChannel();
        
        if (plugin.getConfigManager().getPluginConfig().isDebug()) {
            logger.info("Paper 后端适配器已启用");
        }
    }
    
    @Override
    public void onDisable() {
        // 注销通信通道
        unregisterCommunicationChannel();
        logger.info("Paper 后端适配器已禁用");
    }
    
    /**
     * 注销通信通道
     */
    private void unregisterCommunicationChannel() {
        try {
            String channelName = "customjoinmessage:sync";
            
            // 注销传出通道
            if (bukkitPlugin.getServer().getMessenger().isOutgoingChannelRegistered(bukkitPlugin, channelName)) {
                bukkitPlugin.getServer().getMessenger()
                    .unregisterOutgoingPluginChannel(bukkitPlugin, channelName);
                
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("已注销传出通信通道: {}", channelName);
                }
            }
            
            // 注销传入通道
            if (bukkitPlugin.getServer().getMessenger().isIncomingChannelRegistered(bukkitPlugin, channelName)) {
                bukkitPlugin.getServer().getMessenger()
                    .unregisterIncomingPluginChannel(bukkitPlugin, channelName, this);
                
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("已注销传入通信通道: {}", channelName);
                }
            }
            
        } catch (Exception e) {
            logger.error("注销通信通道失败", e);
        }
    }
    
    @Override
    public String getAdapterType() {
        return "Paper Backend";
    }
    
    @Override
    public String getPlatformVersion() {
        return bukkitPlugin.getServer().getVersion();
    }
    
    @Override
    public void reload() {
        // 重新加载配置
        plugin.getConfigManager().reload();
        logger.info("Paper适配器已重新加载");
    }
    
    @Override
    public void sendMessageToPlayer(Object player, String message) {
        if (player instanceof Player) {
            sendMessageToPlayer((Player) player, message);
        }
    }
    
    @Override
    public void sendMessageToServer(String serverName, String message) {
        // Bukkit环境中没有多服务器概念，发送给所有玩家
        sendMessageToAll(message);
    }
    
    @Override
    public int getOnlinePlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }
    
    @Override
    public boolean isPlayerOnline(String playerName) {
        return Bukkit.getPlayer(playerName) != null;
    }
    
    /**
     * 处理玩家加入事件
     * 根据运行模式决定是否拦截系统消息和发送自定义消息
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 拦截系统消息（所有模式都需要）
        if (plugin.getConfigManager().getPluginConfig().isInterceptJoinMessages()) {
            event.joinMessage(null);
            if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                logger.info("已拦截 {} 的系统加入消息", player.getName());
            }
        }
        
        // 根据运行模式决定是否发送自定义消息
        if (plugin.getPluginMode() == PluginMode.BACKEND_STANDALONE) {
            // 独立模式：后端负责发送所有消息
            if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                logger.info("后端独立模式 - 开始发送自定义消息");
            }
            handlePlayerJoin(player);
        } else if (plugin.getPluginMode() == PluginMode.BACKEND_SLAVE) {
            // 从属模式：只拦截系统消息，通知代理端处理
            if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                logger.info("后端从属模式 - 通知代理端处理玩家加入");
            }
            notifyProxyPlayerJoin(player);
        }
    }
    
    /**
     * 处理玩家离开事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 拦截系统消息
        if (plugin.getConfigManager().getPluginConfig().isInterceptLeaveMessages()) {
            event.quitMessage(null);
            if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                logger.info("已拦截 {} 的系统离开消息", player.getName());
            }
        }
        
        // 根据运行模式决定是否发送自定义消息
        if (plugin.getPluginMode() == PluginMode.BACKEND_STANDALONE) {
            handlePlayerLeave(player);
        } else if (plugin.getPluginMode() == PluginMode.BACKEND_SLAVE) {
            notifyProxyPlayerLeave(player);
        }
    }
    
    /**
     * 处理玩家加入（独立模式）
     */
    protected void handlePlayerJoin(Player player) {
        try {
            // 检测玩家权限组
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            
            // 判断是否首次加入
            boolean isFirstTime = isFirstTimeJoin(player);
            
            if (isFirstTime) {
                // 首次加入玩家
                logger.info("新玩家 {} 首次加入服务器", player.getName());
                
                // 发送全局首次加入消息
                sendGlobalFirstJoinMessage(player, permissionGroup);
                
                // 发送首次加入欢迎消息
                if (plugin.getConfigManager().getPluginConfig().isFirstTimeWelcomeEnabled()) {
                    sendWelcomeMessage(player, "first-time");
                }
            } else {
                // 判断是否为回归玩家
                if (shouldShowReturningMessage(player)) {
                    // 回归玩家
                    logger.info("回归玩家 {} 重新加入服务器", player.getName());
                    
                    // 发送全局回归消息
                    sendGlobalReturningMessage(player, permissionGroup);
                    
                    // 发送回归欢迎消息
                    if (plugin.getConfigManager().getPluginConfig().isReturningWelcomeEnabled()) {
                        sendWelcomeMessage(player, "returning");
                    }
                } else {
                    // 普通玩家加入
                    logger.info("玩家 {} 加入了服务器", player.getName());
                    sendGlobalJoinMessage(player, permissionGroup);
                }
            }
        } catch (Exception e) {
            logger.error("处理玩家加入时发生错误: {}", player.getName(), e);
        }
    }
    
    /**
     * 处理玩家离开（独立模式）
     */
    private void handlePlayerLeave(Player player) {
        try {
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            logger.info("玩家 {} 离开了服务器", player.getName());
            sendGlobalLeaveMessage(player, permissionGroup);
        } catch (Exception e) {
            logger.error("处理玩家离开时发生错误: {}", player.getName(), e);
        }
    }
    
    /**
     * 判断是否为首次加入
     * 使用多种方式检测以确保准确性
     */
    protected boolean isFirstTimeJoin(Player player) {
        // 只在独立模式下进行首次加入检测
        if (plugin.getPluginMode() != PluginMode.BACKEND_STANDALONE) {
            return false;
        }
        
        try {
            // 方法1: 使用 hasPlayedBefore()
            if (!player.hasPlayedBefore()) {
                return true;
            }
            
            // 方法2: 检查统计数据（如果支持）
            try {
                Object stats = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                if (stats != null && ((Integer) stats) == 0) {
                    return true;
                }
            } catch (Exception ignored) {
                // 统计数据不可用，忽略
            }
            
            // 方法3: 检查玩家数据文件存在性
            if (player.getFirstPlayed() == 0) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.warn("检测首次加入状态时发生错误，默认为非首次: {}", player.getName(), e);
            return false;
        }
    }
    
    /**
     * 判断是否应该显示回归消息
     */
    protected boolean shouldShowReturningMessage(Player player) {
        if (!plugin.getConfigManager().getPluginConfig().isReturningWelcomeEnabled()) {
            return false;
        }
        
        try {
            long thresholdSeconds = plugin.getConfigManager().getPluginConfig().getReturningThreshold();
            long thresholdMillis = thresholdSeconds * 1000;
            long lastPlayed = player.getLastPlayed();
            long currentTime = System.currentTimeMillis();
            
            return (currentTime - lastPlayed) >= thresholdMillis;
        } catch (Exception e) {
            logger.warn("检查回归消息条件时发生错误: {}", player.getName(), e);
            return false;
        }
    }
    
    /**
     * 发送全局首次加入消息
     */
    private void sendGlobalFirstJoinMessage(Player player, String permissionGroup) {
        String template = plugin.getConfigManager().getMessageConfig()
            .getJoinMessage(permissionGroup, "first-time");
        
        if (template != null && !template.trim().isEmpty()) {
            String message = formatMessage(template, player);
            // 发送给除了加入玩家之外的所有玩家
            sendMessageToAllExcept(player, message);
        }
    }
    
    /**
     * 发送全局回归消息
     */
    private void sendGlobalReturningMessage(Player player, String permissionGroup) {
        String template = plugin.getConfigManager().getMessageConfig()
            .getJoinMessage(permissionGroup, "returning");
        
        if (template != null && !template.trim().isEmpty()) {
            String message = formatMessage(template, player);
            // 发送给除了加入玩家之外的所有玩家
            sendMessageToAllExcept(player, message);
        }
    }
    
    /**
     * 发送全局加入消息
     */
    private void sendGlobalJoinMessage(Player player, String permissionGroup) {
        String template = plugin.getConfigManager().getMessageConfig()
            .getJoinMessage(permissionGroup, "default");
        
        if (template != null && !template.trim().isEmpty()) {
            String message = formatMessage(template, player);
            // 普通加入消息发送给所有玩家（包括加入的玩家自己）
            sendMessageToAll(message);
        }
    }
    
    /**
     * 发送全局离开消息
     */
    private void sendGlobalLeaveMessage(Player player, String permissionGroup) {
        String template = plugin.getConfigManager().getMessageConfig()
            .getLeaveMessage(permissionGroup, "default");
        
        if (template != null && !template.trim().isEmpty()) {
            String message = formatMessage(template, player);
            sendMessageToAll(message);
        }
    }
    
    /**
     * 发送欢迎消息给玩家
     */
    protected void sendWelcomeMessage(Player player) {
        // 判断消息类型
        String messageType;
        if (isFirstTimeJoin(player)) {
            messageType = "first-time";
        } else if (shouldShowReturningMessage(player)) {
            messageType = "returning";
        } else {
            return; // 不需要发送欢迎消息
        }
        
        sendWelcomeMessage(player, messageType);
    }
    
    /**
     * 发送指定类型的欢迎消息
     */
    protected void sendWelcomeMessage(Player player, String messageType) {
        try {
            int delay = "first-time".equals(messageType) 
                ? plugin.getConfigManager().getPluginConfig().getFirstTimeWelcomeDelay()
                : 0; // 回归消息立即发送
            
            // 延迟发送欢迎消息
            Bukkit.getScheduler().runTaskLater(bukkitPlugin, () -> {
                String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
                String template = plugin.getConfigManager().getMessageConfig()
                    .getWelcomeMessage(permissionGroup, messageType);
                
                if (template != null && !template.trim().isEmpty()) {
                    String message = formatWelcomeMessage(template, player);
                    sendMessageToPlayer(player, message);
                }
            }, delay / 50); // 转换为ticks
            
        } catch (Exception e) {
            logger.error("发送欢迎消息失败: {}", player.getName(), e);
        }
    }
    
    /**
     * 格式化欢迎消息（支持特殊占位符）
     */
    private String formatWelcomeMessage(String template, Player player) {
        String message = formatMessage(template, player);
        
        // 处理特殊占位符
        try {
            // {online_count} - 在线玩家数
            message = message.replace("{online_count}", String.valueOf(Bukkit.getOnlinePlayers().size()));
            
            // {max_players} - 最大玩家数
            message = message.replace("{max_players}", String.valueOf(Bukkit.getMaxPlayers()));
            
            // {last_seen} - 上次游戏时间（仅回归消息）
            if (message.contains("{last_seen}")) {
                if (shouldShowReturningMessage(player)) {
                    long lastPlayed = player.getLastPlayed();
                    String lastSeenText = formatTimeAgo(System.currentTimeMillis() - lastPlayed);
                    message = message.replace("{last_seen}", lastSeenText);
                } else {
                    // 非回归消息，移除占位符
                    message = message.replace("{last_seen}", "").trim();
                }
            }
        } catch (Exception e) {
            logger.warn("格式化欢迎消息占位符时发生错误: {}", player.getName(), e);
        }
        
        return message;
    }
    
    /**
     * 格式化时间差为友好显示
     */
    private String formatTimeAgo(long millisAgo) {
        long seconds = millisAgo / 1000;
        
        if (seconds < 60) return "刚刚";
        if (seconds < 3600) return (seconds / 60) + "分钟前";
        if (seconds < 86400) return (seconds / 3600) + "小时前";
        if (seconds < 2592000) return (seconds / 86400) + "天前";
        if (seconds < 31536000) return (seconds / 2592000) + "个月前";
        return (seconds / 31536000) + "年前";
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
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            
            out.writeUTF(action);
            out.writeUTF(player.getName());
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(bukkitPlugin.getServer().getName());
            
            player.sendPluginMessage(bukkitPlugin, "customjoinmessage:sync", b.toByteArray());
        } catch (IOException e) {
            logger.error("发送插件消息失败: {}", action, e);
        }
    }
    
    /**
     * 发送消息给所有玩家
     */
    @Override
    public void sendMessageToAll(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessageToPlayer(player, message);
        }
    }
    
    /**
     * 发送消息给除指定玩家外的所有玩家
     */
    private void sendMessageToAllExcept(Player exceptPlayer, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(exceptPlayer)) {
                sendMessageToPlayer(player, message);
            }
        }
    }
    
    /**
     * 发送消息给指定玩家
     */
    protected void sendMessageToPlayer(Player player, String message) {
        try {
            // 尝试使用Adventure API（Paper）
            if (hasAdventureSupport()) {
                sendAdventureMessage(player, message);
            } else {
                // 回退到传统方法
                player.sendMessage(message);
            }
        } catch (Exception e) {
            // 最终回退
            player.sendMessage(message);
        }
    }
    
    /**
     * 检查是否支持Adventure API
     */
    private boolean hasAdventureSupport() {
        try {
            Class.forName("net.kyori.adventure.text.Component");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 发送Adventure消息（如果支持）
     */
    private void sendAdventureMessage(Player player, String message) {
        try {
            // 使用反射调用Adventure方法，避免编译时依赖
            Method sendMessageMethod = player.getClass().getMethod("sendMessage", 
                Class.forName("net.kyori.adventure.text.Component"));
            
            // 创建MiniMessage组件
            Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Object miniMessage = miniMessageClass.getMethod("miniMessage").invoke(null);
            Object component = miniMessageClass.getMethod("deserialize", String.class)
                .invoke(miniMessage, message);
            
            sendMessageMethod.invoke(player, component);
        } catch (Exception e) {
            // 回退到普通消息
            player.sendMessage(message);
        }
    }
    
    /**
     * 格式化消息（基础占位符替换）
     */
    private String formatMessage(String template, Player player) {
        String message = template.replace("{player}", player.getName())
                               .replace("{server}", bukkitPlugin.getServer().getName());
        
        // 应用颜色代码转换
        return MessageFormatter.translateColorCodes(message);
    }
    
    /**
     * 确定欢迎消息类型
     */
    protected String determineWelcomeMessageType(Player player) {
        if (isFirstTimeJoin(player)) {
            return "first-time";
        } else if (shouldShowReturningMessage(player)) {
            return "returning";
        } else {
            return "default";
        }
    }
    
    @Override
    public void registerCommunicationChannel() {
        try {
            String channelName = "customjoinmessage:sync";
            
            // 检查通道是否已注册，避免重复注册
            if (!bukkitPlugin.getServer().getMessenger().isOutgoingChannelRegistered(bukkitPlugin, channelName)) {
                bukkitPlugin.getServer().getMessenger()
                    .registerOutgoingPluginChannel(bukkitPlugin, channelName);
                
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("已注册传出通信通道: {}", channelName);
                }
            } else {
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("传出通信通道已存在，跳过注册: {}", channelName);
                }
            }
            
            if (!bukkitPlugin.getServer().getMessenger().isIncomingChannelRegistered(bukkitPlugin, channelName)) {
                bukkitPlugin.getServer().getMessenger()
                    .registerIncomingPluginChannel(bukkitPlugin, channelName, this);
                
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("已注册传入通信通道: {}", channelName);
                }
            } else {
                if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                    logger.info("传入通信通道已存在，跳过注册: {}", channelName);
                }
            }
            
        } catch (Exception e) {
            logger.error("注册通信通道失败", e);
        }
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // 处理来自代理端的消息
        if ("customjoinmessage:sync".equals(channel)) {
            // 这里可以处理代理端发送的消息
            if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                logger.info("收到来自代理端的消息");
            }
        }
    }
}
