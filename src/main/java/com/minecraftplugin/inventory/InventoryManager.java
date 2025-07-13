package com.minecraftplugin.inventory;

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
import org.bukkit.inventory.PlayerInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

public class InventoryManager implements Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryManager.class);
    
    private final AdvancedMinecraftPlugin plugin;
    private final DatabaseManager databaseManager;
    private final RedisManager redisManager;
    private final Gson gson;
    
    public InventoryManager(DatabaseManager databaseManager, RedisManager redisManager, AdvancedMinecraftPlugin plugin) {
        this.databaseManager = databaseManager;
        this.redisManager = redisManager;
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        setupRedisSubscriptions();
    }
    
    private void setupRedisSubscriptions() {
        redisManager.subscribeToInventoryUpdates(message -> {
            try {
                UUID playerUuid = message.getPlayerUuid();
                String operation = message.getOperation();
                String inventoryName = message.getData();
                
                logger.debug("Received inventory update: {} {} {}", playerUuid, operation, inventoryName);
                
                // Notify online players about inventory changes
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    String currencySymbol = plugin.getConfigManager().getConfig().getString("plugin.economy.currency_symbol", "$");
                    String messageText = MessageUtils.formatMessage(plugin.getConfigManager().getConfig().getString("messages.inventory.updated"), 
                                                                  "operation", operation, "name", inventoryName);
                    player.sendMessage(messageText);
                }
                
            } catch (Exception e) {
                logger.error("Error processing inventory update message", e);
            }
        });
    }
    
    public boolean saveInventory(UUID playerUuid, String inventoryName) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            if (!isValidInventoryName(inventoryName)) {
                return false;
            }
            
            PlayerInventory playerInventory = player.getInventory();
            String inventoryData = serializeInventory(playerInventory);
            
            if (!databaseManager.saveInventory(playerUuid, inventoryName, inventoryData)) {
                return false;
            }
            
            redisManager.setCachedInventory(playerUuid, inventoryName, inventoryData, 3600);
            redisManager.publishInventoryUpdate(playerUuid, inventoryName, "save");
            
            logger.info("Saved inventory '{}' for player {}", inventoryName, playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error saving inventory for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean loadInventory(UUID playerUuid, String inventoryName) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            String cachedData = redisManager.getCachedInventory(playerUuid, inventoryName);
            String inventoryData = cachedData != null ? cachedData : databaseManager.loadInventory(playerUuid, inventoryName);
            
            if (inventoryData == null) {
                return false;
            }
            
            PlayerInventory playerInventory = player.getInventory();
            deserializeInventory(playerInventory, inventoryData);
            
            if (cachedData == null) {
                redisManager.setCachedInventory(playerUuid, inventoryName, inventoryData, 3600);
            }
            
            redisManager.publishInventoryUpdate(playerUuid, inventoryName, "load");
            
            logger.info("Loaded inventory '{}' for player {}", inventoryName, playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error loading inventory for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean updateInventory(UUID playerUuid, String inventoryName) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            // Check if inventory exists
            String existingData = databaseManager.loadInventory(playerUuid, inventoryName);
            if (existingData == null) {
                return false;
            }
            
            PlayerInventory playerInventory = player.getInventory();
            String inventoryData = serializeInventory(playerInventory);
            
            if (!databaseManager.updateInventory(playerUuid, inventoryName, inventoryData)) {
                return false;
            }
            
            redisManager.setCachedInventory(playerUuid, inventoryName, inventoryData, 3600);
            redisManager.publishInventoryUpdate(playerUuid, inventoryName, "update");
            
            logger.info("Updated inventory '{}' for player {}", inventoryName, playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error updating inventory for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean clearInventory(UUID playerUuid) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            PlayerInventory playerInventory = player.getInventory();
            playerInventory.clear();
            
            redisManager.publishInventoryUpdate(playerUuid, "all", "clear");
            
            logger.info("Cleared inventory for player {}", playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error clearing inventory for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean backupInventory(UUID playerUuid, String backupName) {
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String backupInventoryName = "backup_" + backupName + "_" + timestamp;
            
            return saveInventory(playerUuid, backupInventoryName);
            
        } catch (Exception e) {
            logger.error("Error backing up inventory for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean restoreInventory(UUID playerUuid, String backupName) {
        try {
            List<String> inventories = getSavedInventories(playerUuid);
            String targetBackup = null;
            
            for (String inventory : inventories) {
                if (inventory.startsWith("backup_" + backupName + "_")) {
                    targetBackup = inventory;
                    break;
                }
            }
            
            if (targetBackup == null) {
                return false;
            }
            
            return loadInventory(playerUuid, targetBackup);
            
        } catch (Exception e) {
            logger.error("Error restoring inventory for {}", playerUuid, e);
            return false;
        }
    }
    
    public Map<String, String> getInventoryInfo(UUID playerUuid, String inventoryName) {
        try {
            String inventoryData = databaseManager.loadInventory(playerUuid, inventoryName);
            if (inventoryData == null) {
                return null;
            }
            
            Map<String, String> info = new HashMap<>();
            info.put("name", inventoryName);
            info.put("size", String.valueOf(inventoryData.length()));
            info.put("created", "N/A"); // Could be enhanced with creation date
            
            return info;
            
        } catch (Exception e) {
            logger.error("Error getting inventory info for {}", playerUuid, e);
            return null;
        }
    }
    
    public List<String> getSavedInventories(UUID playerUuid) {
        try {
            return databaseManager.getSavedInventories(playerUuid);
        } catch (Exception e) {
            logger.error("Error getting saved inventories for {}", playerUuid, e);
            return List.of();
        }
    }
    
    public boolean deleteInventory(UUID playerUuid, String inventoryName) {
        try {
            if (!databaseManager.deleteInventory(playerUuid, inventoryName)) {
                return false;
            }
            
            redisManager.publishInventoryUpdate(playerUuid, inventoryName, "delete");
            
            logger.info("Deleted inventory '{}' for player {}", inventoryName, playerUuid);
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting inventory for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean deleteAllInventories(UUID playerUuid) {
        try {
            List<String> inventories = getSavedInventories(playerUuid);
            boolean allDeleted = true;
            
            for (String inventoryName : inventories) {
                if (!deleteInventory(playerUuid, inventoryName)) {
                    allDeleted = false;
                }
            }
            
            return allDeleted;
            
        } catch (Exception e) {
            logger.error("Error deleting all inventories for {}", playerUuid, e);
            return false;
        }
    }
    
    private boolean isValidInventoryName(String name) {
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
    
    private String serializeInventory(PlayerInventory inventory) {
        try {
            ItemStack[] contents = inventory.getContents();
            return gson.toJson(contents);
        } catch (Exception e) {
            logger.error("Error serializing inventory", e);
            return null;
        }
    }
    
    private void deserializeInventory(PlayerInventory inventory, String data) {
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
        
        if (plugin.getConfigManager().getConfig().getBoolean("plugin.inventory.auto_save_on_quit", true)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String autoSaveName = "auto_" + timestamp;
            
            saveInventory(playerUuid, autoSaveName);
        }
    }
    
    // Async methods
    public CompletableFuture<Boolean> saveInventoryAsync(UUID playerUuid, String inventoryName) {
        return CompletableFuture.supplyAsync(() -> saveInventory(playerUuid, inventoryName));
    }
    
    public CompletableFuture<Boolean> loadInventoryAsync(UUID playerUuid, String inventoryName) {
        return CompletableFuture.supplyAsync(() -> loadInventory(playerUuid, inventoryName));
    }
    
    public CompletableFuture<Boolean> updateInventoryAsync(UUID playerUuid, String inventoryName) {
        return CompletableFuture.supplyAsync(() -> updateInventory(playerUuid, inventoryName));
    }
    
    public CompletableFuture<Boolean> clearInventoryAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> clearInventory(playerUuid));
    }
    
    public CompletableFuture<Boolean> backupInventoryAsync(UUID playerUuid, String backupName) {
        return CompletableFuture.supplyAsync(() -> backupInventory(playerUuid, backupName));
    }
    
    public CompletableFuture<Boolean> restoreInventoryAsync(UUID playerUuid, String backupName) {
        return CompletableFuture.supplyAsync(() -> restoreInventory(playerUuid, backupName));
    }
    
    public CompletableFuture<List<String>> getSavedInventoriesAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getSavedInventories(playerUuid));
    }
    
    public CompletableFuture<Boolean> deleteInventoryAsync(UUID playerUuid, String inventoryName) {
        return CompletableFuture.supplyAsync(() -> deleteInventory(playerUuid, inventoryName));
    }
    
    public CompletableFuture<Boolean> deleteAllInventoriesAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> deleteAllInventories(playerUuid));
    }
    
    public CompletableFuture<Map<String, String>> getInventoryInfoAsync(UUID playerUuid, String inventoryName) {
        return CompletableFuture.supplyAsync(() -> getInventoryInfo(playerUuid, inventoryName));
    }
    
    public AdvancedMinecraftPlugin getPlugin() {
        return plugin;
    }
} 