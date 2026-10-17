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

/**
 * 規則設定（3 排）。
 */
public final class RuleEditGui extends Gui {

    private static final int ITEM = 4, TIME = 10, MATCH = 12, WORLDS = 14, UP = 15, DOWN = 16,
            CANCEL = 18, DELETE = 22, SAVE = 26;

    private final DropTimer plugin;
    private final RuleDraft draft;
    private final int listPage;

    RuleEditGui(DropTimer plugin, RuleDraft draft, int listPage) {
        super(27, draft.isNew() ? "&8新增規則" : "&8編輯規則 #" + draft.id);
        this.plugin = plugin;
        this.draft = draft;
        this.listPage = listPage;
    }

    /** 列表總數（新規則還沒加進去，所以 +1） */
    private int total() {
        int size = plugin.rules().all().size();
        return draft.isNew() ? size + 1 : size;
    }

    @Override
    protected void render() {
        draft.index = Math.max(0, Math.min(draft.index, total() - 1));

        ItemStack shown = draft.item.clone();
        ItemMeta meta = shown.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (meta.lore() != null) lore.addAll(meta.lore());
        if (!lore.isEmpty()) lore.add(Component.empty());
        lore.addAll(Msg.lore(RuleListGui.infoLines(draft.seconds, draft.exact, draft.worlds, draft.index, total())));
        lore.add(Msg.item("&7這是實際用來比對的物品"));
        meta.lore(lore);
        shown.setItemMeta(meta);
        set(ITEM, shown);

        set(TIME, icon(Material.CLOCK, "&e消失時間：&f" + TimeUtil.format(draft.seconds),
                "&7點擊在聊天室輸入",
                "&7格式：&f90&7、&f1:30&7 或 &fnever"));

        set(MATCH, draft.exact
                ? icon(Material.NAME_TAG, "&e比對方式：&f完整 NBT",
                "&a▶ 完整 NBT", "&7  只比種類", "",
                "&7名稱、附魔、Lore、耐久都要相同",
                "&7有耐久的工具用過一次就不算同一個", "", "&e點擊切換")
                : icon(Material.PAPER, "&e比對方式：&f只比種類",
                "&7  完整 NBT", "&a▶ 只比種類", "",
                "&7同種類物品都算", "", "&e點擊切換"));

        set(WORLDS, icon(Material.COMPASS, "&e範圍：&f" + (draft.worlds.isEmpty() ? "所有世界" : draft.worlds.size() + " 個世界"),
                worldLore()));

        String pos = "&7目前：&f第 " + (draft.index + 1) + " / " + total() + " 條";
        set(UP, draft.index > 0
                ? icon(Material.LIME_DYE, "&e優先順序：往前", pos, "&e點擊往前一格")
                : icon(Material.GRAY_DYE, "&7優先順序：往前", pos, "&7已經是第一條"));
        set(DOWN, draft.index < total() - 1
                ? icon(Material.ORANGE_DYE, "&e優先順序：往後", pos, "&e點擊往後一格")
                : icon(Material.GRAY_DYE, "&7優先順序：往後", pos, "&7已經是最後一條"));

        set(CANCEL, icon(Material.BARRIER, "&c取消", "&7不儲存，返回列表"));
        if (!draft.isNew()) {
            set(DELETE, draft.deleteArmed
                    ? glow(icon(Material.LAVA_BUCKET, "&4再點一次確認刪除", "&7刪除後無法復原"))
                    : icon(Material.LAVA_BUCKET, "&c刪除規則", "&7需要點兩次"));
        }
        set(SAVE, icon(Material.LIME_CONCRETE, "&a儲存", "&7儲存後，受影響的掉落物會重新計時"));
        fillEmpty(0, 27);
    }

    private List<String> worldLore() {
        List<String> l = new ArrayList<>();
        if (draft.worlds.isEmpty()) l.add("&f所有世界");
        else for (String w : draft.worlds) l.add("&f- " + w);
        l.add("");
        l.add("&e點擊選擇世界");
        return l;
    }

    @Override
    public void onTopClick(Player player, int slot) {
        if (slot != DELETE && draft.deleteArmed) {
            draft.deleteArmed = false;
        }
        switch (slot) {
            case TIME -> askTime(player);
            case MATCH -> {
                draft.exact = !draft.exact;
                refresh();
            }
            case WORLDS -> new WorldSelectGui(plugin, this, draft).open(player);
            case UP -> {
                if (draft.index > 0) draft.index--;
                refresh();
            }
            case DOWN -> {
                if (draft.index < total() - 1) draft.index++;
                refresh();
            }
            case CANCEL -> new RuleListGui(plugin, listPage).open(player);
            case DELETE -> delete(player);
            case SAVE -> save(player);
            default -> refresh();
        }
    }

    private void askTime(Player player) {
        plugin.chatInput().ask(player, "&e請輸入消失時間（&f90&e、&f1:30&e 或 &fnever&e）",
                text -> {
                    Integer v = TimeUtil.parse(text, true);
                    if (v == null) {
                        Msg.send(player, "&c時間格式錯誤，請用 秒 或 分:秒，例如 90 或 1:30（最長 99:59），或 never");
                    } else {
                        draft.seconds = v;
                        if (v == TimeUtil.NEVER) Msg.send(player, "&e注意：永不消失的掉落物會一直留在地上，數量多時可能造成卡頓");
                    }
                    open(player);
                },
                () -> open(player));
    }

    private void delete(Player player) {
        if (draft.isNew()) return;
        if (!draft.deleteArmed) {
            draft.deleteArmed = true;
            refresh();
            return;
        }
        plugin.rules().remove(draft.id);
        int n = plugin.despawn().applyAll();
        Msg.send(player, "&a已刪除規則 #" + draft.id + "，已重新計時 &f" + n + " &a個掉落物");
        new RuleListGui(plugin, listPage).open(player);
    }

    private void save(Player player) {
        int id = draft.isNew() ? plugin.rules().nextId() : draft.id;
        if (!draft.isNew() && plugin.rules().get(id) == null) {
            Msg.send(player, "&c這條規則已經被刪除（可能是其他人或 /dt reload），未儲存");
            new RuleListGui(plugin, listPage).open(player);
            return;
        }
        Rule rule = new Rule(id, draft.item, draft.seconds, draft.exact, draft.worlds);
        plugin.rules().put(rule, draft.index);
        int n = plugin.despawn().applyAll();
        Msg.send(player, "&a規則 #" + id + " 已儲存（" + TimeUtil.format(rule.seconds()) + "），已重新計時 &f" + n + " &a個掉落物");
        new RuleListGui(plugin, draft.index / 45).open(player);
    }
}
