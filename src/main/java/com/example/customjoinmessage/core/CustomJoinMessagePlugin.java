package com.example.customjoinmessage.core;


import com.example.customjoinmessage.config.ConfigManager;
import com.example.customjoinmessage.platform.AbstractPlatformAdapter;
import com.example.customjoinmessage.utils.PermissionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.io.File;

/**
 * CustomJoinMessage 统一主插件类
 * 
 * 智能检测运行环境并选择合适的平台适配器
 * 支持 Velocity/BungeeCord + Paper/Spigot/Folia
 */
public class CustomJoinMessagePlugin {
    
    private static final Logger logger = LoggerFactory.getLogger("CustomJoinMessage");
    
    // 插件信息
    public static final String PLUGIN_ID = "customjoinmessage";
    public static final String PLUGIN_NAME = "CustomJoinMessage";
    public static final String PLUGIN_VERSION = "2.0.0";
    
    // 通信通道
    public static final String COMMUNICATION_CHANNEL = "customjoinmessage:sync";
    
    // 核心组件
    private final PlatformDetector.PlatformType platformType;
    private volatile PluginMode pluginMode; // 改为volatile，支持动态修改
    private final ConfigManager configManager;
    private final AbstractPlatformAdapter platformAdapter;
    

    
    // 插件实例（根据平台类型可能是不同的对象）
    private final Object pluginInstance;
    private final Path dataDirectory;
    
    // 静态实例（用于全局访问）
    private static CustomJoinMessagePlugin instance;
    
    /**
     * 构造函数 - 初始化插件
     * 
     * @param pluginInstance 平台特定的插件实例
     * @param dataDirectory 数据目录
     */
    public CustomJoinMessagePlugin(Object pluginInstance, Path dataDirectory) {
        instance = this;
        
        this.pluginInstance = pluginInstance;
        this.dataDirectory = dataDirectory;
        
        // 检测平台类型
        this.platformType = PlatformDetector.detectPlatform();
        
        // 初始化配置管理器
        this.configManager = new ConfigManager(dataDirectory);
        

        
        // 先设置为临时默认模式，配置加载后重新决定
        this.pluginMode = PluginMode.BACKEND_SLAVE;
        
        // 创建平台适配器
        this.platformAdapter = createPlatformAdapter();
    }
    
