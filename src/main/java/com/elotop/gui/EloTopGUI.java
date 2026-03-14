package com.elotop.gui;

import com.elotop.EloTopPlugin;
import com.elotop.manager.EloManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

            // ===== BASLIK =====
            pb.append(Component.text("  ✦ ", NamedTextColor.GOLD));
            pb.append(Component.text("Elo Sıralaması", NamedTextColor.BLACK)
                    .decoration(TextDecoration.BOLD, true));
            pb.append(Component.text(" ✦", NamedTextColor.GOLD));
            pb.append(Component.newline());
            pb.append(Component.newline());

            // ===== OYUNCULAR =====
            for (int i = startIndex; i < endIndex; i++) {
                EloManager.EloEntry entry = leaderboard.get(i);
                int rank = i + 1;
                int elo = entry.getElo();

                // Sıra numarası
                pb.append(Component.text(rank + ": ", NamedTextColor.DARK_GRAY));

                // Rank emojisi - Lig bazli
                String emoji = getEmojiForLeague(entry.getLeagueTag(), player);
                if (emoji != null && !emoji.isEmpty()) {
                    pb.append(Component.text(emoji + " ").color(NamedTextColor.WHITE));
                }

                // Hover detay
                String leagueTag = entry.getLeagueTag();
                Component hover = Component.text("")
                        .append(Component.text(entry.getPlayerName(), NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("Sıra: ", NamedTextColor.GRAY))
                        .append(Component.text("#" + rank, NamedTextColor.YELLOW)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("Elo: ", NamedTextColor.GRAY))
                        .append(Component.text(elo, NamedTextColor.GREEN)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("Lig: ", NamedTextColor.GRAY))
                        .append(leagueTag != null && !leagueTag.isEmpty()
                                ? deserializeHex(leagueTag) : Component.text("?", NamedTextColor.GRAY));

                // Oyuncu ismi
                pb.append(Component.text(entry.getPlayerName(), NamedTextColor.BLACK)
                        .hoverEvent(HoverEvent.showText(hover)));

                pb.append(Component.newline());
            }

            // ===== SON SAYFADA SEN BILGISI =====
            if (page == totalPages - 1) {
                pb.append(Component.newline());
                pb.append(Component.text(" ★ ", NamedTextColor.GOLD));
                pb.append(Component.text("Sen: ", NamedTextColor.DARK_GRAY));
                pb.append(Component.text("#" + (yourRank > 0 ? yourRank : "?"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.BOLD, true));
                pb.append(Component.text(" | ", NamedTextColor.DARK_GRAY));

                // Senin ligin - canli al
                String yourLeague = PlaceholderAPI.setPlaceholders(player,
                        plugin.getConfig().getString("league-placeholder", "%alonsoleagues_league_display%"));
                String yourEmoji = getEmojiForLeague(yourLeague, player);
                if (yourEmoji != null && !yourEmoji.isEmpty()) {
                    pb.append(Component.text(yourEmoji + " ").color(NamedTextColor.WHITE));
                }
                pb.append(Component.text(yourElo + " ELO", NamedTextColor.GREEN));
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
     * Lig ismine gore emoji al
     */
    private String getEmojiForLeague(String leagueDisplay, Player player) {
        if (!plugin.getConfig().getBoolean("rank-icons.enabled", true)) {
            return null;
        }
        if (leagueDisplay == null || leagueDisplay.isEmpty()) {
            return null;
        }

        // Renk kodlarini temizle
        String cleanLeague = stripColors(leagueDisplay).toUpperCase().trim();

        ConfigurationSection leaguesSection = plugin.getConfig().getConfigurationSection("rank-icons.leagues");
        if (leaguesSection == null) return null;

        for (String leagueKey : leaguesSection.getKeys(false)) {
            String configLeague = leagueKey.toUpperCase().trim();

            // Eslesme kontrol
            if (cleanLeague.contains(configLeague) || configLeague.contains(cleanLeague)
                    || matchLeague(cleanLeague, configLeague)) {
                
                String placeholder = leaguesSection.getString(leagueKey + ".placeholder", "");
                if (!placeholder.isEmpty()) {
                    String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
                    if (parsed != null && !parsed.equals(placeholder)) {
                        return parsed;
                    }
                }
                return null;
            }
        }

        return null;
    }

    /**
     * Lig isimlerini eslestirir
     * "BRONZ [II]" ile "BRONZ_2" veya "BRONZ2" eslesir
     */
    private boolean matchLeague(String display, String configKey) {
        // Tum ozel karakterleri temizle
        String cleanDisplay = display.replaceAll("[^A-Za-z0-9ÜĞŞÇÖİüğşçöı]", "").toUpperCase();
        String cleanConfig = configKey.replaceAll("[^A-Za-z0-9ÜĞŞÇÖİüğşçöı]", "").toUpperCase();

        // Roman rakamlarini sayiya cevir
        cleanDisplay = romanToNumber(cleanDisplay);
        cleanConfig = romanToNumber(cleanConfig);

        return cleanDisplay.equals(cleanConfig);
    }

    /**
     * Roman rakamlarini sayiya cevirir
     * BRONZII -> BRONZ2, BRONZV -> BRONZ5
     */
    private String romanToNumber(String text) {
        return text
                .replace("VIII", "8")
                .replace("VII", "7")
                .replace("VI", "6")
                .replace("IV", "4")
                .replace("IX", "9")
                .replace("III", "3")
                .replace("II", "2")
                .replace("V", "5")
                .replace("I", "1")
                .replace("X", "10");
    }

    /**
     * Renk kodlarini temizler
     */
    private String stripColors(String text) {
        if (text == null) return "";
        // &l, &7, &f gibi kodlari temizle
        String stripped = text.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        // #RRGGBB hex kodlarini temizle
        stripped = stripped.replaceAll("#[0-9A-Fa-f]{6}", "");
        // § kodlarini temizle
        stripped = stripped.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        return stripped;
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
