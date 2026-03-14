package com.elotop.manager;

import com.elotop.EloTopPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GistUploader {

    private final EloTopPlugin plugin;
    private final String gistId;
    private final String githubToken;

    public GistUploader(EloTopPlugin plugin) {
        this.plugin = plugin;
        this.gistId = plugin.getConfig().getString("web.gist-id", "");
        this.githubToken = plugin.getConfig().getString("web.github-token", "");
    }

    public void uploadLeaderboard() {
        if (gistId.isEmpty() || githubToken.isEmpty()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String json = buildJson();
                updateGist(json);
                plugin.getLogger().info("Leaderboard Gist'e yuklendi!");
            } catch (Exception e) {
                plugin.getLogger().warning("Gist yuklenemedi: " + e.getMessage());
            }
        });
    }

    private String buildJson() {
        List<EloManager.EloEntry> leaderboard = plugin.getEloManager().getLeaderboard();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"lastUpdate\":").append(System.currentTimeMillis());
        sb.append(",\"totalPlayers\":").append(leaderboard.size());
        sb.append(",\"leaderboard\":[");

        for (int i = 0; i < leaderboard.size(); i++) {
            EloManager.EloEntry entry = leaderboard.get(i);
            if (i > 0) sb.append(",");

            String name = escapeJson(entry.getPlayerName());
            String league = escapeJson(stripColors(entry.getLeagueTag()));

            sb.append("{");
            sb.append("\"rank\":").append(i + 1);
            sb.append(",\"name\":\"").append(name).append("\"");
            sb.append(",\"elo\":").append(entry.getElo());
            sb.append(",\"league\":\"").append(league).append("\"");
            sb.append(",\"uuid\":\"").append(entry.getUuid().toString()).append("\"");
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private void updateGist(String jsonContent) throws Exception {
        URL url = new URL("https://api.github.com/gists/" + gistId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PATCH");
        conn.setRequestProperty("Authorization", "token " + githubToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setDoOutput(true);

        String escapedContent = escapeJson(jsonContent);
        String body = "{\"files\":{\"elotop.json\":{\"content\":\"" + escapedContent + "\"}}}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new Exception("GitHub API hata: " + code);
        }

        conn.disconnect();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String stripColors(String text) {
        if (text == null) return "";
        String s = text.replaceAll("§x(§[0-9A-Fa-f]){6}", "");
        s = s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        s = s.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        s = s.replaceAll("#[0-9A-Fa-f]{6}", "");
        return s.trim();
    }
}
