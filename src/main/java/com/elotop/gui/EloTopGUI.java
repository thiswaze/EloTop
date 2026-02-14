package com.elotop.gui;

import com.elotop.EloTopPlugin;
import com.elotop.manager.EloManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class EloTopGUI {

    private final EloTopPlugin plugin;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public EloTopGUI(EloTopPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player, int page) {
        List<EloManager.EloEntry> leaderboard = plugin.getEloManager().getLeaderboard();

        if (leaderboard.isEmpty()) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6EloTop&8] ");
            String msg = plugin.getConfig().getString("messages.no-data", "&cVeri yok!");
            player.sendMessage(colorize(prefix + msg));
            return;
        }

        // 6 satir = 54 slot
        int guiSize = 54;

        // Oyuncu slotlari - ortada duzgun yerlesim
        // Ust satir: dekorasyon
        // 2-5. satirlar: oyuncular (28 slot kullanilabilir)
        // Alt satir: navigasyon
        int[] playerSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };

        int playersPerPage = playerSlots.length; // 28
        int totalPages = (int) Math.ceil((double) leaderboard.size() / playersPerPage);

        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        playerPages.put(player.getUniqueId(), page);

        // Baslik
        String title = "&8&l┃ &6&lElo Sıralaması &8(&e" + page + "&7/&e" + totalPages + "&8)";
        Inventory gui = Bukkit.createInventory(null, guiSize, colorize(title));

        // === TUM SLOTLARI SIYAH CAM PANEL ILE DOLDUR ===
        ItemStack blackPane = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < guiSize; i++) {
            gui.setItem(i, blackPane);
        }

        // === UST DEKORASYON ===
        // Ust satir ortasina sari cam panel
        ItemStack yellowPane = createPane(Material.YELLOW_STAINED_GLASS_PANE, " ");
        ItemStack orangePane = createPane(Material.ORANGE_STAINED_GLASS_PANE, " ");
        gui.setItem(3, yellowPane);
        gui.setItem(4, orangePane);
        gui.setItem(5, yellowPane);

        // Ust ortaya bilgi itemi
        int yourElo = plugin.getEloManager().getPlayerEloLive(player);
        int yourRank = plugin.getEloManager().getPlayerRank(player.getUniqueId());
        String yourRankStr = yourRank > 0 ? "#" + yourRank : "Sıralamada Yok";

        ItemStack infoItem = createItem(Material.NETHER_STAR,
                "&6&l⭐ ELO SIRALAMA &6&l⭐",
                Arrays.asList(
                        "",
                        "&7Toplam &e" + plugin.getEloManager().getTotalPlayers() + " &7oyuncu",
                        "&7Sayfa: &e" + page + "&7/&e" + totalPages,
                        "",
                        "&6Senin Elon: &f&l" + yourElo,
                        "&6Sıralaman: &f&l" + yourRankStr,
                        ""
                ));
        gui.setItem(4, infoItem);

        // === OYUNCULARI YERLESTIR ===
        int startIndex = (page - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, leaderboard.size());

        for (int i = startIndex; i < endIndex; i++) {
            EloManager.EloEntry entry = leaderboard.get(i);
            int rank = i + 1;
            int slotIndex = i - startIndex;

            if (slotIndex >= playerSlots.length) break;

            ItemStack playerItem = createRankedPlayerItem(entry, rank);
            gui.setItem(playerSlots[slotIndex], playerItem);
        }

        // === KENAR DEKORASYONU ===
        // Sol ve sag kenarlara gri cam panel
        ItemStack grayPane = createPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        int[] sideSlots = {0, 1, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44};
        for (int s : sideSlots) {
            gui.setItem(s, grayPane);
        }

        // Ust satir
        gui.setItem(2, grayPane);
        gui.setItem(6, grayPane);

        // === ALT NAVIGASYON SATIRI ===
        // Alt satir dekorasyon
        ItemStack redPane = createPane(Material.RED_STAINED_GLASS_PANE, " ");
        ItemStack limePane = createPane(Material.LIME_STAINED_GLASS_PANE, " ");

        for (int i = 45; i < 54; i++) {
            gui.setItem(i, blackPane);
        }

        // Onceki sayfa (sol)
        if (page > 1) {
            ItemStack prevItem = createItem(Material.ARROW,
                    "&c&l◀ Önceki Sayfa",
                    Arrays.asList(
                            "",
                            "&7Sayfa &e" + (page - 1) + " &7e dön",
                            "&eTıkla!"
                    ));
            gui.setItem(45, redPane);
            gui.setItem(46, prevItem);
            gui.setItem(47, redPane);
        }

        // Kapat butonu (orta)
        ItemStack closeItem = createItem(Material.BARRIER,
                "&c&lKapat",
                Arrays.asList("", "&7Menüyü kapatmak için tıkla"));
        gui.setItem(49, closeItem);

        // Senin bilgilerin (orta sol)
        ItemStack yourInfo = createPlayerHead(player,
                "&e&lSenin Bilgilerin",
                Arrays.asList(
                        "",
                        "&7İsim: &f" + player.getName(),
                        "&7Elo: &a&l" + yourElo,
                        "&7Sıralama: &e" + yourRankStr,
                        ""
                ));
        gui.setItem(48, yourInfo);

        // Sayfa bilgisi (orta sag)
        ItemStack pageInfo = createItem(Material.BOOK,
                "&e&lSayfa Bilgisi",
                Arrays.asList(
                        "",
                        "&7Mevcut Sayfa: &e" + page,
                        "&7Toplam Sayfa: &e" + totalPages,
                        "&7Toplam Oyuncu: &e" + plugin.getEloManager().getTotalPlayers(),
                        ""
                ));
        gui.setItem(50, pageInfo);

        // Sonraki sayfa (sag)
        if (page < totalPages) {
            ItemStack nextItem = createItem(Material.ARROW,
                    "&a&lSonraki Sayfa ▶",
                    Arrays.asList(
                            "",
                            "&7Sayfa &e" + (page + 1) + " &7e geç",
                            "&eTıkla!"
                    ));
            gui.setItem(51, limePane);
            gui.setItem(52, nextItem);
            gui.setItem(53, limePane);
        }

        player.openInventory(gui);
    }

    private ItemStack createRankedPlayerItem(EloManager.EloEntry entry, int rank) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getUuid()));

            // Rank'e gore isim rengi
            String rankColor;
            String rankSymbol;
            switch (rank) {
                case 1:
                    rankColor = "&6&l";
                    rankSymbol = "👑 ";
                    break;
                case 2:
                    rankColor = "&f&l";
                    rankSymbol = "🥈 ";
                    break;
                case 3:
                    rankColor = "&c&l";
                    rankSymbol = "🥉 ";
                    break;
                default:
                    rankColor = "&e";
                    rankSymbol = "";
                    break;
            }

            String displayName = rankColor + rankSymbol + "#" + rank + " " + entry.getPlayerName();
            meta.displayName(colorize(displayName));

            // Lore
            List<Component> lore = new ArrayList<>();
            lore.add(colorize(""));

            // Rank'e gore ozel lore
            if (rank == 1) {
                lore.add(colorize("&8▪ &6&lBİRİNCİ &8▪"));
            } else if (rank == 2) {
                lore.add(colorize("&8▪ &f&lİKİNCİ &8▪"));
            } else if (rank == 3) {
                lore.add(colorize("&8▪ &c&lÜÇÜNCÜ &8▪"));
            }

            lore.add(colorize(""));
            lore.add(colorize("&7Sıralama: " + rankColor + "#" + rank));
            lore.add(colorize("&7Elo: &a&l" + entry.getElo()));
            lore.add(colorize(""));

            if (rank <= 3) {
                lore.add(colorize("&6⭐ Top 3 Oyuncu! ⭐"));
                lore.add(colorize(""));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createPlayerHead(Player player, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(colorize(name));
            if (lore != null) {
                List<Component> cl = new ArrayList<>();
                for (String line : lore) cl.add(colorize(line));
                meta.lore(cl);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(colorize(name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> cl = new ArrayList<>();
                for (String line : lore) cl.add(colorize(line));
                meta.lore(cl);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static Component colorize(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public int getPlayerPage(UUID uuid) {
        return playerPages.getOrDefault(uuid, 1);
    }

    public void removePlayer(UUID uuid) {
        playerPages.remove(uuid);
    }
}
