package com.minecraftplugin.config;

import com.minecraftplugin.AdvancedMinecraftPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    private final AdvancedMinecraftPlugin plugin;
    private FileConfiguration config;
    
    private String mongoUri;
    private String mongoDatabase;
    
    private String redisHost;
    private int redisPort;
    private String redisPassword;
    private int redisDatabase;
    
    private boolean grpcEnabled;
    private int grpcPort;
    private String grpcSecretKey;
    
    private boolean autoSaveEnabled;
    private int autoSaveInterval;
    private boolean debugMode;
    
    public ConfigManager(AdvancedMinecraftPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        loadEnvironmentVariables();
        loadDatabaseConfig();
        loadRedisConfig();
        loadGrpcConfig();
        loadPluginSettings();
        
        logger.info("Configuration loaded successfully");
    }
    
    private void loadEnvironmentVariables() {
        mongoUri = getEnvOrDefault("MONGO_URI", config.getString("database.mongo_uri", "mongodb://localhost:27017"));
        mongoDatabase = getEnvOrDefault("MONGO_DATABASE", config.getString("database.database_name", "minecraft_plugin"));
        
        redisHost = getEnvOrDefault("REDIS_HOST", config.getString("redis.host", "localhost"));
        redisPort = Integer.parseInt(getEnvOrDefault("REDIS_PORT", config.getString("redis.port", "6379")));
        redisPassword = getEnvOrDefault("REDIS_PASSWORD", config.getString("redis.password", ""));
        redisDatabase = Integer.parseInt(getEnvOrDefault("REDIS_DATABASE", config.getString("redis.database", "0")));
        
        grpcEnabled = Boolean.parseBoolean(getEnvOrDefault("GRPC_ENABLED", config.getString("grpc.enabled", "false")));
        grpcPort = Integer.parseInt(getEnvOrDefault("GRPC_PORT", config.getString("grpc.port", "9090")));
        grpcSecretKey = getEnvOrDefault("GRPC_SECRET_KEY", config.getString("grpc.secret_key", "default_secret_key"));
    }
    
    private void loadDatabaseConfig() {
        logger.info("Database configuration loaded - URI: {}, Database: {}", 
                   maskUri(mongoUri), mongoDatabase);
    }
    
    private void loadRedisConfig() {
        logger.info("Redis configuration loaded - Host: {}:{}, Database: {}", 
                   redisHost, redisPort, redisDatabase);
    }
    
    private void loadGrpcConfig() {
        if (grpcEnabled) {
            logger.info("gRPC configuration loaded - Port: {}, Enabled: {}", grpcPort, grpcEnabled);
        }
    }
    
    private void loadPluginSettings() {
        autoSaveEnabled = config.getBoolean("plugin.auto_save.enabled", true);
        autoSaveInterval = config.getInt("plugin.auto_save.interval_seconds", 300);
        debugMode = config.getBoolean("plugin.debug_mode", false);
        
        logger.info("Plugin settings loaded - Auto-save: {} ({}s), Debug: {}", 
                   autoSaveEnabled, autoSaveInterval, debugMode);
    }
    
    private String getEnvOrDefault(String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : defaultValue;
    }
    
    private String maskUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "null";
        }
        if (uri.contains("@")) {
            String[] parts = uri.split("@");
            if (parts.length == 2) {
                String credentials = parts[0];
                String rest = parts[1];
                if (credentials.contains(":")) {
                    String[] credParts = credentials.split(":");
                    if (credParts.length >= 3) {
                        return credParts[0] + ":***@" + rest;
                    }
                }
            }
        }
        return uri;
    }
    
    public String getMongoUri() {
        return mongoUri;
    }
    
    public String getMongoDatabase() {
        return mongoDatabase;
    }
    
    public String getRedisHost() {
        return redisHost;
    }
    
    public int getRedisPort() {
        return redisPort;
    }
    
    public String getRedisPassword() {
        return redisPassword;
    }
    
    public int getRedisDatabase() {
        return redisDatabase;
    }
    
    public boolean isGrpcEnabled() {
        return grpcEnabled;
    }
    
    public int getGrpcPort() {
        return grpcPort;
    }
    
    public String getGrpcSecretKey() {
        return grpcSecretKey;
    }
    
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }
    
    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
} 