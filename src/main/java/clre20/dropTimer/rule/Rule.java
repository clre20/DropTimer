package clre20.dropTimer.rule;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 一條物品規則。
 */
public final class Rule {

    private final int id;
    private final ItemStack item;
    private final int seconds;
    private final boolean exact;
    private final Set<String> worlds;

    public Rule(int id, ItemStack item, int seconds, boolean exact, Set<String> worlds) {
        this.id = id;
        ItemStack copy = item.clone();
        copy.setAmount(1);
        this.item = copy;
        this.seconds = seconds;
        this.exact = exact;
        this.worlds = java.util.Collections.unmodifiableSet(new LinkedHashSet<>(worlds));
    }

    public int id() {
        return id;
    }

    /** 規則物品（數量 1） */
    public ItemStack item() {
        return item.clone();
    }

    /** 秒數或 TimeUtil.NEVER */
    public int seconds() {
        return seconds;
    }

    /** true = 完整 NBT；false = 只比種類 */
    public boolean exact() {
        return exact;
    }

    /** 空 = 所有世界 */
    public Set<String> worlds() {
        return worlds;
    }

    public boolean appliesTo(String world) {
        return worlds.isEmpty() || worlds.contains(world);
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType() != item.getType()) return false;
        return !exact || item.isSimilar(stack);
    }
}
