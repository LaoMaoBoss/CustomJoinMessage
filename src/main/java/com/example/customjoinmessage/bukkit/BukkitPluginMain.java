package com.example.customjoinmessage.bukkit;

import com.example.customjoinmessage.core.CustomJoinMessagePlugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit/Paper/Spigot/Folia 插件入口点
 * 
 * 这个类负责在Bukkit系列平台上启动我们的统一插件
 */
public class BukkitPluginMain extends JavaPlugin {
    
    // 我们的统一插件实例
    private CustomJoinMessagePlugin customJoinMessagePlugin;
    
    /**
     * 插件启用
     */
    @Override
    public void onEnable() {
        try {
            // 静默启动
            
            // 创建统一插件实例
            customJoinMessagePlugin = new CustomJoinMessagePlugin(this, getDataFolder().toPath());
            
            // 启用插件
            customJoinMessagePlugin.onEnable();
            
            // 静默启动完成
            
        } catch (Exception e) {
            getLogger().severe("CustomJoinMessage for Bukkit 启动失败: " + e.getMessage());
            e.printStackTrace();
            
            // 禁用插件
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * 插件禁用
     */
    @Override
    public void onDisable() {
        try {
            // 静默关闭
            
            // 禁用插件
            if (customJoinMessagePlugin != null) {
                customJoinMessagePlugin.onDisable();
            }
            
            // 静默关闭完成
            
        } catch (Exception e) {
            getLogger().severe("CustomJoinMessage for Bukkit 关闭时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ================================
    // Getter 方法（给适配器使用）
    // ================================
    
    public CustomJoinMessagePlugin getCustomJoinMessagePlugin() {
        return customJoinMessagePlugin;
    }
}