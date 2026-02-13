package com.elotop.command;

import com.elotop.EloTopPlugin;
import com.elotop.gui.EloTopGUI;
import com.elotop.listener.JoinListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EloTopCommand implements CommandExecutor, TabCompleter {

    private final EloTopPlugin plugin;

    public EloTopCommand(EloTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6EloTop&8] ");

        if (command.getName().equalsIgnoreCase("elotopreload")) {
            if (!sender.hasPermission("elotop.admin")) {
                sender.sendMessage(EloTopGUI.colorize(prefix + "&cYetkin yok!"));
                return true;
            }
            plugin.reload();
            sender.sendMessage(EloTopGUI.colorize(prefix + "&aConfig yeniden yuklendi!"));
            if (sender instanceof Player) {
                JoinListener listener = new JoinListener(plugin);
                plugin.getServer().getOnlinePlayers().forEach(listener::giveEloPaper);
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("elotop")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(EloTopGUI.colorize(prefix + "&cSadece oyuncular kullanabilir!"));
                return true;
            }
            if (!player.hasPermission("elotop.use")) {
                player.sendMessage(EloTopGUI.colorize(prefix + "&cYetkin yok!"));
                return true;
            }

            plugin.getEloManager().updatePlayer(player);

            int page = 1;
            if (args.length > 0) {
                try { page = Integer.parseInt(args[0]); }
                catch (NumberFormatException e) { page = 1; }
            }

            plugin.getEloTopGUI().openGUI(player, page);
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
            @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("elotop") && args.length == 1) {
            suggestions.add("1");
            suggestions.add("2");
            suggestions.add("3");
        }
        return suggestions;
    }
}
