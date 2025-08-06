package com.example.customjoinmessage.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类
 * 
 * 提供统一的日志管理功能
 */
public class LoggerUtil {
    
    private static final Logger logger = LoggerFactory.getLogger("CustomJoinMessage");
    
    /**
     * 记录信息日志
     */
    public static void info(String message, Object... args) {
        logger.info(message, args);
    }
    
    /**
     * 记录警告日志
     */
    public static void warn(String message, Object... args) {
        logger.warn(message, args);
    }
    
    /**
     * 记录错误日志
     */
    public static void error(String message, Object... args) {
        logger.error(message, args);
    }
    
    /**
     * 记录调试日志
     */
    public static void debug(String message, Object... args) {
        logger.debug(message, args);
    }
    
    /**
     * 记录详细日志
     */
    public static void trace(String message, Object... args) {
        logger.trace(message, args);
    }
    
    /**
     * 获取原始Logger
     */
    public static Logger getLogger() {
        return logger;
    }
    
    /**
     * 获取指定名称的Logger
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
    
    /**
     * 获取指定类的Logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}