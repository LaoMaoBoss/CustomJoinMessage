package com.example.customjoinmessage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 统一配置管理器
 * 
 * 负责加载、保存和管理所有配置文件
 */
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger("CustomJoinMessage");
    
    private final Path dataDirectory;
    private final Yaml yaml;
    
    // 配置对象
    private PluginConfig pluginConfig;
    private MessageConfig messageConfig;
    
    public ConfigManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.yaml = new Yaml();
    }
    
    /**
     * 加载所有配置文件
     */
    public void loadConfigs() {
        try {
            // 确保数据目录存在
            if (Files.notExists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            
            // 加载统一配置（包含插件设置和消息模板）
            loadUnifiedConfig();
            
            // 静默完成，不输出日志
            
        } catch (Exception e) {
            logger.error("配置文件加载失败: {}", e.getMessage(), e);
            // 使用默认配置
            useDefaultConfigs();
        }
    }
    
    /**
     * 保存所有配置文件
     */
    public void saveConfigs() {
        try {
            // 这里可以实现配置保存逻辑
            // 当前版本先跳过，因为我们主要读取配置
            logger.debug("配置保存功能待实现");
            
        } catch (Exception e) {
            logger.error("配置文件保存失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 加载统一配置文件（包含插件设置和消息模板）
     */
    private void loadUnifiedConfig() throws IOException {
        Path configFile = dataDirectory.resolve("config.yml");
        
        // 如果配置文件不存在，复制默认配置
        if (Files.notExists(configFile)) {
            copyDefaultConfig("config.yml", configFile);
        }
        
        // 加载统一配置
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            Map<String, Object> configData = yaml.load(inputStream);
            
            // 创建插件配置对象
            this.pluginConfig = new PluginConfig(configData);
            
            // 创建消息配置对象（从同一个配置文件中读取messages部分）
            this.messageConfig = new MessageConfig(configData);
        }
    }
    
    /**
     * 复制默认配置文件
     */
    private void copyDefaultConfig(String resourceName, Path targetFile) throws IOException {
        try (InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(resourceName)) {
            
            if (resourceStream == null) {
                throw new IOException("无法找到默认配置文件: " + resourceName);
            }
            
            Files.copy(resourceStream, targetFile);
            // 静默复制，不输出日志
        }
    }
    
    /**
     * 使用默认配置
     */
    private void useDefaultConfigs() {
        logger.warn("使用默认配置");
        this.pluginConfig = new PluginConfig(null);
        this.messageConfig = new MessageConfig(null);
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        // 静默重新加载配置
        loadConfigs();
    }
    
    // ================================
    // Getter 方法
    // ================================
    
    public PluginConfig getPluginConfig() {
        return pluginConfig != null ? pluginConfig : new PluginConfig(null);
    }
    
    public MessageConfig getMessageConfig() {
        return messageConfig != null ? messageConfig : new MessageConfig(null);
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
}