package clre20.dropTimer.gui;

import clre20.dropTimer.DropTimer;
import clre20.dropTimer.rule.Rule;
import clre20.dropTimer.util.Msg;
import clre20.dropTimer.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 物品規則列表（6 排）。
 */
public final class RuleListGui extends Gui {

    private static final int PER_PAGE = 45;
    private static final int PREV = 45, INFO = 47, CLOSE = 49, NEXT = 53;

    private final DropTimer plugin;
    private int page;

    public RuleListGui(DropTimer plugin, int page) {
        super(54, "&8掉落物規則");
        this.plugin = plugin;
        this.page = page;
    }

    private int maxPage() {
        int size = plugin.rules().all().size();
        return Math.max(0, (size - 1) / PER_PAGE);
    }

    @Override
    protected void render() {
        page = Math.max(0, Math.min(page, maxPage()));
        List<Rule> all = plugin.rules().all();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
            set(i, display(all.get(start + i), start + i, all.size()));
        }

        if (page > 0) set(PREV, icon(Material.ARROW, "&e上一頁", "&7第 " + page + " 頁"));
        set(INFO, icon(Material.BOOK, "&e新增規則",
                "&7點擊下方&f背包裡的物品&7新增規則",
                "&7會複製一份，背包裡的物品不會被拿走",
                "",
                "&7共 &f" + all.size() + " &7條規則",
                "&7完整 NBT 規則優先於只比種類",
                "&7同類型則越前面越優先"));
        set(CLOSE, icon(Material.BARRIER, "&c關閉"));
        if (page < maxPage()) set(NEXT, icon(Material.ARROW, "&e下一頁", "&7第 " + (page + 2) + " 頁"));
        fillEmpty(45, 54);
    }

    /** 規則顯示用的物品：原本的 Lore 後面加上規則資訊 */
    static ItemStack display(Rule r, int index, int total) {
        ItemStack item = r.item();
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (meta.lore() != null) lore.addAll(meta.lore());
        if (!lore.isEmpty()) lore.add(Component.empty());
        lore.addAll(Msg.lore(infoLines(r.seconds(), r.exact(), r.worlds(), index, total)));
        lore.add(Msg.item("&e點擊編輯"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    static List<String> infoLines(int seconds, boolean exact, Set<String> worlds, int index, int total) {
        List<String> l = new ArrayList<>();
        l.add("&8────────────");
        l.add("&7消失時間：&f" + TimeUtil.format(seconds));
        l.add("&7比對方式：&f" + (exact ? "完整 NBT" : "只比種類"));
        l.add("&7範圍：&f" + (worlds.isEmpty() ? "所有世界" : String.join("、", worlds)));
        l.add("&7優先順序：&f第 " + (index + 1) + " / " + total + " 條");
        return l;
    }

    @Override
    public void onTopClick(Player player, int slot) {
        if (slot == PREV && page > 0) {
            page--;
            refresh();
        } else if (slot == NEXT && page < maxPage()) {
            page++;
            refresh();
        } else if (slot == CLOSE) {
            player.closeInventory();
        } else if (slot < PER_PAGE) {
            int idx = page * PER_PAGE + slot;
            List<Rule> all = plugin.rules().all();
            if (idx < all.size()) new RuleEditGui(plugin, RuleDraft.of(all.get(idx), idx), page).open(player);
        }
    }

    @Override
    public void onBottomClick(Player player, ItemStack item) {
        Rule same = plugin.rules().findSameItem(item);
        if (same != null) {
            Msg.send(player, "&e已有相同物品的規則，已開啟該規則");
            int idx = plugin.rules().indexOf(same.id());
            new RuleEditGui(plugin, RuleDraft.of(same, idx), page).open(player);
            return;
        }
        RuleDraft draft = new RuleDraft(null, item, plugin.config().getGlobalSeconds(), true, Set.of(),
                plugin.rules().all().size());
        new RuleEditGui(plugin, draft, page).open(player);
    }
}
