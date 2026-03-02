package com.elotop.command;

import com.elotop.EloTopPlugin;
import com.elotop.listener.JoinListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EloTopCommand implements CommandExecutor {

    private final EloTopPlugin plugin;

    public EloTopCommand(EloTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("elotopreload")) {
            if (!sender.hasPermission("elotop.admin")) return true;
            plugin.reload();
            sender.sendMessage("§aEloTop reload basarili!");
            return true;
        }

        if (sender instanceof Player player) {
            if (!player.hasPermission("elotop.use")) return true;
            plugin.getEloManager().updatePlayer(player);
            plugin.getEloTopGUI().openBook(player);
        }
        return true;
    }
}
