package com.elotop.gui;

import com.elotop.EloTopPlugin;
import com.elotop.manager.EloManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

        plugin.getEloManager().updatePlayer(player);

        int guiSize = 54; // 6 satır
        int playersPerPage = 21; // 3 satır x 7 oyuncu

        int totalPages = (int) Math.ceil((double) leaderboard.size() / playersPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        playerPages.put(player.getUniqueId(), page);

        // GUI başlığı - ItemsAdder emoji kullan
        String title = "§8§l┃ §6§l:gold_rank: §e§lELO TOP §6§l:gold_rank: §8§l┃ §7" + page + "/" + totalPages;
        Inventory gui = Bukkit.createInventory(null, guiSize, colorize(title));

        // Üst süsleme (sarı-turuncu gradient)
        ItemStack topDecor = createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, "§6✦");
        ItemStack topDecor2 = createGlassPane(Material.YELLOW_STAINED_GLASS_PANE, "§e✦");
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, i % 2 == 0 ? topDecor : topDecor2);
        }

        // Alt süsleme
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, i % 2 == 0 ? topDecor : topDecor2);
        }

        // Yan süslemeler
        ItemStack sideDecor = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        int[] sideSlots = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : sideSlots) {
            gui.setItem(slot, sideDecor);
        }

        // Oyuncu slotları (3 satır x 7 oyuncu)
        int[] playerSlots = {
            10, 11, 12, 13, 14, 15, 16,  // 1. satır
            19, 20, 21, 22, 23, 24, 25,  // 2. satır
            28, 29, 30, 31, 32, 33, 34   // 3. satır
        };

        int startIndex = (page - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, leaderboard.size());

        for (int i = startIndex; i < endIndex; i++) {
            EloManager.EloEntry entry = leaderboard.get(i);
            int rank = i + 1;
            int slotIndex = i - startIndex;

            if (slotIndex >= playerSlots.length) break;

            ItemStack skull = createPlayerHead(entry, rank);
            gui.setItem(playerSlots[slotIndex], skull);
        }

        // Boş slotları doldur
        ItemStack emptySlot = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot : playerSlots) {
            if (gui.getItem(slot) == null) {
                gui.setItem(slot, emptySlot);
            }
        }

        // Alt navigasyon satırı
        int yourRank = plugin.getEloManager().getPlayerRank(player.getUniqueId());
        int yourElo = plugin.getEloManager().getPlayerEloLive(player);

        // Önceki sayfa butonu
        if (page > 1) {
            ItemStack prevButton = createNavButton(Material.ARROW, 
                "§c§l◀ ÖNCEKİ SAYFA",
                Arrays.asList(
                    "",
                    "§7Sayfa: §e" + (page - 1) + "§7/§e" + totalPages,
                    "",
                    "§aTıkla!"
                ));
            gui.setItem(45, prevButton);
        }

        // Sonraki sayfa butonu
        if (page < totalPages) {
            ItemStack nextButton = createNavButton(Material.ARROW,
                "§a§lSONRAKİ SAYFA ▶",
                Arrays.asList(
                    "",
                    "§7Sayfa: §e" + (page + 1) + "§7/§e" + totalPages,
                    "",
                    "§aTıkla!"
                ));
            gui.setItem(53, nextButton);
        }

        // Ortada oyuncunun kendi bilgisi
        ItemStack selfInfo = createSelfInfoHead(player, yourRank, yourElo, leaderboard.size());
        gui.setItem(49, selfInfo);

        // Kapat butonu
        ItemStack closeButton = createNavButton(Material.BARRIER,
            "§c§l✖ KAPAT",
            Arrays.asList(
                "",
                "§7Menüyü kapatmak için tıkla"
            ));
        gui.setItem(47, closeButton);

        // Bilgi butonu
        ItemStack infoButton = createNavButton(Material.BOOK,
            "§6§l📊 BİLGİ",
            Arrays.asList(
                "",
                "§7Toplam Oyuncu: §e" + leaderboard.size(),
                "§7Sayfa: §e" + page + "§7/§e" + totalPages,
                "",
                "§7Senin Sıran: §e#" + (yourRank > 0 ? yourRank : "?"),
                "§7Senin Elon: §a" + yourElo
            ));
        gui.setItem(51, infoButton);

        player.openInventory(gui);
    }

    private ItemStack createPlayerHead(EloManager.EloEntry entry, int rank) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getUuid()));

            // Rank'e göre isim rengi ve IA emoji
            String rankEmoji = getRankEmoji(rank);
            String rankColor = getRankColor(rank);
            String displayName = rankColor + "§l#" + rank + " " + rankEmoji + " §f" + entry.getPlayerName();

            meta.displayName(colorize(displayName));

            // Lore
            List<Component> lore = new ArrayList<>();
            lore.add(colorize(""));
            lore.add(colorize("§8▪ §7Sıralama: " + rankColor + "#" + rank));
            lore.add(colorize("§8▪ §7Elo: §a§l" + entry.getElo()));
            
            String leagueTag = entry.getLeagueTag();
            if (leagueTag != null && !leagueTag.isEmpty()) {
                lore.add(colorize("§8▪ §7Lig: " + leagueTag));
            }
            
            lore.add(colorize(""));
            
            // Top 3 için özel yazı
            if (rank == 1) {
                lore.add(colorize("§6§l⭐ BİRİNCİ ⭐"));
            } else if (rank == 2) {
                lore.add(colorize("§f§l🥈 İKİNCİ 🥈"));
            } else if (rank == 3) {
                lore.add(colorize("§c§l🥉 ÜÇÜNCÜ 🥉"));
            }

            meta.lore(lore);
            skull.setItemMeta(meta);
        }

        return skull;
    }

    private ItemStack createSelfInfoHead(Player player, int yourRank, int yourElo, int totalPlayers) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(player);
            
            String rankEmoji = getRankEmojiByElo(yourElo);
            meta.displayName(colorize("§e§l⭐ SENİN BİLGİLERİN ⭐"));

            List<Component> lore = new ArrayList<>();
            lore.add(colorize(""));
            lore.add(colorize("§8▪ §7İsim: §f" + player.getName()));
            lore.add(colorize("§8▪ §7Sıralama: §e#" + (yourRank > 0 ? yourRank : "?")));
            lore.add(colorize("§8▪ §7Elo: §a§l" + yourElo));
            lore.add(colorize("§8▪ §7Rank: " + rankEmoji));
            lore.add(colorize(""));
            lore.add(colorize("§7Top §e" + totalPlayers + " §7oyuncu arasındasın!"));
            lore.add(colorize(""));

            meta.lore(lore);
            skull.setItemMeta(meta);
        }

        return skull;
    }

    private String getRankEmoji(int rank) {
        if (rank == 1) return ":gold_rank:";
        if (rank == 2) return ":iron_rank:";
        if (rank == 3) return ":copper_rank:";
        if (rank <= 10) return ":lapis_rank:";
        if (rank <= 25) return ":emerald_rank:";
        if (rank <= 50) return ":diamond_rank:";
        return ":amethyst_rank:";
    }

    private String getRankEmojiByElo(int elo) {
        if (elo >= 2800) return ":gold_rank:";
        if (elo >= 1001) return ":iron_rank:";
        return ":copper_rank:";
    }

    private String getRankColor(int rank) {
        if (rank == 1) return "§6"; // Altın
        if (rank == 2) return "§f"; // Beyaz (gümüş)
        if (rank == 3) return "§c"; // Kırmızı (bronz)
        if (rank <= 10) return "§e"; // Sarı
        if (rank <= 25) return "§a"; // Yeşil
        if (rank <= 50) return "§b"; // Açık mavi
        return "§7"; // Gri
    }

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(colorize(name));
            List<Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(colorize(line));
            }
            meta.lore(componentLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static Component colorize(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacySection().deserialize(
            text.replace("&", "§")
        );
    }

    public int getPlayerPage(UUID uuid) {
        return playerPages.getOrDefault(uuid, 1);
    }

    public void removePlayer(UUID uuid) {
        playerPages.remove(uuid);
    }

    // Eski kitap metodu - artık kullanılmıyor ama uyumluluk için kalıyor
    public void openBook(Player player) {
        openGUI(player, 1);
    }
}
