package com.mcgod;

import com.mcgod.commands.*;
import com.mcgod.utility.FileStorageUtil;
import com.mcgod.utility.ObeliskManager;
import com.mcgod.utility.WishManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class MCGodMain extends JavaPlugin {
    private Map<Player, String> playerWishes;
    private Map<Player, Double> playerChances;
    private Set<Player> playersSacrificed;
    private Location holySiteLocation;
    private String apiKey;
    private int intervalTaskId;
    private double baseChance = 0.25;
    private boolean isHolySiteValid;
    private ObeliskManager obeliskManager;
    private WishManager wishManager;
    private boolean isActive;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        apiKey = config.getString("openai.apiKey");
        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("OpenAI API key not found in config.yml!");
        }

        playerWishes = new HashMap<>();
        playerChances = new HashMap<>();
        playersSacrificed = new HashSet<>();

        obeliskManager = new ObeliskManager(this, config);
        isHolySiteValid = obeliskManager.initializeHolySite(config);

        wishManager = new WishManager(this, apiKey, playerWishes, playerChances, baseChance, playersSacrificed, isHolySiteValid, obeliskManager);

        QuestCommand questCommand = new QuestCommand(apiKey);
        Objects.requireNonNull(getCommand("pray")).setExecutor(new PrayCommand(wishManager));
        Objects.requireNonNull(getCommand("sacrifice")).setExecutor(new SacrificeCommand(wishManager));
        Objects.requireNonNull(getCommand("advice")).setExecutor(new AdviceCommand(apiKey));
        Objects.requireNonNull(getCommand("spy")).setExecutor(new SpyCommand(apiKey));
        Objects.requireNonNull(getCommand("start_quest")).setExecutor(questCommand);
        Objects.requireNonNull(getCommand("quest_reminder")).setExecutor(questCommand);
        Objects.requireNonNull(getCommand("end_quest")).setExecutor(questCommand);
        Objects.requireNonNull(getCommand("start")).setExecutor(new StartCommand(this));
        Objects.requireNonNull(getCommand("stop")).setExecutor(new StopCommand(this));

        activatePlugin();

        getLogger().info("MCGod plugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        deactivatePlugin();
        FileStorageUtil.saveQuests(FileStorageUtil.loadQuests());
        getLogger().info("MCGod plugin disabled!");
    }

    public void activatePlugin() {
        if (!isActive) {
            FileConfiguration config = this.getConfig();
            int actionInterval = config.getInt("action-interval", 3600);
            intervalTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                wishManager.performRandomAction();
            }, actionInterval, actionInterval);
            isActive = true;
        }
    }

    public void deactivatePlugin() {
        if (isActive) {
            Bukkit.getScheduler().cancelTask(intervalTaskId);
            isActive = false;
        }
    }

    public boolean isPluginActive() {
        return isActive;
    }
}
