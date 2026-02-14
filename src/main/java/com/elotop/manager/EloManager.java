package com.elotop.manager;

import com.elotop.EloTopPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EloManager {

    private final EloTopPlugin plugin;
    private final Map<UUID, EloEntry> eloCache = new ConcurrentHashMap<>();
    private List<EloEntry> sortedLeaderboard = new ArrayList<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public EloManager(EloTopPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("data.yml olusturulamadi!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String name = dataConfig.getString("players." + uuidStr + ".name", "Unknown");
                    int elo = dataConfig.getInt("players." + uuidStr + ".elo", 0);
                    String league = dataConfig.getString("players." + uuidStr + ".league", "");
                    eloCache.put(uuid, new EloEntry(uuid, name, elo, league));
                } catch (Exception e) {
                    // skip
                }
            }
            rebuildLeaderboard();
            plugin.getLogger().info(eloCache.size() + " oyuncu verisi yuklendi!");
        }
    }

    public void saveData() {
        if (dataConfig == null || dataFile == null) return;

        dataConfig.set("players", null);

        for (Map.Entry<UUID, EloEntry> entry : eloCache.entrySet()) {
            String path = "players." + entry.getKey().toString();
            dataConfig.set(path + ".name", entry.getValue().getPlayerName());
            dataConfig.set(path + ".elo", entry.getValue().getElo());
            dataConfig.set(path + ".league", entry.getValue().getLeagueTag());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("data.yml kaydedilemedi!");
        }
    }

    public void updateCache() {
        String placeholder = plugin.getConfig().getString("elo-placeholder", "%alonsoleagues_points%");
        String leaguePlaceholder = plugin.getConfig().getString("league-placeholder", "%alonsoleagues_league_displayname%");
        boolean changed = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
                String leagueParsed = PlaceholderAPI.setPlaceholders(player, leaguePlaceholder);

                if (parsed != null && !parsed.isEmpty() && !parsed.equals(placeholder)) {
                    String cleaned = parsed.replaceAll("[^0-9.-]", "");
                    if (!cleaned.isEmpty()) {
                        int elo = (int) Double.parseDouble(cleaned);
                        String league = "";
                        if (leagueParsed != null && !leagueParsed.isEmpty()
                                && !leagueParsed.equals(leaguePlaceholder)) {
                            league = leagueParsed;
                        }

                        EloEntry existing = eloCache.get(player.getUniqueId());
                        if (existing == null || existing.getElo() != elo
                                || !existing.getPlayerName().equals(player.getName())
                                || !existing.getLeagueTag().equals(league)) {
                            eloCache.put(player.getUniqueId(),
                                    new EloEntry(player.getUniqueId(), player.getName(), elo, league));
                            changed = true;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // skip
            }
        }

        if (changed) {
            rebuildLeaderboard();
            saveData();
        }
    }

    public void updatePlayer(Player player) {
        String placeholder = plugin.getConfig().getString("elo-placeholder", "%alonsoleagues_points%");
        String leaguePlaceholder = plugin.getConfig().getString("league-placeholder", "%alonsoleagues_league_displayname%");

        try {
            String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
            String leagueParsed = PlaceholderAPI.setPlaceholders(player, leaguePlaceholder);

            if (parsed != null && !parsed.isEmpty() && !parsed.equals(placeholder)) {
                String cleaned = parsed.replaceAll("[^0-9.-]", "");
                if (!cleaned.isEmpty()) {
                    int elo = (int) Double.parseDouble(cleaned);
                    String league = "";
                    if (leagueParsed != null && !leagueParsed.isEmpty()
                            && !leagueParsed.equals(leaguePlaceholder)) {
                        league = leagueParsed;
                    }

                    eloCache.put(player.getUniqueId(),
                            new EloEntry(player.getUniqueId(), player.getName(), elo, league));
                    rebuildLeaderboard();
                    saveData();
                }
            }
        } catch (NumberFormatException e) {
            // skip
        }
    }

    private void rebuildLeaderboard() {
        int maxPlayers = plugin.getConfig().getInt("max-players", 250);
        sortedLeaderboard = eloCache.values().stream()
                .sorted(Comparator.comparingInt(EloEntry::getElo).reversed())
                .limit(maxPlayers)
                .collect(Collectors.toList());
    }

    public List<EloEntry> getLeaderboard() {
        return Collections.unmodifiableList(sortedLeaderboard);
    }

    public int getPlayerRank(UUID uuid) {
        for (int i = 0; i < sortedLeaderboard.size(); i++) {
            if (sortedLeaderboard.get(i).getUuid().equals(uuid)) return i + 1;
        }
        return -1;
    }

    public int getPlayerEloLive(Player player) {
        String placeholder = plugin.getConfig().getString("elo-placeholder", "%alonsoleagues_points%");
        try {
            String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
            if (parsed != null && !parsed.isEmpty() && !parsed.equals(placeholder)) {
                String cleaned = parsed.replaceAll("[^0-9.-]", "");
                if (!cleaned.isEmpty()) return (int) Double.parseDouble(cleaned);
            }
        } catch (NumberFormatException e) { /* skip */ }
        return 0;
    }

    public String getPlayerLeague(UUID uuid) {
        EloEntry entry = eloCache.get(uuid);
        return entry != null ? entry.getLeagueTag() : "";
    }

    public void clearCache() {
        eloCache.clear();
        sortedLeaderboard.clear();
    }

    public void shutdown() {
        saveData();
        clearCache();
    }

    public int getTotalPlayers() { return sortedLeaderboard.size(); }

    public static class EloEntry {
        private final UUID uuid;
        private final String playerName;
        private final int elo;
        private final String leagueTag;

        public EloEntry(UUID uuid, String playerName, int elo, String leagueTag) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.elo = elo;
            this.leagueTag = leagueTag != null ? leagueTag : "";
        }

        public UUID getUuid() { return uuid; }
        public String getPlayerName() { return playerName; }
        public int getElo() { return elo; }
        public String getLeagueTag() { return leagueTag; }
    }
}
