package com.elotop.listener;

import com.elotop.EloTopPlugin;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class GUIClickListener implements Listener {

    private final EloTopPlugin plugin;

    public GUIClickListener(EloTopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());
        
        if (!title.contains("ELO TOP")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int currentPage = plugin.getEloTopGUI().getPlayerPage(player.getUniqueId());
        int slot = event.getRawSlot();

        // Sonraki sayfa - slot 53
        if (slot == 53 && clicked.getType() == Material.ARROW) {
            plugin.getEloTopGUI().openGUI(player, currentPage + 1);
            return;
        }

        // Önceki sayfa - slot 45
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            plugin.getEloTopGUI().openGUI(player, currentPage - 1);
            return;
        }

        // Kapat - slot 47
        if (slot == 47 && clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());
        if (title.contains("ELO TOP")) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());
        if (title.contains("ELO TOP")) {
            plugin.getEloTopGUI().removePlayer(player.getUniqueId());
        }
    }
}
