package clre20.dropTimer.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * 訊息工具，使用 &amp; 色碼。
 */
public final class Msg {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static String prefix = "&8[&eDropTimer&8] &r";

    private Msg() {
    }

    public static void setPrefix(String p) {
        prefix = p == null ? "" : p;
    }

    /** 轉成 Component（不加前綴） */
    public static Component c(String text) {
        return LEGACY.deserialize(text);
    }

    /** 物品名稱、Lore 用：去掉預設斜體 */
    public static Component item(String text) {
        return LEGACY.deserialize(text).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> lore(List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String l : lines) out.add(item(l));
        return out;
    }

    /** 加前綴送出 */
    public static void send(CommandSender to, String text) {
        to.sendMessage(LEGACY.deserialize(prefix + text));
    }

    /** 不加前綴送出 */
    public static void raw(CommandSender to, String text) {
        to.sendMessage(LEGACY.deserialize(text));
    }
}
