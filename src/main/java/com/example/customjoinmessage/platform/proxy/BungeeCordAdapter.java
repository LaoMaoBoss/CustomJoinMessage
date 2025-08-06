package com.example.customjoinmessage.platform.proxy;


import com.example.customjoinmessage.core.CustomJoinMessagePlugin;
import com.example.customjoinmessage.platform.AbstractPlatformAdapter;
import com.example.customjoinmessage.utils.PermissionUtil;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.Instant;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * BungeeCord 代理平台适配器
 * 
 * 实现 BungeeCord 环境下的完整消息功能
 * 参考 VelocityAdapter 实现
 */
public class BungeeCordAdapter extends AbstractPlatformAdapter implements Listener {
    
    private final ProxyServer proxyServer;
    private final Plugin bungeePlugin;
    
    // 玩家状态追踪（保留用于未来扩展）
    @SuppressWarnings("unused")
    private final Set<UUID> recentlyJoinedPlayers;
    @SuppressWarnings("unused")
    private final Set<UUID> recentlyLeftPlayers;
    
    // 持久化玩家数据
    private final Gson gson;
    private final File playersDataFile;
    
    public BungeeCordAdapter(CustomJoinMessagePlugin plugin, Object platformInstance) {
        super(plugin, platformInstance);
        
        this.bungeePlugin = (Plugin) platformInstance;
        this.proxyServer = bungeePlugin.getProxy();
        
        // 初始化状态追踪
        this.recentlyJoinedPlayers = ConcurrentHashMap.newKeySet();
        this.recentlyLeftPlayers = ConcurrentHashMap.newKeySet();
        
        // 初始化数据文件
        this.gson = new Gson();
        this.playersDataFile = new File(plugin.getDataDirectory().toFile(), "players.json");
    }
    
