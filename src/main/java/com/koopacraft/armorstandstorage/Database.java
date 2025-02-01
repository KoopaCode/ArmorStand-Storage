package com.koopacraft.armorstandstorage;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.Base64;

public class Database {
    private Connection connection;
    private final ArmorStandStorage plugin;

    public Database(ArmorStandStorage plugin, File file) {
        this.plugin = plugin;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            
            // Create tables if they don't exist
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS armor_stands (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "world TEXT NOT NULL," +
                        "x DOUBLE NOT NULL," +
                        "y DOUBLE NOT NULL," +
                        "z DOUBLE NOT NULL," +
                        "inventory TEXT," +
                        "registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerArmorStand(Location location) {
        try {
            String sql = "INSERT OR IGNORE INTO armor_stands (world, x, y, z) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, location.getWorld().getName());
                pstmt.setDouble(2, location.getX());
                pstmt.setDouble(3, location.getY());
                pstmt.setDouble(4, location.getZ());
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isRegistered(Location location) {
        try {
            String sql = "SELECT id FROM armor_stands WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, location.getWorld().getName());
                pstmt.setDouble(2, location.getX());
                pstmt.setDouble(3, location.getY());
                pstmt.setDouble(4, location.getZ());
                
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void removeArmorStand(Location location) {
        try {
            String sql = "DELETE FROM armor_stands WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, location.getWorld().getName());
                pstmt.setDouble(2, location.getX());
                pstmt.setDouble(3, location.getY());
                pstmt.setDouble(4, location.getZ());
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveArmorStand(Location location, ItemStack[] inventory) {
        try {
            String serializedInventory = serializeItems(inventory);
            plugin.debug("Saving inventory: " + serializedInventory);
            
            String sql = "UPDATE armor_stands SET inventory = ? WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, serializedInventory);
                pstmt.setString(2, location.getWorld().getName());
                pstmt.setDouble(3, location.getX());
                pstmt.setDouble(4, location.getY());
                pstmt.setDouble(5, location.getZ());
                int updated = pstmt.executeUpdate();
                
                // If no rows were updated, the armor stand might not be registered yet
                if (updated == 0) {
                    sql = "INSERT INTO armor_stands (world, x, y, z, inventory) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(sql)) {
                        insertStmt.setString(1, location.getWorld().getName());
                        insertStmt.setDouble(2, location.getX());
                        insertStmt.setDouble(3, location.getY());
                        insertStmt.setDouble(4, location.getZ());
                        insertStmt.setString(5, serializedInventory);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ItemStack[] getArmorStandInventory(Location location) {
        try {
            String sql = "SELECT inventory FROM armor_stands WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, location.getWorld().getName());
                pstmt.setDouble(2, location.getX());
                pstmt.setDouble(3, location.getY());
                pstmt.setDouble(4, location.getZ());
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String inventoryData = rs.getString("inventory");
                    plugin.debug("Loading inventory: " + inventoryData);
                    if (inventoryData != null && !inventoryData.isEmpty()) {
                        return deserializeItems(inventoryData);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ItemStack[27];
    }

    private String serializeItems(ItemStack[] items) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        
        // Count non-null items
        int nonNullCount = 0;
        for (ItemStack item : items) {
            if (item != null) nonNullCount++;
        }
        dataOutput.writeInt(nonNullCount);
        
        // Write items with their slot numbers
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                dataOutput.writeInt(i); // Write the slot number
                dataOutput.writeObject(items[i]);
            }
        }
        
        dataOutput.close();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private ItemStack[] deserializeItems(String data) throws Exception {
        if (data == null || data.isEmpty()) {
            return new ItemStack[27]; // Return empty array with correct size
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        
        int size = dataInput.readInt();
        ItemStack[] items = new ItemStack[27]; // Fixed size for chest inventory
        
        // Read items into their exact positions
        for (int i = 0; i < size; i++) {
            int slot = dataInput.readInt(); // Read the slot number
            ItemStack item = (ItemStack) dataInput.readObject();
            if (slot >= 0 && slot < items.length) {
                items[slot] = item;
            }
        }
        
        dataInput.close();
        return items;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
} 