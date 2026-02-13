package com.elotop.listener;

import com.elotop.EloTopPlugin;
import com.elotop.gui.EloTopGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class JoinListener implements Listener {

    private final EloTopPlugin plugin;

    public JoinListener(EloTopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getEloManager().updatePlayer(player);
            giveEloPaper(player);
        }, 20L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            giveEloPaper(player);
        }, 5L);
    }

    public void giveEloPaper(Player player) {
        if (!plugin.getConfig().getBoolean("auto-give-paper", true)) return;

        int slot = plugin.getConfig().getInt("paper-slot", 8);
        ItemStack paper = createEloPaper();

        ItemStack currentItem = player.getInventory().getItem(slot);
        if (currentItem != null && isEloPaper(currentItem)) return;

        if (currentItem != null && !currentItem.getType().isAir()) {
            int emptySlot = player.getInventory().firstEmpty();
            if (emptySlot != -1) {
                player.getInventory().setItem(emptySlot, currentItem);
                player.getInventory().setItem(slot, paper);
            }
            return;
        }

        player.getInventory().setItem(slot, paper);
    }

    public static ItemStack createEloPaper() {
        EloTopPlugin plugin = EloTopPlugin.getInstance();
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString("paper.name", "&6&l* &e&lELO SIRALAMA &6&l*");
            meta.displayName(EloTopGUI.colorize(name));
            List<String> loreConfig = plugin.getConfig().getStringList("paper.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : loreConfig) lore.add(EloTopGUI.colorize(line));
            meta.lore(lore);
            meta.setCustomModelData(91723);
            paper.setItemMeta(meta);
        }
        return paper;
    }

    public static boolean isEloPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasCustomModelData() && meta.getCustomModelData() == 91723;
    }
}
