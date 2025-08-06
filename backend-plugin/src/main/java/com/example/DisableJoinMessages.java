package com.example;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 禁用默认加入/离开消息的后端服务器插件
 * 配合Velocity的CustomJoinMessage插件使用
 */
public class DisableJoinMessages extends JavaPlugin implements Listener, CommandExecutor {
    
    private boolean disableMessages = true;
    
    @Override
    public void onEnable() {
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);
        
        // 注册命令
        getCommand("disablejoin").setExecutor(this);
        
        // 从配置文件读取设置
        saveDefaultConfig();
        disableMessages = getConfig().getBoolean("disable-messages", true);
        
        getLogger().info("DisableJoinMessages 插件已启用！");
        getLogger().info("默认加入/离开消息已" + (disableMessages ? "禁用" : "启用"));
        getLogger().info("这个插件配合Velocity的CustomJoinMessage使用");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("DisableJoinMessages 插件已禁用！");
    }
    
    /**
     * 拦截玩家加入事件，禁用默认消息
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (disableMessages) {
            getLogger().info("禁用玩家 " + event.getPlayer().getName() + " 的加入消息");
            event.setJoinMessage(null); // 完全禁用加入消息
        }
    }
    
    /**
     * 拦截玩家离开事件，禁用默认消息  
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (disableMessages) {
            getLogger().info("禁用玩家 " + event.getPlayer().getName() + " 的离开消息");
            event.setQuitMessage(null); // 完全禁用离开消息
        }
    }
    
    /**
     * 命令处理器
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("disablejoin")) {
            return false;
        }
        
        if (!sender.hasPermission("disablejoin.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用这个命令！");
            return true;
        }
        
        if (args.length == 0) {
            // 显示当前状态
            String status = disableMessages ? "已禁用" : "已启用";
            sender.sendMessage(ChatColor.YELLOW + "默认加入/离开消息当前状态: " + ChatColor.GREEN + status);
            sender.sendMessage(ChatColor.GRAY + "使用 /disablejoin [on|off] 来切换状态");
            return true;
        }
        
        if (args.length == 1) {
            String action = args[0].toLowerCase();
            
            switch (action) {
                case "on":
                case "enable":
                case "true":
                    disableMessages = true;
                    getConfig().set("disable-messages", true);
                    saveConfig();
                    sender.sendMessage(ChatColor.GREEN + "已禁用默认加入/离开消息");
                    getLogger().info(sender.getName() + " 启用了消息禁用功能");
                    break;
                    
                case "off":
                case "disable":
                case "false":
                    disableMessages = false;
                    getConfig().set("disable-messages", false);
                    saveConfig();
                    sender.sendMessage(ChatColor.YELLOW + "已启用默认加入/离开消息");
                    getLogger().info(sender.getName() + " 禁用了消息禁用功能");
                    break;
                    
                case "status":
                case "info":
                    String currentStatus = disableMessages ? "禁用" : "启用";
                    sender.sendMessage(ChatColor.YELLOW + "当前状态: " + ChatColor.GREEN + currentStatus);
                    break;
                    
                default:
                    sender.sendMessage(ChatColor.RED + "无效参数！使用 on, off, 或 status");
                    return true;
            }
            return true;
        }
        
        sender.sendMessage(ChatColor.RED + "用法: /disablejoin [on|off|status]");
        return true;
    }
}