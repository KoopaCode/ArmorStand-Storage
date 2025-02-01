package com.koopacraft.armorstandstorage;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

import java.util.HashMap;
import java.util.UUID;

public class ArmorStandListener implements Listener {
    private final ArmorStandStorage plugin;
    private final HashMap<UUID, ArmorStand> openInventories = new HashMap<>();
    private final boolean isLegacyVersion;

    public ArmorStandListener(ArmorStandStorage plugin) {
        this.plugin = plugin;
        // Check if we're running on a legacy version (pre 1.13)
        isLegacyVersion = !isMethodAvailable("org.bukkit.entity.ArmorStand", "getEquipment");
        
        // Register all existing armor stands on startup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof ArmorStand) {
                        plugin.getDatabase().registerArmorStand(entity.getLocation());
                    }
                }
            }
            plugin.getLogger().info("Registered all existing armor stands!");
        }, 20L);
    }

    private boolean isMethodAvailable(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            clazz.getMethod(methodName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void setArmorStandEquipment(ArmorStand armorStand, ItemStack[] contents) {
        EntityEquipment equipment = armorStand.getEquipment();
        if (equipment == null) return;

        equipment.clear();
        
        try {
            // Scan entire inventory for armor and items
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null) continue;

                String type = item.getType().name().toUpperCase();
                
                // Check for armor pieces
                if (type.endsWith("_HELMET") || type.contains("_HEAD") || type.equals("PLAYER_HEAD") || type.equals("SKULL") || type.equals("SKULL_ITEM")) {
                    equipment.setHelmet(item);
                }
                else if (type.endsWith("_CHESTPLATE") || type.equals("ELYTRA")) {
                    equipment.setChestplate(item);
                }
                else if (type.endsWith("_LEGGINGS")) {
                    equipment.setLeggings(item);
                }
                else if (type.endsWith("_BOOTS")) {
                    equipment.setBoots(item);
                }
                // Handle weapons/tools/items for hands
                else if (i == 4 || (equipment.getHelmet() == null && equipment.getChestplate() == null 
                        && equipment.getLeggings() == null && equipment.getBoots() == null)) {
                    if (isLegacyVersion) {
                        equipment.setItemInHand(item);
                    } else {
                        equipment.setItemInMainHand(item);
                    }
                }
                else if (!isLegacyVersion && i == 5) {
                    equipment.setItemInOffHand(item);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting armor stand equipment: " + e.getMessage());
        }
    }

    private void getArmorStandEquipment(ArmorStand armorStand, Inventory inventory) {
        EntityEquipment equipment = armorStand.getEquipment();
        if (equipment == null) return;

        try {
            inventory.setItem(0, equipment.getHelmet());
            inventory.setItem(1, equipment.getChestplate());
            inventory.setItem(2, equipment.getLeggings());
            inventory.setItem(3, equipment.getBoots());
            
            // Handle main and off hand differently for legacy versions
            if (isLegacyVersion) {
                inventory.setItem(4, equipment.getItemInHand()); // Legacy method
            } else {
                inventory.setItem(4, equipment.getItemInMainHand());
                inventory.setItem(5, equipment.getItemInOffHand());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting armor stand equipment: " + e.getMessage());
        }
    }

    @EventHandler
    public void onArmorStandPlace(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.ARMOR_STAND) {
            plugin.getDatabase().registerArmorStand(event.getLocation());
            plugin.debug("New armor stand registered at: " + formatLocation(event.getLocation()));
        }
    }

    @EventHandler
    public void onArmorStandRemove(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.ARMOR_STAND) {
            if (plugin.getDatabase().isRegistered(event.getEntity().getLocation())) {
                plugin.getDatabase().removeArmorStand(event.getEntity().getLocation());
                plugin.getLogger().info("Registered armor stand removed at: " + formatLocation(event.getEntity().getLocation()));
            } else {
                plugin.getLogger().info("Unregistered armor stand removed at: " + formatLocation(event.getEntity().getLocation()));
            }
        }
    }

    private String formatLocation(org.bukkit.Location loc) {
        return String.format("World: %s, X: %.2f, Y: %.2f, Z: %.2f", 
            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    @EventHandler
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand)) {
            return;
        }

        Player player = event.getPlayer();
        ArmorStand armorStand = (ArmorStand) event.getRightClicked();

        // Check if in disabled world
        if (plugin.isWorldDisabled(player.getWorld().getName())) {
            player.sendMessage(plugin.getMessage("blocked-world"));
            return;
        }

        // Check if player is sneaking
        if (!player.isSneaking()) {
            return; // Allow normal armor stand interaction when not sneaking
        }

        // Check permission
        if (!player.hasPermission("armorstandstorage.use")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        event.setCancelled(true);

        // Create inventory with configured size
        int size = Math.max(27, plugin.getInventoryRows() * 9);
        Inventory inventory = Bukkit.createInventory(null, size, plugin.getInventoryTitle());

        // First, check if there's saved inventory data
        ItemStack[] savedItems = plugin.getDatabase().getArmorStandInventory(armorStand.getLocation());
        
        if (savedItems.length > 0 && hasItems(savedItems)) {
            // Load saved inventory
            for (int i = 0; i < Math.min(savedItems.length, size); i++) {
                inventory.setItem(i, savedItems[i]);
            }
        } else {
            // No saved items, get current equipment
            EntityEquipment equipment = armorStand.getEquipment();
            if (equipment != null) {
                // Store current equipment in first slots
                inventory.setItem(0, equipment.getHelmet());
                inventory.setItem(1, equipment.getChestplate());
                inventory.setItem(2, equipment.getLeggings());
                inventory.setItem(3, equipment.getBoots());
                if (isLegacyVersion) {
                    inventory.setItem(4, equipment.getItemInHand());
                } else {
                    inventory.setItem(4, equipment.getItemInMainHand());
                    inventory.setItem(5, equipment.getItemInOffHand());
                }

                // Save this initial state to database
                plugin.getDatabase().saveArmorStand(armorStand.getLocation(), inventory.getContents());
            }
        }

        // Open inventory
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), armorStand);
    }

    // Helper method to check if an ItemStack array has any non-null items
    private boolean hasItems(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();
        
        // Don't interfere if player is sneaking (our storage UI handles that)
        if (event.getPlayer().isSneaking()) {
            return;
        }

        // Schedule a task to save the new equipment state after the vanilla interaction
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!plugin.getDatabase().isRegistered(armorStand.getLocation())) {
                plugin.getDatabase().registerArmorStand(armorStand.getLocation());
            }
            
            // Get current equipment and save it
            EntityEquipment equipment = armorStand.getEquipment();
            if (equipment != null) {
                ItemStack[] contents = new ItemStack[27];
                contents[0] = equipment.getHelmet();
                contents[1] = equipment.getChestplate();
                contents[2] = equipment.getLeggings();
                contents[3] = equipment.getBoots();
                if (isLegacyVersion) {
                    contents[4] = equipment.getItemInHand();
                } else {
                    contents[4] = equipment.getItemInMainHand();
                    contents[5] = equipment.getItemInOffHand();
                }
                plugin.getDatabase().saveArmorStand(armorStand.getLocation(), contents);
                plugin.debug("Saved armor stand equipment after manual interaction");
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ArmorStand armorStand = openInventories.get(player.getUniqueId());
        
        if (armorStand == null || !event.getView().getTitle().equals(plugin.getInventoryTitle())) {
            return;
        }

        plugin.debug("Player " + player.getName() + " clicked in armor stand inventory");
        
        // Update equipment immediately after click
        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory inv = event.getView().getTopInventory();
            ItemStack[] contents = inv.getContents();
            
            // Update armor stand equipment
            setArmorStandEquipment(armorStand, contents);
            
            // Save to database
            plugin.getDatabase().saveArmorStand(armorStand.getLocation(), contents);
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        ArmorStand armorStand = openInventories.get(player.getUniqueId());
        
        if (armorStand == null || !event.getView().getTitle().equals(plugin.getInventoryTitle())) {
            return;
        }

        plugin.debug("Player " + player.getName() + " dragged in armor stand inventory");
        
        // Update equipment immediately after drag
        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory inv = event.getView().getTopInventory();
            ItemStack[] contents = inv.getContents();
            
            // Update armor stand equipment
            setArmorStandEquipment(armorStand, contents);
            
            // Save to database
            plugin.getDatabase().saveArmorStand(armorStand.getLocation(), contents);
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        ArmorStand armorStand = openInventories.remove(player.getUniqueId());

        if (armorStand == null || !event.getView().getTitle().equals(plugin.getInventoryTitle())) {
            return;
        }

        // Save the exact inventory contents, preserving null slots
        Inventory inv = event.getView().getTopInventory();
        ItemStack[] contents = new ItemStack[inv.getSize()];
        
        // Count non-null items while copying contents
        int itemCount = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            contents[i] = inv.getItem(i);
            if (contents[i] != null) {
                itemCount++;
            }
        }

        // Save to database
        plugin.getDatabase().saveArmorStand(armorStand.getLocation(), contents);

        // Update armor stand equipment
        setArmorStandEquipment(armorStand, contents);

        plugin.debug("Saving inventory with " + contents.length + " slots");
        plugin.debug("Found " + itemCount + " items to save");
        plugin.debug("Saved armor stand inventory for " + player.getName());
    }
} 