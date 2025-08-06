package com.example.customjoinmessage.platform.proxy;


import com.example.customjoinmessage.core.CustomJoinMessagePlugin;
import com.example.customjoinmessage.platform.AbstractPlatformAdapter;
import com.example.customjoinmessage.utils.PermissionUtil;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

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
 * Velocity 代理平台适配器
 * 
 * 参考 Proxy-Messages 实现
 * 主要功能：管理全局玩家状态 + 发送跨服务器消息
 */
public class VelocityAdapter extends AbstractPlatformAdapter {
    
    private final ProxyServer proxyServer;
    private final MiniMessage miniMessage;
    private final MinecraftChannelIdentifier channelIdentifier;
    
    // 玩家状态追踪
    private final Set<UUID> recentlyJoinedPlayers;
    private final Set<UUID> recentlyLeftPlayers;
    
    // 持久化玩家数据
    private final File playerDataFile;
    private final Gson gson;
    
    public VelocityAdapter(CustomJoinMessagePlugin plugin, Object platformInstance) {
        super(plugin, platformInstance);
        
        // 从VelocityPluginMain获取ProxyServer实例
        if (platformInstance instanceof com.example.customjoinmessage.velocity.VelocityPluginMain) {
            com.example.customjoinmessage.velocity.VelocityPluginMain velocityMain = 
                (com.example.customjoinmessage.velocity.VelocityPluginMain) platformInstance;
            this.proxyServer = velocityMain.getServer();
        } else {
            throw new IllegalArgumentException("VelocityAdapter 需要 VelocityPluginMain 实例，收到: " + 
                (platformInstance != null ? platformInstance.getClass().getSimpleName() : "null"));
        }
        
        this.miniMessage = MiniMessage.miniMessage();
        
        // 创建通信通道标识符（分离namespace和key以避免冒号问题）
        this.channelIdentifier = MinecraftChannelIdentifier.create("customjoinmessage", "sync");
        
        // 初始化玩家追踪
        this.recentlyJoinedPlayers = ConcurrentHashMap.newKeySet();
        this.recentlyLeftPlayers = ConcurrentHashMap.newKeySet();
        
        // 初始化持久化数据
        this.gson = new Gson();
        this.playerDataFile = new File(plugin.getDataFolder(), "players.json");
        
        // 确保数据目录存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }
    
    @Override
    public void onEnable() {
        // 静默启用
        
        // 注册事件监听器
        proxyServer.getEventManager().register(platformInstance, this);
        
        // 根据模式执行不同逻辑
        switch (plugin.getPluginMode()) {
            case PROXY_MASTER:
                // 静默启动 - 代理主控模式
                break;
                
            default:
                logger.warn("不支持的运行模式: {}", plugin.getPluginMode());
                break;
        }
        
        // 静默启用完成
    }
    
    @Override
    public void onDisable() {
        // 静默禁用
        
        // 注销事件监听器
        proxyServer.getEventManager().unregisterListener(platformInstance, this);
        
        // 清理玩家追踪
        recentlyJoinedPlayers.clear();
        recentlyLeftPlayers.clear();
        
        // 静默禁用完成
    }
    
    @Override
    public void reload() {
        // 静默重新加载
        // Velocity适配器无需特殊重载逻辑
    }
    
    @Override
    public void registerCommunicationChannel() {
        // 注册插件消息通道
        proxyServer.getChannelRegistrar().register(channelIdentifier);
        
        // 静默注册通信通道
    }
    

    
    // ================================
    // 核心事件处理 - 参考 Proxy-Messages
    // ================================
    
    /**
     * 玩家连接到服务器事件（第一次加入代理）
     */
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        RegisteredServer server = event.getServer();
        RegisteredServer previousServer = event.getPreviousServer().orElse(null);
        
        if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("服务器连接 - 玩家 {} 连接到服务器 {}", 
                       player.getUsername(), server.getServerInfo().getName());
        }
        if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("运行模式 - 当前插件模式: {}", plugin.getPluginMode());
        }
        
        // 🚨 关键修复：检查是否应该发送消息
        if (!plugin.getPluginMode().shouldSendCustomMessages()) {
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("跳过消息 - Velocity当前模式不发送自定义消息: {}", plugin.getPluginMode());
            }
            return;
        }
        
        debug("玩家服务器连接: {} -> {}", player.getUsername(), server.getServerInfo().getName());
        
        if (previousServer == null) {
            // 玩家首次加入代理
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("首次加入网络 - {} 首次加入代理网络", player.getUsername());
            }
            handlePlayerJoinNetwork(player, server);
        } else {
            // 玩家切换服务器
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("服务器切换 - {} 从 {} 切换到 {}", 
                           player.getUsername(), 
                           previousServer.getServerInfo().getName(),
                           server.getServerInfo().getName());
            }
            handlePlayerSwitchServer(player, previousServer, server);
        }
    }
    
    /**
     * 玩家断开连接事件（离开代理）
     */
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("玩家离开 - {} 离开网络", player.getUsername());
        }
        if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("运行模式 - 当前插件模式: {}", plugin.getPluginMode());
        }
        
        // 🚨 关键修复：检查是否应该发送消息
        if (!plugin.getPluginMode().shouldSendCustomMessages()) {
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("跳过消息 - Velocity当前模式不发送自定义消息: {}", plugin.getPluginMode());
            }
            return;
        }
        
        debug("玩家断开连接: {}", player.getUsername());
        
        handlePlayerLeaveNetwork(player);
    }
    
    /**
     * 插件消息事件（来自后端服务器的通信）
     */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // 检查是否是我们的通道
        if (!channelIdentifier.equals(event.getIdentifier())) {
            return;
        }
        
        try {
            // 解析消息数据
            ByteArrayDataInput data = ByteStreams.newDataInput(event.getData());
            String messageType = data.readUTF();
            
            switch (messageType) {
                case "PLAYER_JOIN":
                    handleBackendPlayerJoin(data, event);
                    break;
                    
                case "PLAYER_LEAVE":
                    handleBackendPlayerLeave(data, event);
                    break;
                    
                default:
                    verbose("收到未知消息类型: {}", messageType);
                    break;
            }
            
        } catch (Exception e) {
            logger.error("处理插件消息失败: {}", e.getMessage(), e);
        }
    }
    
    // ================================
    // 玩家网络事件处理
    // ================================
    
    /**
     * 处理玩家加入网络
     * 🚨 关键修复：代理端负责首次加入网络判断和消息发送
     */
    private void handlePlayerJoinNetwork(Player player, RegisteredServer server) {
        try {
            // 添加到最近加入列表
            recentlyJoinedPlayers.add(player.getUniqueId());
            
            // 🔥 关键：判断是否是首次加入网络（而不是子服务器）
            boolean isFirstTimeJoinNetwork = isFirstTimeJoinNetwork(player);
            boolean joinEnabled = plugin.getConfigManager().getPluginConfig().isCustomJoinFormatEnabled();
            
            // 根据是否首次加入来判断欢迎消息是否启用
            boolean welcomeEnabled;
            if (isFirstTimeJoinNetwork) {
                welcomeEnabled = plugin.getConfigManager().getPluginConfig().isFirstTimeWelcomeEnabled();
            } else {
                welcomeEnabled = plugin.getConfigManager().getPluginConfig().isReturningWelcomeEnabled() && 
                                 shouldShowReturningMessage(player);
            }
            
            // 🔥 新增：更新玩家最后见面时间（如果不是首次）
            if (!isFirstTimeJoinNetwork) {
                updatePlayerLastSeen(player.getUniqueId(), player.getUsername());
            }
            
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("配置检查 - 自定义加入格式启用: {}, 欢迎消息启用: {}", joinEnabled, welcomeEnabled);
        }
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("首次检测 - {} 是否首次加入网络: {}", player.getUsername(), isFirstTimeJoinNetwork);
        }
            
            if (joinEnabled) {
                if (isFirstTimeJoinNetwork) {
                    // 🎉 首次加入网络：发送加入消息和欢迎消息（如果启用）
                    logger.info("新玩家 {} 首次加入服务器", player.getUsername());
                    
                    // 📢 发送全局首次加入通知
                    sendGlobalFirstJoinMessage(player);
                    
                    // 🎁 发送欢迎消息给玩家（如果启用）
                    if (welcomeEnabled) {
                        sendWelcomeMessageToPlayer(player, "first-time");
                    }
                } else {
                    // 🔄 非首次加入：检查是否为回归玩家，决定发送消息类型
                    if (welcomeEnabled) {
                        // 🏠 回归玩家：发送回归欢迎消息和全局回归通知
                        logger.info("回归玩家 {} 重新加入服务器", player.getUsername());
                        
                        // 📢 发送全局回归通知（让其他玩家知道）
                        sendGlobalReturningMessage(player);
                        
                        // 🎁 发送个人回归欢迎消息
                        sendWelcomeMessageToPlayer(player, "returning");
                    } else {
                        // 📢 普通加入：发送全局加入消息
                        logger.info("玩家 {} 加入了服务器", player.getUsername());
                    sendGlobalJoinMessage(player);
                }
            }
        }
            
            // 清理追踪（延迟）
            schedulePlayerTracking(player.getUniqueId(), recentlyJoinedPlayers, 5000);
            
            debug("已处理玩家加入网络: {}", player.getUsername());
            
        } catch (Exception e) {
            logger.error("处理玩家加入网络失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理玩家离开网络
     */
    private void handlePlayerLeaveNetwork(Player player) {
        try {
            // 添加到最近离开列表
            recentlyLeftPlayers.add(player.getUniqueId());
            
            // 🔥 新增：更新玩家最后见面时间
            updatePlayerLastSeen(player.getUniqueId(), player.getUsername());
            
            // 发送全局离开消息
            if (plugin.getConfigManager().getPluginConfig().isCustomLeaveFormatEnabled()) {
                logger.info("玩家 {} 离开了服务器", player.getUsername());
                sendGlobalLeaveMessage(player);
            }
            
            // 清理所有追踪
            recentlyJoinedPlayers.remove(player.getUniqueId());
            schedulePlayerTracking(player.getUniqueId(), recentlyLeftPlayers, 5000);
            
            debug("已处理玩家离开网络: {}", player.getUsername());
            
        } catch (Exception e) {
            logger.error("处理玩家离开网络失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理玩家切换服务器
     */
    private void handlePlayerSwitchServer(Player player, RegisteredServer from, RegisteredServer to) {
        try {
            // 发送服务器切换消息
            if (plugin.getConfigManager().getPluginConfig().isServerSwitchMessageEnabled()) {
                logger.info("玩家 {} 从 {} 切换到 {}", 
                           player.getUsername(), 
                           from.getServerInfo().getName(), 
                           to.getServerInfo().getName());
                sendServerSwitchMessage(player, from, to);
            }
            
            debug("已处理玩家切换服务器: {} ({} -> {})", 
                player.getUsername(), 
                from.getServerInfo().getName(), 
                to.getServerInfo().getName()
            );
            
        } catch (Exception e) {
            logger.error("处理玩家切换服务器失败: {}", e.getMessage(), e);
        }
    }
    
    // ================================
    // 后端通信处理
    // ================================
    
    /**
     * 处理后端玩家加入消息
     */
    private void handleBackendPlayerJoin(ByteArrayDataInput data, PluginMessageEvent event) {
        try {
            String playerName = data.readUTF();
            data.readLong(); // 读取但不使用 UUID lsb
            data.readLong(); // 读取但不使用 UUID msb
            String serverName = data.readUTF();
            
            verbose("收到后端玩家加入通知: {} (服务器: {})", playerName, serverName);
            
            // 这里可以添加额外的处理逻辑
            // 比如与全局消息系统的协调
            
        } catch (Exception e) {
            logger.error("处理后端玩家加入消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理后端玩家离开消息
     */
    private void handleBackendPlayerLeave(ByteArrayDataInput data, PluginMessageEvent event) {
        try {
            String playerName = data.readUTF();
            data.readLong(); // 读取但不使用 UUID lsb
            data.readLong(); // 读取但不使用 UUID msb
            String serverName = data.readUTF();
            
            verbose("收到后端玩家离开通知: {} (服务器: {})", playerName, serverName);
            
            // 这里可以添加额外的处理逻辑
            
        } catch (Exception e) {
            logger.error("处理后端玩家离开消息失败: {}", e.getMessage(), e);
        }
    }
    
    // ================================
    // 消息发送
    // ================================
    
    /**
     * 发送全局加入消息
     */
    private void sendGlobalJoinMessage(Player player) {
        try {
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("消息处理 - 开始处理玩家 {} 的加入消息", player.getUsername());
        }
            
            // 确定玩家权限组
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("权限组 - 玩家 {} 的权限组: {}", player.getUsername(), permissionGroup);
        }
            
            // 获取消息模板（使用权限组和默认类型）
            String messageTemplate = plugin.getConfigManager().getMessageConfig().getJoinMessage(permissionGroup, "default");
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("消息模板 - 获取到模板: {}", messageTemplate);
            }
            
            // 格式化消息
            String formattedMessage = plugin.getConfigManager().getMessageConfig()
                .formatMessage(messageTemplate, player.getUsername());
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("格式化 - 格式化后的消息: {}", formattedMessage);
            }
            
            // 检查服务器数量
            int serverCount = proxyServer.getAllServers().size();
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("服务器 - 当前有 {} 个服务器", serverCount);
            }
            
            if (serverCount == 0) {
                logger.warn("警告 - 没有可用的服务器来发送消息");
                return;
            }
            
            // 发送到所有服务器
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("开始广播 - 准备发送消息到所有服务器...");
            }
            sendMessageToAll(formattedMessage);
            
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("发送完成 - 已发送全局加入消息: {} ({})", player.getUsername(), permissionGroup);
            }
            
        } catch (Exception e) {
            logger.error("发送全局加入消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送全局离开消息
     */
    private void sendGlobalLeaveMessage(Player player) {
        try {
            // 确定玩家权限组
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            
            // 获取消息模板（使用权限组和默认类型）
            String messageTemplate = plugin.getConfigManager().getMessageConfig().getLeaveMessage(permissionGroup, "default");
            
            // 格式化消息
            String formattedMessage = plugin.getConfigManager().getMessageConfig()
                .formatMessage(messageTemplate, player.getUsername());
            
            // 发送到所有服务器
            sendMessageToAll(formattedMessage);
            
            debug("已发送全局离开消息: {} ({})", player.getUsername(), permissionGroup);
            
        } catch (Exception e) {
            logger.error("发送全局离开消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送服务器切换消息
     */
    private void sendServerSwitchMessage(Player player, RegisteredServer from, RegisteredServer to) {
        try {
            // 确定玩家权限组
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            
            // 获取消息模板（使用权限组和默认类型）
            String messageTemplate = plugin.getConfigManager().getMessageConfig().getServerSwitchMessage(permissionGroup, "default");
            
            // 获取服务器别名
            String fromServerAlias = plugin.getConfigManager().getPluginConfig()
                .getServerAlias(from.getServerInfo().getName());
            String toServerAlias = plugin.getConfigManager().getPluginConfig()
                .getServerAlias(to.getServerInfo().getName());
            
            // 格式化消息（使用服务器别名）
            String formattedMessage = plugin.getConfigManager().getMessageConfig()
                .formatSwitchMessage(
                    messageTemplate, 
                    player.getUsername(), 
                    fromServerAlias, 
                    toServerAlias
                );
            
            // 根据配置决定发送范围
            if (plugin.getConfigManager().getPluginConfig().isServerSwitchShowToAll()) {
                sendMessageToAll(formattedMessage);
            } else {
                // 只发送给来源和目标服务器
                sendMessageToServer(from.getServerInfo().getName(), formattedMessage);
                sendMessageToServer(to.getServerInfo().getName(), formattedMessage);
            }
            
            debug("已发送服务器切换消息: {} ({}) ({} -> {})", 
                player.getUsername(), 
                permissionGroup,
                from.getServerInfo().getName(), 
                to.getServerInfo().getName()
            );
            
        } catch (Exception e) {
            logger.error("发送服务器切换消息失败: {}", e.getMessage(), e);
        }
    }
    
    // ================================
    // 消息类型判断
    // ================================
    
    /**
     * 🔥 修复：判断是否是首次加入网络（支持持久化）
     * 代理端的首次加入判断逻辑
     */
    private boolean isFirstTimeJoinNetwork(Player player) {
        // 检查配置是否启用首次加入功能
        if (!plugin.getConfigManager().getPluginConfig().isFirstJoinEnabled()) {
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("首次检测 - 首次加入功能已禁用");
        }
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        String playerName = player.getUsername();
        
        if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("首次检测 - 开始检测 {} 是否首次加入网络", playerName);
        }
        
        // 🔥 步骤1：检查持久化数据
        boolean hasPersistedRecord = hasPlayerRecord(playerId);
        if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("  持久化记录存在: {}", hasPersistedRecord);
        }
        
        if (hasPersistedRecord) {
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("首次检测 - {} 在持久化数据中存在，不是首次加入", playerName);
            }
            return false;
        }
        
        // 🔥 步骤2：检查内存追踪（仅作为辅助）
        boolean recentlyJoined = recentlyJoinedPlayers.contains(playerId);
        boolean recentlyLeft = recentlyLeftPlayers.contains(playerId);
        
        if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("  最近加入列表中: {}", recentlyJoined);
            logger.info("  最近离开列表中: {}", recentlyLeft);
        }
        
        if (recentlyLeft) {
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("首次检测 - {} 在最近离开列表中，不是首次加入", playerName);
            }
            return false;
        }
        
        // 🔥 步骤3：确认是首次加入，记录到持久化数据
        boolean isFirstTime = true;
        if (isFirstTime) {
            savePlayerRecord(playerId, playerName);
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("持久化 - 已保存 {} 的首次加入记录", playerName);
            }
        }
        
        if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("首次检测 - {} 判断结果: {}", playerName, isFirstTime);
        }
        return isFirstTime;
    }
    
    /**
     * 检查玩家是否有持久化记录
     */
    private boolean hasPlayerRecord(UUID playerId) {
        try {
            if (!playerDataFile.exists()) {
                if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("持久化 - 玩家数据文件不存在，创建新文件");
        }
                return false;
            }
            
            String jsonContent = Files.readString(playerDataFile.toPath());
            if (jsonContent.trim().isEmpty()) {
                if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("持久化 - 玩家数据文件为空");
            }
                return false;
            }
            
            JsonObject playersData = JsonParser.parseString(jsonContent).getAsJsonObject();
            boolean hasRecord = playersData.has(playerId.toString());
            
            if (hasRecord) {
                JsonObject playerData = playersData.getAsJsonObject(playerId.toString());
                String name = playerData.get("name").getAsString();
                long firstJoinTime = playerData.get("firstJoinTime").getAsLong();
                
                if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                    logger.info("持久化 - 找到 {} 的记录，首次加入时间: {}", 
                               name, Instant.ofEpochMilli(firstJoinTime));
                }
            }
            
            return hasRecord;
            
        } catch (Exception e) {
            logger.error("读取玩家数据失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 保存玩家记录到持久化文件
     */
    private void savePlayerRecord(UUID playerId, String playerName) {
        try {
            JsonObject playersData;
            
            // 读取现有数据
            if (playerDataFile.exists() && playerDataFile.length() > 0) {
                String jsonContent = Files.readString(playerDataFile.toPath());
                if (!jsonContent.trim().isEmpty()) {
                    playersData = JsonParser.parseString(jsonContent).getAsJsonObject();
                } else {
                    playersData = new JsonObject();
                }
            } else {
                playersData = new JsonObject();
            }
            
            // 添加玩家记录
            JsonObject playerRecord = new JsonObject();
            playerRecord.addProperty("name", playerName);
            playerRecord.addProperty("firstJoinTime", System.currentTimeMillis());
            playerRecord.addProperty("lastSeen", System.currentTimeMillis());
            
            playersData.add(playerId.toString(), playerRecord);
            
            // 保存到文件
            try (FileWriter writer = new FileWriter(playerDataFile)) {
                gson.toJson(playersData, writer);
            }
            
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("持久化 - 成功保存 {} 的记录", playerName);
            }
            
        } catch (Exception e) {
            logger.error("保存玩家数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 更新玩家最后见面时间
     */
    private void updatePlayerLastSeen(UUID playerId, String playerName) {
        try {
            if (!playerDataFile.exists()) {
                return;
            }
            
            String jsonContent = Files.readString(playerDataFile.toPath());
            if (jsonContent.trim().isEmpty()) {
                return;
            }
            
            JsonObject playersData = JsonParser.parseString(jsonContent).getAsJsonObject();
            if (playersData.has(playerId.toString())) {
                JsonObject playerRecord = playersData.getAsJsonObject(playerId.toString());
                playerRecord.addProperty("lastSeen", System.currentTimeMillis());
                playerRecord.addProperty("name", playerName); // 更新名称
                
                // 保存到文件
                try (FileWriter writer = new FileWriter(playerDataFile)) {
                    gson.toJson(playersData, writer);
                }
                
                if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                    logger.info("持久化 - 更新 {} 的最后见面时间", playerName);
                }
            }
            
        } catch (Exception e) {
            logger.error("更新玩家数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取玩家最后见面时间（毫秒时间戳，转换为秒）
     */
    private long getPlayerLastSeenTime(UUID playerId) {
        try {
            if (!playerDataFile.exists()) {
                return 0;
            }
            
            String jsonContent = Files.readString(playerDataFile.toPath());
            if (jsonContent.trim().isEmpty()) {
                return 0;
            }
            
            JsonObject playersData = JsonParser.parseString(jsonContent).getAsJsonObject();
            if (playersData.has(playerId.toString())) {
                JsonObject playerRecord = playersData.getAsJsonObject(playerId.toString());
                if (playerRecord.has("lastSeen")) {
                    long lastSeenMillis = playerRecord.get("lastSeen").getAsLong();
                    return lastSeenMillis / 1000; // 转换为秒
                }
            }
            
            return 0; // 没有记录
            
        } catch (Exception e) {
            logger.error("读取玩家最后见面时间失败: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 发送欢迎消息给玩家（明确指定消息类型）
     */
     private void sendWelcomeMessageToPlayer(Player player, String messageType) {
        try {
            // 根据消息类型确定延迟
            int delay = "first-time".equals(messageType) ? 
                plugin.getConfigManager().getPluginConfig().getFirstTimeWelcomeDelay() : 
                plugin.getConfigManager().getPluginConfig().getFirstTimeWelcomeDelay(); // 使用相同延迟
            
            // 确定玩家权限组
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("欢迎消息 - {} 的消息类型: {}, 权限组: {}", player.getUsername(), messageType, permissionGroup);
            }
            
            // 获取欢迎消息模板（基于权限组）
            String welcomeTemplate = plugin.getConfigManager().getMessageConfig().getWelcomeMessage(permissionGroup, messageType);
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("欢迎模板: {}", welcomeTemplate);
            }
            
            // 检查消息模板是否有效
            if (welcomeTemplate == null || welcomeTemplate.trim().isEmpty()) {
                if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                    logger.info("欢迎消息模板为空，跳过发送: {}", messageType);
                }
                return;
            }
            
            // 格式化消息（增强版，支持更多占位符）
            String formattedWelcome = formatWelcomeMessage(welcomeTemplate, player, messageType);
            
            // 延迟发送欢迎消息给玩家
            proxyServer.getScheduler().buildTask(platformInstance, () -> {
                if (formattedWelcome != null && !formattedWelcome.trim().isEmpty()) {
                sendMessageToPlayer(player, formattedWelcome);
                // 静默发送完成
                }
            }).delay(delay, java.util.concurrent.TimeUnit.MILLISECONDS).schedule();
            
        } catch (Exception e) {
            logger.error("发送欢迎消息失败", e);
        }
    }
    
    /**
     * 发送全局首次加入消息（通知所有玩家有新玩家加入）
     */
    private void sendGlobalFirstJoinMessage(Player player) {
        try {
            // 确定玩家权限组
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            
            // 发送给所有其他玩家的欢迎通知
            String notificationTemplate = plugin.getConfigManager().getMessageConfig().getJoinMessage(permissionGroup, "first-time");
            if (notificationTemplate != null && !notificationTemplate.isEmpty()) {
                String formattedNotification = plugin.getConfigManager().getMessageConfig()
                    .formatMessage(notificationTemplate, player.getUsername());
                
                // 发送给所有其他玩家
                for (Player otherPlayer : proxyServer.getAllPlayers()) {
                    if (!otherPlayer.getUniqueId().equals(player.getUniqueId())) {
                        sendMessageToPlayer(otherPlayer, formattedNotification);
                    }
                }
                
                if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                    logger.info("首次通知 - 已通知所有玩家: {} ({}) 首次加入", player.getUsername(), permissionGroup);
                }
            }
            
        } catch (Exception e) {
            logger.error("发送首次加入通知失败", e);
        }
    }
    
    /**
     * 发送全局回归消息（通知所有玩家有回归玩家加入）
     */
    private void sendGlobalReturningMessage(Player player) {
        try {
            // 确定玩家权限组
            String permissionGroup = PermissionUtil.getPlayerPermissionGroup(player);
            
            // 发送给所有其他玩家的回归通知
            String notificationTemplate = plugin.getConfigManager().getMessageConfig().getJoinMessage(permissionGroup, "returning");
            if (notificationTemplate != null && !notificationTemplate.isEmpty()) {
                String formattedNotification = plugin.getConfigManager().getMessageConfig()
                    .formatMessage(notificationTemplate, player.getUsername());
                
                // 发送给所有其他玩家
                for (Player otherPlayer : proxyServer.getAllPlayers()) {
                    if (!otherPlayer.getUniqueId().equals(player.getUniqueId())) {
                        sendMessageToPlayer(otherPlayer, formattedNotification);
                    }
                }
                
                if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                    logger.info("回归通知 - 已通知所有玩家: {} ({}) 回归加入", player.getUsername(), permissionGroup);
                }
            }
            
        } catch (Exception e) {
            logger.error("发送回归加入通知失败", e);
        }
    }
    

    
    /**
     * 格式化欢迎消息（支持 Velocity 特定占位符）
     */
    private String formatWelcomeMessage(String template, Player player, String messageType) {
        try {
            // 基础格式化
            String formatted = plugin.getConfigManager().getMessageConfig()
                .formatMessage(template, player.getUsername());
            
            // Velocity 环境特定占位符
            
            // {online_count} 和 {max_players}
            int onlineCount = proxyServer.getAllPlayers().size();
            int maxPlayers = proxyServer.getConfiguration().getShowMaxPlayers();
            formatted = formatted.replace("{online_count}", String.valueOf(onlineCount));
            formatted = formatted.replace("{max_players}", String.valueOf(maxPlayers));
            
            // {last_seen} 占位符（仅回归玩家有效）
            if ("returning".equals(messageType)) {
                String lastSeenTime = getFormattedLastSeenTime(player.getUniqueId());
                formatted = formatted.replace("{last_seen}", lastSeenTime);
            } else {
                // 对于非回归消息，移除 {last_seen} 占位符
                formatted = formatted.replace("{last_seen}", "");
            }
            
            return formatted;
            
        } catch (Exception e) {
            logger.error("格式化欢迎消息失败: {}", e.getMessage(), e);
            return template; // 返回原始模板
        }
    }
    
    /**
     * 获取格式化的最后见面时间
     */
    private String getFormattedLastSeenTime(java.util.UUID playerId) {
        try {
            long lastSeenSeconds = getPlayerLastSeenTime(playerId);
            if (lastSeenSeconds == 0) {
                return "未知";
            }
            
            // 将时间戳转换为友好格式
            long currentSeconds = System.currentTimeMillis() / 1000;
            long offlineSeconds = currentSeconds - lastSeenSeconds;
            
            return formatTimeAgo(offlineSeconds);
            
        } catch (Exception e) {
            logger.error("格式化最后见面时间失败: {}", e.getMessage(), e);
            return "未知";
        }
    }
    
    /**
     * 将秒数转换为 "X天前", "X小时前" 等格式
     */
    private String formatTimeAgo(long seconds) {
        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) { // 小于1小时
            long minutes = seconds / 60;
            return minutes + "分钟前";
        } else if (seconds < 86400) { // 小于1天
            long hours = seconds / 3600;
            return hours + "小时前";
        } else if (seconds < 2592000) { // 小于30天
            long days = seconds / 86400;
            return days + "天前";
        } else if (seconds < 31536000) { // 小于1年
            long months = seconds / 2592000;
            return months + "个月前";
        } else {
            long years = seconds / 31536000;
            return years + "年前";
        }
    }
    
    /**
     * 判断是否应该显示回归消息
     */
    private boolean shouldShowReturningMessage(Player player) {
        try {
            long thresholdSeconds = plugin.getConfigManager().getPluginConfig().getReturningThreshold();
            long lastSeenTime = getPlayerLastSeenTime(player.getUniqueId());
            
            if (lastSeenTime == 0) {
                // 没有记录的最后见面时间，不显示回归消息
                return false;
            }
            
            long currentTime = System.currentTimeMillis() / 1000; // 转换为秒
            long offlineTime = currentTime - lastSeenTime;
            
            return offlineTime >= thresholdSeconds;
        } catch (Exception e) {
            logger.error("检查回归消息条件失败: {}", e.getMessage(), e);
            return false;
        }
    }
    

    
    // ================================
    // 实现抽象方法
    // ================================
    
    @Override
    public void sendMessageToPlayer(Object player, String message) {
        if (player instanceof Player) {
            Player velocityPlayer = (Player) player;
            Component component = miniMessage.deserialize(message);
            velocityPlayer.sendMessage(component);
        }
    }
    
    @Override
    public void sendMessageToAll(String message) {
        try {
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("组件转换 - 开始转换消息: {}", message);
        }
            Component component = miniMessage.deserialize(message);
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
            logger.info("组件转换 - 消息转换成功");
        }
            
            // 获取所有在线玩家
            int totalPlayers = proxyServer.getPlayerCount();
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("玩家统计 - 代理上总共有 {} 个在线玩家", totalPlayers);
            }
            
            if (totalPlayers == 0) {
                logger.warn("警告 - 没有在线玩家，跳过消息发送");
                return;
            }
            
            // 🔥 修复重复消息问题：只使用一种发送方式
            // 直接发送给所有在线玩家（避免重复发送）
            int sentToPlayersCount = 0;
            for (Player onlinePlayer : proxyServer.getAllPlayers()) {
                onlinePlayer.sendMessage(component);
                sentToPlayersCount++;
                if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("发送给玩家 - 发送消息给: {}", onlinePlayer.getUsername());
            }
            }
            
            if (plugin.getConfigManager().getPluginConfig().isVerboseLogging()) {
                logger.info("广播完成 - 已发送给 {} 个玩家", sentToPlayersCount);
        }
            
        } catch (Exception e) {
            logger.error("发送消息失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void sendMessageToServer(String serverName, String message) {
        proxyServer.getServer(serverName).ifPresent(server -> {
            Component component = miniMessage.deserialize(message);
            server.sendMessage(component);
        });
    }
    
    @Override
    public int getOnlinePlayerCount() {
        return proxyServer.getPlayerCount();
    }
    
    @Override
    public boolean isPlayerOnline(String playerName) {
        return proxyServer.getPlayer(playerName).isPresent();
    }
    
    @Override
    public String getAdapterType() {
        return "Velocity Proxy";
    }
    
    @Override
    public String getPlatformVersion() {
        return proxyServer.getVersion().getVersion();
    }
    
    // ================================
    // 辅助方法
    // ================================
    
    /**
     * 调度玩家追踪清理
     */
    private void schedulePlayerTracking(UUID playerUUID, Set<UUID> trackingSet, int delayMs) {
        proxyServer.getScheduler().buildTask(platformInstance, () -> {
            trackingSet.remove(playerUUID);
            verbose("已清理玩家追踪: {}", playerUUID);
        }).delay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS).schedule();
    }
}