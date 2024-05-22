package com.mcgod.commands;

import com.mcgod.MCGodMain;
import com.mcgod.utility.OpenAIClient;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AdviceCommand implements CommandExecutor {
    private final String apiKey;

    public AdviceCommand(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                String message = String.join(" ", args);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        OpenAIClient openAIClient = new OpenAIClient(apiKey);
                        String response = null;
                        try {
                            response = openAIClient.getAdvice(message);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (response != null) {
                            player.sendMessage("§aChatGPT's advice: " + response);
                        } else {
                            player.sendMessage("Failed to get advice from §aChatGPT.");
                        }
                    }
                }.runTaskAsynchronously(MCGodMain.getPlugin(MCGodMain.class));
            } else {
                player.sendMessage("You must specify your message.");
            }
            return true;
        }
        return false;
    }
}
