package com.mcgod;

import static com.mcgod.DefaultEvents.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
    private Map<Player, String> playerWishes;
    private Map<Player, Double> playerChances;
    private Set<Player> playersSacrificed; // Track players who have sacrificed in the current cycle
    private Location holySiteLocation;
    private Random random;
    private String apiKey;
    private String gptModel;
    private int intervalTaskId;
    private double baseChance;
    private boolean isHolySiteValid;
    private boolean isPluginActive; // To track if the plugin is active

    @Override
    public void onEnable() {
        // Start the plugin by default
        startPlugin();
        getLogger().info("MCGod plugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        // Stop the plugin on disable
        stopPlugin();
        getLogger().info("MCGod plugin disabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("start")) {
                if (isPluginActive) {
                    player.sendMessage("MCGod plugin is already running.");
                } else {
                    startPlugin();
                    player.sendMessage("MCGod plugin started.");
                }
                return true;
            } else if (command.getName().equalsIgnoreCase("stop")) {
                if (!isPluginActive) {
                    player.sendMessage("MCGod plugin is not running.");
                } else {
                    stopPlugin();
                    player.sendMessage("MCGod plugin stopped.");
                }
                return true;
            } else if (isPluginActive) {
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
                    if (player.getLocation().distance(holySiteLocation) < 10) {
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
                } else if (command.getName().equalsIgnoreCase("advice")) {
                    if (args.length > 0) {
                        String message = String.join(" ", args);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                String response = getAdviceFromChatGPT(message);
                                Bukkit.getScheduler().runTask(MCGodMain.this, () -> {
                                    if (response != null) {
                                        player.sendMessage("§aChatGPT§r's advice: " + response);
                                    } else {
                                        player.sendMessage("Failed to get advice from §aChatGPT§r.");
                                    }
                                });
                            }
                        }.runTaskAsynchronously(this);
                    } else {
                        player.sendMessage("You must specify your message.");
                    }
                    return true;
                } else if (command.getName().equalsIgnoreCase("spy")) {
                    if (args.length == 1) {
                        String targetPlayerName = args[0];
                        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
                        if (targetPlayer != null) {
                            double spyChance = 0.5; // 50% chance to get information
                            if (random.nextDouble() < spyChance) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        String info = getSpyInfoFromChatGPT(player, targetPlayer);
                                        Bukkit.getScheduler().runTask(MCGodMain.this, () -> {
                                            if (info != null) {
                                                player.sendMessage("§aChatGPT§r's info about " + targetPlayerName + ": " + info);
                                            } else {
                                                player.sendMessage("Failed to get info about " + targetPlayerName + " from §aChatGPT§r.");
                                            }
                                        });
                                    }
                                }.runTaskAsynchronously(this);
                            } else {
                                player.sendMessage("§aChatGPT§r couldn't find any interesting information about " + targetPlayerName + ".");
                            }
                        } else {
                            player.sendMessage("Player " + targetPlayerName + " not found.");
                        }
                    } else {
                        player.sendMessage("You must specify the player to spy on.");
                    }
                    return true;
                }
            } else {
                player.sendMessage("MCGod plugin is currently stopped.");
            }
        }
        return false;
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

    private String getRandomModification() {
        // Basic actions if ChatGPT is inaccessible
        String[] modifications = {"set_fire", "build_cabin", "spawn_animals", "give_item", "smite_player", "teleport_player"};
        return modifications[random.nextInt(modifications.length)];
    }

    private Player[] getPlayers() {
        return Bukkit.getOnlinePlayers().toArray(new Player[0]);
    }

    private String getAdviceFromChatGPT(String message) {
        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("OpenAI API key not found in config.yml!");
            return null;
        }

        try {
            OpenAIClient openAIClient = new OpenAIClient(apiKey, gptModel);
            return openAIClient.getAdvice(message);
        } catch (Exception e) {
            getLogger().severe("Error getting advice from OpenAI: " + e.getMessage());
            return null;
        }
    }

    private String getSpyInfoFromChatGPT(Player player, Player targetPlayer) {
        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("OpenAI API key not found in config.yml!");
            return null;
        }

        try {
            OpenAIClient openAIClient = new OpenAIClient(apiKey, gptModel);
            return openAIClient.getSpyInfo(player, targetPlayer);
        } catch (Exception e) {
            getLogger().severe("Error getting spy info from OpenAI: " + e.getMessage());
            return null;
        }
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

    private void performRandomAction() {
        if (playerWishes.isEmpty()) {
            // No wishes, generate a random command using ChatGPT
            new BukkitRunnable() {
                @Override
                public void run() {
                    String randomWish = "Please generate a random command to run in my world in Minecraft. Be creative.";
                    String response = generateCommandForWish(randomWish);
                    String command = extractCommand(response);
                    if (command != null) {
                        final String finalCommand = sanitizeCommand(command);
                        Bukkit.getScheduler().runTask(MCGodMain.this, () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                            Bukkit.broadcastMessage("§aChatGPT§r is performing a random action: " + finalCommand);
                        });
                    } else {
                        String modification = getRandomModification();
                        Bukkit.getScheduler().runTask(MCGodMain.this, () -> {
                            executeWorldModification(modification);
                            Bukkit.broadcastMessage("§aChatGPT§r is performing a random action: " + modification);
                        });
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
                                    player.sendMessage("§aChatGPT§r has granted your wish: " + finalCommand);
                                });
                            } else {
                                String modification = getRandomModification();
                                Bukkit.getScheduler().runTask(MCGodMain.this, () -> {
                                    executeWorldModification(modification);
                                    player.sendMessage("§aChatGPT§r has granted a wish: " + modification);
                                });
                            }
                        }
                    }.runTaskAsynchronously(MCGodMain.this);
                } else {
                    player.sendMessage("§aChatGPT§r has ignored your wish.");
                }
            }
            playerWishes.clear(); // Clear wishes
        }
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
        Bukkit.broadcastMessage("§aChatGPT§r is performing an action: " + modification);
    }

    private String generateCommandForWish(String wish) {
        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("OpenAI API key not found in config.yml!");
            return null;
        }

        try {
            // Call OpenAI API to generate the command
            OpenAIClient openAIClient = new OpenAIClient(apiKey, gptModel);
            Player[] players = getPlayers();
            return openAIClient.generateCommand(wish, players);
        } catch (Exception e) {
            getLogger().severe("Error generating command from OpenAI: " + e.getMessage());
            return null;
        }
    }

    private String extractCommand(String response) {
        if (response != null) {
            OpenAIClient openAIClient = new OpenAIClient(apiKey, gptModel);
            return openAIClient.extractCommand(response);
        }
        return null;
    }

    private String sanitizeCommand(String command) {
        command = command.replaceAll("^(bash\\s+|/|java\\s+|plaintext\\s+)+", ""); // Remove "bash ", "java ", "plaintext ", or any leading slashes
        return command;
    }

    private void startPlugin() {
        FileConfiguration config = this.getConfig();

        String worldName = config.getString("holy-site.world");
        double x = config.getDouble("holy-site.x", 0);
        double z = config.getDouble("holy-site.z", 0);
        int actionInterval = config.getInt("action-interval", 3600); // Default to 3 minutes (3600 ticks, 20 per second) if not set
        apiKey = config.getString("openai.apiKey");
        gptModel = config.getString("openai.model", "gpt-3.5-turbo");
        baseChance = config.getDouble("base-chance", 0.3);

        if (worldName != null && Bukkit.getWorld(worldName) != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                int highestY = world.getHighestBlockYAt((int) x, (int) z);
                Block block = world.getBlockAt((int) x, highestY, (int) z);
                if (isValidLocation(block)) {
                    Location obeliskLocation = block.getLocation();
                    generateObelisk(obeliskLocation);
                    holySiteLocation = obeliskLocation;
                    isHolySiteValid = true;
                } else {
                    getLogger().severe("Invalid obelisk location! Sacrifice command will be disabled.");
                    isHolySiteValid = false;
                }
            } else {
                getLogger().severe("Invalid world configuration! Sacrifice command will be disabled.");
                isHolySiteValid = false;
            }
        } else {
            getLogger().severe("Invalid holy site configuration! Sacrifice command will be disabled.");
            isHolySiteValid = false;
        }

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
            baseChance = config.getDouble("base-chance", 0.3); // Reset the base chance
        }, actionInterval, actionInterval);

        isPluginActive = true;
    }

    private void stopPlugin() {
        // Cancel the interval task
        Bukkit.getScheduler().cancelTask(intervalTaskId);
        isPluginActive = false;
    }
}