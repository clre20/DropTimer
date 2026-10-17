package clre20.dropTimer.despawn;

import clre20.dropTimer.DropTimer;
import clre20.dropTimer.config.ConfigManager;
import clre20.dropTimer.rule.Rule;
import clre20.dropTimer.rule.RuleManager;
import clre20.dropTimer.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 決定每個掉落物套用哪個設定，並調整它的消失時間。
 * <p>
 * 原版掉落物的年齡每 tick +1，達到 spigot.yml 的 item-despawn-rate 就消失。
 * <ul>
 *     <li>設定時間 ≤ 門檻：直接把年齡設為「門檻 − 設定時間」，交給原版計時</li>
 *     <li>設定時間 ＞ 門檻：先設為永不消失，由插件倒數多出來的時間，剩下的再交給原版</li>
 *     <li>永不消失：設為永不消失</li>
 * </ul>
 * 每個掉落物用 PDC 記下套用的設定（applied），設定改變時才重新計時。
 */
public final class DespawnService {

    private static final int TASK_PERIOD = 20;

    private final DropTimer plugin;
    private final ConfigManager config;
    private final RuleManager rules;
    private final NamespacedKey appliedKey;
    private final NamespacedKey remainKey;

    /** 正在由插件倒數的掉落物 */
    private final Map<UUID, Item> extended = new HashMap<>();
    private BukkitTask task;

    public DespawnService(DropTimer plugin, ConfigManager config, RuleManager rules) {
        this.plugin = plugin;
        this.config = config;
        this.rules = rules;
        this.appliedKey = new NamespacedKey(plugin, "applied");
        this.remainKey = new NamespacedKey(plugin, "remain");
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickExtended, TASK_PERIOD, TASK_PERIOD);
    }

    public void stop() {
        if (task != null) task.cancel();
        // 把剩餘時間寫回去，下次啟動接著算
        for (Item item : extended.values()) {
            if (item.isValid()) item.getPersistentDataContainer().set(remainKey, PersistentDataType.LONG, remainingOf(item));
        }
        extended.clear();
    }

    // ---------------------------------------------------------------- 決定設定

    /** 套用結果：sig 用來判斷設定是否改變 */
    public record Target(String sig, int seconds, String source) {
    }

    public Target resolve(World world, org.bukkit.inventory.ItemStack stack) {
        String w = world.getName();
        Rule r = rules.match(stack, w);
        if (r != null) return new Target("r" + r.id() + ":" + r.seconds(), r.seconds(), "物品規則 #" + r.id());
        Integer ws = config.getWorldSeconds(w);
        if (ws != null) return new Target("w" + w + ":" + ws, ws, "世界設定");
        int g = config.getGlobalSeconds();
        return new Target("g:" + g, g, "全域設定");
    }

    /** 世界層級的有效時間（不看物品規則） */
    public int worldEffective(String world) {
        Integer ws = config.getWorldSeconds(world);
        return ws != null ? ws : config.getGlobalSeconds();
    }

    // ---------------------------------------------------------------- 套用

    /**
     * 檢查並套用設定。
     *
     * @return 是否重新計時
     */
    public boolean apply(Item item) {
        PersistentDataContainer pdc = item.getPersistentDataContainer();
        String old = pdc.get(appliedKey, PersistentDataType.STRING);

        // 被其他插件設成永不消失的掉落物，一律不動
        if (old == null && item.isUnlimitedLifetime()) return false;

        Target t = resolve(item.getWorld(), item.getItemStack());
        if (t.sig().equals(old)) {
            if (pdc.has(remainKey, PersistentDataType.LONG)) extended.put(item.getUniqueId(), item);
            return false;
        }

        pdc.set(appliedKey, PersistentDataType.STRING, t.sig());
        pdc.remove(remainKey);
        extended.remove(item.getUniqueId());

        if (t.seconds() == TimeUtil.NEVER) {
            item.setUnlimitedLifetime(true);
            return true;
        }

        long ticks = t.seconds() * 20L;
        int rate = config.getDespawnRate(item.getWorld());
        if (ticks <= rate) {
            handToVanilla(item, ticks, rate);
        } else {
            item.setUnlimitedLifetime(true);
            pdc.set(remainKey, PersistentDataType.LONG, ticks);
            extended.put(item.getUniqueId(), item);
        }
        return true;
    }

    /** 重新檢查所有已載入的掉落物，回傳重新計時的數量 */
    public int applyAll() {
        int n = 0;
        for (World w : Bukkit.getWorlds()) {
            for (Item item : w.getEntitiesByClass(Item.class)) {
                if (apply(item)) n++;
            }
        }
        return n;
    }

    /** 剩下 ticks 由原版接手：年齡 = 門檻 − ticks */
    private void handToVanilla(Item item, long ticks, int rate) {
        item.setUnlimitedLifetime(false);
        ItemAge.set(item, (int) Math.max(0, rate - ticks));
    }

    // ---------------------------------------------------------------- 倒數

    private long remainingOf(Item item) {
        Long v = item.getPersistentDataContainer().get(remainKey, PersistentDataType.LONG);
        return v == null ? 0 : v;
    }

    private void tickExtended() {
        Iterator<Item> it = extended.values().iterator();
        while (it.hasNext()) {
            Item item = it.next();
            // 區塊卸載或已被撿起；區塊重新載入時會再加回來
            if (!item.isValid()) {
                it.remove();
                continue;
            }
            PersistentDataContainer pdc = item.getPersistentDataContainer();
            if (!pdc.has(remainKey, PersistentDataType.LONG)) {
                it.remove();
                continue;
            }
            long remain = remainingOf(item) - TASK_PERIOD;
            int rate = config.getDespawnRate(item.getWorld());
            if (remain <= rate) {
                pdc.remove(remainKey);
                handToVanilla(item, Math.max(remain, 1), rate);
                it.remove();
            } else {
                pdc.set(remainKey, PersistentDataType.LONG, remain);
            }
        }
    }

    /** 正在由插件倒數的掉落物數量 */
    public int extendedCount() {
        return extended.size();
    }
}
