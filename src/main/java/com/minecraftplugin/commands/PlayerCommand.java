package com.minecraftplugin.commands;

import com.minecraftplugin.AdvancedMinecraftPlugin;
import com.minecraftplugin.economy.EconomyManager;
import com.minecraftplugin.inventory.InventoryManager;
import com.minecraftplugin.enderchest.EnderChestManager;
import com.minecraftplugin.utils.MessageUtils;
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
import java.util.stream.Collectors;

public class PlayerCommand implements CommandExecutor, TabCompleter {
    
    private static final Logger logger = LoggerFactory.getLogger(PlayerCommand.class);
    
    private final EconomyManager economyManager;
    private final InventoryManager inventoryManager;
    private final EnderChestManager enderChestManager;
    private final AdvancedMinecraftPlugin plugin;
    
    public PlayerCommand(EconomyManager economyManager, InventoryManager inventoryManager, EnderChestManager enderChestManager) {
        this.economyManager = economyManager;
        this.inventoryManager = inventoryManager;
        this.enderChestManager = enderChestManager;
        this.plugin = economyManager.getPlugin();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.formatColors("&cThis command can only be used by players"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "balance":
            case "money":
                handleBalanceCommand(player);
                break;
            case "inventory":
            case "inv":
                handleInventoryCommand(player, args);
                break;
            case "enderchest":
            case "ec":
                handleEnderChestCommand(player, args);
                break;
            case "help":
                sendHelpMessage(player);
                break;
            default:
                sendHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    private void handleBalanceCommand(Player player) {
        double balance = economyManager.getBalance(player.getUniqueId());
        String currencySymbol = plugin.getConfigManager().getConfig().getString("plugin.economy.currency_symbol", "$");
        String message = plugin.getConfigManager().getConfig().getString("messages.economy.balance");
        String formattedMessage = MessageUtils.formatMessage(message, "currency", currencySymbol, "balance", String.format("%.2f", balance));
        player.sendMessage(MessageUtils.formatColors(formattedMessage));
    }
    
    private void handleInventoryCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.formatColors("&cUsage: /plugin inventory <save|load|list> [name]"));
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "save":
                if (args.length < 3) {
                    player.sendMessage(MessageUtils.formatColors("&cUsage: /plugin inventory save <name>"));
                    return;
                }
                String saveName = args[2];
                if (inventoryManager.saveInventory(player.getUniqueId(), saveName)) {
                    String message = plugin.getConfigManager().getConfig().getString("messages.inventory.saved_self");
                    String formattedMessage = MessageUtils.formatMessage(message, "name", saveName);
                    player.sendMessage(MessageUtils.formatColors(formattedMessage));
                } else {
                    player.sendMessage(MessageUtils.formatColors("&cFailed to save inventory"));
                }
                break;
                
            case "load":
                if (args.length < 3) {
                    player.sendMessage(MessageUtils.formatColors("&cUsage: /plugin inventory load <name>"));
                    return;
                }
                String loadName = args[2];
                if (inventoryManager.loadInventory(player.getUniqueId(), loadName)) {
                    String message = plugin.getConfigManager().getConfig().getString("messages.inventory.loaded_self");
                    String formattedMessage = MessageUtils.formatMessage(message, "name", loadName);
                    player.sendMessage(MessageUtils.formatColors(formattedMessage));
                } else {
                    String message = plugin.getConfigManager().getConfig().getString("messages.inventory.not_found_self");
                    String formattedMessage = MessageUtils.formatMessage(message, "name", loadName);
                    player.sendMessage(MessageUtils.formatColors(formattedMessage));
                }
                break;
                
            case "list":
                List<String> inventories = inventoryManager.getSavedInventories(player.getUniqueId());
                if (inventories.isEmpty()) {
                    String message = plugin.getConfigManager().getConfig().getString("messages.inventory.no_saved_inventories_self");
                    player.sendMessage(MessageUtils.formatColors(message));
                } else {
                    String message = plugin.getConfigManager().getConfig().getString("messages.inventory.list_header_self");
                    player.sendMessage(MessageUtils.formatColors(message));
                    
                    for (String inventoryName : inventories) {
                        String listItem = plugin.getConfigManager().getConfig().getString("messages.inventory.list_item");
                        String formattedListItem = MessageUtils.formatMessage(listItem, "name", inventoryName, "date", "");
                        player.sendMessage(MessageUtils.formatColors(formattedListItem));
                    }
                }
                break;
                
            default:
                player.sendMessage(MessageUtils.formatColors("&cUsage: /plugin inventory <save|load|list> [name]"));
                break;
        }
    }
    
    private void handleEnderChestCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.formatColors("&cUsage: /plugin enderchest <save|load|list> [name]"));
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "save":
                if (args.length < 3) {
                    player.sendMessage(MessageUtils.formatColors("&cUsage: /plugin enderchest save <name>"));
                    return;
                }
                String saveName = args[2];
                if (enderChestManager.saveEnderChest(player.getUniqueId(), saveName)) {
                    String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.saved_self");
                    String formattedMessage = MessageUtils.formatMessage(message, "name", saveName);
                    player.sendMessage(MessageUtils.formatColors(formattedMessage));
                } else {
                    player.sendMessage(MessageUtils.formatColors("&cFailed to save ender chest"));
                }
                break;
                
            case "load":
                if (args.length < 3) {
                    player.sendMessage(MessageUtils.formatColors("&cUsage: /plugin enderchest load <name>"));
                    return;
                }
                String loadName = args[2];
                if (enderChestManager.loadEnderChest(player.getUniqueId(), loadName)) {
                    String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.loaded_self");
                    String formattedMessage = MessageUtils.formatMessage(message, "name", loadName);
                    player.sendMessage(MessageUtils.formatColors(formattedMessage));
                } else {
                    String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.not_found_self");
                    String formattedMessage = MessageUtils.formatMessage(message, "name", loadName);
                    player.sendMessage(MessageUtils.formatColors(formattedMessage));
                }
                break;
                
            case "list":
                List<String> enderChests = enderChestManager.getSavedEnderChests(player.getUniqueId());
                if (enderChests.isEmpty()) {
                    String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.no_saved_ender_chests_self");
                    player.sendMessage(MessageUtils.formatColors(message));
                } else {
                    String message = plugin.getConfigManager().getConfig().getString("messages.ender_chest.list_header_self");
                    player.sendMessage(MessageUtils.formatColors(message));
                    
                    for (String enderChestName : enderChests) {
                        String listItem = plugin.getConfigManager().getConfig().getString("messages.ender_chest.list_item");
                        String formattedListItem = MessageUtils.formatMessage(listItem, "name", enderChestName, "date", "");
                        player.sendMessage(MessageUtils.formatColors(formattedListItem));
                    }
                }
                break;
                
            default:
                player.sendMessage(MessageUtils.formatColors("&cUsage: /plugin enderchest <save|load|list> [name]"));
                break;
        }
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage(MessageUtils.formatColors("&8&m&l                    &r &bPlugin Commands &8&m&l                    "));
        player.sendMessage(MessageUtils.formatColors("&7/plugin balance &8- &fCheck your balance"));
        player.sendMessage(MessageUtils.formatColors("&7/plugin inventory save <name> &8- &fSave your inventory"));
        player.sendMessage(MessageUtils.formatColors("&7/plugin inventory load <name> &8- &fLoad saved inventory"));
        player.sendMessage(MessageUtils.formatColors("&7/plugin inventory list &8- &fList saved inventories"));
        player.sendMessage(MessageUtils.formatColors("&7/plugin enderchest save <name> &8- &fSave your ender chest"));
        player.sendMessage(MessageUtils.formatColors("&7/plugin enderchest load <name> &8- &fLoad saved ender chest"));
        player.sendMessage(MessageUtils.formatColors("&7/plugin enderchest list &8- &fList saved ender chests"));
        player.sendMessage(MessageUtils.formatColors("&7/plugin help &8- &fShow this help message"));
        player.sendMessage(MessageUtils.formatColors("&8&m&l                                                        "));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("balance", "money", "inventory", "inv", "enderchest", "ec", "help").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("inventory") || args[0].equalsIgnoreCase("inv")) {
                return Arrays.asList("save", "load", "list").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("enderchest") || args[0].equalsIgnoreCase("ec")) {
                return Arrays.asList("save", "load", "list").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            Player player = (Player) sender;
            if ((args[0].equalsIgnoreCase("inventory") || args[0].equalsIgnoreCase("inv")) && 
                (args[1].equalsIgnoreCase("load"))) {
                return inventoryManager.getSavedInventories(player.getUniqueId()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } else if ((args[0].equalsIgnoreCase("enderchest") || args[0].equalsIgnoreCase("ec")) && 
                       (args[1].equalsIgnoreCase("load"))) {
                return enderChestManager.getSavedEnderChests(player.getUniqueId()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
} 