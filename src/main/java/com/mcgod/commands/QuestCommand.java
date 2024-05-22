package com.mcgod.commands;

import com.mcgod.MCGodMain;
import com.mcgod.utility.FileStorageUtil;
import com.mcgod.utility.OpenAIClient;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

public class QuestCommand implements CommandExecutor {
    private final String apiKey;
    private final Random random;
    private final Logger logger;

    public QuestCommand(String apiKey) {
        this.apiKey = apiKey;
        this.random = new Random();
        this.logger = Bukkit.getLogger();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (command.getName().equalsIgnoreCase("start_quest")) {
                Map<UUID, QuestData> playerQuests = FileStorageUtil.loadQuests();
                if (playerQuests.containsKey(player.getUniqueId())) {
                    player.sendMessage("You already have an active quest. Complete it before starting a new one.");
                    return true;
                }
                startQuest(player);
                return true;
            } else if (command.getName().equalsIgnoreCase("quest_reminder")) {
                questReminder(player);
                return true;
            } else if (command.getName().equalsIgnoreCase("end_quest")) {
                endQuest(player);
                return true;
            }
        }
        return false;
    }

    private void startQuest(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                OpenAIClient openAIClient = new OpenAIClient(apiKey);
                String npcName;
                try {
                    npcName = openAIClient.generateNpcName(player.getName());
                } catch (IOException e) {
                    player.sendMessage("Failed to get NPC name from ChatGPT.");
                    return;
                }
                Location location = player.getLocation();
                String questItem = openAIClient.getValidQuestItem(); // Get a valid quest item
                String questDescription;

                try {
                    questDescription = openAIClient.getQuestText(player.getName(), npcName, questItem);
                } catch (IOException e) {
                    player.sendMessage("Failed to get quest text from ChatGPT.");
                    return;
                }

                if (questDescription != null && Material.matchMaterial(questItem) != null) {
                    Bukkit.getScheduler().runTask(MCGodMain.getPlugin(MCGodMain.class), () -> {
                        Map<UUID, QuestData> playerQuests = FileStorageUtil.loadQuests();
                        playerQuests.put(player.getUniqueId(), new QuestData(npcName, questDescription, questItem, location));
                        FileStorageUtil.saveQuests(playerQuests);
                        logger.info("Quest started for player " + player.getName() + " with UUID " + player.getUniqueId() + ". Current map contents: " + playerQuests);
                        String summonCommand = String.format(
                                "summon villager %d %d %d {CustomName:'{\"text\":\"%s\"}',NoAI:1b,Invulnerable:1b}",
                                location.getBlockX(), location.getBlockY(), location.getBlockZ(), npcName
                        );
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), summonCommand);
                        player.sendMessage(questDescription);
                    });
                } else {
                    player.sendMessage("Failed to start the quest.");
                }
            }
        }.runTaskAsynchronously(MCGodMain.getPlugin(MCGodMain.class));
    }

    private void questReminder(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<UUID, QuestData> playerQuests = FileStorageUtil.loadQuests();
        QuestData questData = playerQuests.get(playerUUID);
        logger.info("Retrieving quest for player " + player.getName() + " with UUID " + playerUUID + ". Current map contents: " + playerQuests);
        if (questData != null) {
            player.sendMessage("Quest Reminder: " + questData.getQuestDescription());
            logger.info("Quest reminder for player " + player.getName() + " with UUID " + playerUUID + " found in map.");
        } else {
            player.sendMessage("You have no active quest.");
            logger.warning("No active quest found for player " + player.getName() + " with UUID " + playerUUID + " in map.");
        }
    }

    private void endQuest(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<UUID, QuestData> playerQuests = FileStorageUtil.loadQuests();
        QuestData questData = playerQuests.get(playerUUID);
        logger.info("Ending quest for player " + player.getName() + " with UUID " + playerUUID + ". Current map contents: " + playerQuests);
        if (questData != null) {
            Location playerLocation = player.getLocation();
            if (playerLocation.distance(questData.getNpcLocation()) < 10) {
                Material itemInHand = player.getInventory().getItemInMainHand().getType();
                if (itemInHand == Material.matchMaterial(questData.getQuestItem())) {
                    player.getInventory().removeItem(new ItemStack(itemInHand, 1));
                    playerQuests.remove(playerUUID);
                    FileStorageUtil.saveQuests(playerQuests);
                    String reward = getRandomReward();
                    player.getInventory().addItem(new ItemStack(Objects.requireNonNull(Material.matchMaterial(reward)), 1));
                    player.sendMessage("Quest completed! You have received: " + reward);
                    logger.info("Quest completed for player " + player.getName() + " with UUID " + playerUUID);

                    // Remove NPC after quest completion
                    String removeNpcCommand = String.format(
                            "execute at %s run kill @e[type=villager,name=\"%s\",distance=..1]",
                            player.getName(), questData.getNpcName()
                    );
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), removeNpcCommand);
                } else {
                    player.sendMessage("You don't have the required item to complete the quest.");
                    logger.warning("Player " + player.getName() + " with UUID " + playerUUID + " does not have the required item.");
                }
            } else {
                player.sendMessage("You are too far from the NPC to complete the quest.");
                logger.warning("Player " + player.getName() + " with UUID " + playerUUID + " is too far from the NPC.");
            }
        } else {
            player.sendMessage("You have no active quest.");
            logger.warning("No active quest found for player " + player.getName() + " with UUID " + playerUUID + " in map.");
        }
    }

    private String getRandomReward() {
        String[] rewards = {"DIAMOND", "GOLD_INGOT", "EMERALD", "NETHERITE_INGOT"};
        return rewards[random.nextInt(rewards.length)];
    }

    public static class QuestData {
        private final String npcName;
        private final String questDescription;
        private final String questItem;
        private final Location npcLocation;

        public QuestData(String npcName, String questDescription, String questItem, Location npcLocation) {
            this.npcName = npcName;
            this.questDescription = questDescription;
            this.questItem = questItem;
            this.npcLocation = npcLocation;
        }

        public String getNpcName() {
            return npcName;
        }

        public String getQuestDescription() {
            return questDescription;
        }

        public String getQuestItem() {
            return questItem;
        }

        public Location getNpcLocation() {
            return npcLocation;
        }

        @Override
        public String toString() {
            return "QuestData{" +
                    "npcName='" + npcName + '\'' +
                    ", questDescription='" + questDescription + '\'' +
                    ", questItem='" + questItem + '\'' +
                    ", npcLocation=" + npcLocation +
                    '}';
        }
    }
}
