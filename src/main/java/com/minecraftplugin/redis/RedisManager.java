package com.minecraftplugin.redis;

import com.minecraftplugin.config.ConfigManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class RedisManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisManager.class);
    
    private final ConfigManager configManager;
    private JedisPool jedisPool;
    private Jedis subscriberJedis;
    private final ExecutorService executorService;
    
    private static final String ECONOMY_CHANNEL = "minecraft:economy";
    private static final String INVENTORY_CHANNEL = "minecraft:inventory";
    private static final String ENDER_CHEST_CHANNEL = "minecraft:ender_chest";
    
    private static final String ECONOMY_CACHE_PREFIX = "economy:";
    private static final String INVENTORY_CACHE_PREFIX = "inventory:";
    private static final String ENDER_CHEST_CACHE_PREFIX = "ender_chest:";
    
    public RedisManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.executorService = Executors.newCachedThreadPool();
    }
    
    public boolean connect() {
        try {
            logger.info("Connecting to Redis...");
            
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(5);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            
            if (configManager.getRedisPassword().isEmpty()) {
                jedisPool = new JedisPool(poolConfig, configManager.getRedisHost(), configManager.getRedisPort());
            } else {
                jedisPool = new JedisPool(poolConfig, configManager.getRedisHost(), configManager.getRedisPort(), 
                                        2000, configManager.getRedisPassword(), configManager.getRedisDatabase());
            }
            
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }
            
            logger.info("Successfully connected to Redis at {}:{}", configManager.getRedisHost(), configManager.getRedisPort());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to connect to Redis", e);
            return false;
        }
    }
    
    public void disconnect() {
        if (subscriberJedis != null) {
            try {
                subscriberJedis.close();
                logger.info("Redis subscriber connection closed");
            } catch (Exception e) {
                logger.error("Error closing Redis subscriber connection", e);
            }
        }
        
        if (jedisPool != null) {
            try {
                jedisPool.close();
                logger.info("Redis connection pool closed");
            } catch (Exception e) {
                logger.error("Error closing Redis connection pool", e);
            }
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    public void publishEconomyUpdate(UUID playerUuid, double newBalance, String operation) {
        try (Jedis jedis = jedisPool.getResource()) {
            String message = String.format("%s:%s:%.2f", playerUuid.toString(), operation, newBalance);
            jedis.publish(ECONOMY_CHANNEL, message);
            
            jedis.setex(ECONOMY_CACHE_PREFIX + playerUuid.toString(), 3600, String.valueOf(newBalance));
            
            logger.debug("Published economy update: {}", message);
        } catch (Exception e) {
            logger.error("Error publishing economy update for {}", playerUuid, e);
        }
    }
    
    public void subscribeToEconomyUpdates(Consumer<RedisMessage> callback) {
        subscribeToChannel(ECONOMY_CHANNEL, callback);
    }
    
    public void publishInventoryUpdate(UUID playerUuid, String inventoryName, String operation) {
        try (Jedis jedis = jedisPool.getResource()) {
            String message = String.format("%s:%s:%s", playerUuid.toString(), operation, inventoryName);
            jedis.publish(INVENTORY_CHANNEL, message);
            
            String cacheKey = INVENTORY_CACHE_PREFIX + playerUuid.toString() + ":" + inventoryName;
            if ("delete".equals(operation)) {
                jedis.del(cacheKey);
            }
            
            logger.debug("Published inventory update: {}", message);
        } catch (Exception e) {
            logger.error("Error publishing inventory update for {}", playerUuid, e);
        }
    }
    
    public void subscribeToInventoryUpdates(Consumer<RedisMessage> callback) {
        subscribeToChannel(INVENTORY_CHANNEL, callback);
    }
    
    public void publishEnderChestUpdate(UUID playerUuid, String enderChestName, String operation) {
        try (Jedis jedis = jedisPool.getResource()) {
            String message = String.format("%s:%s:%s", playerUuid.toString(), operation, enderChestName);
            jedis.publish(ENDER_CHEST_CHANNEL, message);
            
            String cacheKey = ENDER_CHEST_CACHE_PREFIX + playerUuid.toString() + ":" + enderChestName;
            if ("delete".equals(operation)) {
                jedis.del(cacheKey);
            }
            
            logger.debug("Published ender chest update: {}", message);
        } catch (Exception e) {
            logger.error("Error publishing ender chest update for {}", playerUuid, e);
        }
    }
    
    public void subscribeToEnderChestUpdates(Consumer<RedisMessage> callback) {
        subscribeToChannel(ENDER_CHEST_CHANNEL, callback);
    }
    
    public Double getCachedBalance(UUID playerUuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(ECONOMY_CACHE_PREFIX + playerUuid.toString());
            return value != null ? Double.parseDouble(value) : null;
        } catch (Exception e) {
            logger.error("Error getting cached balance for {}", playerUuid, e);
            return null;
        }
    }
    
    public void setCachedBalance(UUID playerUuid, double balance, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(ECONOMY_CACHE_PREFIX + playerUuid.toString(), expireSeconds, String.valueOf(balance));
        } catch (Exception e) {
            logger.error("Error setting cached balance for {}", playerUuid, e);
        }
    }
    
    public String getCachedInventory(UUID playerUuid, String inventoryName) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(INVENTORY_CACHE_PREFIX + playerUuid.toString() + ":" + inventoryName);
        } catch (Exception e) {
            logger.error("Error getting cached inventory for {}", playerUuid, e);
            return null;
        }
    }
    
    public void setCachedInventory(UUID playerUuid, String inventoryName, String inventoryData, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(INVENTORY_CACHE_PREFIX + playerUuid.toString() + ":" + inventoryName, expireSeconds, inventoryData);
        } catch (Exception e) {
            logger.error("Error setting cached inventory for {}", playerUuid, e);
        }
    }
    
    public String getCachedEnderChest(UUID playerUuid, String enderChestName) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(ENDER_CHEST_CACHE_PREFIX + playerUuid.toString() + ":" + enderChestName);
        } catch (Exception e) {
            logger.error("Error getting cached ender chest for {}", playerUuid, e);
            return null;
        }
    }
    
    public void setCachedEnderChest(UUID playerUuid, String enderChestName, String enderChestData, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(ENDER_CHEST_CACHE_PREFIX + playerUuid.toString() + ":" + enderChestName, expireSeconds, enderChestData);
        } catch (Exception e) {
            logger.error("Error setting cached ender chest for {}", playerUuid, e);
        }
    }
    
    private void subscribeToChannel(String channel, Consumer<RedisMessage> callback) {
        executorService.submit(() -> {
            try {
                subscriberJedis = jedisPool.getResource();
                subscriberJedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        try {
                            RedisMessage redisMessage = parseMessage(channel, message);
                            if (redisMessage != null) {
                                callback.accept(redisMessage);
                            }
                        } catch (Exception e) {
                            logger.error("Error processing Redis message: {}", message, e);
                        }
                    }
                    
                    @Override
                    public void onSubscribe(String channel, int subscribedChannels) {
                        logger.info("Subscribed to Redis channel: {}", channel);
                    }
                    
                    @Override
                    public void onUnsubscribe(String channel, int subscribedChannels) {
                        logger.info("Unsubscribed from Redis channel: {}", channel);
                    }
                }, channel);
            } catch (Exception e) {
                logger.error("Error subscribing to Redis channel: {}", channel, e);
            }
        });
    }
    
    private RedisMessage parseMessage(String channel, String message) {
        if (channel == null || message == null || message.trim().isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = message.split(":", 3);
            if (parts.length >= 2) {
                UUID playerUuid = UUID.fromString(parts[0]);
                String operation = parts[1];
                String data = parts.length > 2 ? parts[2] : "";
                
                return new RedisMessage(channel, playerUuid, operation, data);
            }
        } catch (Exception e) {
            logger.error("Error parsing Redis message: {}", message, e);
        }
        return null;
    }
    
    public CompletableFuture<Void> publishEconomyUpdateAsync(UUID playerUuid, double newBalance, String operation) {
        return CompletableFuture.runAsync(() -> publishEconomyUpdate(playerUuid, newBalance, operation), executorService);
    }
    
    public CompletableFuture<Void> publishInventoryUpdateAsync(UUID playerUuid, String inventoryName, String operation) {
        return CompletableFuture.runAsync(() -> publishInventoryUpdate(playerUuid, inventoryName, operation), executorService);
    }
    
    public CompletableFuture<Void> publishEnderChestUpdateAsync(UUID playerUuid, String enderChestName, String operation) {
        return CompletableFuture.runAsync(() -> publishEnderChestUpdate(playerUuid, enderChestName, operation), executorService);
    }
    
    public CompletableFuture<Double> getCachedBalanceAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getCachedBalance(playerUuid), executorService);
    }
    
    public CompletableFuture<String> getCachedInventoryAsync(UUID playerUuid, String inventoryName) {
        return CompletableFuture.supplyAsync(() -> getCachedInventory(playerUuid, inventoryName), executorService);
    }
    
    public CompletableFuture<String> getCachedEnderChestAsync(UUID playerUuid, String enderChestName) {
        return CompletableFuture.supplyAsync(() -> getCachedEnderChest(playerUuid, enderChestName), executorService);
    }
    
    public static class RedisMessage {
        private final String channel;
        private final UUID playerUuid;
        private final String operation;
        private final String data;
        
        public RedisMessage(String channel, UUID playerUuid, String operation, String data) {
            this.channel = channel;
            this.playerUuid = playerUuid;
            this.operation = operation;
            this.data = data;
        }
        
        public String getChannel() { return channel; }
        public UUID getPlayerUuid() { return playerUuid; }
        public String getOperation() { return operation; }
        public String getData() { return data; }
        
        @Override
        public String toString() {
            return String.format("RedisMessage{channel='%s', playerUuid=%s, operation='%s', data='%s'}", 
                               channel, playerUuid, operation, data);
        }
    }
} 