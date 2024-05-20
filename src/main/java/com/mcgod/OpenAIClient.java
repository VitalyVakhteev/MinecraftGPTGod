package com.mcgod;

import org.bukkit.entity.Player;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;

public class OpenAIClient {
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final String model;
    private final Gson gson;

    public OpenAIClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.gson = new Gson();
    }

    public String generateCommand(String wish, Player[] players) throws IOException {
        OkHttpClient client = new OkHttpClient();

        JsonObject json = new JsonObject();
        json.addProperty("model", model);
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a Minecraft plugin that generates commands based on players' wishes. Make sure the commands are valid and executable in Minecraft. Be less benevolent to the players, add some challenges or tricky conditions.");
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
            return parts[1].trim(); // The command should be between the ```
        }
        return null;
    }

    public String getAdvice(String message) throws IOException {
        OkHttpClient client = new OkHttpClient();

        JsonObject json = new JsonObject();
        json.addProperty("model", model);
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a helpful assistant that provides advice to players in a Minecraft world. However, be less benevolent and add some challenges or tricky conditions.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);
        messages.add(userMessage);

        json.add("messages", messages);

        return getString(client, json);
    }

    public String getSpyInfo(Player player, Player targetPlayer) throws IOException {
        OkHttpClient client = new OkHttpClient();
        JsonObject json = new JsonObject();
        json.addProperty("model", model);
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a helpful assistant that provides information about players in a Minecraft world. Be less benevolent and add some challenges or tricky conditions.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Provide some interesting information about player " + targetPlayer.getName() + " to " + player.getName() + ".");
        messages.add(userMessage);

        json.add("messages", messages);

        return getString(client, json);
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
}