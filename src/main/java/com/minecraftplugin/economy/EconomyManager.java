package com.minecraftplugin.economy;

import com.minecraftplugin.AdvancedMinecraftPlugin;
import com.minecraftplugin.database.DatabaseManager;
import com.minecraftplugin.redis.RedisManager;
import com.minecraftplugin.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager implements Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(EconomyManager.class);
    
    private final AdvancedMinecraftPlugin plugin;
    private final DatabaseManager databaseManager;
    private final RedisManager redisManager;
    
    private final ConcurrentHashMap<UUID, Double> balanceCache;
    
    public EconomyManager(DatabaseManager databaseManager, RedisManager redisManager, AdvancedMinecraftPlugin plugin) {
        this.databaseManager = databaseManager;
        this.redisManager = redisManager;
        this.plugin = plugin;
        this.balanceCache = new ConcurrentHashMap<>();
        
        setupRedisSubscriptions();
    }
    
    private void setupRedisSubscriptions() {
        redisManager.subscribeToEconomyUpdates(message -> {
            try {
                UUID playerUuid = message.getPlayerUuid();
                String operation = message.getOperation();
                double newBalance = Double.parseDouble(message.getData());
                
                balanceCache.put(playerUuid, newBalance);
                
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    String currencySymbol = plugin.getConfigManager().getConfig().getString("plugin.economy.currency_symbol", "$");
                    String messageText = MessageUtils.formatMessage(plugin.getConfigManager().getConfig().getString("messages.economy.balance"), 
                                                                  "currency", currencySymbol, "balance", String.format("%.2f", newBalance));
                    player.sendMessage(messageText);
                }
                
                logger.debug("Received economy update: {} {} {}", playerUuid, operation, newBalance);
                
            } catch (Exception e) {
                logger.error("Error processing economy update message", e);
            }
        });
    }
    
    public double getBalance(UUID playerUuid) {
        Double cachedBalance = balanceCache.get(playerUuid);
        if (cachedBalance != null) {
            return cachedBalance;
        }
        
        Double redisBalance = redisManager.getCachedBalance(playerUuid);
        if (redisBalance != null) {
            balanceCache.put(playerUuid, redisBalance);
            return redisBalance;
        }
        
        double balance = databaseManager.getPlayerBalance(playerUuid);
        balanceCache.put(playerUuid, balance);
        
        redisManager.setCachedBalance(playerUuid, balance, 3600);
        
        return balance;
    }
    
    public boolean setBalance(UUID playerUuid, double balance) {
        if (!isValidBalance(balance)) {
            return false;
        }
        
        if (!databaseManager.setPlayerBalance(playerUuid, balance)) {
            return false;
        }
        
        balanceCache.put(playerUuid, balance);
        redisManager.setCachedBalance(playerUuid, balance, 3600);
        redisManager.publishEconomyUpdate(playerUuid, balance, "set");
        
        logger.info("Set balance for {} to {}", playerUuid, balance);
        return true;
    }
    
    public boolean addBalance(UUID playerUuid, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        double currentBalance = getBalance(playerUuid);
        double newBalance = currentBalance + amount;
        
        if (!isValidBalance(newBalance)) {
            return false;
        }
        
        if (!databaseManager.updatePlayerBalance(playerUuid, amount)) {
            return false;
        }
        
        balanceCache.put(playerUuid, newBalance);
        redisManager.setCachedBalance(playerUuid, newBalance, 3600);
        redisManager.publishEconomyUpdate(playerUuid, newBalance, "add");
        
        logger.info("Added {} to balance for {}, new balance: {}", amount, playerUuid, newBalance);
        return true;
    }
    
    public boolean removeBalance(UUID playerUuid, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        double currentBalance = getBalance(playerUuid);
        if (currentBalance < amount) {
            return false;
        }
        
        double newBalance = currentBalance - amount;
        
        if (!databaseManager.updatePlayerBalance(playerUuid, -amount)) {
            return false;
        }
        
        balanceCache.put(playerUuid, newBalance);
        redisManager.setCachedBalance(playerUuid, newBalance, 3600);
        redisManager.publishEconomyUpdate(playerUuid, newBalance, "remove");
        
        logger.info("Removed {} from balance for {}, new balance: {}", amount, playerUuid, newBalance);
        return true;
    }
    
    public boolean hasBalance(UUID playerUuid, double amount) {
        return getBalance(playerUuid) >= amount;
    }
    
    public boolean transfer(UUID fromUuid, UUID toUuid, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        if (!hasBalance(fromUuid, amount)) {
            return false;
        }
        
        if (!removeBalance(fromUuid, amount)) {
            return false;
        }
        
        if (!addBalance(toUuid, amount)) {
            addBalance(fromUuid, amount);
            return false;
        }
        
        logger.info("Transferred {} from {} to {}", amount, fromUuid, toUuid);
        return true;
    }
    
    private boolean isValidBalance(double balance) {
        double maxBalance = plugin.getConfigManager().getConfig().getDouble("plugin.economy.max_balance", 0.0);
        double minBalance = plugin.getConfigManager().getConfig().getDouble("plugin.economy.min_balance", 0.0);
        
        if (maxBalance > 0 && balance > maxBalance) {
            return false;
        }
        
        if (balance < minBalance) {
            return false;
        }
        
        return true;
    }
    
    @SuppressWarnings("deprecation")
    public OfflinePlayer getPlayer(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        
        try {
            UUID uuid = UUID.fromString(identifier);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException e) {
            // Use the newer API method to avoid deprecation warning
            return Bukkit.getOfflinePlayer(identifier);
        }
    }
    
    public void clearCache(UUID playerUuid) {
        balanceCache.remove(playerUuid);
    }
    
    public void clearAllCache() {
        balanceCache.clear();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        getBalance(playerUuid);
        
        logger.debug("Loaded balance for player: {}", player.getName());
    }
    
    public CompletableFuture<Double> getBalanceAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getBalance(playerUuid));
    }
    
    public CompletableFuture<Boolean> setBalanceAsync(UUID playerUuid, double balance) {
        return CompletableFuture.supplyAsync(() -> setBalance(playerUuid, balance));
    }
    
    public CompletableFuture<Boolean> addBalanceAsync(UUID playerUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> addBalance(playerUuid, amount));
    }
    
    public CompletableFuture<Boolean> removeBalanceAsync(UUID playerUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> removeBalance(playerUuid, amount));
    }
    
    public CompletableFuture<Boolean> transferAsync(UUID fromUuid, UUID toUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> transfer(fromUuid, toUuid, amount));
    }
    
    public CompletableFuture<Boolean> hasBalanceAsync(UUID playerUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> hasBalance(playerUuid, amount));
    }
    
    public AdvancedMinecraftPlugin getPlugin() {
        return plugin;
    }
} 