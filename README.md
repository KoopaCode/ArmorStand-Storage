

![Armor Stand Storage Banner](https://lapislabs.dev/images/ass.png)

🛡️ Armor Stand Storage v1.0
============================
<p align="center"> <img src="https://lapislabs.dev/images/inv.png" alt="Storage Image"> </p>

Transform your armor stands into functional storage units while maintaining their display capabilities! Perfect for kits, storage rooms, and displays.

**Version:** 1.0

**Minecraft Versions:** 1.8 - 1.21.x

**Release Date:** 1/27/25

✨ Features
----------

*   **Smart Storage System**
    *   Shift + Right-Click any armor stand to access storage
    *   27 slots of storage space per armor stand
    *   Items in armor slots automatically display on the stand
    *   Regular right-click still works for vanilla interactions
*   **Intelligent Armor Detection**
    *   Automatically detects and displays armor pieces
    *   Works with any armor type (leather, iron, gold, diamond, netherite)
    *   Supports modded armor that follows naming conventions
    *   Compatible with Elytra and player heads
*   **Server Friendly**
    *   Multi-world support with configurable blocked worlds
    *   Minimal performance impact
    *   SQLite database for reliable storage
    *   Works on Minecraft 1.8+

🎮 Usage
--------

1.  Place an armor stand
2.  Add armor/items normally OR
3.  Shift + Right-Click to open storage
4.  Place items anywhere in the inventory:
    *   Armor pieces will automatically display
    *   Other items are safely stored
    *   First 6 slots are prioritized for display

⚙️ Configuration
----------------

    # Disable storage in specific worlds
    disabled-worlds:
      #- spawn_world
      #- event_world
    
    # Debug mode for detailed logging
    debug: false
    
    # Storage settings
    storage:
      inventory-title: "Armor Stand Storage"
      inventory-rows: 3

🔒 Permissions
--------------

*   `armorstandstorage.use` - Access to armor stand storage (default: op)

📋 Commands
-----------

No commands - just shift-right-click to use!

💡 Tips
-------

*   Place armor in any slot - it will automatically display
*   Use regular right-click for vanilla armor stand features
*   Items are saved even if the armor stand breaks
*   Works great for shop displays and storage rooms
*   Perfect for RPG servers and town builds

🔧 Installation
---------------

1.  Download the plugin
2.  Place in your plugins folder
3.  Restart server
4.  Configure (optional)
5.  Start using!

🎯 Planned Features
-------------------

*   Custom inventory sizes
*   Per-world inventory settings
*   Hologram support
*   Shop integration
*   API for developers

🆕 What's New in 1.0
--------------------

*   Initial release
*   Full armor stand storage functionality
*   Multi-world support
*   Automatic armor detection and display
*   SQLite database integration
*   Legacy version support (1.8+)

📝 Version History
------------------

*   **1.0** (1.27.2025)
    *   Initial release
    *   Core functionality implemented
    *   Database storage system
    *   Multi-version support

ArmorStandStorage v1.0 - © 2024 Koopa

Made with ❤️ by Koopa

[GitHub Issues](https://github.com/KoopaCode/ArmorStand-Storage/issues) | [Discord](https://discord.gg/KmHGjaHWct) 

`storage` `free` `armorstand` `plugin` `1.8-1.21`
