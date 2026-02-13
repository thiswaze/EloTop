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

        int rows = plugin.getConfig().getInt("gui.rows", 6);
        int guiSize = rows * 9;
        int maxPlayerSlots = (rows - 1) * 9;
        int totalPages = (int) Math.ceil((double) leaderboard.size() / maxPlayerSlots);

        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        playerPages.put(player.getUniqueId(), page);

        String titleFormat = plugin.getConfig().getString("gui.title",
                "&8&l| &6&lElo Siralamasi &8&l- &e&lSayfa {page}/{maxpage}");
        String title = titleFormat
                .replace("{page}", String.valueOf(page))
                .replace("{maxpage}", String.valueOf(totalPages));

        Inventory gui = Bukkit.createInventory(null, guiSize, colorize(title));

        int startIndex = (page - 1) * maxPlayerSlots;
        int endIndex = Math.min(startIndex + maxPlayerSlots, leaderboard.size());
        boolean useHeads = plugin.getConfig().getBoolean("gui.use-player-heads", true);

        for (int i = startIndex; i < endIndex; i++) {
            EloManager.EloEntry entry = leaderboard.get(i);
            int rank = i + 1;
            int slot = i - startIndex;
            if (slot >= maxPlayerSlots) break;
            gui.setItem(slot, createPlayerItem(entry, rank, useHeads));
        }

        if (plugin.getConfig().getBoolean("gui.filler.enabled", true)) {
            String fillerMat = plugin.getConfig().getString("gui.filler.material", "BLACK_STAINED_GLASS_PANE");
            ItemStack filler = createItem(Material.valueOf(fillerMat), " ", null);
            int bottomRowStart = (rows - 1) * 9;
            for (int s = bottomRowStart; s < guiSize; s++) {
                gui.setItem(s, filler);
            }
        }

        if (page > 1) {
            int prevSlot = plugin.getConfig().getInt("gui.previous-page.slot", 45);
            String prevName = plugin.getConfig().getString("gui.previous-page.name", "&c&l<< Onceki Sayfa");
            gui.setItem(prevSlot, createItem(Material.ARROW, prevName, null));
        }

        if (page < totalPages) {
            int nextSlot = plugin.getConfig().getInt("gui.next-page.slot", 53);
            String nextName = plugin.getConfig().getString("gui.next-page.name", "&a&lSonraki Sayfa >>");
            gui.setItem(nextSlot, createItem(Material.ARROW, nextName, null));
        }

        if (plugin.getConfig().getBoolean("gui.info.enabled", true)) {
            int infoSlot = plugin.getConfig().getInt("gui.info.slot", 49);
            int yourElo = plugin.getEloManager().getPlayerEloLive(player);
            int yourRank = plugin.getEloManager().getPlayerRank(player.getUniqueId());
            String yourRankStr = yourRank > 0 ? String.valueOf(yourRank) : "Yok";

            List<String> infoLore = plugin.getConfig().getStringList("gui.info.lore");
            List<String> formatted = new ArrayList<>();
            int fp = page, ftp = totalPages;
            for (String line : infoLore) {
                formatted.add(line
                        .replace("{total}", String.valueOf(plugin.getEloManager().getTotalPlayers()))
                        .replace("{page}", String.valueOf(fp))
                        .replace("{maxpage}", String.valueOf(ftp))
                        .replace("{yourelo}", String.valueOf(yourElo))
                        .replace("{yourrank}", yourRankStr));
            }
            gui.setItem(infoSlot, createItem(Material.BOOK, "&6&lBilgi", formatted));
        }

        if (plugin.getConfig().getBoolean("gui.close.enabled", true)) {
            int closeSlot = plugin.getConfig().getInt("gui.close.slot", 48);
            gui.setItem(closeSlot, createItem(Material.BARRIER, "&c&lKapat", null));
        }

        player.openInventory(gui);
    }

    private ItemStack createPlayerItem(EloManager.EloEntry entry, int rank, boolean useHead) {
        String topColor = "";
        if (rank <= 3) topColor = plugin.getConfig().getString("top-colors." + rank, "&e");

        String nameFormat = rank <= 3
                ? topColor + "#{rank} {player}"
                : plugin.getConfig().getString("gui.player-item.name", "&e#{rank} &6&l{player}");

        String name = nameFormat
                .replace("{rank}", String.valueOf(rank))
                .replace("{player}", entry.getPlayerName())
                .replace("{elo}", String.valueOf(entry.getElo()));

        List<String> loreFormat = plugin.getConfig().getStringList("gui.player-item.lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreFormat) {
            lore.add(line
                    .replace("{rank}", String.valueOf(rank))
                    .replace("{player}", entry.getPlayerName())
                    .replace("{elo}", String.valueOf(entry.getElo())));
        }

        if (useHead) {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getUuid()));
                meta.displayName(colorize(name));
                List<Component> cl = new ArrayList<>();
                for (String l : lore) cl.add(colorize(l));
                meta.lore(cl);
                item.setItemMeta(meta);
            }
            return item;
        } else {
            return createItem(Material.PAPER, name, lore);
        }
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
