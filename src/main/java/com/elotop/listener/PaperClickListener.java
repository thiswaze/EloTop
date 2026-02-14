package com.elotop.listener;

import com.elotop.EloTopPlugin;
import com.elotop.gui.EloTopGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PaperClickListener implements Listener {

    private final EloTopPlugin plugin;

    public PaperClickListener(EloTopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (item == null || !JoinListener.isEloPaper(item)) return;

        event.setCancelled(true);

        if (!player.hasPermission("elotop.use")) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6EloTop&8] ");
            String msg = plugin.getConfig().getString("messages.no-permission", "&cYetkin yok!");
            player.sendMessage(EloTopGUI.colorize(prefix + msg));
            return;
        }

        plugin.getEloManager().updatePlayer(player);
        plugin.getEloTopGUI().openBook(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (JoinListener.isEloPaper(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
