package com.mcgod.commands;

import com.mcgod.MCGodMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class StartCommand implements CommandExecutor {
    private final MCGodMain plugin;

    public StartCommand(MCGodMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!plugin.isPluginActive()) {
            plugin.activatePlugin();
            sender.sendMessage("MCGod plugin has been started.");
        } else {
            sender.sendMessage("MCGod plugin is already running.");
        }
        return true;
    }
}
