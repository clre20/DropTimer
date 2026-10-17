package clre20.dropTimer.command;

import clre20.dropTimer.DropTimer;
import clre20.dropTimer.gui.RuleListGui;
import clre20.dropTimer.rule.Rule;
import clre20.dropTimer.util.Msg;
import clre20.dropTimer.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * /dt 指令。
 */
public final class DtCommand implements TabExecutor {

    private static final String P_USE = "droptimer.use";
    private static final String P_SET = "droptimer.set";
    private static final String P_WORLD = "droptimer.world";
    private static final String P_ITEM = "droptimer.item";
    private static final String P_RELOAD = "droptimer.reload";

    private static final List<String> TIMES = List.of("0:30", "1:00", "5:00", "10:00", "never");
    private static final String FORMAT_ERROR = "&c時間格式錯誤，請用 秒 或 分:秒，例如 90 或 1:30（最長 99:59）";

    private final DropTimer plugin;

    public DtCommand(DropTimer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] a) {
        if (a.length == 0) {
            if (!check(s, P_USE)) return true;
            status(s);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "help" -> {
                if (check(s, P_USE)) help(s);
            }
            case "info" -> {
                if (check(s, P_USE)) info(s);
            }
            case "set" -> {
                if (check(s, P_SET)) set(s, a);
            }
            case "world" -> {
                if (check(s, P_WORLD)) world(s, a);
            }
            case "item" -> {
                if (check(s, P_ITEM)) item(s, a);
            }
            case "reload" -> {
                if (check(s, P_RELOAD)) reload(s);
            }
            default -> Msg.send(s, "&c未知的子指令，輸入 &f/dt help &c查看說明");
        }
        return true;
    }

    private boolean check(CommandSender s, String perm) {
        if (s.hasPermission(perm)) return true;
        Msg.send(s, "&c你沒有權限使用這個指令");
        return false;
    }

    // ---------------------------------------------------------------- 子指令

    private void status(CommandSender s) {
        plugin.updater().checkAsync(false);
        Msg.raw(s, "&8&m          &r &eDropTimer &fv" + plugin.getPluginMeta().getVersion() + " &8&m          ");
        Msg.raw(s, "&7作者：&fClre20");
        Msg.raw(s, plugin.updater().statusLine());
        if (s instanceof Player p) {
            String w = p.getWorld().getName();
            Integer ws = plugin.config().getWorldSeconds(w);
            Msg.raw(s, "&7目前世界 &f" + w + "&7：&f" + TimeUtil.format(plugin.despawn().worldEffective(w))
                    + (ws == null ? " &8（跟隨全域）" : " &8（世界設定）"));
        } else {
            Msg.raw(s, "&7全域：&f" + TimeUtil.format(plugin.config().getGlobalSeconds()));
        }
        Msg.raw(s, "&7輸入 &f/dt help &7查看指令");
    }

    private void help(CommandSender s) {
        Msg.raw(s, "&8&m          &r &eDropTimer 指令 &8&m          ");
        line(s, P_USE, "/dt", "版本與目前設定");
        line(s, P_USE, "/dt info", "列出所有設定");
        line(s, P_SET, "/dt set <時間>", "設定全域（default = 原版 5:00）");
        line(s, P_WORLD, "/dt world <世界>", "查看世界設定");
        line(s, P_WORLD, "/dt world <世界> set <時間>", "設定世界（default = 跟隨全域）");
        line(s, P_ITEM, "/dt item", "開啟物品規則介面");
        line(s, P_ITEM, "/dt item add <時間>", "以主手物品新增規則");
        line(s, P_RELOAD, "/dt reload", "重新讀取設定");
        Msg.raw(s, "&7時間格式：&f90&7、&f1:30&7（最長 99:59）或 &fnever&7（永不消失）");
    }

    private void line(CommandSender s, String perm, String usage, String desc) {
        if (s.hasPermission(perm)) Msg.raw(s, "&e" + usage + " &7- " + desc);
    }

    private void info(CommandSender s) {
        Msg.raw(s, "&8&m          &r &eDropTimer 設定 &8&m          ");
        Msg.raw(s, "&7全域：&f" + TimeUtil.format(plugin.config().getGlobalSeconds()));
        Msg.raw(s, "&7世界：");
        Set<String> names = new LinkedHashSet<>();
        for (World w : Bukkit.getWorlds()) names.add(w.getName());
        names.addAll(plugin.config().getWorldSettings().keySet());
        for (String w : names) {
            Integer ws = plugin.config().getWorldSeconds(w);
            String loaded = Bukkit.getWorld(w) == null ? " &8（未載入）" : "";
            Msg.raw(s, "&8 - &f" + w + "&7：" + (ws == null
                    ? "&7" + TimeUtil.format(plugin.config().getGlobalSeconds()) + " &8（跟隨全域）"
                    : "&f" + TimeUtil.format(ws)) + loaded);
        }
        List<Rule> rules = plugin.rules().all();
        Msg.raw(s, "&7物品規則：&f" + rules.size() + " &7條" + (rules.isEmpty() ? "" : "（/dt item 查看）"));
        int ext = plugin.despawn().extendedCount();
        if (ext > 0) Msg.raw(s, "&7超過原版時間、由插件倒數中的掉落物：&f" + ext + " &7個");
    }

    private void set(CommandSender s, String[] a) {
        if (a.length < 2) {
            Msg.send(s, "&c用法：/dt set <時間>");
            return;
        }
        int seconds;
        if (a[1].equalsIgnoreCase("default")) {
            seconds = TimeUtil.VANILLA;
        } else {
            Integer v = TimeUtil.parse(a[1], true);
            if (v == null) {
                Msg.send(s, FORMAT_ERROR);
                return;
            }
            seconds = v;
        }
        plugin.config().setGlobal(seconds);
        int n = plugin.despawn().applyAll();
        Msg.send(s, "&a全域掉落物消失時間已設為 &f" + TimeUtil.format(seconds) + " &a（原版 5:00），已重新計時 &f" + n + " &a個掉落物");
        neverWarn(s, seconds);
    }

    private void world(CommandSender s, String[] a) {
        if (a.length < 2) {
            Msg.send(s, "&c用法：/dt world <世界> [set <時間>]");
            return;
        }
        String name = a[1];
        // Bukkit 找世界不分大小寫，統一成真正的世界名稱，否則設定會對不上
        World found = Bukkit.getWorld(name);
        if (found != null) name = found.getName();
        boolean known = Bukkit.getWorld(name) != null || plugin.config().getWorldSeconds(name) != null;

        if (a.length == 2) {
            if (!known) {
                Msg.send(s, "&c找不到世界：" + name);
                return;
            }
            Integer ws = plugin.config().getWorldSeconds(name);
            Msg.send(s, "&f" + name + " &7的掉落物消失時間：&f" + TimeUtil.format(plugin.despawn().worldEffective(name))
                    + (ws == null ? " &8（跟隨全域）" : " &8（世界設定）"));
            return;
        }
        if (!a[2].equalsIgnoreCase("set") || a.length < 4) {
            Msg.send(s, "&c用法：/dt world <世界> set <時間>");
            return;
        }
        if (!known) {
            Msg.send(s, "&c找不到世界：" + name);
            return;
        }
        Integer seconds;
        if (a[3].equalsIgnoreCase("default")) {
            seconds = null;
        } else {
            seconds = TimeUtil.parse(a[3], true);
            if (seconds == null) {
                Msg.send(s, FORMAT_ERROR);
                return;
            }
        }
        plugin.config().setWorld(name, seconds);
        int n = plugin.despawn().applyAll();
        if (seconds == null) {
            Msg.send(s, "&f" + name + " &a已改為跟隨全域（&f" + TimeUtil.format(plugin.config().getGlobalSeconds())
                    + "&a），已重新計時 &f" + n + " &a個掉落物");
        } else {
            Msg.send(s, "&f" + name + " &a的掉落物消失時間已設為 &f" + TimeUtil.format(seconds) + "&a，已重新計時 &f" + n + " &a個掉落物");
            neverWarn(s, seconds);
        }
    }

    private void item(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) {
            Msg.send(s, "&c只有玩家可以使用");
            return;
        }
        if (a.length == 1) {
            new RuleListGui(plugin, 0).open(p);
            return;
        }
        if (!a[1].equalsIgnoreCase("add") || a.length < 3) {
            Msg.send(s, "&c用法：/dt item 或 /dt item add <時間>");
            return;
        }
        Integer seconds = TimeUtil.parse(a[2], true);
        if (seconds == null) {
            Msg.send(s, FORMAT_ERROR);
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Msg.send(s, "&c請把物品拿在主手");
            return;
        }
        Rule same = plugin.rules().findSameItem(hand);
        Rule saved;
        if (same != null) {
            saved = plugin.rules().put(new Rule(same.id(), same.item(), seconds, true, same.worlds()),
                    plugin.rules().indexOf(same.id()));
        } else {
            saved = plugin.rules().put(new Rule(plugin.rules().nextId(), hand, seconds, true, Set.of()),
                    plugin.rules().all().size());
        }
        int n = plugin.despawn().applyAll();
        Msg.send(s, (same != null ? "&a已更新規則 #" : "&a已新增規則 #") + saved.id()
                + "（完整 NBT、" + TimeUtil.format(seconds) + "），已重新計時 &f" + n + " &a個掉落物");
        neverWarn(s, seconds);
    }

    private void reload(CommandSender s) {
        plugin.reloadAll();
        int n = plugin.despawn().applyAll();
        Msg.send(s, "&a已重新讀取設定，已重新計時 &f" + n + " &a個掉落物");
    }

    private void neverWarn(CommandSender s, int seconds) {
        if (seconds == TimeUtil.NEVER) {
            Msg.send(s, "&e注意：永不消失的掉落物會一直留在地上，數量多時可能造成卡頓");
        }
    }

    // ---------------------------------------------------------------- Tab

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            if (s.hasPermission(P_USE)) out.addAll(List.of("help", "info"));
            if (s.hasPermission(P_SET)) out.add("set");
            if (s.hasPermission(P_WORLD)) out.add("world");
            if (s.hasPermission(P_ITEM)) out.add("item");
            if (s.hasPermission(P_RELOAD)) out.add("reload");
        } else {
            String sub = a[0].toLowerCase(Locale.ROOT);
            if (sub.equals("set") && a.length == 2 && s.hasPermission(P_SET)) {
                out.addAll(TIMES);
                out.add("default");
            } else if (sub.equals("world") && s.hasPermission(P_WORLD)) {
                if (a.length == 2) {
                    for (World w : Bukkit.getWorlds()) out.add(w.getName());
                    for (Map.Entry<String, Integer> e : plugin.config().getWorldSettings().entrySet()) {
                        if (!out.contains(e.getKey())) out.add(e.getKey());
                    }
                } else if (a.length == 3) {
                    out.add("set");
                } else if (a.length == 4 && a[2].equalsIgnoreCase("set")) {
                    out.addAll(TIMES);
                    out.add("default");
                }
            } else if (sub.equals("item") && s.hasPermission(P_ITEM)) {
                if (a.length == 2) out.add("add");
                else if (a.length == 3 && a[1].equalsIgnoreCase("add")) out.addAll(TIMES);
            }
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
