package com.elotop.gui;

import com.elotop.EloTopPlugin;
import com.elotop.manager.EloManager;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.*;

public class EloTopGUI {

    private final EloTopPlugin plugin;
    private final Map<String, String> emojiCache = new HashMap<>();
    private boolean cacheLoaded = false;

    public EloTopGUI(EloTopPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskLater(plugin, this::loadEmojiCache, 400L);
    }

    public void loadEmojiCache() {
        emojiCache.clear();
        plugin.getLogger().info("===========================================");
        plugin.getLogger().info("ItemsAdder emoji yuklemesi basliyor...");
        plugin.getLogger().info("===========================================");

        if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") == null) {
            plugin.getLogger().severe("ItemsAdder YUKLENMEMIS!");
            return;
        }

        // Hangi class'lar mevcut kontrol et
        checkAvailableClasses();

        String namespace = plugin.getConfig().getString("rank-icons.namespace", "rankicons");
        ConfigurationSection ranksSection = plugin.getConfig().getConfigurationSection("rank-icons.ranks");

        if (ranksSection == null) {
            plugin.getLogger().warning("Config'de rank-icons.ranks bulunamadi!");
            return;
        }

        // Sadece rank1 ile test et
        String testEmoji = ":rank1:";
        String testName = "rank1";
        
        plugin.getLogger().info("TEST: " + testEmoji + " yukleniyor...");
        plugin.getLogger().info("Namespace: " + namespace);
        plugin.getLogger().info("Name: " + testName);
        plugin.getLogger().info("Full: " + namespace + ":" + testName);

        String character = null;

        // YONTEM 1
        plugin.getLogger().info("--- YONTEM 1: FontImageWrapper(namespace:name) ---");
        character = tryFontImageWrapper(namespace + ":" + testName, true);
        if (character != null) {
            plugin.getLogger().info("BASARILI! Karakter: " + character);
            emojiCache.put(testEmoji, character);
        }

        // YONTEM 2
        if (character == null) {
            plugin.getLogger().info("--- YONTEM 2: FontImageWrapper(name) ---");
            character = tryFontImageWrapper(testName, true);
            if (character != null) {
                plugin.getLogger().info("BASARILI! Karakter: " + character);
                emojiCache.put(testEmoji, character);
            }
        }

        // YONTEM 3
        if (character == null) {
            plugin.getLogger().info("--- YONTEM 3: FontImageWrapper constructor parametreleri ---");
            character = tryFontImageWrapperAlt(namespace, testName);
            if (character != null) {
                plugin.getLogger().info("BASARILI! Karakter: " + character);
                emojiCache.put(testEmoji, character);
            }
        }

        // YONTEM 4 - Tum emojileri yukle
        if (character != null) {
            plugin.getLogger().info("=== ILK TEST BASARILI! Diger emojiler yukleniyor... ===");
            loadAllEmojis(ranksSection, namespace);
        } else {
            plugin.getLogger().severe("=== HICBIR YONTEM CALISMADI ===");
        }

        plugin.getLogger().info("===========================================");
        plugin.getLogger().info("Yuklenen emoji sayisi: " + emojiCache.size());
        plugin.getLogger().info("===========================================");
        
