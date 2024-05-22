package com.mcgod.utility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.mcgod.utility.DefaultEvents.*;

public class WishManager {
    private final JavaPlugin plugin;
    private final String apiKey;
    private final Map<Player, String> playerWishes;
    private final Map<Player, Double> playerChances;
    private final double baseChance;
    private final Set<Player> playersSacrificed;
    private final boolean isHolySiteValid;
    private final ObeliskManager obeliskManager;
    private final Random random;

    public WishManager(JavaPlugin plugin, String apiKey, Map<Player, String> playerWishes, Map<Player, Double> playerChances, double baseChance, Set<Player> playersSacrificed, boolean isHolySiteValid, ObeliskManager obeliskManager) {
        this.plugin = plugin;
        this.apiKey = apiKey;
        this.playerWishes = new ConcurrentHashMap<>(playerWishes);
        this.playerChances = new ConcurrentHashMap<>(playerChances);
        this.baseChance = baseChance;
        this.playersSacrificed = playersSacrificed;
        this.isHolySiteValid = isHolySiteValid;
        this.obeliskManager = obeliskManager;
        this.random = new Random();
    }

    public void recordPrayer(Player player, String wish) {
        playerWishes.put(player, wish);
        player.sendMessage("Your prayer has been heard: " + wish);
    }

    public void handleSacrifice(Player player) {
        if (!isHolySiteValid) {
            player.sendMessage("Sacrifices are currently disabled due to an invalid holy site configuration.");
            return;
        }
        if (playersSacrificed.contains(player)) {
            player.sendMessage("You have already performed a sacrifice in this cycle.");
            return;
        }
        Location holySiteLocation = obeliskManager.getHolySiteLocation();
        if (holySiteLocation == null) {
            player.sendMessage("Holy site location is not set.");
            return;
        }
        if (isPlayerAtHolySite(player.getLocation(), holySiteLocation)) {
            if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                player.getInventory().setItemInMainHand(null);
                double newChance = baseChance + (Math.random() * 0.5);
                playerChances.put(player, newChance);
                playersSacrificed.add(player);
                player.sendMessage("Your sacrifice has been accepted. Your chance of having prayers answered has increased.");
            } else {
                player.sendMessage("You must hold an item in your hand to sacrifice.");
            }
        } else {
            player.sendMessage("You must be at the holy site to sacrifice.");
        }
    }

    private boolean isPlayerAtHolySite(Location playerLocation, Location holySiteLocation) {
        double distance = playerLocation.distance(holySiteLocation);
        return distance <= 5;
    }

    public void performRandomAction() {
        if (playerWishes.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    String randomWish = "Please generate a random command to run in my world in Minecraft. Be creative.";
                    String response = generateCommandForWish(randomWish);
                    String command = extractCommand(response);
                    if (command != null) {
                        final String finalCommand = sanitizeCommand(command);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                            Bukkit.broadcastMessage("§aChatGPT is performing a random action: " + finalCommand);
                        });
                    } else {
                        String modification = getRandomModification();
                        executeWorldModification(modification);
                        Bukkit.broadcastMessage("§aChatGPT is performing a random action: " + modification);
                    }
                }
            }.runTaskAsynchronously(plugin);
        } else {
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
                                command = sanitizeCommand(command.replace("@p", player.getName()));
                                final String finalCommand = command;
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                                    player.sendMessage("§aChatGPT has granted your wish: " + finalCommand);
                                });
                            } else {
                                String modification = getRandomModification();
                                executeWorldModification(modification);
                                player.sendMessage("§aChatGPT has granted a wish: " + modification);
                            }
                        }
                    }.runTaskAsynchronously(plugin);
                } else {
                    player.sendMessage("§aChatGPT has ignored your wish.");
                }
            }
            playerWishes.clear();
            playersSacrificed.clear(); // Clear the sacrifices for the next cycle
        }
    }

    private String generateCommandForWish(String wish) {
        if (apiKey == null || apiKey.isEmpty()) {
            plugin.getLogger().severe("OpenAI API key not found in config.yml!");
            return null;
        }

        try {
            OpenAIClient openAIClient = new OpenAIClient(apiKey);
            Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
            return openAIClient.generateCommand(wish, players);
        } catch (Exception e) {
            plugin.getLogger().severe("Error generating command from OpenAI: " + e.getMessage());
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
        command = command.replaceAll("^(bash\\s+|/|java\\s+)+", "");
        return command;
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
        Bukkit.broadcastMessage("§aChatGPT is performing an action: " + modification);
    }

    private String getRandomModification() {
        String[] modifications = {"set_fire", "build_cabin", "spawn_animals", "give_item", "smite_player", "teleport_player"};
        return modifications[random.nextInt(modifications.length)];
    }
}