    /**
     * 插件启用
     */
    public void onEnable() {
        try {
            // 加载配置
            configManager.loadConfigs();
            
            // 配置加载后重新决定运行模式
            this.pluginMode = determineInitialMode();
            
            // 打印启动信息（只打印一次，显示正确的模式）
            printStartupInfo();
            
            // 初始化智能权限检测
            PermissionUtil.initializePermissions(configManager.getPluginConfig());
            
            // 启用平台适配器
            platformAdapter.onEnable();
            
            // 注册常规通信通道
            platformAdapter.registerCommunicationChannel();
            

            
        } catch (Exception e) {
            logger.error("插件启动失败: {}", e.getMessage(), e);
            onDisable();
        }
    }

    
    /**
     * 插件禁用
     */
    public void onDisable() {
        try {
            // 静默关闭
            
            // 禁用平台适配器
            if (platformAdapter != null) {
                platformAdapter.onDisable();
            }
            
            // 保存配置
            if (configManager != null) {
                configManager.saveConfigs();
            }
            
            // 静默关闭完成
            
        } catch (Exception e) {
            logger.error("插件关闭时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 重新加载插件
     */
    public void reload() {
        try {
            // 静默重新加载
            
            // 重新加载配置
            configManager.loadConfigs();
            
            // 重新加载适配器
            platformAdapter.reload();
            
            logger.info("配置已重新加载");
            
        } catch (Exception e) {
            logger.error("重新加载失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 决定初始运行模式（基于平台类型和配置文件）
     */
    private PluginMode determineInitialMode() {
        // 代理端强制使用主控模式，忽略配置文件
        if (platformType.isProxy()) {
            if (configManager.getPluginConfig().isDebug()) {
                logger.info("模式决定: 检测到代理平台，强制启用代理主控模式");
            }
            return PluginMode.PROXY_MASTER;
        }
        
        // 后端服务器根据配置文件决定模式
        String configMode = configManager.getPluginConfig().getMode();
        
        switch (configMode.toLowerCase()) {
            case "standalone":
                return PluginMode.BACKEND_STANDALONE;
            case "backend":
                return PluginMode.BACKEND_SLAVE;
            case "auto":  // 兼容旧配置，默认为 backend 模式
                if (configManager.getPluginConfig().isDebug()) {
                    logger.info("'auto' 模式已废弃，自动转换为后端从属模式");
                }
                return PluginMode.BACKEND_SLAVE;
            default:
                // 后端无效配置默认为从属模式
                logger.warn("无效的配置模式 '{}', 默认为后端从属模式", configMode);
                return PluginMode.BACKEND_SLAVE;
        }
    }
    
    /**
     * 动态切换运行模式
     */
    public void switchMode(PluginMode newMode) {
        if (this.pluginMode == newMode) {
            logger.debug("模式切换: 目标模式与当前模式相同，无需切换");
            return;
        }
        
        PluginMode oldMode = this.pluginMode;
        logger.info("模式切换: {} -> {}", oldMode.getDisplayName(), newMode.getDisplayName());
        
        try {
            // 停止当前适配器
            if (platformAdapter != null) {
                platformAdapter.onDisable();
            }
            
            // 切换模式
            this.pluginMode = newMode;
            
            // 重新启用适配器
            if (platformAdapter != null) {
                platformAdapter.onEnable();
            }
            
            logger.info("模式切换完成: 现在运行于{}", newMode.getDisplayName());
            
        } catch (Exception e) {
            logger.error("模式切换失败: {}", e.getMessage(), e);
            // 回滚到原模式
            this.pluginMode = oldMode;
        }
    }
    
    
    /**
     * 创建平台适配器 - 使用反射动态加载，避免类加载冲突
     */
    private AbstractPlatformAdapter createPlatformAdapter() {
        String adapterClassName;
        
        switch (platformType) {
            case VELOCITY:
                adapterClassName = "com.example.customjoinmessage.platform.proxy.VelocityAdapter";
                break;
                
            case BUNGEECORD:
                adapterClassName = "com.example.customjoinmessage.platform.proxy.BungeeCordAdapter";
                break;
                
            case PAPER:
                adapterClassName = "com.example.customjoinmessage.platform.backend.PaperAdapter";
                break;
                
            case FOLIA:
                adapterClassName = "com.example.customjoinmessage.platform.backend.FoliaAdapter";
                break;
                
            case SPIGOT:
            case CRAFTBUKKIT:
                adapterClassName = "com.example.customjoinmessage.platform.backend.SpigotAdapter";
                break;
                
            default:
                throw new UnsupportedOperationException(
                    "不支持的平台类型: " + platformType.getDisplayName()
                );
        }
        
        try {
            // 使用反射动态创建适配器，避免在编译时加载所有平台的类
            Class<?> adapterClass = Class.forName(adapterClassName);
            
            // 获取构造函数
            java.lang.reflect.Constructor<?> constructor = adapterClass.getConstructor(
                CustomJoinMessagePlugin.class, 
                Object.class
            );
            
            // 创建实例
            Object adapterInstance = constructor.newInstance(this, pluginInstance);
            
            return (AbstractPlatformAdapter) adapterInstance;
            
        } catch (Exception e) {
            logger.error("创建平台适配器失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法创建平台适配器: " + platformType.getDisplayName(), e);
        }
    }
    
    /**
     * 打印启动信息
     */
    private void printStartupInfo() {
        // 显示简洁的启动信息
        logger.info("CustomJoinMessage 启动中...");
        logger.info("平台: {} | 模式: {}", 
                   platformType.getDisplayName(), 
                   pluginMode.getDisplayName());
        logger.info("CustomJoinMessage 启动完成");
    }
    
    // ================================
    // Getter 方法
    // ================================
    
    public static CustomJoinMessagePlugin getInstance() {
        return instance;
    }
    
    public PlatformDetector.PlatformType getPlatformType() {
        return platformType;
    }
    
    public PluginMode getPluginMode() {
        return pluginMode;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public AbstractPlatformAdapter getPlatformAdapter() {
        return platformAdapter;
    }
    
    public Object getPluginInstance() {
        return pluginInstance;
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * 获取数据文件夹（File类型，兼容性方法）
     */
    public File getDataFolder() {
        return dataDirectory.toFile();
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    // ================================
    // 便捷方法
    // ================================
    
    /**
     * 是否为代理服务器
     */
    public boolean isProxy() {
        return platformType.isProxy();
    }
    
    /**
     * 是否为后端服务器
     */
    public boolean isBackend() {
        return platformType.isBackend();
    }
    
    /**
     * 获取插件状态信息
     */
    public String getStatusInfo() {
        return String.format(
            "%s v%s [%s] - %s",
            PLUGIN_NAME,
            PLUGIN_VERSION,
            platformType.getDisplayName(),
            pluginMode.getDisplayName()
        );
    }
    
    /**
     * 检查功能是否启用
     */
    public boolean isFeatureEnabled(String feature) {
        return configManager.getPluginConfig().isFeatureEnabled(feature);
    }
}