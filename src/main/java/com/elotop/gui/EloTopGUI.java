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

                // Rank emojisi - PlaceholderAPI ile al (BEYAZ RENK)
                String emoji = getEmojiForElo(elo, player);
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

                String yourEmoji = getEmojiForElo(yourElo, player);
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
     * Elo'ya gore emoji al - PlaceholderAPI ile
     */
    private String getEmojiForElo(int elo, Player player) {
        if (!plugin.getConfig().getBoolean("rank-icons.enabled", true)) {
            return null;
        }

        ConfigurationSection ranksSection = plugin.getConfig().getConfigurationSection("rank-icons.ranks");
        if (ranksSection == null) return null;

        for (String rankKey : ranksSection.getKeys(false)) {
            int minElo = ranksSection.getInt(rankKey + ".min-elo", 0);
            int maxElo = ranksSection.getInt(rankKey + ".max-elo", 999999);

            if (elo >= minElo && elo <= maxElo) {
                String emojiPlaceholder = ranksSection.getString(rankKey + ".placeholder", "");
                
                if (emojiPlaceholder.isEmpty()) {
                    // Fallback - emoji name'den placeholder olustur
                    String emojiName = ranksSection.getString(rankKey + ".emoji", "").replace(":", "");
                    emojiPlaceholder = "%img_" + emojiName + "%";
                }

                // PlaceholderAPI ile parse et
                String parsed = PlaceholderAPI.setPlaceholders(player, emojiPlaceholder);
                
                // Eger placeholder parse edilmisse (degismisse), dondur
                if (parsed != null && !parsed.equals(emojiPlaceholder)) {
                    return parsed;
                }
                
                return null;
            }
        }

        return null;
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
