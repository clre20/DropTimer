package clre20.dropTimer.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 介面點擊：全部取消，只有左鍵會交給介面處理。
 */
public final class GuiListener implements Listener {

    @EventHandler(ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Gui gui)) return;
        event.setCancelled(true);
        if (event.getClick() != ClickType.LEFT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;
        if (clicked.equals(top)) {
            gui.onTopClick(player, event.getRawSlot());
        } else {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir()) gui.onBottomClick(player, item.clone());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Gui) event.setCancelled(true);
    }
}
