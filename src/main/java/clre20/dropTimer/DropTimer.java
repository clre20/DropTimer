package clre20.dropTimer;

import clre20.dropTimer.command.DtCommand;
import clre20.dropTimer.config.ConfigManager;
import clre20.dropTimer.despawn.DespawnService;
import clre20.dropTimer.despawn.ItemListener;
import clre20.dropTimer.gui.ChatInput;
import clre20.dropTimer.gui.Gui;
import clre20.dropTimer.gui.GuiListener;
import clre20.dropTimer.rule.RuleManager;
import clre20.dropTimer.update.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class DropTimer extends JavaPlugin {

    private ConfigManager config;
    private RuleManager rules;
    private DespawnService despawn;
    private ChatInput chatInput;
    private UpdateChecker updater;

    @Override
    public void onEnable() {
        if (!isPaper()) {
            getLogger().severe("DropTimer 只支援 Paper 核心（1.21.11、26.1～26.3），目前的核心不是 Paper，插件已停用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        config = new ConfigManager(this);
        rules = new RuleManager(this);
        config.load();
        rules.load();

        despawn = new DespawnService(this, config, rules);
        chatInput = new ChatInput(this);
        updater = new UpdateChecker(this);

        getServer().getPluginManager().registerEvents(new ItemListener(despawn), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(chatInput, this);

        PluginCommand cmd = getCommand("dt");
        if (cmd != null) {
            DtCommand dt = new DtCommand(this);
            cmd.setExecutor(dt);
            cmd.setTabCompleter(dt);
        }

        despawn.start();
        // 套用到已經在地上的掉落物（例如 /reload 或插件晚載入）
        Bukkit.getScheduler().runTask(this, () -> despawn.applyAll());

        warnAltDespawnRate();
        updater.checkAsync(true);
        getLogger().info("DropTimer 已啟用，全域消失時間 " + clre20.dropTimer.util.TimeUtil.format(config.getGlobalSeconds())
                + "，物品規則 " + rules.all().size() + " 條");
    }

    @Override
    public void onDisable() {
        if (despawn != null) despawn.stop();
        if (chatInput != null) chatInput.clearAll();
        // 關掉開著的介面，避免插件卸載後還能點
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Gui) p.closeInventory();
        }
    }

    private static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.event.entity.EntityAddToWorldEvent");
            Class.forName("net.kyori.adventure.text.Component");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void reloadAll() {
        config.load();
        rules.load();
        warnAltDespawnRate();
    }

    /** Paper 的 alt-item-despawn-rate 有開時提示 */
    private void warnAltDespawnRate() {
        List<String> where = new ArrayList<>();
        File root = Bukkit.getWorldContainer();
        File defaults = new File(root, "config/paper-world-defaults.yml");
        if (altEnabled(defaults)) where.add("config/paper-world-defaults.yml");
        for (World w : Bukkit.getWorlds()) {
            File f = new File(w.getWorldFolder(), "paper-world.yml");
            if (altEnabled(f)) where.add(w.getName() + "/paper-world.yml");
        }
        if (!where.isEmpty()) {
            getLogger().warning("偵測到 Paper 的 alt-item-despawn-rate 已開啟（" + String.join("、", where)
                    + "），它指定的物品會和 DropTimer 的設定衝突");
        }
    }

    private static boolean altEnabled(File f) {
        if (!f.isFile()) return false;
        try {
            return YamlConfiguration.loadConfiguration(f).getBoolean("entities.spawning.alt-item-despawn-rate.enabled", false);
        } catch (Exception e) {
            return false;
        }
    }

    public ConfigManager config() {
        return config;
    }

    public RuleManager rules() {
        return rules;
    }

    public DespawnService despawn() {
        return despawn;
    }

    public ChatInput chatInput() {
        return chatInput;
    }

    public UpdateChecker updater() {
        return updater;
    }
}
