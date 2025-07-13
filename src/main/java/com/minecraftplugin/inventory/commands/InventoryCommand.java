package com.minecraftplugin.inventory.commands;

import com.minecraftplugin.AdvancedMinecraftPlugin;
import com.minecraftplugin.inventory.InventoryManager;
import com.minecraftplugin.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;

public class InventoryCommand implements CommandExecutor, TabCompleter {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryCommand.class);
    
    private final InventoryManager inventoryManager;
    private final AdvancedMinecraftPlugin plugin;
    
    public InventoryCommand(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
        this.plugin = inventoryManager.getPlugin();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("advancedplugin.inventory")) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.no_permission");
            sender.sendMessage(MessageUtils.formatColors(message));
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "save":
                handleSaveCommand(sender, args);
                break;
            case "load":
                handleLoadCommand(sender, args);
                break;
            case "update":
                handleUpdateCommand(sender, args);
                break;
            case "clear":
                handleClearCommand(sender, args);
                break;
            case "backup":
                handleBackupCommand(sender, args);
                break;
            case "restore":
                handleRestoreCommand(sender, args);
                break;
            case "info":
                handleInfoCommand(sender, args);
                break;
            case "delete":
                handleDeleteCommand(sender, args);
                break;
            case "deleteall":
                handleDeleteAllCommand(sender, args);
                break;
            case "list":
                handleListCommand(sender, args);
                break;
            case "help":
                sendHelpMessage(sender);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }
    
    @SuppressWarnings("deprecation")
    private void handleSaveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv save <player> [name]"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        Player onlinePlayer = targetPlayer.getPlayer();
        if (onlinePlayer == null || !onlinePlayer.isOnline()) {
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to save inventory"));
            return;
        }
        
        String inventoryName = args.length > 2 ? args[2] : "default";
        
        if (inventoryManager.saveInventory(targetPlayer.getUniqueId(), inventoryName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.saved");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.database_error");
            sender.sendMessage(MessageUtils.formatColors(message));
        }
    }
    
    @SuppressWarnings("deprecation")
    private void handleLoadCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv load <player> [name]"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        Player onlinePlayer = targetPlayer.getPlayer();
        if (onlinePlayer == null || !onlinePlayer.isOnline()) {
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to load inventory"));
            return;
        }
        
        String inventoryName = args.length > 2 ? args[2] : "default";
        
        if (inventoryManager.loadInventory(targetPlayer.getUniqueId(), inventoryName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.loaded");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    @SuppressWarnings("deprecation")
    private void handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv delete <player> <name>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        String inventoryName = args[2];
        
        if (inventoryManager.deleteInventory(targetPlayer.getUniqueId(), inventoryName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.deleted");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.database_error");
            sender.sendMessage(MessageUtils.formatColors(message));
        }
    }
    
    @SuppressWarnings("deprecation")
    private void handleListCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv list <player>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        List<String> inventories = inventoryManager.getSavedInventories(targetPlayer.getUniqueId());
        
        if (inventories.isEmpty()) {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.no_saved_inventories");
            sender.sendMessage(MessageUtils.formatColors(message));
        } else {
        String message = plugin.getConfigManager().getConfig().getString("messages.inventory.list_header");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        
        for (String inventoryName : inventories) {
            String listItem = plugin.getConfigManager().getConfig().getString("messages.inventory.list_item");
                String formattedListItem = MessageUtils.formatMessage(listItem, "name", inventoryName, "date", "");
                sender.sendMessage(MessageUtils.formatColors(formattedListItem));
            }
        }
    }
    
    private void handleUpdateCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv update <player> <name>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        Player onlinePlayer = targetPlayer.getPlayer();
        if (onlinePlayer == null || !onlinePlayer.isOnline()) {
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to update inventory"));
            return;
        }
        
        String inventoryName = args[2];
        
        if (inventoryManager.updateInventory(targetPlayer.getUniqueId(), inventoryName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.updated");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "name", inventoryName);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void handleClearCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv clear <player>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        Player onlinePlayer = targetPlayer.getPlayer();
        if (onlinePlayer == null || !onlinePlayer.isOnline()) {
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to clear inventory"));
            return;
        }
        
        if (inventoryManager.clearInventory(targetPlayer.getUniqueId())) {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.cleared");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.database_error");
            sender.sendMessage(MessageUtils.formatColors(message));
        }
    }
    
    private void handleBackupCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv backup <player> <name>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        Player onlinePlayer = targetPlayer.getPlayer();
        if (onlinePlayer == null || !onlinePlayer.isOnline()) {
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to backup inventory"));
            return;
        }
        
        String backupName = args[2];
        
        if (inventoryManager.backupInventory(targetPlayer.getUniqueId(), backupName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.backed_up");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "name", backupName);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.database_error");
            sender.sendMessage(MessageUtils.formatColors(message));
        }
    }
    
    private void handleRestoreCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv restore <player> <name>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        Player onlinePlayer = targetPlayer.getPlayer();
        if (onlinePlayer == null || !onlinePlayer.isOnline()) {
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to restore inventory"));
            return;
        }
        
        String backupName = args[2];
        
        if (inventoryManager.restoreInventory(targetPlayer.getUniqueId(), backupName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.restored");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "name", backupName);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.backup_not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "name", backupName);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void handleInfoCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv info <player> <name>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        String inventoryName = args[2];
        Map<String, String> info = inventoryManager.getInventoryInfo(targetPlayer.getUniqueId(), inventoryName);
        
        if (info != null) {
            sender.sendMessage(MessageUtils.formatColors("&8&m&l                    &r &bInventory Info &8&m&l                    "));
            sender.sendMessage(MessageUtils.formatColors("&7Name: &f" + info.get("name")));
            sender.sendMessage(MessageUtils.formatColors("&7Size: &f" + info.get("size") + " bytes"));
            sender.sendMessage(MessageUtils.formatColors("&7Created: &f" + info.get("created")));
            sender.sendMessage(MessageUtils.formatColors("&8&m&l                                                        "));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void handleDeleteAllCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /inv deleteall <player>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        if (inventoryManager.deleteAllInventories(targetPlayer.getUniqueId())) {
            String message = plugin.getConfigManager().getConfig().getString("messages.inventory.all_deleted");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.database_error");
            sender.sendMessage(MessageUtils.formatColors(message));
        }
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MessageUtils.formatColors("&8&m&l                    &r &bInventory Commands &8&m&l                    "));
        sender.sendMessage(MessageUtils.formatColors("&7/inv save <player> [name] &8- &fSave player inventory"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv load <player> [name] &8- &fLoad saved inventory"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv update <player> <name> &8- &fUpdate saved inventory"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv clear <player> &8- &fClear player inventory"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv backup <player> <name> &8- &fCreate backup"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv restore <player> <name> &8- &fRestore from backup"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv info <player> <name> &8- &fShow inventory info"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv delete <player> <name> &8- &fDelete saved inventory"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv deleteall <player> &8- &fDelete all inventories"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv list <player> &8- &fList saved inventories"));
        sender.sendMessage(MessageUtils.formatColors("&7/inv help &8- &fShow this help message"));
        sender.sendMessage(MessageUtils.formatColors("&8&m&l                                                        "));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("advancedplugin.inventory")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("save", "load", "update", "clear", "backup", "restore", "info", "delete", "deleteall", "list", "help").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            return getOnlinePlayerNames(args[1]);
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("load") || args[0].equalsIgnoreCase("update") || 
                args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("info")) {
                // Return saved inventory names for the player
                try {
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
                    if (targetPlayer != null) {
                        return inventoryManager.getSavedInventories(targetPlayer.getUniqueId()).stream()
                                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                } catch (Exception e) {
                    // Ignore errors
                }
            }
        }
        
        return new ArrayList<>();
    }
    
    private List<String> getOnlinePlayerNames(String partial) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }
} 