        cacheLoaded = !emojiCache.isEmpty();
    }

    private void checkAvailableClasses() {
        String[] classes = {
            "dev.lone.itemsadder.api.FontImages.FontImageWrapper",
            "dev.lone.itemsadder.api.FontImages.TexturedCharacter",
            "dev.lone.itemsadder.api.FontImages.FontImage",
            "dev.lone.itemsadder.api.CustomStack",
            "dev.lone.itemsadder.api.ItemsAdder"
        };

        plugin.getLogger().info("--- Mevcut ItemsAdder Class'lari ---");
        for (String className : classes) {
            try {
                Class<?> clazz = Class.forName(className);
                plugin.getLogger().info("✓ " + className);
                
                // FontImageWrapper ise method'lari listele
                if (className.contains("FontImageWrapper")) {
                    plugin.getLogger().info("  Methods:");
                    for (Method m : clazz.getMethods()) {
                        if (m.getDeclaringClass() == clazz) {
                            plugin.getLogger().info("    - " + m.getName() + "()");
                        }
                    }
                    plugin.getLogger().info("  Constructors:");
                    for (Constructor<?> c : clazz.getConstructors()) {
                        plugin.getLogger().info("    - " + Arrays.toString(c.getParameterTypes()));
                    }
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().info("✗ " + className + " (BULUNAMADI)");
            }
        }
    }

    private String tryFontImageWrapper(String name, boolean debug) {
        try {
            Class<?> clazz = Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
            
            if (debug) plugin.getLogger().info("  Creating wrapper for: " + name);
            
            Object wrapper = clazz.getConstructor(String.class).newInstance(name);
            
            if (debug) plugin.getLogger().info("  Wrapper created: " + wrapper);

            // exists() kontrol
            try {
                Method existsMethod = clazz.getMethod("exists");
                boolean exists = (boolean) existsMethod.invoke(wrapper);
                if (debug) plugin.getLogger().info("  exists() = " + exists);
                if (!exists) return null;
            } catch (Exception e) {
                if (debug) plugin.getLogger().info("  exists() method yok veya hata: " + e.getMessage());
            }

            // Tum get methodlarini dene
            String[] methods = {"getString", "getCharacter", "getChar", "getValue", "getSymbol", "replaceFontImages"};
            for (String methodName : methods) {
                try {
                    Method m = clazz.getMethod(methodName);
                    Object result = m.invoke(wrapper);
                    if (debug) plugin.getLogger().info("  " + methodName + "() = " + result);
                    if (result != null && !result.toString().isEmpty() && !result.toString().equals(name)) {
                        return result.toString();
                    }
                } catch (Exception e) {
                    if (debug) plugin.getLogger().info("  " + methodName + "() = HATA: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            if (debug) plugin.getLogger().info("  GENEL HATA: " + e.getMessage());
        }
        return null;
    }

    private String tryFontImageWrapperAlt(String namespace, String name) {
        try {
            Class<?> clazz = Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
            
            // 2 parametreli constructor var mi?
            for (Constructor<?> c : clazz.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                plugin.getLogger().info("  Constructor params: " + Arrays.toString(params));
                
                if (params.length == 2 && params[0] == String.class && params[1] == String.class) {
                    Object wrapper = c.newInstance(namespace, name);
                    plugin.getLogger().info("  2-param wrapper created");
                    
                    // getString dene
                    try {
                        Method m = clazz.getMethod("getString");
                        Object result = m.invoke(wrapper);
                        if (result != null && !result.toString().isEmpty()) {
                            return result.toString();
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            plugin.getLogger().info("  Alt yontem hatasi: " + e.getMessage());
        }
        return null;
    }

    private void loadAllEmojis(ConfigurationSection ranksSection, String namespace) {
        for (String rankKey : ranksSection.getKeys(false)) {
            String emoji = ranksSection.getString(rankKey + ".emoji", "");
            if (emoji.isEmpty() || emojiCache.containsKey(emoji)) continue;

            String cleanName = emoji.replace(":", "");
            String character = tryFontImageWrapper(namespace + ":" + cleanName, false);
            
            if (character == null) {
                character = tryFontImageWrapper(cleanName, false);
            }

            if (character != null) {
                emojiCache.put(emoji, character);
                plugin.getLogger().info("Emoji yuklendi: " + emoji);
            }
        }
    }

    public void openBook(Player player) {
        if (!cacheLoaded) {
            loadEmojiCache();
        }

        List<EloManager.EloEntry> leaderboard = plugin.getEloManager().getLeaderboard();

        if (leaderboard.isEmpty()) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6EloTop&8] ");
            String msg = plugin.getConfig().getString("messages.no-data", "&cVeri yok!");
            player.sendMessage(colorize(prefix + msg));
            return;
        }

        plugin.getEloManager().updatePlayer(player);

        int playersPerPage = 10;
        int totalPages = (int) Math.ceil((double) leaderboard.size() / playersPerPage);

        int yourRank = plugin.getEloManager().getPlayerRank(player.getUniqueId());
        int yourElo = plugin.getEloManager().getPlayerEloLive(player);

        List<Component> pages = new ArrayList<>();

        for (int page = 0; page < totalPages; page++) {
            int startIndex = page * playersPerPage;
            int endIndex = Math.min(startIndex + playersPerPage, leaderboard.size());

            TextComponent.Builder pb = Component.text();

            pb.append(Component.text("  ✦ ", NamedTextColor.GOLD));
            pb.append(Component.text("Elo Sıralaması", NamedTextColor.BLACK)
                    .decoration(TextDecoration.BOLD, true));
            pb.append(Component.text(" ✦", NamedTextColor.GOLD));
            pb.append(Component.newline());
            pb.append(Component.newline());

            for (int i = startIndex; i < endIndex; i++) {
                EloManager.EloEntry entry = leaderboard.get(i);
                int rank = i + 1;
                int elo = entry.getElo();

                pb.append(Component.text(rank + ": ", NamedTextColor.DARK_GRAY));

                String emoji = getEmojiForElo(elo);
                if (emoji != null && !emoji.isEmpty()) {
                    pb.append(Component.text(emoji + " "));
                }

                String leagueTag = entry.getLeagueTag();
                Component hover = Component.text("")
                        .append(Component.text(entry.getPlayerName(), NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("Sıra: ", NamedTextColor.GRAY))
                        .append(Component.text("#" + rank, NamedTextColor.YELLOW)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("Elo: ", NamedTextColor.GRAY))
                        .append(Component.text(elo, NamedTextColor.GREEN)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.newline())
                        .append(Component.text("Lig: ", NamedTextColor.GRAY))
                        .append(leagueTag != null && !leagueTag.isEmpty()
                                ? deserializeHex(leagueTag) : Component.text("?", NamedTextColor.GRAY));

                pb.append(Component.text(entry.getPlayerName(), NamedTextColor.BLACK)
                        .hoverEvent(HoverEvent.showText(hover)));

                pb.append(Component.newline());
            }

            if (page == totalPages - 1) {
                pb.append(Component.newline());
                pb.append(Component.text(" ★ ", NamedTextColor.GOLD));
                pb.append(Component.text("Sen: ", NamedTextColor.DARK_GRAY));
                pb.append(Component.text("#" + (yourRank > 0 ? yourRank : "?"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.BOLD, true));
                pb.append(Component.text(" | ", NamedTextColor.DARK_GRAY));

                String yourEmoji = getEmojiForElo(yourElo);
                if (yourEmoji != null && !yourEmoji.isEmpty()) {
                    pb.append(Component.text(yourEmoji + " "));
                }
                pb.append(Component.text(yourElo + " ELO", NamedTextColor.GREEN));
            }

            pages.add(pb.build());
        }

        Book book = Book.book(
                Component.text("Elo Siralama"),
                Component.text("Server"),
                pages
        );

        player.openBook(book);
    }

    private String getEmojiForElo(int elo) {
        if (!plugin.getConfig().getBoolean("rank-icons.enabled", true)) {
            return null;
        }

        ConfigurationSection ranksSection = plugin.getConfig().getConfigurationSection("rank-icons.ranks");
        if (ranksSection == null) return null;

        for (String rankKey : ranksSection.getKeys(false)) {
            int minElo = ranksSection.getInt(rankKey + ".min-elo", 0);
            int maxElo = ranksSection.getInt(rankKey + ".max-elo", 999999);

            if (elo >= minElo && elo <= maxElo) {
                String emojiKey = ranksSection.getString(rankKey + ".emoji", "");
                return emojiCache.getOrDefault(emojiKey, null);
            }
        }

        return null;
    }

    public static Component deserializeHex(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        StringBuilder sb = new StringBuilder();
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '#' && i + 6 < chars.length) {
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

    public int getPlayerPage(UUID uuid) { return 1; }
    public void removePlayer(UUID uuid) { }
}
