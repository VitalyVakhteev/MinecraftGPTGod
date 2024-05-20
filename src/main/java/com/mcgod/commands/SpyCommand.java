package com.mcgod.commands;

import com.mcgod.MCGodMain;
import com.mcgod.utility.OpenAIClient;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static org.bukkit.Bukkit.getLogger;

public class SpyCommand implements CommandExecutor {
    private final String apiKey;

    public SpyCommand(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                String targetPlayerName = args[0];
                Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
                if (targetPlayer != null) {
                    double spyChance = 0.5;
                    if (Math.random() < spyChance) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                OpenAIClient openAIClient = new OpenAIClient(apiKey);
                                String info = null;
                                try {
                                    info = openAIClient.getSpyInfo(player, targetPlayer);
                                } catch (IOException e) {
                                    getLogger().info("Failed to get info about " + targetPlayerName + " from ChatGPT.");
                                    throw new RuntimeException(e);
                                }
                                if (info != null) {
                                    player.sendMessage("ChatGPT's info about " + targetPlayerName + ": " + info);
                                } else {
                                    player.sendMessage("Failed to get info about " + targetPlayerName + " from ChatGPT.");
                                }
                            }
                        }.runTaskAsynchronously(MCGodMain.getPlugin(MCGodMain.class));
                    } else {
                        player.sendMessage("ChatGPT refuses to find any interesting information about " + targetPlayerName + ".");
                    }
                } else {
                    player.sendMessage("Player " + targetPlayerName + " not found.");
                }
            } else {
                player.sendMessage("You must specify the player to spy on.");
            }
            return true;
        }
        return false;
    }
}
