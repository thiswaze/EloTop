package com.elotop;

import com.elotop.command.EloTopCommand;
import com.elotop.gui.EloTopGUI;
import com.elotop.listener.GUIClickListener;
import com.elotop.listener.JoinListener;
import com.elotop.listener.PaperClickListener;
import com.elotop.manager.EloManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EloTopPlugin extends JavaPlugin {

    private static EloTopPlugin instance;
    private EloManager eloManager;
    private EloTopGUI eloTopGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI bulunamadi! Plugin kapaniyor...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        eloManager = new EloManager(this);
        eloTopGUI = new EloTopGUI(this);

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PaperClickListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIClickListener(this), this);

        EloTopCommand command = new EloTopCommand(this);
        getCommand("elotop").setExecutor(command);
        getCommand("elotop").setTabCompleter(command);
        getCommand("elotopreload").setExecutor(command);

        startCacheTask();
        getLogger().info("EloTop plugin aktif!");
    }

    @Override
    public void onDisable() {
        if (eloManager != null) eloManager.shutdown();
    }

    private void startCacheTask() {
        long interval = getConfig().getLong("cache-update-interval", 60) * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            getServer().getScheduler().runTask(this, () -> eloManager.updateCache());
        }, 100L, interval);
    }

    public void reload() {
        reloadConfig();
        eloManager.clearCache();
        getServer().getScheduler().runTask(this, () -> eloManager.updateCache());
    }

    public static EloTopPlugin getInstance() { return instance; }
    public EloManager getEloManager() { return eloManager; }
    public EloTopGUI getEloTopGUI() { return eloTopGUI; }
}
