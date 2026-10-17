package clre20.dropTimer.gui;

import clre20.dropTimer.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 箱子介面基底。只接受左鍵，點擊交給子類別處理。
 */
public abstract class Gui implements InventoryHolder {

    private final Inventory inventory;

    protected Gui(int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, Msg.c(title));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /** 重新繪製內容 */
    protected abstract void render();

    /** 點擊介面上方的格子 */
    public abstract void onTopClick(Player player, int slot);

    /** 點擊玩家自己的背包 */
    public void onBottomClick(Player player, ItemStack item) {
    }

    public void open(Player player) {
        inventory.clear();
        render();
        player.openInventory(inventory);
    }

    /** 重繪但不重新開啟 */
    protected void refresh() {
        inventory.clear();
        render();
    }

    protected void set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    /** 把空格填上灰色玻璃片 */
    protected void fillEmpty(int from, int to) {
        ItemStack pane = icon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = from; i < to; i++) if (inventory.getItem(i) == null) inventory.setItem(i, pane);
    }

    // ---------------------------------------------------------------- 物品

    public static ItemStack icon(Material type, String name, String... lore) {
        return icon(type, name, List.of(lore));
    }

    public static ItemStack icon(Material type, String name, List<String> lore) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Msg.item(name));
        if (!lore.isEmpty()) meta.lore(Msg.lore(lore));
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack glow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }
}
