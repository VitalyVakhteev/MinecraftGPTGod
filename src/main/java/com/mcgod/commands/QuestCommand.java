package com.mcgod.commands;

import com.mcgod.MCGodMain;
import com.mcgod.utility.OpenAIClient;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class QuestCommand implements CommandExecutor {
    private final String apiKey;
    private final Map<Player, QuestData> playerQuests;
    private final Random random;

    public QuestCommand(String apiKey) {
        this.apiKey = apiKey;
        this.playerQuests = new HashMap<>();
        this.random = new Random();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (command.getName().equalsIgnoreCase("start_quest")) {
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
                String questDescription;
                String questItem = "diamond";

                try {
                    questDescription = openAIClient.getQuestText(player.getName(), npcName);
                } catch (IOException e) {
                    player.sendMessage("Failed to get quest text from ChatGPT.");
                    return;
                }

                if (questDescription != null) {
                    playerQuests.put(player, new QuestData(npcName, questDescription, questItem, location));
                    String summonCommand = "summon villager " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + " {CustomName:'{\"text\":\"" + npcName + "\"}'}";
                    Bukkit.getScheduler().runTask(MCGodMain.getPlugin(MCGodMain.class), () -> {
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
        QuestData questData = playerQuests.get(player);
        if (questData != null) {
            player.sendMessage("Quest Reminder: " + questData.getQuestDescription());
        } else {
            player.sendMessage("You have no active quest.");
        }
    }

    private void endQuest(Player player) {
        QuestData questData = playerQuests.get(player);
        if (questData != null) {
            Location playerLocation = player.getLocation();
            if (playerLocation.distance(questData.getNpcLocation()) < 10) {
                Material itemInHand = player.getInventory().getItemInMainHand().getType();
                if (itemInHand == Material.matchMaterial(questData.getQuestItem())) {
                    playerQuests.remove(player);
                    String reward = getRandomReward();
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Objects.requireNonNull(Material.matchMaterial(reward)), 1));
                    player.sendMessage("Quest completed! You have received: " + reward);
                } else {
                    player.sendMessage("You don't have the required item to complete the quest.");
                }
            } else {
                player.sendMessage("You are too far from the NPC to complete the quest.");
            }
        } else {
            player.sendMessage("You have no active quest.");
        }
    }

    private String getRandomReward() {
        String[] rewards = {"diamond", "gold_ingot", "emerald", "netherite_ingot"};
        return rewards[random.nextInt(rewards.length)];
    }

    private static class QuestData {
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
    }
}