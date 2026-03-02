package com.elotop.gui;

import com.elotop.EloTopPlugin;
import com.elotop.manager.EloManager;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class EloTopGUI {

    private final EloTopPlugin plugin;

    public EloTopGUI(EloTopPlugin plugin) {
        this.plugin = plugin;
    }

    public void openBook(Player player) {
        List<EloManager.EloEntry> leaderboard = plugin.getEloManager().getLeaderboard();

        if (leaderboard.isEmpty()) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6EloTop&8] ");
            String msg = plugin.getConfig().getString("messages.no-data", "&cVeri yok!");
            player.sendMessage(colorize(prefix + msg));
            return;
        }

        plugin.getEloManager().updatePlayer(player);

        int playersPerPage = 10;
        int totalPages = (int) Math.ceil((double) leaderboard.size() / playersPerPage);

        int yourRank = plugin.getEloManager().getPlayerRank(player.getUniqueId());
        int yourElo = plugin.getEloManager().getPlayerEloLive(player);

        List<Component> pages = new ArrayList<>();

        for (int page = 0; page < totalPages; page++) {
            int startIndex = page * playersPerPage;
            int endIndex = Math.min(startIndex + playersPerPage, leaderboard.size());

            TextComponent.Builder pb = Component.text();

            // Baslik
            pb.append(Component.text("  ✦ ", TextColor.color(0xFF, 0xAA, 0x00)));
            pb.append(Component.text("Elo Sıralaması", NamedTextColor.BLACK)
                    .decoration(TextDecoration.BOLD, true));
            pb.append(Component.text(" ✦", TextColor.color(0xFF, 0xAA, 0x00)));
            pb.append(Component.newline());
            pb.append(Component.newline());

            // Oyuncular
            for (int i = startIndex; i < endIndex; i++) {
                EloManager.EloEntry entry = leaderboard.get(i);
                int rank = i + 1;
                int elo = entry.getElo();

                // Rank emoji ve rengini al
                RankInfo rankInfo = getRankInfo(elo);

                // Emoji
                pb.append(Component.text(" " + rankInfo.emoji + " ", rankInfo.color));

                // Hover detay
                String leagueTag = entry.getLeagueTag();
                Component hover = Component.text("")
                        .append(Component.text(entry.getPlayerName(), NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("━━━━━━━━━━━", NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(Component.text("Sıra: ", NamedTextColor.GRAY))
                        .append(Component.text("#" + rank, NamedTextColor.YELLOW)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("Elo: ", NamedTextColor.GRAY))
                        .append(Component.text(elo, NamedTextColor.GREEN)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("Rank: ", NamedTextColor.GRAY))
                        .append(Component.text(rankInfo.name, rankInfo.color))
                        .append(Component.newline())
                        .append(Component.text("Lig: ", NamedTextColor.GRAY))
                        .append(leagueTag != null && !leagueTag.isEmpty()
                                ? deserializeHex(leagueTag) : Component.text("?", NamedTextColor.GRAY));

                // Oyuncu ismi - siyah
                pb.append(Component.text(entry.getPlayerName(), NamedTextColor.BLACK)
                        .hoverEvent(HoverEvent.showText(hover)));

                // Elo degeri
                pb.append(Component.text(" " + elo, TextColor.color(0x55, 0xAA, 0x55)));

                pb.append(Component.newline());
            }

            // Son sayfada oyuncunun bilgisi
            if (page == totalPages - 1) {
                pb.append(Component.newline());
                RankInfo yourRankInfo = getRankInfo(yourElo);
                pb.append(Component.text(" ★ ", TextColor.color(0xFF, 0xAA, 0x00)));
                pb.append(Component.text("Sen: ", NamedTextColor.DARK_GRAY));
                pb.append(Component.text(yourRankInfo.emoji + " ", yourRankInfo.color));
                pb.append(Component.text("#" + (yourRank > 0 ? yourRank : "?"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.BOLD, true));
                pb.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
                pb.append(Component.text(yourElo + " ELO", TextColor.color(0x55, 0xAA, 0x55)));
            }

            pages.add(pb.build());
        }

        Book book = Book.book(
                Component.text("Elo Siralama"),
                Component.text("Server"),
                pages
        );

        player.openBook(book);
    }

    /**
     * Elo'ya gore rank bilgisini dondurur
     */
    private RankInfo getRankInfo(int elo) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("rank-emojis.ranks");
        
        if (ranks == null || !plugin.getConfig().getBoolean("rank-emojis.enabled", true)) {
            // Varsayilan
            return new RankInfo("◈", "#AAAAAA", "Unknown");
        }

        for (String rankKey : ranks.getKeys(false)) {
            int min = ranks.getInt(rankKey + ".min-elo", 0);
            int max = ranks.getInt(rankKey + ".max-elo", 999999);
            
            if (elo >= min && elo <= max) {
                String emoji = ranks.getString(rankKey + ".emoji", "◈");
                String colorHex = ranks.getString(rankKey + ".color", "#AAAAAA");
                String name = rankKey.substring(0, 1).toUpperCase() + rankKey.substring(1);
                
                return new RankInfo(emoji, colorHex, name);
            }
        }

        // Bulunamazsa varsayilan
        return new RankInfo("◈", "#AAAAAA", "Unknown");
    }

    /**
     * Rank bilgilerini tutan sinif
     */
    private static class RankInfo {
        final String emoji;
        final TextColor color;
        final String name;

        RankInfo(String emoji, String hexColor, String name) {
            this.emoji = emoji;
            this.name = name;
            
            // Hex rengi TextColor'a cevirme
            if (hexColor.startsWith("#")) {
                hexColor = hexColor.substring(1);
            }
            try {
                int rgb = Integer.parseInt(hexColor, 16);
                this.color = TextColor.color(rgb);
            } catch (Exception e) {
                this.color = NamedTextColor.GRAY;
            }
        }
    }

    public static Component deserializeHex(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        StringBuilder sb = new StringBuilder();
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '#' && i + 6 < chars.length) {
                String hex = text.substring(i + 1, i + 7);
                if (hex.matches("[0-9A-Fa-f]{6}")) {
                    sb.append("&#").append(hex);
                    i += 6;
                    continue;
                }
            }
            sb.append(chars[i]);
        }

        return LegacyComponentSerializer.legacyAmpersand().deserialize(sb.toString());
    }

    public static Component colorize(String text) {
        if (text == null) return Component.empty();
        return deserializeHex(text);
    }

    public int getPlayerPage(UUID uuid) { return 1; }
    public void removePlayer(UUID uuid) { }
}
