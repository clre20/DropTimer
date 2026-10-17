package clre20.dropTimer.gui;

import clre20.dropTimer.DropTimer;
import clre20.dropTimer.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 聊天室文字輸入：攔截玩家下一句話。
 */
public final class ChatInput implements Listener {

    private record Pending(Consumer<String> onInput, Runnable onCancel, BukkitTask timeout) {
    }

    private final DropTimer plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatInput(DropTimer plugin) {
        this.plugin = plugin;
    }

    /**
     * 等待玩家輸入。
     *
     * @param onInput  收到輸入（主執行緒）
     * @param onCancel 輸入取消字時（主執行緒）
     */
    public void ask(Player player, String prompt, Consumer<String> onInput, Runnable onCancel) {
        clear(player.getUniqueId());
        player.closeInventory();
        int timeout = plugin.config().getChatTimeout();
        String cancel = plugin.config().getCancelWord();
        Msg.send(player, prompt);
        Msg.send(player, "&7請在聊天室輸入，輸入「&f" + cancel + "&7」返回介面（" + timeout + " 秒後逾時）");

        UUID id = player.getUniqueId();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(id) != null) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) Msg.send(p, "&c輸入逾時，已取消");
            }
        }, timeout * 20L);
        pending.put(id, new Pending(onInput, onCancel, task));
    }

    private void clear(UUID id) {
        Pending p = pending.remove(id);
        if (p != null) p.timeout().cancel();
    }

    public void clearAll() {
        for (UUID id : pending.keySet()) clear(id);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (!pending.containsKey(id)) return;
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Pending p = pending.remove(id);
            if (p == null) return;
            p.timeout().cancel();
            if (text.equals(plugin.config().getCancelWord())) p.onCancel().run();
            else p.onInput().accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }
}
