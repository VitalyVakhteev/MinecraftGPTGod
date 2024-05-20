package com.mcgod.commands;

import com.mcgod.utility.WishManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PrayCommand implements CommandExecutor {
    private final WishManager wishManager;

    public PrayCommand(WishManager wishManager) {
        this.wishManager = wishManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                String wish = String.join(" ", args);
                wishManager.recordPrayer(player, wish);
            } else {
                player.sendMessage("You must specify your prayer.");
            }
            return true;
        }
        return false;
    }
}
