package clre20.dropTimer.rule;

import clre20.dropTimer.DropTimer;
import clre20.dropTimer.util.TimeUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * 管理 items.yml 的物品規則。列表順序即優先順序。
 */
public final class RuleManager {

    private final DropTimer plugin;
    private final File file;
    private final List<Rule> rules = new ArrayList<>();
    /** 依物品種類分組，加快比對 */
    private final Map<Material, List<Rule>> byType = new EnumMap<>(Material.class);
    private int nextId = 1;

    public RuleManager(DropTimer plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "items.yml");
    }

    // ---------------------------------------------------------------- 讀寫

    public void load() {
        rules.clear();
        nextId = 1;
        if (!file.exists()) {
            rebuildIndex();
            save();
            return;
        }
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.load(file);
        } catch (Exception e) {
            // 格式壞掉時先備份，避免之後存檔把使用者的規則蓋掉
            File backup = BrokenFiles.backup(file);
            plugin.getLogger().severe("items.yml 格式錯誤，無法讀取物品規則：" + String.valueOf(e.getMessage()).split("\n")[0]);
            plugin.getLogger().severe("原檔已備份為 " + backup.getName() + "，修正後可改回 items.yml 再 /dt reload");
            rebuildIndex();
            return;
        }
        for (Map<?, ?> m : y.getMapList("rules")) {
            try {
                int id = Integer.parseInt(String.valueOf(m.get("id")));
                ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(String.valueOf(m.get("item"))));
                Integer seconds = TimeUtil.parse(String.valueOf(m.get("time")), true);
                if (seconds == null) throw new IllegalArgumentException("time 格式錯誤：" + m.get("time"));
                boolean exact = !"type".equalsIgnoreCase(String.valueOf(m.get("match")));
                Set<String> worlds = new LinkedHashSet<>();
                Object w = m.get("worlds");
                if (w instanceof List<?> list) for (Object o : list) worlds.add(String.valueOf(o));
                rules.add(new Rule(id, item, seconds, exact, worlds));
                nextId = Math.max(nextId, id + 1);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "items.yml 有一條規則讀取失敗，已略過：" + m.get("preview"), e);
            }
        }
        rebuildIndex();
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.options().setHeader(List.of(
                "DropTimer 物品規則",
                "請用 /dt item 介面編輯，列表順序 = 優先順序",
                "match: exact = 完整 NBT；type = 只比種類",
                "worlds: 空 = 所有世界",
                "preview 只是給人看的，插件不會讀取"));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Rule r : rules) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.id());
            m.put("item", Base64.getEncoder().encodeToString(r.item().serializeAsBytes()));
            m.put("preview", preview(r.item()));
            m.put("time", TimeUtil.toConfig(r.seconds()));
            m.put("match", r.exact() ? "exact" : "type");
            m.put("worlds", new ArrayList<>(r.worlds()));
            list.add(m);
        }
        y.set("rules", list);
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "無法儲存 items.yml", e);
        }
    }

    private void rebuildIndex() {
        byType.clear();
        for (Rule r : rules) byType.computeIfAbsent(r.item().getType(), k -> new ArrayList<>()).add(r);
    }

    // ---------------------------------------------------------------- 比對

    /** 找出命中的規則：完整 NBT 優先，再看只比種類；同類型依列表順序 */
    public Rule match(ItemStack stack, String world) {
        if (stack == null) return null;
        List<Rule> candidates = byType.get(stack.getType());
        if (candidates == null) return null;
        for (Rule r : candidates) if (r.exact() && r.appliesTo(world) && r.matches(stack)) return r;
        for (Rule r : candidates) if (!r.exact() && r.appliesTo(world)) return r;
        return null;
    }

    /** 找出物品完全相同（完整 NBT）的規則 */
    public Rule findSameItem(ItemStack stack) {
        for (Rule r : rules) if (r.exact() && r.item().isSimilar(stack)) return r;
        return null;
    }

    // ---------------------------------------------------------------- 編輯

    public List<Rule> all() {
        return Collections.unmodifiableList(rules);
    }

    public int indexOf(int id) {
        for (int i = 0; i < rules.size(); i++) if (rules.get(i).id() == id) return i;
        return -1;
    }

    public Rule get(int id) {
        int i = indexOf(id);
        return i < 0 ? null : rules.get(i);
    }

    public int nextId() {
        return nextId;
    }

    /** 新增或取代規則，放在 index 位置，並存檔 */
    public Rule put(Rule rule, int index) {
        int old = indexOf(rule.id());
        if (old >= 0) rules.remove(old);
        index = Math.max(0, Math.min(index, rules.size()));
        rules.add(index, rule);
        nextId = Math.max(nextId, rule.id() + 1);
        rebuildIndex();
        save();
        return rule;
    }

    public boolean remove(int id) {
        int i = indexOf(id);
        if (i < 0) return false;
        rules.remove(i);
        rebuildIndex();
        save();
        return true;
    }

    // ---------------------------------------------------------------- 顯示

    public static String preview(ItemStack item) {
        String s = item.getType().name();
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            s += " 「" + PlainTextComponentSerializer.plainText().serialize(meta.displayName()) + "」";
        }
        return s;
    }
}
