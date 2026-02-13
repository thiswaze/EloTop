package com.elotop.manager;

import com.elotop.EloTopPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EloManager {

    private final EloTopPlugin plugin;
    private final Map<UUID, EloEntry> eloCache = new ConcurrentHashMap<>();
    private List<EloEntry> sortedLeaderboard = new ArrayList<>();

    public EloManager(EloTopPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateCache() {
        String placeholder = plugin.getConfig().getString("elo-placeholder", "%alonsoleagues_elo%");
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
                if (parsed != null && !parsed.isEmpty() && !parsed.equals(placeholder)) {
                    String cleaned = parsed.replaceAll("[^0-9.-]", "");
                    if (!cleaned.isEmpty()) {
                        int elo = (int) Double.parseDouble(cleaned);
                        eloCache.put(player.getUniqueId(),
                                new EloEntry(player.getUniqueId(), player.getName(), elo));
                    }
                }
            } catch (NumberFormatException e) {
                // skip
            }
        }
        rebuildLeaderboard();
    }

    public void updatePlayer(Player player) {
        String placeholder = plugin.getConfig().getString("elo-placeholder", "%alonsoleagues_elo%");
        try {
            String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
            if (parsed != null && !parsed.isEmpty() && !parsed.equals(placeholder)) {
                String cleaned = parsed.replaceAll("[^0-9.-]", "");
                if (!cleaned.isEmpty()) {
                    int elo = (int) Double.parseDouble(cleaned);
                    eloCache.put(player.getUniqueId(),
                            new EloEntry(player.getUniqueId(), player.getName(), elo));
                    rebuildLeaderboard();
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
        String placeholder = plugin.getConfig().getString("elo-placeholder", "%alonsoleagues_elo%");
        try {
            String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
            if (parsed != null && !parsed.isEmpty() && !parsed.equals(placeholder)) {
                String cleaned = parsed.replaceAll("[^0-9.-]", "");
                if (!cleaned.isEmpty()) return (int) Double.parseDouble(cleaned);
            }
        } catch (NumberFormatException e) { /* skip */ }
        return 0;
    }

    public void clearCache() {
        eloCache.clear();
        sortedLeaderboard.clear();
    }

    public void shutdown() { clearCache(); }
    public int getTotalPlayers() { return sortedLeaderboard.size(); }

    public static class EloEntry {
        private final UUID uuid;
        private final String playerName;
        private final int elo;

        public EloEntry(UUID uuid, String playerName, int elo) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.elo = elo;
        }

        public UUID getUuid() { return uuid; }
        public String getPlayerName() { return playerName; }
        public int getElo() { return elo; }
    }
}
