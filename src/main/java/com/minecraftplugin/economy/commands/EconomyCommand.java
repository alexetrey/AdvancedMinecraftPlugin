package com.minecraftplugin.economy.commands;

import com.minecraftplugin.AdvancedMinecraftPlugin;
import com.minecraftplugin.economy.EconomyManager;
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

public class EconomyCommand implements CommandExecutor, TabCompleter {
    
    private static final Logger logger = LoggerFactory.getLogger(EconomyCommand.class);
    
    private final EconomyManager economyManager;
    private final AdvancedMinecraftPlugin plugin;
    
    public EconomyCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.plugin = economyManager.getPlugin();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("advancedplugin.economy")) {
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
                case "get":
                    handleGetCommand(sender, args);
                    break;
                case "set":
                    handleSetCommand(sender, args);
                    break;
                case "add":
                    handleAddCommand(sender, args);
                    break;
                case "remove":
                    handleRemoveCommand(sender, args);
                    break;
                case "transfer":
                    handleTransferCommand(sender, args);
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
    
    private void handleGetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                double balance = economyManager.getBalance(player.getUniqueId());
                String currencySymbol = plugin.getConfigManager().getConfig().getString("plugin.economy.currency_symbol", "$");
                String message = plugin.getConfigManager().getConfig().getString("messages.economy.balance");
                String formattedMessage = MessageUtils.formatMessage(message, "currency", currencySymbol, "balance", String.format("%.2f", balance));
                sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            } else {
                sender.sendMessage(MessageUtils.formatColors("&cThis command can only be used by players"));
            }
        } else {
        OfflinePlayer targetPlayer = economyManager.getPlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.player_not_found");
                String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
                sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        double balance = economyManager.getBalance(targetPlayer.getUniqueId());
            String currencySymbol = plugin.getConfigManager().getConfig().getString("plugin.economy.currency_symbol", "$");
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.balance_other");
            String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "currency", currencySymbol, "balance", String.format("%.2f", balance));
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /money set <player> <amount>"));
            return;
        }
        
        OfflinePlayer targetPlayer = economyManager.getPlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.player_not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            if (economyManager.setBalance(targetPlayer.getUniqueId(), amount)) {
                String currencySymbol = plugin.getConfigManager().getConfig().getString("plugin.economy.currency_symbol", "$");
                String message = plugin.getConfigManager().getConfig().getString("messages.economy.balance_set");
                String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "currency", currencySymbol, "balance", String.format("%.2f", amount));
                sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            } else {
                sender.sendMessage(MessageUtils.formatColors("&cFailed to set balance"));
            }
        } catch (NumberFormatException e) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.invalid_amount");
            String formattedMessage = MessageUtils.formatMessage(message, "amount", args[2]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void handleAddCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /money add <player> <amount>"));
            return;
        }
        
        OfflinePlayer targetPlayer = economyManager.getPlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.player_not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            if (economyManager.addBalance(targetPlayer.getUniqueId(), amount)) {
                String currencySymbol = plugin.getConfigManager().getConfig().getString("plugin.economy.currency_symbol", "$");
                String message = plugin.getConfigManager().getConfig().getString("messages.economy.balance_added");
                String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "currency", currencySymbol, "amount", String.format("%.2f", amount));
                sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            } else {
                sender.sendMessage(MessageUtils.formatColors("&cFailed to add balance"));
            }
        } catch (NumberFormatException e) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.invalid_amount");
            String formattedMessage = MessageUtils.formatMessage(message, "amount", args[2]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void handleRemoveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /money remove <player> <amount>"));
            return;
        }
        
        OfflinePlayer targetPlayer = economyManager.getPlayer(args[1]);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.player_not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            if (economyManager.removeBalance(targetPlayer.getUniqueId(), amount)) {
                String currencySymbol = plugin.getConfigManager().getConfig().getString("plugin.economy.currency_symbol", "$");
                String message = plugin.getConfigManager().getConfig().getString("messages.economy.balance_removed");
                String formattedMessage = MessageUtils.formatMessage(message, "player", targetPlayer.getName(), "currency", currencySymbol, "amount", String.format("%.2f", amount));
                sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            } else {
                sender.sendMessage(MessageUtils.formatColors("&cFailed to remove balance"));
            }
        } catch (NumberFormatException e) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.invalid_amount");
            String formattedMessage = MessageUtils.formatMessage(message, "amount", args[2]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void handleTransferCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(MessageUtils.formatColors("&cUsage: /money transfer <from> <to> <amount>"));
            return;
        }
        
        OfflinePlayer fromPlayer = economyManager.getPlayer(args[1]);
        OfflinePlayer toPlayer = economyManager.getPlayer(args[2]);
        
        if (fromPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.player_not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[1]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        if (toPlayer == null) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.player_not_found");
            String formattedMessage = MessageUtils.formatMessage(message, "player", args[2]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[3]);
            if (economyManager.transfer(fromPlayer.getUniqueId(), toPlayer.getUniqueId(), amount)) {
                String currencySymbol = plugin.getConfigManager().getConfig().getString("plugin.economy.currency_symbol", "$");
                sender.sendMessage(MessageUtils.formatColors(String.format("&aTransferred &e%s%.2f &afrom &e%s &ato &e%s", 
                    currencySymbol, amount, fromPlayer.getName(), toPlayer.getName())));
            } else {
                sender.sendMessage(MessageUtils.formatColors("&cFailed to transfer money"));
            }
        } catch (NumberFormatException e) {
            String message = plugin.getConfigManager().getConfig().getString("messages.economy.invalid_amount");
            String formattedMessage = MessageUtils.formatMessage(message, "amount", args[3]);
            sender.sendMessage(MessageUtils.formatColors(formattedMessage));
        }
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MessageUtils.formatColors("&8&m&l                    &r &bEconomy Commands &8&m&l                    "));
        sender.sendMessage(MessageUtils.formatColors("&7/money get [player] &8- &fCheck balance"));
        sender.sendMessage(MessageUtils.formatColors("&7/money set <player> <amount> &8- &fSet player balance"));
        sender.sendMessage(MessageUtils.formatColors("&7/money add <player> <amount> &8- &fAdd to player balance"));
        sender.sendMessage(MessageUtils.formatColors("&7/money remove <player> <amount> &8- &fRemove from player balance"));
        sender.sendMessage(MessageUtils.formatColors("&7/money transfer <from> <to> <amount> &8- &fTransfer money"));
        sender.sendMessage(MessageUtils.formatColors("&7/money help &8- &fShow this help message"));
        sender.sendMessage(MessageUtils.formatColors("&8&m&l                                                        "));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("advancedplugin.economy")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("get", "set", "add", "remove", "transfer", "help").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("get")) {
                return getOnlinePlayerNames(args[1]);
            } else if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add") || 
                       args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("transfer")) {
                return getOnlinePlayerNames(args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("transfer")) {
                return getOnlinePlayerNames(args[2]);
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