package clre20.dropTimer.despawn;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 掉落物加入世界時（新生成或區塊載入）套用設定。
 */
public final class ItemListener implements Listener {

    private final DespawnService service;

    public ItemListener(DespawnService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdd(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Item item) service.apply(item);
    }
}
