package com.example.customjoinmessage.velocity;

import com.example.customjoinmessage.core.CustomJoinMessagePlugin;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Velocity 插件入口点
 * 
 * 这个类负责在Velocity平台上启动我们的统一插件
 */
@Plugin(
    id = "customjoinmessage",
    name = "CustomJoinMessage",
    version = "2.0.0",
    description = "Universal custom join and leave messages for Velocity/BungeeCord + Paper/Spigot/Folia",
    authors = {"CustomJoinMessage Team"}
)
public class VelocityPluginMain {
    
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    // 我们的统一插件实例
    private CustomJoinMessagePlugin customJoinMessagePlugin;
    
    /**
     * 构造函数 - Velocity依赖注入
     */
    @Inject
    public VelocityPluginMain(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }
    
    /**
     * 代理初始化事件
     */
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            // 静默启动
            
            // 创建统一插件实例 - 传递插件实例，适配器需要插件实例来注册事件
            customJoinMessagePlugin = new CustomJoinMessagePlugin(this, dataDirectory);
            
            // 启用插件
            customJoinMessagePlugin.onEnable();
            
            // 静默启动完成
            
        } catch (Exception e) {
            logger.error("CustomJoinMessage for Velocity 启动失败", e);
        }
    }
    
    /**
     * 代理关闭事件
     */
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        try {
            // 静默关闭
            
            // 禁用插件
            if (customJoinMessagePlugin != null) {
                customJoinMessagePlugin.onDisable();
            }
            
            // 静默关闭完成
            
        } catch (Exception e) {
            logger.error("CustomJoinMessage for Velocity 关闭时出错", e);
        }
    }
    
    // ================================
    // Getter 方法（给适配器使用）
    // ================================
    
    public ProxyServer getServer() {
        return server;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    public CustomJoinMessagePlugin getCustomJoinMessagePlugin() {
        return customJoinMessagePlugin;
    }
}