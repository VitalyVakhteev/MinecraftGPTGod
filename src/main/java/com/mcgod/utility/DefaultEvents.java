package com.mcgod.utility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Random;

public class DefaultEvents {
    private static final Random random = new Random();

    protected static void setFire() {
        if (random.nextBoolean()) {
            // Set a radius of 3 blocks around a random player on fire
            Player player = getRandomPlayer();
            if (player != null) {
                Location loc = player.getLocation();
                int radius = 3;
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        Location fireLoc = loc.clone().add(x, 0, z);
                        fireLoc.getBlock().setType(Material.FIRE);
                    }
                }
            }
        }
    }

    protected static void buildCabin() {
        Player player = getRandomPlayer();
        if (player != null) {
            Location loc = player.getLocation();
            int width = 5, height = 5, depth = 5;
            loc = loc.add(random.nextInt(20) - 10, 0, random.nextInt(20) - 10); // Random location around player
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < depth; z++) {
                        Location blockLoc = loc.clone().add(x, y, z);
                        if (y == 0 || y == height - 1 || x == 0 || x == width - 1 || z == 0 || z == depth - 1) {
                            blockLoc.getBlock().setType(Material.OAK_WOOD);
                        } else {
                            blockLoc.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }
            Location doorLoc = loc.clone().add(1, 1, 0);
            doorLoc.getBlock().setType(Material.OAK_DOOR);
        }
    }

    protected static void spawnAnimals() {
        Player player = getRandomPlayer();
        if (player != null) {
            EntityType[] animals = {EntityType.PIG, EntityType.CHICKEN, EntityType.COW, EntityType.SHEEP, EntityType.WOLF, EntityType.CAT};
            EntityType animal = animals[random.nextInt(animals.length)];
            for (int i = 0; i < 5; i++) {
                player.getWorld().spawnEntity(player.getLocation(), animal);
            }
        }
    }

    protected static void giveItem() {
        Player player = getRandomPlayer();
        if (player != null) {
            Material[] items = {Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.GOLD_INGOT, Material.IRON_INGOT, Material.BRICK};
            Material item = items[random.nextInt(items.length)];
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(item, 1));
        }
    }

    protected static void smitePlayer() {
        Player player = getRandomPlayer();
        if (player != null) {
            player.getWorld().strikeLightning(player.getLocation());
        }
    }

    protected static void teleportPlayer() {
        Player player1 = getRandomPlayer();
        Player player2 = getRandomPlayer();
        if (player1 != null && player2 != null && !player1.equals(player2)) {
            player1.teleport(player2.getLocation());
        }
    }

    private static Player getRandomPlayer() {
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        if (players.length > 0) {
            return players[random.nextInt(players.length)];
        }
        return null;
    }
}
