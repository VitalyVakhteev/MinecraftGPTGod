package com.mcgod;

import static com.mcgod.DefaultEvents.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MCGodMain extends JavaPlugin {
    private Location holySiteLocation;
    private int actionInterval;
    private Map<Player, String> playerWishes;
    private Random random;
    private double baseChance = 0.4; // Base chance of wishes
    private Map<Player, Double> playerChances; // Map to track individual player chances
    private int intervalTaskId;
    private boolean isHolySiteValid;
    private String apiKey;
    private Set<Player> playersSacrificed; // Track players who have sacrificed in the current cycle

    @Override
    public void onEnable() {
        // Save the default config
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        String worldName = config.getString("holy-site.world");
        double x = config.getDouble("holy-site.x", 0);
        double y = config.getDouble("holy-site.y", 0);
        double z = config.getDouble("holy-site.z", 0);

        if (worldName != null && Bukkit.getWorld(worldName) != null) {
            holySiteLocation = new Location(Bukkit.getWorld(worldName), x, y, z);
            isHolySiteValid = true;
        } else {
            getLogger().severe("Invalid holy site configuration! Sacrifice command will be disabled.");
            isHolySiteValid = false;
        }

        actionInterval = config.getInt("action-interval", 3600); // Default to 3 minutes (3600 ticks, 20 per second) if not set
        apiKey = config.getString("openai.apiKey");
        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("OpenAI API key not found in config.yml!");
        }

        playerWishes = new HashMap<>();
        random = new Random();
        playerChances = new HashMap<>();
        playersSacrificed = new HashSet<>();

        // Schedule the action interval task
        intervalTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            performRandomAction();
            playersSacrificed.clear(); // Reset the sacrifice tracker for the new cycle
            baseChance = 0.4; // Reset the base chance
        }, actionInterval, actionInterval);

        getLogger().info("MCGod plugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel the interval task
        Bukkit.getScheduler().cancelTask(intervalTaskId);
        getLogger().info("MCGod plugin disabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
                if (!isHolySiteValid) {
                    player.sendMessage("Sacrifices are currently disabled due to an invalid holy site configuration.");
                    return true;
                }
                if (playersSacrificed.contains(player)) {
                    player.sendMessage("You have already performed a sacrifice in this cycle.");
                    return true;
                }
                if (player.getLocation().distance(holySiteLocation) < 5) {
                    if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                        player.getInventory().setItemInMainHand(null); // Remove the item
                        double newChance = baseChance + (Math.random() * 0.5);
                        playerChances.put(player, newChance); // Update individual player chance
                        playersSacrificed.add(player); // Mark the player as having sacrificed
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
            // No wishes, generate a random command using ChatGPT
            new BukkitRunnable() {
                @Override
                public void run() {
                    String randomWish = "Perform a random command in Minecraft";
                    String response = generateCommandForWish(randomWish);
                    String command = extractCommand(response);
                    if (command != null) {
                        final String finalCommand = sanitizeCommand(command);
                        Bukkit.getScheduler().runTask(MCGodMain.this, () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                            Bukkit.broadcastMessage("ChatGPT is performing a random action: " + finalCommand);
                        });
                    } else {
                        String modification = getRandomModification();
                        executeWorldModification(modification);
                        Bukkit.broadcastMessage("ChatGPT is performing a random action: " + modification);
                    }
                }
            }.runTaskAsynchronously(MCGodMain.this);
        } else {
            // Process wishes
            for (Map.Entry<Player, String> entry : playerWishes.entrySet()) {
                final Player player = entry.getKey();
                final String wish = entry.getValue();
                double playerChance = playerChances.getOrDefault(player, baseChance);
                if (random.nextDouble() < playerChance) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            String response = generateCommandForWish(wish);
                            String command = extractCommand(response);
                            if (command != null) {
                                command = sanitizeCommand(command.replace("@p", player.getName())); // Replace @p with the player's name and sanitize
                                final String finalCommand = command;
                                Bukkit.getScheduler().runTask(MCGodMain.this, () -> {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                                    player.sendMessage("ChatGPT has granted your wish: " + finalCommand);
                                });
                            } else {
                                String modification = getRandomModification();
                                executeWorldModification(modification);
                                player.sendMessage("ChatGPT has granted a wish: " + modification);
                            }
                        }
                    }.runTaskAsynchronously(MCGodMain.this);
                } else {
                    player.sendMessage("ChatGPT has ignored your wish.");
                }
            }
            playerWishes.clear(); // Clear wishes
        }
    }

    private String getRandomModification() {
        // Basic actions if ChatGPT is inaccessible
        String[] modifications = {"set_fire", "build_cabin", "spawn_animals", "give_item", "smite_player", "teleport_player"};
        return modifications[random.nextInt(modifications.length)];
    }

    private void executeWorldModification(String modification) {
        switch (modification) {
            case "set_fire":
                setFire();
                break;
            case "build_cabin":
                buildCabin();
                break;
            case "spawn_animals":
                spawnAnimals();
                break;
            case "give_item":
                giveItem();
                break;
            case "smite_player":
                smitePlayer();
                break;
            case "teleport_player":
                teleportPlayer();
                break;
            default:
                break;
        }
        Bukkit.broadcastMessage("ChatGPT is performing an action: " + modification);
    }

    private Player[] getPlayers() {
        return Bukkit.getOnlinePlayers().toArray(new Player[0]);
    }

    private String generateCommandForWish(String wish) {
        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("OpenAI API key not found in config.yml!");
            return null;
        }

        try {
            // Call OpenAI API to generate the command
            OpenAIClient openAIClient = new OpenAIClient(apiKey);
            Player[] players = getPlayers();
            return openAIClient.generateCommand(wish, players);
        } catch (Exception e) {
            getLogger().severe("Error generating command from OpenAI: " + e.getMessage());
            return null;
        }
    }

    private String extractCommand(String response) {
        if (response != null) {
            OpenAIClient openAIClient = new OpenAIClient(apiKey);
            return openAIClient.extractCommand(response);
        }
        return null;
    }

    private String sanitizeCommand(String command) {
        command = command.replaceAll("^(bash\\s+|\\/)+", ""); // Remove "bash " or any leading slashes
        return command;
    }
}
