package com.mcgod.commands;

import com.mcgod.utility.WishManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SacrificeCommand implements CommandExecutor {
    private final WishManager wishManager;

    public SacrificeCommand(WishManager wishManager) {
        this.wishManager = wishManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            wishManager.handleSacrifice(player);
            return true;
        }
        return false;
    }
}
