package com.minecraftplugin;

import com.minecraftplugin.config.ConfigManager;
import com.minecraftplugin.economy.EconomyManager;
import com.minecraftplugin.economy.commands.EconomyCommand;
import com.minecraftplugin.inventory.InventoryManager;
import com.minecraftplugin.inventory.commands.InventoryCommand;
import com.minecraftplugin.enderchest.EnderChestManager;
import com.minecraftplugin.enderchest.commands.EnderChestCommand;
import com.minecraftplugin.commands.PlayerCommand;
import com.minecraftplugin.database.DatabaseManager;
import com.minecraftplugin.redis.RedisManager;
import com.minecraftplugin.grpc.GrpcServer;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedMinecraftPlugin extends JavaPlugin {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedMinecraftPlugin.class);
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RedisManager redisManager;
    private EconomyManager economyManager;
    private InventoryManager inventoryManager;
    private EnderChestManager enderChestManager;
    private GrpcServer grpcServer;
    
    @Override
    public void onEnable() {
        logger.info("Starting AdvancedMinecraftPlugin...");
        
        try {
            configManager = new ConfigManager(this);
            configManager.loadConfig();
            
            databaseManager = new DatabaseManager(configManager);
            if (!databaseManager.connect()) {
                logger.error("Failed to connect to MongoDB. Plugin will be disabled.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            redisManager = new RedisManager(configManager);
            if (!redisManager.connect()) {
                logger.error("Failed to connect to Redis. Plugin will be disabled.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            economyManager = new EconomyManager(databaseManager, redisManager, this);
            inventoryManager = new InventoryManager(databaseManager, redisManager, this);
            enderChestManager = new EnderChestManager(databaseManager, redisManager, this);
            
            registerCommands();
            registerEventListeners();
            
            if (configManager.isGrpcEnabled()) {
                try {
                    grpcServer = new GrpcServer(configManager, databaseManager);
                    grpcServer.start();
                    logger.info("gRPC server started on port {}", configManager.getGrpcPort());
                } catch (Exception e) {
                    logger.error("Failed to start gRPC server, continuing without it", e);
                }
            }
            
            logger.info("AdvancedMinecraftPlugin has been enabled successfully!");
            
        } catch (Exception e) {
            logger.error("Failed to enable AdvancedMinecraftPlugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        logger.info("Disabling AdvancedMinecraftPlugin...");
        
        try {
            if (grpcServer != null) {
                grpcServer.stop();
                logger.info("gRPC server stopped");
            }
            
            if (redisManager != null) {
                redisManager.disconnect();
                logger.info("Redis connection closed");
            }
            
            if (databaseManager != null) {
                databaseManager.disconnect();
                logger.info("Database connection closed");
            }
            
            logger.info("AdvancedMinecraftPlugin has been disabled successfully!");
            
        } catch (Exception e) {
            logger.error("Error while disabling AdvancedMinecraftPlugin", e);
        }
    }
    
    private void registerCommands() {
        try {
            EconomyCommand economyCommand = new EconomyCommand(economyManager);
            getCommand("money").setExecutor(economyCommand);
            getCommand("money").setTabCompleter(economyCommand);
            
            InventoryCommand inventoryCommand = new InventoryCommand(inventoryManager);
            getCommand("inv").setExecutor(inventoryCommand);
            getCommand("inv").setTabCompleter(inventoryCommand);
            
            EnderChestCommand enderChestCommand = new EnderChestCommand(enderChestManager);
            getCommand("ec").setExecutor(enderChestCommand);
            getCommand("ec").setTabCompleter(enderChestCommand);
            
            // Add player command for self-management
            PlayerCommand playerCommand = new PlayerCommand(economyManager, inventoryManager, enderChestManager);
            getCommand("plugin").setExecutor(playerCommand);
            getCommand("plugin").setTabCompleter(playerCommand);
            
            logger.info("All commands registered successfully");
        } catch (Exception e) {
            logger.error("Failed to register commands", e);
        }
    }
    
    private void registerEventListeners() {
        try {
            getServer().getPluginManager().registerEvents(economyManager, this);
            getServer().getPluginManager().registerEvents(inventoryManager, this);
            getServer().getPluginManager().registerEvents(enderChestManager, this);
            
            logger.info("All event listeners registered successfully");
        } catch (Exception e) {
            logger.error("Failed to register event listeners", e);
        }
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
    
    public EnderChestManager getEnderChestManager() {
        return enderChestManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public RedisManager getRedisManager() {
        return redisManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
} 