    @Override
    public void onEnable() {
        try {
            // 注册事件监听器
            proxyServer.getPluginManager().registerListener(bungeePlugin, this);
            
            // 权限系统已在主插件中初始化，此处无需重复初始化
            
            if (plugin.getConfigManager().getPluginConfig().isDebug()) {
                logger.info("BungeeCord 适配器已启用");
            }
            
        } catch (Exception e) {
            logger.error("BungeeCord 适配器启用失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            logger.info("BungeeCord 适配器已禁用");
        } catch (Exception e) {
            logger.error("BungeeCord 适配器禁用失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void reload() {
        logger.info("重新加载 BungeeCord 适配器配置");
        PermissionUtil.reloadPermissions(plugin.getConfigManager().getPluginConfig());
    }
    
    // ================================
    // BungeeCord 事件处理
    // ================================
    
    /**
     * 处理玩家首次连接到代理服务器
     */
    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        
        try {
            // 延迟处理，等待玩家完全连接
            proxyServer.getScheduler().schedule(bungeePlugin, () -> {
                handlePlayerJoinNetwork(player);
            }, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            logger.error("处理玩家加入网络失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理玩家断开连接
     */
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        
        try {
            handlePlayerLeaveNetwork(player);
            
        } catch (Exception e) {
            logger.error("处理玩家离开网络失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理服务器切换
     */
    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        
        try {
            // 如果玩家有前一个服务器，说明是切换而不是首次连接
            if (event.getFrom() != null) {
                handleServerSwitch(player, event.getFrom(), event.getPlayer().getServer());
            }
            
        } catch (Exception e) {
            logger.error("处理服务器切换失败: {}", e.getMessage(), e);
        }
    }
    
    // ================================
    // 消息处理逻辑
    // ================================
    
    /**
     * 处理玩家加入网络
     */
    private void handlePlayerJoinNetwork(ProxiedPlayer player) {
        try {
            boolean isFirstTime = isFirstTimeJoin(player.getUniqueId());
            
            if (isFirstTime) {
                // 首次加入网络
                logger.info("新玩家 {} 首次加入服务器", player.getName());
                recordPlayerJoin(player.getUniqueId(), player.getName());
                sendGlobalFirstJoinMessage(player);
                
                if (plugin.getConfigManager().getPluginConfig().isFirstTimeWelcomeEnabled()) {
                    sendWelcomeMessageToPlayer(player, "first-time");
                }
            } else {
                // 检查是否为回归玩家
                boolean welcomeEnabled = plugin.getConfigManager().getPluginConfig().isReturningWelcomeEnabled() && 
                                        shouldShowReturningMessage(player.getUniqueId());
                
                if (welcomeEnabled) {
                    // 回归玩家
                    logger.info("回归玩家 {} 重新加入服务器", player.getName());
                    sendGlobalReturningMessage(player);
                    sendWelcomeMessageToPlayer(player, "returning");
                } else {
                    // 普通加入
                    logger.info("玩家 {} 加入了服务器", player.getName());
                    sendGlobalJoinMessage(player);
                }
                
                updatePlayerLastSeen(player.getUniqueId(), player.getName());
            }
            
        } catch (Exception e) {
            logger.error("处理玩家加入网络失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理玩家离开网络
     */
    private void handlePlayerLeaveNetwork(ProxiedPlayer player) {
        try {
            updatePlayerLastSeen(player.getUniqueId(), player.getName());
            sendGlobalLeaveMessage(player);
            logger.info("玩家 {} 离开了服务器", player.getName());
            
        } catch (Exception e) {
            logger.error("处理玩家离开网络失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理服务器切换
     */
    private void handleServerSwitch(ProxiedPlayer player, net.md_5.bungee.api.config.ServerInfo from, 
                                  net.md_5.bungee.api.connection.Server to) {
        try {
            if (plugin.getConfigManager().getPluginConfig().isServerSwitchEnabled()) {
                sendServerSwitchMessage(player, from.getName(), to.getInfo().getName());
                logger.info("玩家 {} 从 {} 切换到 {}", player.getName(), from.getName(), to.getInfo().getName());
            }
            
        } catch (Exception e) {
            logger.error("处理服务器切换失败: {}", e.getMessage(), e);
        }
    }
    
    // ================================
    // 消息发送方法
    // ================================
    
    @Override
    public void sendMessageToPlayer(Object player, String message) {
        try {
            if (player instanceof ProxiedPlayer) {
                ProxiedPlayer proxiedPlayer = (ProxiedPlayer) player;
                BaseComponent[] components = convertToBaseComponent(message);
                proxiedPlayer.sendMessage(components);
            }
        } catch (Exception e) {
            logger.error("发送玩家消息失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void sendMessageToAll(String message) {
        try {
            BaseComponent[] components = convertToBaseComponent(message);
            for (ProxiedPlayer player : proxyServer.getPlayers()) {
                player.sendMessage(components);
            }
        } catch (Exception e) {
            logger.error("发送全局消息失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void sendMessageToServer(String serverName, String message) {
        try {
            BaseComponent[] components = convertToBaseComponent(message);
            net.md_5.bungee.api.config.ServerInfo server = proxyServer.getServerInfo(serverName);
            if (server != null) {
                for (ProxiedPlayer player : server.getPlayers()) {
                    player.sendMessage(components);
                }
            }
        } catch (Exception e) {
            logger.error("发送服务器消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 将消息格式转换为 BungeeCord BaseComponent
     * 支持基础的MiniMessage颜色格式
     */
    private BaseComponent[] convertToBaseComponent(String message) {
        try {
            // 简单的MiniMessage颜色代码转换
            String converted = convertBasicMiniMessageToBungee(message);
            return TextComponent.fromLegacyText(converted);
        } catch (Exception e) {
            // 如果转换失败，使用简单文本组件
            logger.warn("消息转换失败，使用简单文本: {}", e.getMessage());
            return new BaseComponent[]{new TextComponent(message)};
        }
    }
    
    /**
     * 将基础的MiniMessage格式转换为BungeeCord颜色代码
     * 支持常用的颜色标签
     */
    private String convertBasicMiniMessageToBungee(String message) {
        // 基础颜色转换映射
        message = message.replace("<black>", ChatColor.BLACK.toString());
        message = message.replace("<dark_blue>", ChatColor.DARK_BLUE.toString());
        message = message.replace("<dark_green>", ChatColor.DARK_GREEN.toString());
        message = message.replace("<dark_aqua>", ChatColor.DARK_AQUA.toString());
        message = message.replace("<dark_red>", ChatColor.DARK_RED.toString());
        message = message.replace("<dark_purple>", ChatColor.DARK_PURPLE.toString());
        message = message.replace("<gold>", ChatColor.GOLD.toString());
        message = message.replace("<gray>", ChatColor.GRAY.toString());
        message = message.replace("<dark_gray>", ChatColor.DARK_GRAY.toString());
        message = message.replace("<blue>", ChatColor.BLUE.toString());
        message = message.replace("<green>", ChatColor.GREEN.toString());
        message = message.replace("<aqua>", ChatColor.AQUA.toString());
        message = message.replace("<red>", ChatColor.RED.toString());
        message = message.replace("<light_purple>", ChatColor.LIGHT_PURPLE.toString());
        message = message.replace("<yellow>", ChatColor.YELLOW.toString());
        message = message.replace("<white>", ChatColor.WHITE.toString());
        
        // 样式转换
        message = message.replace("<bold>", ChatColor.BOLD.toString());
        message = message.replace("<italic>", ChatColor.ITALIC.toString());
        message = message.replace("<underlined>", ChatColor.UNDERLINE.toString());
        message = message.replace("<strikethrough>", ChatColor.STRIKETHROUGH.toString());
        message = message.replace("<obfuscated>", ChatColor.MAGIC.toString());
        message = message.replace("<reset>", ChatColor.RESET.toString());
        
        // 移除闭合标签（BungeeCord不需要）
        message = message.replaceAll("</[^>]+>", "");
        
        return message;
    }
    
    /**
     * 发送全局加入消息
     */
    private void sendGlobalJoinMessage(ProxiedPlayer player) {
        try {
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            String messageTemplate = plugin.getConfigManager().getMessageConfig().getJoinMessage(permissionGroup, "default");
            String formattedMessage = plugin.getConfigManager().getMessageConfig()
                .formatMessage(messageTemplate, player.getName());
            
            sendMessageToAll(formattedMessage);
            
        } catch (Exception e) {
            logger.error("发送全局加入消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送全局首次加入消息
     */
    private void sendGlobalFirstJoinMessage(ProxiedPlayer player) {
        try {
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            String messageTemplate = plugin.getConfigManager().getMessageConfig().getJoinMessage(permissionGroup, "first-time");
            
            if (messageTemplate != null && !messageTemplate.isEmpty()) {
                String formattedMessage = plugin.getConfigManager().getMessageConfig()
                    .formatMessage(messageTemplate, player.getName());
                
                for (ProxiedPlayer otherPlayer : proxyServer.getPlayers()) {
                    if (!otherPlayer.getUniqueId().equals(player.getUniqueId())) {
                        sendMessageToPlayer(otherPlayer, formattedMessage);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("发送首次加入消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送全局回归消息
     */
    private void sendGlobalReturningMessage(ProxiedPlayer player) {
        try {
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            String messageTemplate = plugin.getConfigManager().getMessageConfig().getJoinMessage(permissionGroup, "returning");
            
            if (messageTemplate != null && !messageTemplate.isEmpty()) {
                String formattedMessage = plugin.getConfigManager().getMessageConfig()
                    .formatMessage(messageTemplate, player.getName());
                
                for (ProxiedPlayer otherPlayer : proxyServer.getPlayers()) {
                    if (!otherPlayer.getUniqueId().equals(player.getUniqueId())) {
                        sendMessageToPlayer(otherPlayer, formattedMessage);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("发送回归加入消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送全局离开消息
     */
    private void sendGlobalLeaveMessage(ProxiedPlayer player) {
        try {
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            String messageTemplate = plugin.getConfigManager().getMessageConfig().getLeaveMessage(permissionGroup, "default");
            String formattedMessage = plugin.getConfigManager().getMessageConfig()
                .formatMessage(messageTemplate, player.getName());
            
            sendMessageToAll(formattedMessage);
            
        } catch (Exception e) {
            logger.error("发送全局离开消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送服务器切换消息
     */
    private void sendServerSwitchMessage(ProxiedPlayer player, String fromServer, String toServer) {
        try {
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            String messageTemplate = plugin.getConfigManager().getMessageConfig().getServerSwitchMessage(permissionGroup, "default");
            
            // 获取服务器别名
            String fromServerAlias = plugin.getConfigManager().getPluginConfig().getServerAlias(fromServer);
            String toServerAlias = plugin.getConfigManager().getPluginConfig().getServerAlias(toServer);
            
            // 格式化消息（使用服务器别名）
            String formattedMessage = plugin.getConfigManager().getMessageConfig()
                .formatSwitchMessage(messageTemplate, player.getName(), fromServerAlias, toServerAlias);
            
            if (plugin.getConfigManager().getPluginConfig().isServerSwitchShowToAll()) {
                sendMessageToAll(formattedMessage);
            } else {
                sendMessageToServer(fromServer, formattedMessage);
                sendMessageToServer(toServer, formattedMessage);
            }
            
        } catch (Exception e) {
            logger.error("发送服务器切换消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送欢迎消息给玩家
     */
    private void sendWelcomeMessageToPlayer(ProxiedPlayer player, String messageType) {
        try {
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            String welcomeTemplate = plugin.getConfigManager().getMessageConfig().getWelcomeMessage(permissionGroup, messageType);
            
            if (welcomeTemplate == null || welcomeTemplate.trim().isEmpty()) {
                return;
            }
            
            int delay = plugin.getConfigManager().getPluginConfig().getFirstTimeWelcomeDelay();
            
            proxyServer.getScheduler().schedule(bungeePlugin, () -> {
                try {
                    String formattedMessage = formatWelcomeMessage(welcomeTemplate, player, messageType);
                    sendMessageToPlayer(player, formattedMessage);
                } catch (Exception e) {
                    logger.error("发送欢迎消息失败: {}", e.getMessage(), e);
                }
            }, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            logger.error("发送欢迎消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 格式化欢迎消息（支持 BungeeCord 特定占位符）
     */
    private String formatWelcomeMessage(String template, ProxiedPlayer player, String messageType) {
        try {
            String formatted = plugin.getConfigManager().getMessageConfig()
                .formatMessage(template, player.getName());
            
            // BungeeCord 环境特定占位符
            int onlineCount = proxyServer.getOnlineCount();
            @SuppressWarnings("deprecation")
            int maxPlayers = proxyServer.getConfig().getPlayerLimit();
            formatted = formatted.replace("{online_count}", String.valueOf(onlineCount));
            formatted = formatted.replace("{max_players}", String.valueOf(maxPlayers));
            
            // {last_seen} 占位符（仅回归玩家有效）
            if ("returning".equals(messageType)) {
                String lastSeenTime = getFormattedLastSeenTime(player.getUniqueId());
                formatted = formatted.replace("{last_seen}", lastSeenTime);
            } else {
                formatted = formatted.replace("{last_seen}", "");
            }
            
            return formatted;
            
        } catch (Exception e) {
            logger.error("格式化欢迎消息失败: {}", e.getMessage(), e);
            return template;
        }
    }
    
    // ================================
    // 玩家数据管理
    // ================================
    
    /**
     * 检查是否为首次加入
     */
    private boolean isFirstTimeJoin(UUID playerId) {
        return getPlayerLastSeen(playerId) == 0;
    }
    
    /**
     * 检查是否应该显示回归消息
     */
    private boolean shouldShowReturningMessage(UUID playerId) {
        long lastSeen = getPlayerLastSeen(playerId);
        if (lastSeen == 0) return false;
        
        long threshold = plugin.getConfigManager().getPluginConfig().getReturningThreshold();
        long currentTime = Instant.now().getEpochSecond();
        
        return (currentTime - lastSeen) >= threshold;
    }
    
    /**
     * 记录玩家加入
     */
    private void recordPlayerJoin(UUID playerId, String playerName) {
        updatePlayerLastSeen(playerId, playerName);
    }
    
    /**
     * 更新玩家最后在线时间
     */
    private void updatePlayerLastSeen(UUID playerId, String playerName) {
        try {
            JsonObject playersData = loadPlayersData();
            JsonObject playerData = new JsonObject();
            playerData.addProperty("name", playerName);
            playerData.addProperty("lastSeen", Instant.now().getEpochSecond());
            
            playersData.add(playerId.toString(), playerData);
            savePlayersData(playersData);
            
        } catch (Exception e) {
            logger.error("更新玩家数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取玩家最后在线时间
     */
    private long getPlayerLastSeen(UUID playerId) {
        try {
            JsonObject playersData = loadPlayersData();
            if (playersData.has(playerId.toString())) {
                JsonObject playerData = playersData.getAsJsonObject(playerId.toString());
                return playerData.get("lastSeen").getAsLong();
            }
            return 0;
            
        } catch (Exception e) {
            logger.error("读取玩家最后见面时间失败: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 格式化最后在线时间
     */
    private String getFormattedLastSeenTime(UUID playerId) {
        long lastSeen = getPlayerLastSeen(playerId);
        if (lastSeen == 0) return "未知";
        
        long secondsAgo = Instant.now().getEpochSecond() - lastSeen;
        return formatTimeAgo(secondsAgo);
    }
    
    /**
     * 格式化时间间隔
     */
    private String formatTimeAgo(long seconds) {
        if (seconds < 60) return seconds + "秒前";
        if (seconds < 3600) return (seconds / 60) + "分钟前";
        if (seconds < 86400) return (seconds / 3600) + "小时前";
        return (seconds / 86400) + "天前";
    }
    
    /**
     * 加载玩家数据
     */
    private JsonObject loadPlayersData() {
        try {
            if (!playersDataFile.exists()) {
                return new JsonObject();
            }
            
            String content = new String(Files.readAllBytes(playersDataFile.toPath()));
            return JsonParser.parseString(content).getAsJsonObject();
            
        } catch (Exception e) {
            logger.error("加载玩家数据失败: {}", e.getMessage(), e);
            return new JsonObject();
        }
    }
    
    /**
     * 保存玩家数据
     */
    private void savePlayersData(JsonObject playersData) {
        try {
            playersDataFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(playersDataFile)) {
                gson.toJson(playersData, writer);
            }
            
        } catch (Exception e) {
            logger.error("保存玩家数据失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void registerCommunicationChannel() {
        // 注册 BungeeCord 通信通道
        String channel = plugin.getConfigManager().getPluginConfig().getChannel();
        proxyServer.registerChannel(channel);
        logger.debug("BungeeCord 通信通道已注册: {}", channel);
    }
    

    
    @Override
    public int getOnlinePlayerCount() {
        return proxyServer.getOnlineCount();
    }
    
    @Override
    public boolean isPlayerOnline(String playerName) {
        return proxyServer.getPlayer(playerName) != null;
    }
    
    @Override
    public String getAdapterType() {
        return "BungeeCord Proxy";
    }
    
    @Override
    public String getPlatformVersion() {
        return "BungeeCord " + proxyServer.getVersion();
    }
}