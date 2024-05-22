package com.mcgod.commands;

import com.mcgod.MCGodMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class StopCommand implements CommandExecutor {
    private final MCGodMain plugin;

    public StopCommand(MCGodMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (plugin.isPluginActive()) {
            plugin.deactivatePlugin();
            sender.sendMessage("MCGod plugin has been stopped.");
        } else {
            sender.sendMessage("MCGod plugin is not currently running.");
        }
        return true;
    }
}
