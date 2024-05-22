package com.mcgod.utility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Random;

public class OpenAIClient {
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final Gson gson;

    public OpenAIClient(String apiKey) {
        this.apiKey = apiKey;
        this.gson = new Gson();
    }

    public String generateCommand(String wish, Player[] players) throws IOException {
        OkHttpClient client = new OkHttpClient();

        JsonObject json = new JsonObject();
        json.addProperty("model", "gpt-3.5-turbo-0125");
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a Minecraft plugin that generates commands based on players' wishes. Make sure the commands are valid and executable in Minecraft.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        StringBuilder playersNameAndLoc = new StringBuilder();
        for (Player player : players) {
            playersNameAndLoc.append(player.getName()).append(" at ").append(player.getLocation().getBlockX()).append(", ").append(player.getLocation().getBlockY()).append(", ").append(player.getLocation().getBlockZ()).append("; ");
        }

        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Generate a valid and executable Minecraft command to fulfill the wish: \"" + wish + "\". Players: " + playersNameAndLoc);
        messages.add(userMessage);

        json.add("messages", messages);

        return getString(client, json);
    }

    public String extractCommand(String response) {
        String[] parts = response.split("```");
        if (parts.length >= 2) {
            return parts[1].trim();
        }
        return null;
    }

    public String getAdvice(String message) throws IOException {
        OkHttpClient client = new OkHttpClient();

        JsonObject json = getJsonObject("You are a helpful assistant.", message);

        return getString(client, json);
    }

    public String getSpyInfo(Player player, Player targetPlayer) throws IOException {
        OkHttpClient client = new OkHttpClient();
        JsonObject json = getJsonObject("You are a helpful assistant that provides information about players in a Minecraft world.", "Provide some interesting information about player " + targetPlayer.getName() + " to " + player.getName() + ".");

        return getString(client, json);
    }

    public String getQuestText(String playerName, String npcName, String questItem) throws IOException {
        OkHttpClient client = new OkHttpClient();

        JsonObject json = new JsonObject();
        json.addProperty("model", "gpt-3.5-turbo-0125");
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a helpful assistant that creates quests for players in Minecraft.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Generate a quest for player " + playerName + " with NPC " + npcName + ". The quest should involve bringing a " + questItem + ".");
        messages.add(userMessage);

        json.add("messages", messages);

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);

            assert response.body() != null;
            JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
            JsonArray choices = responseBody.getAsJsonArray("choices");
            if (!choices.isEmpty()) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject messageContent = firstChoice.getAsJsonObject("message");
                return messageContent.get("content").getAsString().trim();
            }
        }
        return null;
    }

    public String generateNpcName(String playerName) throws IOException {
        OkHttpClient client = new OkHttpClient();

        JsonObject json = new JsonObject();
        json.addProperty("model", "gpt-3.5-turbo-0125");
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a helpful assistant that generates names for NPCs in a Minecraft quest.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Generate a creative name for an NPC that will give a quest to player " + playerName + ".");
        messages.add(userMessage);

        json.add("messages", messages);

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);

            assert response.body() != null;
            JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
            JsonArray choices = responseBody.getAsJsonArray("choices");
            if (!choices.isEmpty()) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject messageContent = firstChoice.getAsJsonObject("message");
                return messageContent.get("content").getAsString().trim();
            }
        }
        return null;
    }

    private String getString(OkHttpClient client, JsonObject json) throws IOException {
        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);

            assert response.body() != null;
            JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
            JsonArray choices = responseBody.getAsJsonArray("choices");
            if (!choices.isEmpty()) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject messageContent = firstChoice.getAsJsonObject("message");
                return messageContent.get("content").getAsString().trim();
            }
        }
        return null;
    }

    private static JsonObject getJsonObject(String systemContent, String userContent) {
        JsonObject json = new JsonObject();
        json.addProperty("model", "gpt-3.5-turbo-0125");
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemContent);
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userContent);
        messages.add(userMessage);

        json.add("messages", messages);
        return json;
    }

    public String getValidQuestItem() {
        String[] items = {"DIAMOND", "GOLD_INGOT", "EMERALD", "IRON_INGOT"};
        Random random = new Random();
        return items[random.nextInt(items.length)];
    }
}
