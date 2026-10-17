package clre20.dropTimer.config;

import clre20.dropTimer.DropTimer;
import clre20.dropTimer.util.Msg;
import clre20.dropTimer.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 管理 config.yml 與 spigot.yml 的消失門檻。
 */
public final class ConfigManager {

    public static final int CONFIG_VERSION = 1;
    private static final int DEFAULT_DESPAWN_RATE = 6000;

    private final DropTimer plugin;

    private int globalSeconds = TimeUtil.VANILLA;
    private final Map<String, Integer> worldSeconds = new LinkedHashMap<>();
    private final Map<String, Integer> despawnRates = new HashMap<>();
    private int defaultDespawnRate = DEFAULT_DESPAWN_RATE;
    private YamlConfiguration spigotConfig;
    private int chatTimeout = 60;
    private String cancelWord = "取消";
    private boolean updateChecker = true;

    public ConfigManager(DropTimer plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- 讀取

    public void load() {
        plugin.saveDefaultConfig();
        File file = new File(plugin.getDataFolder(), "config.yml");
        try {
            new YamlConfiguration().load(file);
        } catch (Exception e) {
            // 格式壞掉時先備份，避免之後用指令改設定時把整個檔案蓋掉
            File backup = clre20.dropTimer.rule.BrokenFiles.backup(file);
            plugin.getLogger().severe("config.yml 格式錯誤，暫時使用預設值：" + String.valueOf(e.getMessage()).split("\n")[0]);
            plugin.getLogger().severe("原檔已備份為 " + backup.getName() + "，修正後可改回 config.yml 再 /dt reload");
        }
        plugin.reloadConfig();
        upgradeIfNeeded();

        FileConfiguration c = plugin.getConfig();
        Msg.setPrefix(c.getString("prefix", "&8[&eDropTimer&8] &r"));

        Integer g = TimeUtil.parse(c.getString("despawn-time", "5:00"), true);
        if (g == null) {
            plugin.getLogger().warning("despawn-time 格式錯誤：" + c.getString("despawn-time") + "，改用 5:00");
            g = TimeUtil.VANILLA;
        }
        globalSeconds = g;

        worldSeconds.clear();
        ConfigurationSection ws = c.getConfigurationSection("worlds");
        if (ws != null) {
            for (String world : ws.getKeys(false)) {
                String raw = ws.getString(world);
                Integer v = TimeUtil.parse(raw, true);
                if (v == null) {
                    plugin.getLogger().warning("worlds." + world + " 格式錯誤：" + raw + "，已略過");
                    continue;
                }
                worldSeconds.put(world, v);
            }
        }

        chatTimeout = Math.max(5, c.getInt("chat-input.timeout", 60));
        cancelWord = c.getString("chat-input.cancel-word", "取消");
        updateChecker = c.getBoolean("update-checker.enabled", true);

        loadDespawnRates();
    }

    /** config-version 不同時：備份舊檔，以新預設檔為底，保留舊值 */
    private void upgradeIfNeeded() {
        FileConfiguration old = plugin.getConfig();
        int version = old.getInt("config-version", 0);
        if (version == CONFIG_VERSION) return;

        File file = new File(plugin.getDataFolder(), "config.yml");
        try {
            Files.copy(file.toPath(), new File(plugin.getDataFolder(), "config.yml.bak").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            YamlConfiguration fresh;
            try (Reader r = new InputStreamReader(plugin.getResource("config.yml"), StandardCharsets.UTF_8)) {
                fresh = YamlConfiguration.loadConfiguration(r);
            }
            for (String key : old.getKeys(true)) {
                if (key.equals("config-version")) continue;
                if (old.isConfigurationSection(key)) continue;
                // worlds 底下的世界是使用者自訂的，整段保留
                if (fresh.contains(key) || key.startsWith("worlds.")) {
                    fresh.set(key, old.get(key));
                }
            }
            fresh.set("config-version", CONFIG_VERSION);
            fresh.save(file);
            plugin.reloadConfig();
            plugin.getLogger().info("config.yml 已從版本 " + version + " 更新到 " + CONFIG_VERSION + "，舊檔備份為 config.yml.bak");
        } catch (IOException | NullPointerException e) {
            plugin.getLogger().log(Level.WARNING, "config.yml 自動更新失敗", e);
        }
    }

    /** 讀取 spigot.yml 的 item-despawn-rate（各世界在第一次用到時才讀） */
    private void loadDespawnRates() {
        despawnRates.clear();
        defaultDespawnRate = DEFAULT_DESPAWN_RATE;
        spigotConfig = null;
        try {
            spigotConfig = Bukkit.spigot().getSpigotConfig();
            defaultDespawnRate = positive(spigotConfig.getInt("world-settings.default.item-despawn-rate", DEFAULT_DESPAWN_RATE));
        } catch (Throwable t) {
            plugin.getLogger().warning("無法讀取 spigot.yml 的 item-despawn-rate，使用 6000");
        }
    }

    private static int positive(int v) {
        return v > 0 ? v : DEFAULT_DESPAWN_RATE;
    }

    // ---------------------------------------------------------------- 寫入

    public void setGlobal(int seconds) {
        globalSeconds = seconds;
        plugin.getConfig().set("despawn-time", TimeUtil.toConfig(seconds));
        plugin.saveConfig();
    }

    /** seconds 為 null 表示刪除（跟隨全域） */
    public void setWorld(String world, Integer seconds) {
        if (seconds == null) {
            worldSeconds.remove(world);
            plugin.getConfig().set("worlds." + world, null);
        } else {
            worldSeconds.put(world, seconds);
            plugin.getConfig().set("worlds." + world, TimeUtil.toConfig(seconds));
        }
        plugin.saveConfig();
    }

    // ---------------------------------------------------------------- 取值

    public int getGlobalSeconds() {
        return globalSeconds;
    }

    /** 世界自己的設定，沒有則 null */
    public Integer getWorldSeconds(String world) {
        return worldSeconds.get(world);
    }

    public Map<String, Integer> getWorldSettings() {
        return Collections.unmodifiableMap(worldSeconds);
    }

    /** 原版消失門檻（tick） */
    public int getDespawnRate(World world) {
        return despawnRates.computeIfAbsent(world.getName(), name -> {
            String path = "world-settings." + name + ".item-despawn-rate";
            if (spigotConfig != null && spigotConfig.contains(path)) return positive(spigotConfig.getInt(path));
            return defaultDespawnRate;
        });
    }

    public int getChatTimeout() {
        return chatTimeout;
    }

    public String getCancelWord() {
        return cancelWord;
    }

    public boolean isUpdateCheckerEnabled() {
        return updateChecker;
    }
}
