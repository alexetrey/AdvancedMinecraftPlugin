package com.minecraftplugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class AdvancedMinecraftPluginTest {
    
    @Test
    public void testPluginName() {
        assertNotNull(AdvancedMinecraftPlugin.class);
    }
    
    @Test
    public void testPluginPackage() {
        String packageName = AdvancedMinecraftPlugin.class.getPackage().getName();
        assertEquals("com.minecraftplugin", packageName);
    }
    
    @Test
    public void testPluginClassExists() {
        try {
            Class<?> pluginClass = Class.forName("com.minecraftplugin.AdvancedMinecraftPlugin");
            assertNotNull(pluginClass);
        } catch (ClassNotFoundException e) {
            fail("Plugin class not found: " + e.getMessage());
        }
    }
    
    @Test
    public void testPluginExtendsJavaPlugin() {
        assertTrue(AdvancedMinecraftPlugin.class.getSuperclass().getName().contains("JavaPlugin"));
    }
} 