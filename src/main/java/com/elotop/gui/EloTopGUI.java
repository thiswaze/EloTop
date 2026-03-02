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

        // Oyuncunun kendi bilgisi
        int yourRank = plugin.getEloManager().getPlayerRank(player.getUniqueId());
        int yourElo = plugin.getEloManager().getPlayerEloLive(player);

        List<Component> pages = new ArrayList<>();

        for (int page = 0; page < totalPages; page++) {
            int startIndex = page * playersPerPage;
            int endIndex = Math.min(startIndex + playersPerPage, leaderboard.size());

            TextComponent.Builder pb = Component.text();

            // ===== BASLIK =====
            pb.append(Component.text("  ✦ ", TextColor.color(0xFF, 0xAA, 0x00)));
            pb.append(Component.text("Elo Sıralaması", NamedTextColor.BLACK)
                    .decoration(TextDecoration.BOLD, true));
            pb.append(Component.text(" ✦", TextColor.color(0xFF, 0xAA, 0x00)));
            pb.append(Component.newline());
            pb.append(Component.newline());

            // ===== OYUNCULAR =====
            for (int i = startIndex; i < endIndex; i++) {
                EloManager.EloEntry entry = leaderboard.get(i);
                int rank = i + 1;

                // Top 3 ozel emoji, digerleri normal numara
                if (rank == 1) {
                    pb.append(Component.text(" ❶ ", TextColor.color(0xFF, 0xD7, 0x00)));
                } else if (rank == 2) {
                    pb.append(Component.text(" ❷ ", TextColor.color(0xC0, 0xC0, 0xC0)));
                } else if (rank == 3) {
                    pb.append(Component.text(" ❸ ", TextColor.color(0xCD, 0x7F, 0x32)));
                } else {
                    pb.append(Component.text(" #" + rank + " ", NamedTextColor.DARK_GRAY));
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
                        .append(Component.text(entry.getElo(), NamedTextColor.GREEN)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("Lig: ", NamedTextColor.GRAY))
                        .append(leagueTag != null && !leagueTag.isEmpty()
                                ? deserializeHex(leagueTag) : Component.text("?", NamedTextColor.GRAY));

                // Oyuncu ismi - siyah
                pb.append(Component.text(entry.getPlayerName(), NamedTextColor.BLACK)
                        .hoverEvent(HoverEvent.showText(hover)));

                // Elo degeri
                pb.append(Component.text(" " + entry.getElo(), TextColor.color(0x55, 0xAA, 0x55)));

                pb.append(Component.newline());
            }

            // ===== SON SAYFADA SEN BILGISI =====
            if (page == totalPages - 1) {
                pb.append(Component.newline());
                pb.append(Component.text(" ★ ", TextColor.color(0xFF, 0xAA, 0x00)));
                pb.append(Component.text("Sen: ", NamedTextColor.DARK_GRAY));
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
