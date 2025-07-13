package com.minecraftplugin.enderchest;

import com.minecraftplugin.AdvancedMinecraftPlugin;
import com.minecraftplugin.database.DatabaseManager;
import com.minecraftplugin.redis.RedisManager;
import com.minecraftplugin.utils.MessageUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

public class EnderChestManager implements Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(EnderChestManager.class);
    
    private final AdvancedMinecraftPlugin plugin;
    private final DatabaseManager databaseManager;
    private final RedisManager redisManager;
    private final Gson gson;
    
    public EnderChestManager(DatabaseManager databaseManager, RedisManager redisManager, AdvancedMinecraftPlugin plugin) {
        this.databaseManager = databaseManager;
        this.redisManager = redisManager;
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        setupRedisSubscriptions();
    }
    
    private void setupRedisSubscriptions() {
        redisManager.subscribeToEnderChestUpdates(message -> {
            try {
                UUID playerUuid = message.getPlayerUuid();
                String operation = message.getOperation();
                String enderChestName = message.getData();
                
                logger.debug("Received ender chest update: {} {} {}", playerUuid, operation, enderChestName);
                
                // Notify online players about ender chest changes
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    String messageText = MessageUtils.formatMessage(plugin.getConfigManager().getConfig().getString("messages.ender_chest.updated"), 
                                                                  "operation", operation, "name", enderChestName);
                    player.sendMessage(messageText);
                }
                
            } catch (Exception e) {
                logger.error("Error processing ender chest update message", e);
            }
        });
    }
    
    public boolean saveEnderChest(UUID playerUuid, String enderChestName) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            if (!isValidEnderChestName(enderChestName)) {
                return false;
            }
            
            Inventory enderChest = player.getEnderChest();
            String enderChestData = serializeInventory(enderChest);
            
            if (!databaseManager.saveEnderChest(playerUuid, enderChestName, enderChestData)) {
                return false;
            }
            
            redisManager.setCachedEnderChest(playerUuid, enderChestName, enderChestData, 3600);
            redisManager.publishEnderChestUpdate(playerUuid, enderChestName, "save");
            
            logger.info("Saved ender chest '{}' for player {}", enderChestName, playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error saving ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean loadEnderChest(UUID playerUuid, String enderChestName) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            String cachedData = redisManager.getCachedEnderChest(playerUuid, enderChestName);
            String enderChestData = cachedData != null ? cachedData : databaseManager.loadEnderChest(playerUuid, enderChestName);
            
            if (enderChestData == null) {
                return false;
            }
            
            Inventory enderChest = player.getEnderChest();
            deserializeInventory(enderChest, enderChestData);
            
            if (cachedData == null) {
                redisManager.setCachedEnderChest(playerUuid, enderChestName, enderChestData, 3600);
            }
            
            redisManager.publishEnderChestUpdate(playerUuid, enderChestName, "load");
            
            logger.info("Loaded ender chest '{}' for player {}", enderChestName, playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error loading ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean updateEnderChest(UUID playerUuid, String enderChestName) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            // Check if ender chest exists
            String existingData = databaseManager.loadEnderChest(playerUuid, enderChestName);
            if (existingData == null) {
                return false;
            }
            
            Inventory enderChest = player.getEnderChest();
            String enderChestData = serializeInventory(enderChest);
            
            if (!databaseManager.updateEnderChest(playerUuid, enderChestName, enderChestData)) {
                return false;
            }
            
            redisManager.setCachedEnderChest(playerUuid, enderChestName, enderChestData, 3600);
            redisManager.publishEnderChestUpdate(playerUuid, enderChestName, "update");
            
            logger.info("Updated ender chest '{}' for player {}", enderChestName, playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error updating ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean clearEnderChest(UUID playerUuid) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            Inventory enderChest = player.getEnderChest();
            enderChest.clear();
            
            redisManager.publishEnderChestUpdate(playerUuid, "all", "clear");
            
            logger.info("Cleared ender chest for player {}", playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error clearing ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean backupEnderChest(UUID playerUuid, String backupName) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String backupEnderChestName = "backup_" + backupName + "_" + timestamp;
            
            return saveEnderChest(playerUuid, backupEnderChestName);
            
        } catch (Exception e) {
            logger.error("Error backing up ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean restoreEnderChest(UUID playerUuid, String backupName) {
        try {
            List<String> enderChests = getSavedEnderChests(playerUuid);
            String targetBackup = null;
            
            for (String enderChest : enderChests) {
                if (enderChest.startsWith("backup_" + backupName + "_")) {
                    targetBackup = enderChest;
                    break;
                }
            }
            
            if (targetBackup == null) {
                return false;
            }
            
            return loadEnderChest(playerUuid, targetBackup);
            
        } catch (Exception e) {
            logger.error("Error restoring ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public Map<String, String> getEnderChestInfo(UUID playerUuid, String enderChestName) {
        try {
            String enderChestData = databaseManager.loadEnderChest(playerUuid, enderChestName);
            if (enderChestData == null) {
                return null;
            }
            
            Map<String, String> info = new HashMap<>();
            info.put("name", enderChestName);
            info.put("size", String.valueOf(enderChestData.length()));
            info.put("created", "N/A"); // Could be enhanced with creation date
            
            return info;
            
        } catch (Exception e) {
            logger.error("Error getting ender chest info for {}", playerUuid, e);
            return null;
        }
    }
    
    public List<String> getSavedEnderChests(UUID playerUuid) {
        try {
            return databaseManager.getSavedEnderChests(playerUuid);
        } catch (Exception e) {
            logger.error("Error getting saved ender chests for {}", playerUuid, e);
            return List.of();
        }
    }
    
    public boolean deleteEnderChest(UUID playerUuid, String enderChestName) {
        try {
            if (!databaseManager.deleteEnderChest(playerUuid, enderChestName)) {
                return false;
            }
            
            redisManager.publishEnderChestUpdate(playerUuid, enderChestName, "delete");
            
            logger.info("Deleted ender chest '{}' for player {}", enderChestName, playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean deleteAllEnderChests(UUID playerUuid) {
        try {
            List<String> enderChests = getSavedEnderChests(playerUuid);
            boolean allDeleted = true;
            
            for (String enderChestName : enderChests) {
                if (!deleteEnderChest(playerUuid, enderChestName)) {
                    allDeleted = false;
                }
            }
            
            return allDeleted;
            
        } catch (Exception e) {
            logger.error("Error deleting all ender chests for {}", playerUuid, e);
            return false;
        }
    }
    
    private boolean isValidEnderChestName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Check for invalid characters
        if (name.contains(" ") || name.contains("/") || name.contains("\\")) {
            return false;
        }
        
        // Check length
        if (name.length() > 50) {
            return false;
        }
        
        return true;
    }
    
    private String serializeInventory(Inventory inventory) {
        try {
            ItemStack[] contents = inventory.getContents();
            return gson.toJson(contents);
        } catch (Exception e) {
            logger.error("Error serializing inventory", e);
            return null;
        }
    }
    
    private void deserializeInventory(Inventory inventory, String data) {
        try {
            ItemStack[] contents = gson.fromJson(data, ItemStack[].class);
            inventory.setContents(contents);
        } catch (Exception e) {
            logger.error("Error deserializing inventory", e);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        if (plugin.getConfigManager().getConfig().getBoolean("plugin.ender_chest.auto_save_on_quit", true)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String autoSaveName = "auto_" + timestamp;
            
            saveEnderChest(playerUuid, autoSaveName);
        }
    }
    
    // Async methods
    public CompletableFuture<Boolean> saveEnderChestAsync(UUID playerUuid, String enderChestName) {
        return CompletableFuture.supplyAsync(() -> saveEnderChest(playerUuid, enderChestName));
    }
    
    public CompletableFuture<Boolean> loadEnderChestAsync(UUID playerUuid, String enderChestName) {
        return CompletableFuture.supplyAsync(() -> loadEnderChest(playerUuid, enderChestName));
    }
    
    public CompletableFuture<Boolean> updateEnderChestAsync(UUID playerUuid, String enderChestName) {
        return CompletableFuture.supplyAsync(() -> updateEnderChest(playerUuid, enderChestName));
    }
    
    public CompletableFuture<Boolean> clearEnderChestAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> clearEnderChest(playerUuid));
    }
    
    public CompletableFuture<Boolean> backupEnderChestAsync(UUID playerUuid, String backupName) {
        return CompletableFuture.supplyAsync(() -> backupEnderChest(playerUuid, backupName));
    }
    
    public CompletableFuture<Boolean> restoreEnderChestAsync(UUID playerUuid, String backupName) {
        return CompletableFuture.supplyAsync(() -> restoreEnderChest(playerUuid, backupName));
    }
    
    public CompletableFuture<List<String>> getSavedEnderChestsAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getSavedEnderChests(playerUuid));
    }
    
    public CompletableFuture<Boolean> deleteEnderChestAsync(UUID playerUuid, String enderChestName) {
        return CompletableFuture.supplyAsync(() -> deleteEnderChest(playerUuid, enderChestName));
    }
    
    public CompletableFuture<Boolean> deleteAllEnderChestsAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> deleteAllEnderChests(playerUuid));
    }
    
    public CompletableFuture<Map<String, String>> getEnderChestInfoAsync(UUID playerUuid, String enderChestName) {
        return CompletableFuture.supplyAsync(() -> getEnderChestInfo(playerUuid, enderChestName));
    }
    
    public AdvancedMinecraftPlugin getPlugin() {
        return plugin;
    }
} 