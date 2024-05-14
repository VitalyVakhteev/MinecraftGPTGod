package com.mcgod;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

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
    private boolean isHolySiteValid;
    private String apiKey;

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

        actionInterval = config.getInt("action-interval", 6000); // Default to 5 minutes if not set
        apiKey = config.getString("openai.apiKey");
        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("OpenAI API key not found in config.yml!");
        }

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
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            String modification = generateCommandForWish(wish);
                            if (modification != null) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modification);
                                player.sendMessage("ChatGPT has granted your wish: " + modification);
                            } else {
                                modification = getRandomModification();
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

    private void setFire() {
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
        } else {
            // Set fire to the nearest village from a random player
            Player player = getRandomPlayer();
            if (player != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String locateCommand = "execute in " + player.getWorld().getName() + " run locate structure minecraft:village";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), locateCommand);
                        Location villageLocation = parseLocateResult();
                        if (villageLocation != null) {
                            setVillageOnFire(villageLocation);
                        }
                    }
                }.runTaskAsynchronously(this);
            }
        }
    }

    private void setVillageOnFire(Location villageLocation) {
        int radius = 30;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location blockLoc = villageLocation.clone().add(x, y, z);
                    if (blockLoc.getBlock().getType() == Material.OAK_PLANKS) {
                        blockLoc.getBlock().setType(Material.FIRE);
                    }
                }
            }
        }
    }

    private Location parseLocateResult() {
        // This method should parse the result of the locate command and return the village location
        // For demonstration purposes, we'll return a fixed location
        // Replace this with actual parsing logic
        return new Location(Bukkit.getWorlds().get(0), 100, 64, 100);
    }

    private void buildCabin() {
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

    private void spawnAnimals() {
        Player player = getRandomPlayer();
        if (player != null) {
            EntityType[] animals = {EntityType.PIG, EntityType.CHICKEN, EntityType.COW, EntityType.SHEEP, EntityType.WOLF, EntityType.CAT};
            EntityType animal = animals[random.nextInt(animals.length)];
            for (int i = 0; i < 5; i++) {
                player.getWorld().spawnEntity(player.getLocation(), animal);
            }
        }
    }

    private void giveItem() {
        Player player = getRandomPlayer();
        if (player != null) {
            Material[] items = {Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.GOLD_INGOT, Material.IRON_INGOT, Material.BRICK};
            Material item = items[random.nextInt(items.length)];
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(item, 1));
        }
    }

    private void smitePlayer() {
        Player player = getRandomPlayer();
        if (player != null) {
            player.getWorld().strikeLightning(player.getLocation());
        }
    }

    private void teleportPlayer() {
        Player player1 = getRandomPlayer();
        Player player2 = getRandomPlayer();
        if (player1 != null && player2 != null && !player1.equals(player2)) {
            player1.teleport(player2.getLocation());
        }
    }

    private Player getRandomPlayer() {
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        if (players.length > 0) {
            return players[random.nextInt(players.length)];
        }
        return null;
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
            String response = openAIClient.generateCommand(wish, players);
            return response != null ? response : defaultCommand();
        } catch (Exception e) {
            getLogger().severe("Error generating command from OpenAI: " + e.getMessage());
            return null;
        }
    }

    private String defaultCommand() {
        executeWorldModification(getRandomModification());
        return null;
    }
}
