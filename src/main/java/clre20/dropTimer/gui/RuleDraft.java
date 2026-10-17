package clre20.dropTimer.gui;

import clre20.dropTimer.rule.Rule;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 正在編輯、尚未儲存的規則。
 */
final class RuleDraft {

    /** null = 新規則 */
    final Integer id;
    final ItemStack item;
    int seconds;
    boolean exact;
    final Set<String> worlds;
    /** 在列表中的位置（0 起算） */
    int index;
    /** 刪除按鈕已按過一次 */
    boolean deleteArmed;

    RuleDraft(Integer id, ItemStack item, int seconds, boolean exact, Set<String> worlds, int index) {
        this.id = id;
        this.item = item.clone();
        this.item.setAmount(1);
        this.seconds = seconds;
        this.exact = exact;
        this.worlds = new LinkedHashSet<>(worlds);
        this.index = index;
    }

    static RuleDraft of(Rule r, int index) {
        return new RuleDraft(r.id(), r.item(), r.seconds(), r.exact(), r.worlds(), index);
    }

    boolean isNew() {
        return id == null;
    }
}
