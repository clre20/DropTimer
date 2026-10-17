package clre20.dropTimer.gui;

import clre20.dropTimer.DropTimer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 規則的世界範圍選擇。
 */
public final class WorldSelectGui extends Gui {

    private static final int MAX_WORLDS = 44; // 第一格是「所有世界」，最多 5 排

    private final RuleEditGui back;
    private final RuleDraft draft;
    private final List<String> names;
    private final int backSlot;

    WorldSelectGui(DropTimer plugin, RuleEditGui back, RuleDraft draft) {
        this(back, draft, worldNames(draft));
    }

    private WorldSelectGui(RuleEditGui back, RuleDraft draft, List<String> names) {
        super(sizeFor(names.size()), "&8選擇世界");
        this.back = back;
        this.draft = draft;
        this.names = names;
        this.backSlot = sizeFor(names.size()) - 5;
    }

    /** 已載入的世界，加上規則裡有但目前沒載入的世界 */
    private static List<String> worldNames(RuleDraft draft) {
        List<String> list = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) list.add(w.getName());
        for (String w : draft.worlds) if (!list.contains(w)) list.add(w);
        return list.size() > MAX_WORLDS ? list.subList(0, MAX_WORLDS) : list;
    }

    private static int sizeFor(int worlds) {
        int rows = (worlds + 1 + 8) / 9;
        return (Math.min(rows, 5) + 1) * 9;
    }

    @Override
    protected void render() {
        boolean all = draft.worlds.isEmpty();
        ItemStack allIcon = icon(Material.COMPASS, "&e所有世界",
                all ? "&a✔ 目前套用到所有世界" : "&7點擊改回所有世界",
                "&7（全部不勾 = 所有世界）");
        set(0, all ? glow(allIcon) : allIcon);

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            boolean on = draft.worlds.contains(name);
            World w = Bukkit.getWorld(name);
            Material type = w == null ? Material.MAP : switch (w.getEnvironment()) {
                case NETHER -> Material.NETHERRACK;
                case THE_END -> Material.END_STONE;
                default -> Material.GRASS_BLOCK;
            };
            List<String> lore = new ArrayList<>();
            if (w == null) lore.add("&8（目前未載入）");
            lore.add(on ? "&a✔ 已勾選" : "&7未勾選");
            lore.add("&e點擊切換");
            ItemStack it = icon(type, (on ? "&a" : "&f") + name, lore);
            set(i + 1, on ? glow(it) : it);
        }

        set(backSlot, icon(Material.ARROW, "&e返回", "&7回到規則設定"));
        fillEmpty(getInventory().getSize() - 9, getInventory().getSize());
    }

    @Override
    public void onTopClick(Player player, int slot) {
        if (slot == backSlot) {
            back.open(player);
        } else if (slot == 0) {
            draft.worlds.clear();
            refresh();
        } else if (slot >= 1 && slot <= names.size()) {
            String name = names.get(slot - 1);
            if (!draft.worlds.remove(name)) draft.worlds.add(name);
            refresh();
        }
    }
}
