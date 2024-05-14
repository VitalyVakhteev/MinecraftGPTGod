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
        systemMessage.addProperty("content", "You are a Minecraft plugin that generates commands based on players' wishes.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        StringBuilder playersNameAndLoc = new StringBuilder();
        for (Player player : players) {
            playersNameAndLoc.append(player.getName()).append(" at ").append(player.getLocation().getBlockX()).append(", ").append(player.getLocation().getBlockY()).append(", ").append(player.getLocation().getBlockZ()).append("; ");
        }

        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Generate a Minecraft command to fulfill the wish: \"" + wish + "\". Players: " + playersNameAndLoc);
        messages.add(userMessage);

        json.add("messages", messages);

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

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

    public String extractCommand(String response) {
        String[] parts = response.split("```");
        if (parts.length >= 2) {
            return parts[1].trim(); // The command should be between the ```
        }
        return null;
    }
}
