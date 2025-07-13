package com.minecraftplugin.utils;

import org.bukkit.ChatColor;

public class MessageUtils {
    
    public static String formatMessage(String message, String... replacements) {
        if (message == null) {
            return "";
        }
        
        if (replacements == null || replacements.length == 0) {
            return formatColors(message);
        }
        
        String formatted = message;
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String key = "{" + replacements[i] + "}";
                String value = replacements[i + 1] != null ? replacements[i + 1] : "";
                formatted = formatted.replace(key, value);
            }
        }
        
        return formatColors(formatted);
    }
    
    public static String formatColors(String message) {
        if (message == null) {
            return "";
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String stripColors(String message) {
        if (message == null) {
            return "";
        }
        
        return ChatColor.stripColor(formatColors(message));
    }
    
    public static String createProgressBar(double current, double max, int length, String filledChar, String emptyChar) {
        if (max <= 0 || length <= 0 || filledChar == null || emptyChar == null) {
            return "";
        }
        
        double percentage = Math.min(Math.max(current / max, 0.0), 1.0);
        int filledLength = (int) (percentage * length);
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i < filledLength) {
                bar.append(filledChar);
            } else {
                bar.append(emptyChar);
            }
        }
        
        return bar.toString();
    }
    
    public static String formatNumber(double number) {
        return String.format("%,.2f", number);
    }
    
    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }
    
    public static String centerMessage(String message, int lineLength) {
        if (message == null || message.isEmpty() || lineLength <= 0) {
            return "";
        }
        
        String stripped = stripColors(message);
        int messageLength = stripped.length();
        
        if (messageLength >= lineLength) {
            return message;
        }
        
        int spaces = (lineLength - messageLength) / 2;
        return " ".repeat(spaces) + message;
    }
    
    public static String createSeparator(int length, String character, String color) {
        if (length <= 0 || character == null || character.isEmpty() || color == null) {
            return "";
        }
        return formatColors(color + character.repeat(length));
    }
    
    public static String createTitle(String title, int lineLength, String separatorChar, String color) {
        if (title == null || lineLength <= 0 || separatorChar == null || color == null) {
            return "";
        }
        
        String separator = createSeparator(lineLength, separatorChar, color);
        String centeredTitle = centerMessage(title, lineLength);
        
        return separator + "\n" + centeredTitle + "\n" + separator;
    }
} 