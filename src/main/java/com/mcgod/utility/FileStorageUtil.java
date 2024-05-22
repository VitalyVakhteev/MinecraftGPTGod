package com.mcgod.utility;

import com.mcgod.commands.QuestCommand.QuestData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorageUtil {
    private static final Gson gson = new Gson();
    private static final String QUEST_DATA_FILE = "plugins/MCGod/quests.json";

    public static void saveQuests(Map<UUID, QuestData> quests) {
        Map<UUID, QuestDataSerializable> simplifiedQuests = new ConcurrentHashMap<>();
        quests.forEach((uuid, questData) -> simplifiedQuests.put(uuid, new QuestDataSerializable(questData)));

        try (Writer writer = new FileWriter(QUEST_DATA_FILE)) {
            gson.toJson(simplifiedQuests, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<UUID, QuestData> loadQuests() {
        File file = new File(QUEST_DATA_FILE);
        if (!file.exists()) {
            try {
                if (file.getParentFile().mkdirs() && file.createNewFile()) {
                    Bukkit.getLogger().info("Created new quest data file at " + QUEST_DATA_FILE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new ConcurrentHashMap<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<UUID, QuestDataSerializable>>() {}.getType();
            Map<UUID, QuestDataSerializable> simplifiedQuests = gson.fromJson(reader, type);
            if (simplifiedQuests == null) {
                simplifiedQuests = new ConcurrentHashMap<>();
            }
            Map<UUID, QuestData> quests = new ConcurrentHashMap<>();
            simplifiedQuests.forEach((uuid, questDataSerializable) -> quests.put(uuid, questDataSerializable.toQuestData()));
            return quests;
        } catch (IOException e) {
            e.printStackTrace();
            return new ConcurrentHashMap<>();
        }
    }

    private static class QuestDataSerializable {
        private final String npcName;
        private final String questDescription;
        private final String questItem;
        private final String worldName;
        private final double x, y, z;
        private final float pitch, yaw;

        public QuestDataSerializable(QuestData questData) {
            this.npcName = questData.getNpcName();
            this.questDescription = questData.getQuestDescription();
            this.questItem = questData.getQuestItem();
            Location location = questData.getNpcLocation();
            this.worldName = location.getWorld().getName();
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.pitch = location.getPitch();
            this.yaw = location.getYaw();
        }

        public QuestData toQuestData() {
            Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            return new QuestData(npcName, questDescription, questItem, location);
        }
    }
}
