package com.example.customjoinmessage.bungee;

import com.example.customjoinmessage.core.CustomJoinMessagePlugin;

import net.md_5.bungee.api.plugin.Plugin;

import java.nio.file.Path;

/**
 * BungeeCord 插件主类
 * 
 * 处理 BungeeCord 环境下的插件生命周期
 */
public class BungeeCordPluginMain extends Plugin {
    
    private CustomJoinMessagePlugin corePlugin;
    
    @Override
    public void onEnable() {
        try {
            // 获取数据目录
            Path dataDirectory = getDataFolder().toPath();
            
            // 创建核心插件实例
            corePlugin = new CustomJoinMessagePlugin(this, dataDirectory);
            
            // 启用插件
            corePlugin.onEnable();
            
            getLogger().info("CustomJoinMessage v" + CustomJoinMessagePlugin.PLUGIN_VERSION + " 已在 BungeeCord 上启用");
            
        } catch (Exception e) {
            getLogger().severe("插件启用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (corePlugin != null) {
                corePlugin.onDisable();
            }
            
            getLogger().info("CustomJoinMessage 已在 BungeeCord 上禁用");
            
        } catch (Exception e) {
            getLogger().severe("插件禁用时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取核心插件实例
     */
    public CustomJoinMessagePlugin getCorePlugin() {
        return corePlugin;
    }
}