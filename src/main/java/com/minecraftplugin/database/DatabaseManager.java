package com.minecraftplugin.database;

import com.minecraftplugin.config.ConfigManager;
import com.mongodb.client.*;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    
    private final ConfigManager configManager;
    private MongoClient mongoClient;
    private MongoDatabase database;
    
    private static final String ECONOMY_COLLECTION = "economy";
    private static final String INVENTORY_COLLECTION = "inventories";
    private static final String ENDER_CHEST_COLLECTION = "ender_chests";
    
    public DatabaseManager(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public boolean connect() {
        try {
            logger.info("Connecting to MongoDB...");
            
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new com.mongodb.ConnectionString(configManager.getMongoUri()))
                    .build();
            
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(configManager.getMongoDatabase());
            
            database.runCommand(new Document("ping", 1));
            
            logger.info("Successfully connected to MongoDB database: {}", configManager.getMongoDatabase());
            return true;
            
        } catch (MongoException e) {
            logger.error("Failed to connect to MongoDB", e);
            return false;
        }
    }
    
    public void disconnect() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                logger.info("MongoDB connection closed");
            } catch (Exception e) {
                logger.error("Error closing MongoDB connection", e);
            }
        }
    }
    
    public double getPlayerBalance(UUID playerUuid) {
        try {
            MongoCollection<Document> collection = database.getCollection(ECONOMY_COLLECTION);
            Document doc = collection.find(Filters.eq("player_uuid", playerUuid.toString())).first();
            
            if (doc != null) {
                return doc.getDouble("balance");
            }
            
            double defaultBalance = configManager.getConfig().getDouble("plugin.economy.starting_balance", 1000.0);
            Document newDoc = new Document()
                    .append("player_uuid", playerUuid.toString())
                    .append("balance", defaultBalance)
                    .append("created_at", System.currentTimeMillis());
            
            collection.insertOne(newDoc);
            return defaultBalance;
            
        } catch (Exception e) {
            logger.error("Error getting player balance for {}", playerUuid, e);
            return 0.0;
        }
    }
    
    public boolean setPlayerBalance(UUID playerUuid, double balance) {
        try {
            MongoCollection<Document> collection = database.getCollection(ECONOMY_COLLECTION);
            
            Bson filter = Filters.eq("player_uuid", playerUuid.toString());
            Bson update = Updates.combine(
                    Updates.set("balance", balance),
                    Updates.set("updated_at", System.currentTimeMillis())
            );
            
            UpdateResult result = collection.updateOne(filter, update);
            
            if (result.getMatchedCount() == 0) {
                Document doc = new Document()
                        .append("player_uuid", playerUuid.toString())
                        .append("balance", balance)
                        .append("created_at", System.currentTimeMillis())
                        .append("updated_at", System.currentTimeMillis());
                
                collection.insertOne(doc);
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error setting player balance for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean updatePlayerBalance(UUID playerUuid, double amount) {
        try {
            MongoCollection<Document> collection = database.getCollection(ECONOMY_COLLECTION);
            
            Bson filter = Filters.eq("player_uuid", playerUuid.toString());
            Bson update = Updates.combine(
                    Updates.inc("balance", amount),
                    Updates.set("updated_at", System.currentTimeMillis())
            );
            
            UpdateResult result = collection.updateOne(filter, update);
            
            if (result.getMatchedCount() == 0) {
                double defaultBalance = configManager.getConfig().getDouble("plugin.economy.starting_balance", 1000.0);
                Document doc = new Document()
                        .append("player_uuid", playerUuid.toString())
                        .append("balance", defaultBalance + amount)
                        .append("created_at", System.currentTimeMillis())
                        .append("updated_at", System.currentTimeMillis());
                
                collection.insertOne(doc);
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error updating player balance for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean saveInventory(UUID playerUuid, String inventoryName, String inventoryData) {
        try {
            MongoCollection<Document> collection = database.getCollection(INVENTORY_COLLECTION);
            
            Document doc = new Document()
                    .append("player_uuid", playerUuid.toString())
                    .append("name", inventoryName)
                    .append("inventory_data", inventoryData)
                    .append("created_at", System.currentTimeMillis());
            
            collection.insertOne(doc);
            return true;
            
        } catch (Exception e) {
            logger.error("Error saving inventory for {}", playerUuid, e);
            return false;
        }
    }
    
    public String loadInventory(UUID playerUuid, String inventoryName) {
        try {
            MongoCollection<Document> collection = database.getCollection(INVENTORY_COLLECTION);
            
            Bson filter = Filters.and(
                    Filters.eq("player_uuid", playerUuid.toString()),
                    Filters.eq("name", inventoryName)
            );
            
            Document doc = collection.find(filter).first();
            return doc != null ? doc.getString("inventory_data") : null;
            
        } catch (Exception e) {
            logger.error("Error loading inventory for {}", playerUuid, e);
            return null;
        }
    }
    
    public List<String> getSavedInventories(UUID playerUuid) {
        try {
            MongoCollection<Document> collection = database.getCollection(INVENTORY_COLLECTION);
            
            Bson filter = Filters.eq("player_uuid", playerUuid.toString());
            List<String> inventories = new ArrayList<>();
            
            collection.find(filter).forEach(doc -> 
                inventories.add(doc.getString("name"))
            );
            
            return inventories;
            
        } catch (Exception e) {
            logger.error("Error getting saved inventories for {}", playerUuid, e);
            return new ArrayList<>();
        }
    }
    
    public boolean deleteInventory(UUID playerUuid, String inventoryName) {
        try {
            MongoCollection<Document> collection = database.getCollection(INVENTORY_COLLECTION);
            
            Bson filter = Filters.and(
                    Filters.eq("player_uuid", playerUuid.toString()),
                    Filters.eq("name", inventoryName)
            );
            
            collection.deleteOne(filter);
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting inventory for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean saveEnderChest(UUID playerUuid, String enderChestName, String enderChestData) {
        try {
            MongoCollection<Document> collection = database.getCollection(ENDER_CHEST_COLLECTION);
            
            Document doc = new Document()
                    .append("player_uuid", playerUuid.toString())
                    .append("name", enderChestName)
                    .append("ender_chest_data", enderChestData)
                    .append("created_at", System.currentTimeMillis());
            
            collection.insertOne(doc);
            return true;
            
        } catch (Exception e) {
            logger.error("Error saving ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public String loadEnderChest(UUID playerUuid, String enderChestName) {
        try {
            MongoCollection<Document> collection = database.getCollection(ENDER_CHEST_COLLECTION);
            
            Bson filter = Filters.and(
                    Filters.eq("player_uuid", playerUuid.toString()),
                    Filters.eq("name", enderChestName)
            );
            
            Document doc = collection.find(filter).first();
            return doc != null ? doc.getString("ender_chest_data") : null;
            
        } catch (Exception e) {
            logger.error("Error loading ender chest for {}", playerUuid, e);
            return null;
        }
    }
    
    public List<String> getSavedEnderChests(UUID playerUuid) {
        try {
            MongoCollection<Document> collection = database.getCollection(ENDER_CHEST_COLLECTION);
            
            Bson filter = Filters.eq("player_uuid", playerUuid.toString());
            List<String> enderChests = new ArrayList<>();
            
            collection.find(filter).forEach(doc -> 
                enderChests.add(doc.getString("name"))
            );
            
            return enderChests;
            
        } catch (Exception e) {
            logger.error("Error getting saved ender chests for {}", playerUuid, e);
            return new ArrayList<>();
        }
    }
    
    public boolean deleteEnderChest(UUID playerUuid, String enderChestName) {
        try {
            MongoCollection<Document> collection = database.getCollection(ENDER_CHEST_COLLECTION);
            
            Bson filter = Filters.and(
                    Filters.eq("player_uuid", playerUuid.toString()),
                    Filters.eq("name", enderChestName)
            );
            
            collection.deleteOne(filter);
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean updateInventory(UUID playerUuid, String inventoryName, String inventoryData) {
        try {
            MongoCollection<Document> collection = database.getCollection(INVENTORY_COLLECTION);
            
            Bson filter = Filters.and(
                    Filters.eq("player_uuid", playerUuid.toString()),
                    Filters.eq("name", inventoryName)
            );
            
            Bson update = Updates.combine(
                    Updates.set("inventory_data", inventoryData),
                    Updates.set("updated_at", System.currentTimeMillis())
            );
            
            UpdateResult result = collection.updateOne(filter, update);
            return result.getMatchedCount() > 0;
            
        } catch (Exception e) {
            logger.error("Error updating inventory for {}", playerUuid, e);
            return false;
        }
    }

    public boolean updateEnderChest(UUID playerUuid, String enderChestName, String enderChestData) {
        try {
            MongoCollection<Document> collection = database.getCollection(ENDER_CHEST_COLLECTION);
            
            Bson filter = Filters.and(
                    Filters.eq("player_uuid", playerUuid.toString()),
                    Filters.eq("name", enderChestName)
            );
            
            Bson update = Updates.combine(
                    Updates.set("ender_chest_data", enderChestData),
                    Updates.set("updated_at", System.currentTimeMillis())
            );
            
            UpdateResult result = collection.updateOne(filter, update);
            return result.getMatchedCount() > 0;
            
        } catch (Exception e) {
            logger.error("Error updating ender chest for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean deleteAllInventories(UUID playerUuid) {
        try {
            MongoCollection<Document> collection = database.getCollection(INVENTORY_COLLECTION);
            
            Bson filter = Filters.eq("player_uuid", playerUuid.toString());
            
            DeleteResult result = collection.deleteMany(filter);
            return result.getDeletedCount() > 0;
            
        } catch (Exception e) {
            logger.error("Error deleting all inventories for {}", playerUuid, e);
            return false;
        }
    }
    
    public boolean deleteAllEnderChests(UUID playerUuid) {
        try {
            MongoCollection<Document> collection = database.getCollection(ENDER_CHEST_COLLECTION);
            
            Bson filter = Filters.eq("player_uuid", playerUuid.toString());
            
            DeleteResult result = collection.deleteMany(filter);
            return result.getDeletedCount() > 0;
            
        } catch (Exception e) {
            logger.error("Error deleting all ender chests for {}", playerUuid, e);
            return false;
        }
    }
    
    public CompletableFuture<Double> getPlayerBalanceAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getPlayerBalance(playerUuid));
    }
    
    public CompletableFuture<Boolean> setPlayerBalanceAsync(UUID playerUuid, double balance) {
        return CompletableFuture.supplyAsync(() -> setPlayerBalance(playerUuid, balance));
    }
    
    public CompletableFuture<Boolean> updatePlayerBalanceAsync(UUID playerUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> updatePlayerBalance(playerUuid, amount));
    }
    
    public CompletableFuture<Boolean> saveInventoryAsync(UUID playerUuid, String inventoryName, String inventoryData) {
        return CompletableFuture.supplyAsync(() -> saveInventory(playerUuid, inventoryName, inventoryData));
    }
    
    public CompletableFuture<String> loadInventoryAsync(UUID playerUuid, String inventoryName) {
        return CompletableFuture.supplyAsync(() -> loadInventory(playerUuid, inventoryName));
    }
    
    public CompletableFuture<List<String>> getSavedInventoriesAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getSavedInventories(playerUuid));
    }
    
    public CompletableFuture<Boolean> deleteInventoryAsync(UUID playerUuid, String inventoryName) {
        return CompletableFuture.supplyAsync(() -> deleteInventory(playerUuid, inventoryName));
    }
    
    public CompletableFuture<Boolean> saveEnderChestAsync(UUID playerUuid, String enderChestName, String enderChestData) {
        return CompletableFuture.supplyAsync(() -> saveEnderChest(playerUuid, enderChestName, enderChestData));
    }
    
    public CompletableFuture<String> loadEnderChestAsync(UUID playerUuid, String enderChestName) {
        return CompletableFuture.supplyAsync(() -> loadEnderChest(playerUuid, enderChestName));
    }
    
    public CompletableFuture<List<String>> getSavedEnderChestsAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getSavedEnderChests(playerUuid));
    }
    
    public CompletableFuture<Boolean> deleteEnderChestAsync(UUID playerUuid, String enderChestName) {
        return CompletableFuture.supplyAsync(() -> deleteEnderChest(playerUuid, enderChestName));
    }
    
    public CompletableFuture<Boolean> deleteAllInventoriesAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> deleteAllInventories(playerUuid));
    }
    
    public CompletableFuture<Boolean> deleteAllEnderChestsAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> deleteAllEnderChests(playerUuid));
    }
} 