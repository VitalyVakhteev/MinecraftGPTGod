package com.mcgod.utility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ObeliskManager {
    private final JavaPlugin plugin;
    private Location holySiteLocation;

    public ObeliskManager(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        initializeHolySite(config);
    }

    public boolean initializeHolySite(FileConfiguration config) {
        String worldName = config.getString("holy-site.world");
        double x = config.getDouble("holy-site.x", 0);
        double z = config.getDouble("holy-site.z", 0);

        if (worldName != null && Bukkit.getWorld(worldName) != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                int highestY = world.getHighestBlockYAt((int) x, (int) z);
                Block block = world.getBlockAt((int) x, highestY, (int) z);
                if (isValidLocation(block)) {
                    Location obeliskLocation = block.getLocation();
                    generateObelisk(obeliskLocation);
                    holySiteLocation = obeliskLocation;
                    return true;
                } else {
                    plugin.getLogger().severe("Invalid obelisk location! Sacrifice command will be disabled.");
                    return false;
                }
            } else {
                plugin.getLogger().severe("Invalid world configuration! Sacrifice command will be disabled.");
                return false;
            }
        } else {
            plugin.getLogger().severe("Invalid holy site configuration! Sacrifice command will be disabled.");
            return false;
        }
    }

    private boolean isValidLocation(Block block) {
        World world = block.getWorld();
        Location location = block.getLocation();
        for (int i = 1; i <= 5; i++) {
            Block belowBlock = world.getBlockAt(location.subtract(0, i, 0));
            if (belowBlock.getType() == Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private void generateObelisk(Location location) {
        World world = location.getWorld();
        if (world != null) {
            Block baseBlock = world.getBlockAt(location);
            Block topBlock = world.getBlockAt(location.add(0, 1, 0));
            baseBlock.setType(Material.BEDROCK);
            topBlock.setType(Material.BEDROCK);
        }
    }

    public Location getHolySiteLocation() {
        return holySiteLocation;
    }
}
