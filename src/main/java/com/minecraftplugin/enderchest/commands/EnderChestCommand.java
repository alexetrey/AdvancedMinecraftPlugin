package com.minecraftplugin.enderchest.commands;

import com.minecraftplugin.AdvancedMinecraftPlugin;
import com.minecraftplugin.enderchest.EnderChestManager;
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

public class EnderChestCommand implements CommandExecutor, TabCompleter {
    
    private static final Logger logger = LoggerFactory.getLogger(EnderChestCommand.class);
    
    private final EnderChestManager enderChestManager;
    private final AdvancedMinecraftPlugin plugin;
    
    public EnderChestCommand(EnderChestManager enderChestManager) {
        this.enderChestManager = enderChestManager;
        this.plugin = enderChestManager.getPlugin();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("advancedplugin.enderchest")) {
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
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec save <player> [name]"));
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
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to save ender chest"));
            return;
        }
        
        String enderChestName = args.length > 2 ? args[2] : "default";
        
        if (enderChestManager.saveEnderChest(targetPlayer.getUniqueId(), enderChestName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.saved");
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
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec load <player> [name]"));
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
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to load ender chest"));
            return;
        }
        
        String enderChestName = args.length > 2 ? args[2] : "default";
        
        if (enderChestManager.loadEnderChest(targetPlayer.getUniqueId(), enderChestName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.loaded");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    @SuppressWarnings("deprecation")
    private void handleUpdateCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec update <player> <name>"));
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
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to update ender chest"));
            return;
        }
        
        String enderChestName = args[2];
        
        if (enderChestManager.updateEnderChest(targetPlayer.getUniqueId(), enderChestName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.updated");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "name", enderChestName);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void handleClearCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec clear <player>"));
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
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to clear ender chest"));
            return;
        }
        
        if (enderChestManager.clearEnderChest(targetPlayer.getUniqueId())) {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.cleared");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.database_error");
            sender.sendMessage(MessageUtils.formatColors(message));
        }
    }
    
    private void handleBackupCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec backup <player> <name>"));
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
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to backup ender chest"));
            return;
        }
        
        String backupName = args[2];
        
        if (enderChestManager.backupEnderChest(targetPlayer.getUniqueId(), backupName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.backed_up");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "name", backupName);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.database_error");
            sender.sendMessage(MessageUtils.formatColors(message));
        }
    }
    
    private void handleRestoreCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec restore <player> <name>"));
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
            sender.sendMessage(MessageUtils.formatColors("&cPlayer must be online to restore ender chest"));
            return;
        }
        
        String backupName = args[2];
        
        if (enderChestManager.restoreEnderChest(targetPlayer.getUniqueId(), backupName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.restored");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "name", backupName);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.backup_not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "name", backupName);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void handleInfoCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec info <player> <name>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        String enderChestName = args[2];
        Map<String, String> info = enderChestManager.getEnderChestInfo(targetPlayer.getUniqueId(), enderChestName);
        
        if (info != null) {
            sender.sendMessage(MessageUtils.formatColors("&8&m&l                    &r &bEnder Chest Info &8&m&l                    "));
            sender.sendMessage(MessageUtils.formatColors("&7Name: &f" + info.get("name")));
            sender.sendMessage(MessageUtils.formatColors("&7Size: &f" + info.get("size") + " bytes"));
            sender.sendMessage(MessageUtils.formatColors("&7Created: &f" + info.get("created")));
            sender.sendMessage(MessageUtils.formatColors("&8&m&l                                                        "));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    @SuppressWarnings("deprecation")
    private void handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec delete <player> <name>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        String enderChestName = args[2];
        
        if (enderChestManager.deleteEnderChest(targetPlayer.getUniqueId(), enderChestName)) {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.deleted");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        } else {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.database_error");
            sender.sendMessage(MessageUtils.formatColors(message));
        }
    }
    
    private void handleDeleteAllCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec deleteall <player>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        if (enderChestManager.deleteAllEnderChests(targetPlayer.getUniqueId())) {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.all_deleted");
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
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /ec list <player>"));
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.errors.player_not_found", "&cPlayer not found: {player}");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        List<String> enderChests = enderChestManager.getSavedEnderChests(targetPlayer.getUniqueId());
        
        if (enderChests.isEmpty()) {
            String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.no_saved_ender_chests");
            sender.sendMessage(MessageUtils.formatColors(message));
        } else {
        String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.list_header");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName());
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        
        for (String enderChestName : enderChests) {
            String listItem = plugin.getConfigManager().getConfig().getString("messages.ender_chest.list_item");
                String formattedListItem = MessageUtils.formatMessage(listItem, "name", enderChestName, "date", "");
                sender.sendMessage(MessageUtils.formatColors(formattedListItem));
            }
        }
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MessageUtils.formatColors("&8&m&l                    &r &bEnder Chest Commands &8&m&l                    "));
        sender.sendMessage(MessageUtils.formatColors("&7/ec save <player> [name] &8- &fSave player ender chest"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec load <player> [name] &8- &fLoad saved ender chest"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec update <player> <name> &8- &fUpdate saved ender chest"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec clear <player> &8- &fClear player ender chest"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec backup <player> <name> &8- &fCreate backup"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec restore <player> <name> &8- &fRestore from backup"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec info <player> <name> &8- &fShow ender chest info"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec delete <player> <name> &8- &fDelete saved ender chest"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec deleteall <player> &8- &fDelete all ender chests"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec list <player> &8- &fList saved ender chests"));
        sender.sendMessage(MessageUtils.formatColors("&7/ec help &8- &fShow this help message"));
        sender.sendMessage(MessageUtils.formatColors("&8&m&l                                                        "));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("advancedplugin.enderchest")) {
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
                // Return saved ender chest names for the player
                try {
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
                    if (targetPlayer != null) {
                        return enderChestManager.getSavedEnderChests(targetPlayer.getUniqueId()).stream()
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