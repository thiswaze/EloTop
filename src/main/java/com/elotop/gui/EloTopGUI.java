package com.elotop.gui;

import com.elotop.EloTopPlugin;
import com.elotop.manager.EloManager;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
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

        // Oyuncunun kendi verisini guncelle
        plugin.getEloManager().updatePlayer(player);

        int playersPerPage = 10;
        int totalPages = (int) Math.ceil((double) leaderboard.size() / playersPerPage);

        List<Component> pages = new ArrayList<>();

        for (int page = 0; page < totalPages; page++) {
            int startIndex = page * playersPerPage;
            int endIndex = Math.min(startIndex + playersPerPage, leaderboard.size());

            TextComponent.Builder pageBuilder = Component.text();

            // Sayfa basligi
            pageBuilder.append(Component.text("     ", NamedTextColor.BLACK));
            pageBuilder.append(Component.text("⭐ ", TextColor.color(0xFF, 0xD7, 0x00)));
            pageBuilder.append(Component.text("ELO SIRALAMA", TextColor.color(0xFF, 0xAA, 0x00))
                    .decoration(TextDecoration.BOLD, true));
            pageBuilder.append(Component.text(" ⭐", TextColor.color(0xFF, 0xD7, 0x00)));
            pageBuilder.append(Component.newline());

            // Sayfa numarasi
            pageBuilder.append(Component.text("        ", NamedTextColor.BLACK));
            pageBuilder.append(Component.text("Sayfa " + (page + 1) + "/" + totalPages,
                    NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, true));
            pageBuilder.append(Component.newline());

            // Cizgi
            pageBuilder.append(Component.text("  ─────────────", NamedTextColor.DARK_GRAY));
            pageBuilder.append(Component.newline());

            // Oyuncular
            for (int i = startIndex; i < endIndex; i++) {
                EloManager.EloEntry entry = leaderboard.get(i);
                int rank = i + 1;

                // Rank rengi
                TextColor rankColor = getRankNumberColor(rank);

                // Siralama numarasi
                pageBuilder.append(Component.text(" " + rank + ". ", rankColor)
                        .decoration(TextDecoration.BOLD, true));

                // League tag
                String leagueTag = plugin.getEloManager().getPlayerLeague(entry.getUuid());
                if (leagueTag != null && !leagueTag.isEmpty()) {
                    pageBuilder.append(deserializeHex(leagueTag));
                    pageBuilder.append(Component.text(" ", NamedTextColor.WHITE));
                }

                // Oyuncu ismi
                TextColor nameColor = getNameColor(rank);
                Component nameComponent = Component.text(entry.getPlayerName(), nameColor)
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Elo: ", NamedTextColor.GRAY)
                                        .append(Component.text(entry.getElo(), NamedTextColor.GREEN)
                                                .decoration(TextDecoration.BOLD, true))
                                        .append(Component.newline())
                                        .append(Component.text("Siralama: ", NamedTextColor.GRAY))
                                        .append(Component.text("#" + rank, NamedTextColor.YELLOW))
                        ));
                pageBuilder.append(nameComponent);

                // Elo
                pageBuilder.append(Component.text(" ", NamedTextColor.WHITE));
                pageBuilder.append(Component.text("[", NamedTextColor.DARK_GRAY));
                pageBuilder.append(Component.text(String.valueOf(entry.getElo()), NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true));
                pageBuilder.append(Component.text("]", NamedTextColor.DARK_GRAY));

                pageBuilder.append(Component.newline());
            }

            // Alt bilgi - oyuncunun kendi sirasi
            int yourRank = plugin.getEloManager().getPlayerRank(player.getUniqueId());
            int yourElo = plugin.getEloManager().getPlayerEloLive(player);

            pageBuilder.append(Component.text("  ─────────────", NamedTextColor.DARK_GRAY));
            pageBuilder.append(Component.newline());
            pageBuilder.append(Component.text(" Sen: ", NamedTextColor.GRAY));
            pageBuilder.append(Component.text("#" + (yourRank > 0 ? yourRank : "?"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.BOLD, true));
            pageBuilder.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
            pageBuilder.append(Component.text("Elo: ", NamedTextColor.GRAY));
            pageBuilder.append(Component.text(String.valueOf(yourElo), NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true));

            pages.add(pageBuilder.build());
        }

        // Kitabi olustur ve ac
        Book book = Book.book(
                Component.text("Elo Sıralama"),
                Component.text("Server"),
                pages
        );

        player.openBook(book);
    }

    private TextColor getRankNumberColor(int rank) {
        if (rank == 1) return TextColor.color(0xFF, 0xD7, 0x00); // Altin
        if (rank == 2) return TextColor.color(0xC0, 0xC0, 0xC0); // Gumus
        if (rank == 3) return TextColor.color(0xCD, 0x7F, 0x32); // Bronz
        if (rank <= 10) return TextColor.color(0xFF, 0xAA, 0x00); // Turuncu
        return NamedTextColor.DARK_GRAY;
    }

    private TextColor getNameColor(int rank) {
        if (rank == 1) return TextColor.color(0xFF, 0xD7, 0x00);
        if (rank == 2) return TextColor.color(0xC0, 0xC0, 0xC0);
        if (rank == 3) return TextColor.color(0xCD, 0x7F, 0x32);
        return NamedTextColor.BLACK;
    }

    /**
     * Hex renk kodlarini iceren stringleri Component'e cevirir
     * Ornek: #CB7E31&lBRONZ &7&l[&f&lV&7&l]
     */
    public static Component deserializeHex(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // # ile baslayan hex kodlarini & formatina cevir
        // #RRGGBB -> &#RRGGBB (LegacyComponentSerializer icin)
        StringBuilder sb = new StringBuilder();
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '#' && i + 6 < chars.length) {
                // Hex kod kontrolu
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

    // Artik GUI kullanmiyoruz ama uyumluluk icin
    public int getPlayerPage(UUID uuid) { return 1; }
    public void removePlayer(UUID uuid) { }
}
