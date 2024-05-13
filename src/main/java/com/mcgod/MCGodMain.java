package com.mcgod;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MCGodMain extends JavaPlugin {
    private Location holySiteLocation;
    private int actionInterval;
    private Map<Player, String> playerWishes;
    private Random random;
    private double baseChance = 0.2; // Base chance of wishes
    private int intervalTaskId;

    @Override
    public void onEnable() {
        // Save the default config
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        holySiteLocation = new Location(
                Bukkit.getWorld(config.getString("holy-site.world")),
                config.getDouble("holy-site.x"),
                config.getDouble("holy-site.y"),
                config.getDouble("holy-site.z")
        );
        actionInterval = config.getInt("action-interval", 6000); // Default to 5 minutes if not set
        playerWishes = new HashMap<>();
        random = new Random();

        // Schedule the action interval task
        intervalTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::performRandomAction, actionInterval, actionInterval);

        getLogger().info("MCGod plugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel the interval task
        Bukkit.getScheduler().cancelTask(intervalTaskId);
        getLogger().info("MCGod plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("pray")) {
                if (args.length > 0) {
                    String wish = String.join(" ", args);
                    playerWishes.put(player, wish);
                    player.sendMessage("Your prayer has been heard: " + wish);
                } else {
                    player.sendMessage("You must specify your prayer.");
                }
                return true;
            } else if (command.getName().equalsIgnoreCase("sacrifice")) {
                if (player.getLocation().distance(holySiteLocation) < 5) {
                    if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                        player.getInventory().setItemInMainHand(null); // Remove the item
                        baseChance += (Math.random() * 0.5);
                        player.sendMessage("Your sacrifice has been accepted. Your chance of having prayers answered has increased.");
                    } else {
                        player.sendMessage("You must hold an item in your hand to sacrifice.");
                    }
                } else {
                    player.sendMessage("You must be at the holy site to sacrifice.");
                }
                return true;
            }
        }
        return false;
    }

    private void performRandomAction() {
        if (playerWishes.isEmpty()) {
            // No wishes, pick a random action
            executeWorldModification(getRandomModification());
        } else {
            // Process wishes
            for (Map.Entry<Player, String> entry : playerWishes.entrySet()) {
                Player player = entry.getKey();
                String wish = entry.getValue();
                if (random.nextDouble() < baseChance) {
                    String modification = getClosestModification(wish);
                    executeWorldModification(modification);
                    player.sendMessage("ChatGPT has granted your wish: " + modification);
                } else {
                    player.sendMessage("ChatGPT has ignored your wish.");
                }
            }
            playerWishes.clear(); // Clear wishes for the next interval
        }
    }

    private String getRandomModification() {
        // Todo: Return a random world modification
        String[] modifications = {"remove_water", "set_fire", "give_item"}; // Dumb but will do for now
        return modifications[random.nextInt(modifications.length)];
    }

    private String getClosestModification(String wish) {
        // Todo: Return the closest modification based on the wish
        return getRandomModification();
    }

    private void executeWorldModification(String modification) {
        // Todo: Add world modifications, think of more mods to add
        switch (modification) {
            case "remove_water":
                break;
            case "set_fire":
                break;
            case "give_item":
                break;
            default:
                break;
        }
        Bukkit.broadcastMessage("ChatGPT is performing an action: " + modification);
    }
}
