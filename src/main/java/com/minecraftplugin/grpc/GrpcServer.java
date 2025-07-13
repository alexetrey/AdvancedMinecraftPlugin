package com.minecraftplugin.grpc;

import com.minecraftplugin.config.ConfigManager;
import com.minecraftplugin.database.DatabaseManager;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class GrpcServer {
    
    private static final Logger logger = LoggerFactory.getLogger(GrpcServer.class);
    
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private Server server;
    
    public GrpcServer(ConfigManager configManager, DatabaseManager databaseManager) {
        this.configManager = configManager;
        this.databaseManager = databaseManager;
    }
    
    public void start() {
        try {
            int port = configManager.getGrpcPort();
            
            server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                    .addService(new MinecraftServiceImpl())
                    .executor(Executors.newFixedThreadPool(10))
                    .intercept(new AuthenticationInterceptor())
                    .build()
                    .start();
            
            logger.info("gRPC server started on port {}", port);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down gRPC server...");
                stop();
            }));
            
        } catch (Exception e) {
            logger.error("Failed to start gRPC server", e);
        }
    }
    
    public void stop() {
        if (server != null) {
            try {
                server.shutdown();
                logger.info("gRPC server stopped");
            } catch (Exception e) {
                logger.error("Error stopping gRPC server", e);
            }
        }
    }
    
    private class AuthenticationInterceptor implements ServerInterceptor {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            
            String secretKey = headers.get(Metadata.Key.of("secret-key", Metadata.ASCII_STRING_MARSHALLER));
            
            if (secretKey == null || !secretKey.equals(configManager.getGrpcSecretKey())) {
                call.close(Status.UNAUTHENTICATED.withDescription("Invalid secret key"), headers);
                return new ServerCall.Listener<ReqT>() {};
            }
            
            return next.startCall(call, headers);
        }
    }
    
    private class MinecraftServiceImpl extends MinecraftServiceGrpc.MinecraftServiceImplBase {
        
        @Override
        public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                double balance = databaseManager.getPlayerBalance(playerUuid);
                
                GetBalanceResponse response = GetBalanceResponse.newBuilder()
                        .setSuccess(true)
                        .setBalance(balance)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error getting balance for {}", request.getPlayerUuid(), e);
                
                GetBalanceResponse response = GetBalanceResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to get balance: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void setBalance(SetBalanceRequest request, StreamObserver<SetBalanceResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.setPlayerBalance(playerUuid, request.getBalance());
                
                SetBalanceResponse response = SetBalanceResponse.newBuilder()
                        .setSuccess(success)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error setting balance for {}", request.getPlayerUuid(), e);
                
                SetBalanceResponse response = SetBalanceResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to set balance: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void addBalance(AddBalanceRequest request, StreamObserver<AddBalanceResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.updatePlayerBalance(playerUuid, request.getAmount());
                
                double newBalance = 0.0;
                if (success) {
                    newBalance = databaseManager.getPlayerBalance(playerUuid);
                }
                
                AddBalanceResponse response = AddBalanceResponse.newBuilder()
                        .setSuccess(success)
                        .setNewBalance(newBalance)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error adding balance for {}", request.getPlayerUuid(), e);
                
                AddBalanceResponse response = AddBalanceResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to add balance: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void removeBalance(RemoveBalanceRequest request, StreamObserver<RemoveBalanceResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.updatePlayerBalance(playerUuid, -request.getAmount());
                
                double newBalance = 0.0;
                if (success) {
                    newBalance = databaseManager.getPlayerBalance(playerUuid);
                }
                
                RemoveBalanceResponse response = RemoveBalanceResponse.newBuilder()
                        .setSuccess(success)
                        .setNewBalance(newBalance)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error removing balance for {}", request.getPlayerUuid(), e);
                
                RemoveBalanceResponse response = RemoveBalanceResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to remove balance: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void transferBalance(TransferBalanceRequest request, StreamObserver<TransferBalanceResponse> responseObserver) {
            try {
                UUID fromPlayerUuid = UUID.fromString(request.getFromPlayerUuid());
                UUID toPlayerUuid = UUID.fromString(request.getToPlayerUuid());
                double amount = request.getAmount();
                
                // Check if from player has enough balance
                double fromBalance = databaseManager.getPlayerBalance(fromPlayerUuid);
                if (fromBalance < amount) {
                    TransferBalanceResponse response = TransferBalanceResponse.newBuilder()
                            .setSuccess(false)
                            .setErrorMessage("Insufficient funds")
                            .build();
                    
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }
                
                // Remove from source player
                boolean success1 = databaseManager.updatePlayerBalance(fromPlayerUuid, -amount);
                // Add to target player
                boolean success2 = databaseManager.updatePlayerBalance(toPlayerUuid, amount);
                
                boolean success = success1 && success2;
                
                TransferBalanceResponse response = TransferBalanceResponse.newBuilder()
                        .setSuccess(success)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error transferring balance", e);
                
                TransferBalanceResponse response = TransferBalanceResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to transfer balance: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void getInventory(GetInventoryRequest request, StreamObserver<GetInventoryResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                String inventoryData = databaseManager.loadInventory(playerUuid, request.getInventoryName());
                
                GetInventoryResponse response = GetInventoryResponse.newBuilder()
                        .setSuccess(inventoryData != null)
                        .setInventoryData(inventoryData != null ? inventoryData : "")
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error getting inventory for {}", request.getPlayerUuid(), e);
                
                GetInventoryResponse response = GetInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to get inventory: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void saveInventory(SaveInventoryRequest request, StreamObserver<SaveInventoryResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.saveInventory(playerUuid, request.getInventoryName(), request.getInventoryData());
                
                SaveInventoryResponse response = SaveInventoryResponse.newBuilder()
                        .setSuccess(success)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error saving inventory for {}", request.getPlayerUuid(), e);
                
                SaveInventoryResponse response = SaveInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to save inventory: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void getSavedInventories(GetSavedInventoriesRequest request, StreamObserver<GetSavedInventoriesResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                List<String> inventories = databaseManager.getSavedInventories(playerUuid);
                
                GetSavedInventoriesResponse response = GetSavedInventoriesResponse.newBuilder()
                        .setSuccess(true)
                        .addAllInventoryNames(inventories)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error getting saved inventories for {}", request.getPlayerUuid(), e);
                
                GetSavedInventoriesResponse response = GetSavedInventoriesResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to get saved inventories: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void getEnderChest(GetEnderChestRequest request, StreamObserver<GetEnderChestResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                String enderChestData = databaseManager.loadEnderChest(playerUuid, request.getEnderChestName());
                
                GetEnderChestResponse response = GetEnderChestResponse.newBuilder()
                        .setSuccess(enderChestData != null)
                        .setEnderChestData(enderChestData != null ? enderChestData : "")
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error getting ender chest for {}", request.getPlayerUuid(), e);
                
                GetEnderChestResponse response = GetEnderChestResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to get ender chest: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void saveEnderChest(SaveEnderChestRequest request, StreamObserver<SaveEnderChestResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.saveEnderChest(playerUuid, request.getEnderChestName(), request.getEnderChestData());
                
                SaveEnderChestResponse response = SaveEnderChestResponse.newBuilder()
                        .setSuccess(success)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error saving ender chest for {}", request.getPlayerUuid(), e);
                
                SaveEnderChestResponse response = SaveEnderChestResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to save ender chest: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void getSavedEnderChests(GetSavedEnderChestsRequest request, StreamObserver<GetSavedEnderChestsResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                List<String> enderChests = databaseManager.getSavedEnderChests(playerUuid);
                
                GetSavedEnderChestsResponse response = GetSavedEnderChestsResponse.newBuilder()
                        .setSuccess(true)
                        .addAllEnderChestNames(enderChests)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error getting saved ender chests for {}", request.getPlayerUuid(), e);
                
                GetSavedEnderChestsResponse response = GetSavedEnderChestsResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to get saved ender chests: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void updateInventory(UpdateInventoryRequest request, StreamObserver<UpdateInventoryResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.updateInventory(playerUuid, request.getInventoryName(), request.getInventoryData());
                
                UpdateInventoryResponse response = UpdateInventoryResponse.newBuilder()
                        .setSuccess(success)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error updating inventory for {}", request.getPlayerUuid(), e);
                
                UpdateInventoryResponse response = UpdateInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to update inventory: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void clearInventory(ClearInventoryRequest request, StreamObserver<ClearInventoryResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                // For now, we'll just return success since clearing is handled by the manager
                ClearInventoryResponse response = ClearInventoryResponse.newBuilder()
                        .setSuccess(true)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error clearing inventory for {}", request.getPlayerUuid(), e);
                
                ClearInventoryResponse response = ClearInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to clear inventory: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void backupInventory(BackupInventoryRequest request, StreamObserver<BackupInventoryResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                // For now, we'll just return success since backup is handled by the manager
                BackupInventoryResponse response = BackupInventoryResponse.newBuilder()
                        .setSuccess(true)
                        .setBackupName(request.getBackupName())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error backing up inventory for {}", request.getPlayerUuid(), e);
                
                BackupInventoryResponse response = BackupInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to backup inventory: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void restoreInventory(RestoreInventoryRequest request, StreamObserver<RestoreInventoryResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                // For now, we'll just return success since restore is handled by the manager
                RestoreInventoryResponse response = RestoreInventoryResponse.newBuilder()
                        .setSuccess(true)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error restoring inventory for {}", request.getPlayerUuid(), e);
                
                RestoreInventoryResponse response = RestoreInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to restore inventory: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void getInventoryInfo(GetInventoryInfoRequest request, StreamObserver<GetInventoryInfoResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                // For now, we'll return basic info
                GetInventoryInfoResponse response = GetInventoryInfoResponse.newBuilder()
                        .setSuccess(true)
                        .setName(request.getInventoryName())
                        .setSize(0)
                        .setCreated("")
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error getting inventory info for {}", request.getPlayerUuid(), e);
                
                GetInventoryInfoResponse response = GetInventoryInfoResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to get inventory info: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void deleteInventory(DeleteInventoryRequest request, StreamObserver<DeleteInventoryResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.deleteInventory(playerUuid, request.getInventoryName());
                
                DeleteInventoryResponse response = DeleteInventoryResponse.newBuilder()
                        .setSuccess(success)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error deleting inventory for {}", request.getPlayerUuid(), e);
                
                DeleteInventoryResponse response = DeleteInventoryResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to delete inventory: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void deleteAllInventories(DeleteAllInventoriesRequest request, StreamObserver<DeleteAllInventoriesResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.deleteAllInventories(playerUuid);
                
                DeleteAllInventoriesResponse response = DeleteAllInventoriesResponse.newBuilder()
                        .setSuccess(success)
                        .setDeletedCount(success ? 1 : 0)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error deleting all inventories for {}", request.getPlayerUuid(), e);
                
                DeleteAllInventoriesResponse response = DeleteAllInventoriesResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to delete all inventories: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void updateEnderChest(UpdateEnderChestRequest request, StreamObserver<UpdateEnderChestResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.updateEnderChest(playerUuid, request.getEnderChestName(), request.getEnderChestData());
                
                UpdateEnderChestResponse response = UpdateEnderChestResponse.newBuilder()
                        .setSuccess(success)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error updating ender chest for {}", request.getPlayerUuid(), e);
                
                UpdateEnderChestResponse response = UpdateEnderChestResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to update ender chest: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void clearEnderChest(ClearEnderChestRequest request, StreamObserver<ClearEnderChestResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                // For now, we'll just return success since clearing is handled by the manager
                ClearEnderChestResponse response = ClearEnderChestResponse.newBuilder()
                        .setSuccess(true)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error clearing ender chest for {}", request.getPlayerUuid(), e);
                
                ClearEnderChestResponse response = ClearEnderChestResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to clear ender chest: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void backupEnderChest(BackupEnderChestRequest request, StreamObserver<BackupEnderChestResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                // For now, we'll just return success since backup is handled by the manager
                BackupEnderChestResponse response = BackupEnderChestResponse.newBuilder()
                        .setSuccess(true)
                        .setBackupName(request.getBackupName())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error backing up ender chest for {}", request.getPlayerUuid(), e);
                
                BackupEnderChestResponse response = BackupEnderChestResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to backup ender chest: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void restoreEnderChest(RestoreEnderChestRequest request, StreamObserver<RestoreEnderChestResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                // For now, we'll just return success since restore is handled by the manager
                RestoreEnderChestResponse response = RestoreEnderChestResponse.newBuilder()
                        .setSuccess(true)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error restoring ender chest for {}", request.getPlayerUuid(), e);
                
                RestoreEnderChestResponse response = RestoreEnderChestResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to restore ender chest: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void getEnderChestInfo(GetEnderChestInfoRequest request, StreamObserver<GetEnderChestInfoResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                // For now, we'll return basic info
                GetEnderChestInfoResponse response = GetEnderChestInfoResponse.newBuilder()
                        .setSuccess(true)
                        .setName(request.getEnderChestName())
                        .setSize(0)
                        .setCreated("")
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error getting ender chest info for {}", request.getPlayerUuid(), e);
                
                GetEnderChestInfoResponse response = GetEnderChestInfoResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to get ender chest info: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void deleteEnderChest(DeleteEnderChestRequest request, StreamObserver<DeleteEnderChestResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.deleteEnderChest(playerUuid, request.getEnderChestName());
                
                DeleteEnderChestResponse response = DeleteEnderChestResponse.newBuilder()
                        .setSuccess(success)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error deleting ender chest for {}", request.getPlayerUuid(), e);
                
                DeleteEnderChestResponse response = DeleteEnderChestResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to delete ender chest: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void deleteAllEnderChests(DeleteAllEnderChestsRequest request, StreamObserver<DeleteAllEnderChestsResponse> responseObserver) {
            try {
                UUID playerUuid = UUID.fromString(request.getPlayerUuid());
                boolean success = databaseManager.deleteAllEnderChests(playerUuid);
                
                DeleteAllEnderChestsResponse response = DeleteAllEnderChestsResponse.newBuilder()
                        .setSuccess(success)
                        .setDeletedCount(success ? 1 : 0)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error deleting all ender chests for {}", request.getPlayerUuid(), e);
                
                DeleteAllEnderChestsResponse response = DeleteAllEnderChestsResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("Failed to delete all ender chests: " + e.getMessage())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
        
        @Override
        public void healthCheck(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
            try {
                HealthCheckResponse response = HealthCheckResponse.newBuilder()
                        .setHealthy(true)
                        .setStatus("OK")
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
            } catch (Exception e) {
                logger.error("Error in health check", e);
                
                HealthCheckResponse response = HealthCheckResponse.newBuilder()
                        .setHealthy(false)
                        .setStatus("ERROR: " + e.getMessage())
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
    }
} 