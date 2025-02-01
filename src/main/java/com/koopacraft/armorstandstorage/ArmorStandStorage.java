package com.koopacraft.armorstandstorage;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class ArmorStandStorage extends JavaPlugin {
    private Database database;
    private List<String> disabledWorlds;
    private boolean debugMode;

    @Override
    public void onEnable() {
        // Create config if it doesn't exist
        saveDefaultConfig();
        
        // Load settings
        disabledWorlds = getConfig().getStringList("disabled-worlds");
        if (disabledWorlds == null) {
            disabledWorlds = new ArrayList<>();
        }
        debugMode = getConfig().getBoolean("debug", false);
        
        // Initialize database
        database = new Database(this, new File(getDataFolder(), "armorstands.db"));
        
        // Register events
        getServer().getPluginManager().registerEvents(new ArmorStandListener(this), this);
        
        // Log startup
        getLogger().info("ArmorStandStorage has been enabled!");
        if (!disabledWorlds.isEmpty()) {
            getLogger().info("Armor stand storage is disabled in: " + String.join(", ", disabledWorlds));
        }
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("ArmorStandStorage has been disabled!");
    }

    public Database getDatabase() {
        return database;
    }

    public String getMessage(String path) {
        return getConfig().getString("messages." + path, "Message not found: " + path);
    }

    public String getDisabledWorld() {
        return getConfig().getString("disabled-world", "world_KC_SPAWN");
    }

    public String getInventoryTitle() {
        return getConfig().getString("storage.inventory-title", "Armor Stand Storage");
    }

    public int getInventoryRows() {
        return getConfig().getInt("storage.inventory-rows", 3);
    }

    public boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName);
    }

    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[Debug] " + message);
        }
    }

    public boolean isDebugMode() {
        return debugMode;
    }
} 