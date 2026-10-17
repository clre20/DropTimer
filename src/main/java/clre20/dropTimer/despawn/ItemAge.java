package clre20.dropTimer.despawn;

import org.bukkit.entity.Item;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * 設定掉落物的年齡（Age）。
 * <p>
 * Paper API 只有 {@link Item#setTicksLived(int)}，它會連帶改掉實體的 tickCount。
 * Paper 用 tickCount &lt; 200 判斷「剛生成的實體」，讓它在沒有玩家附近時也正常運作（包括掉落物合併），
 * 改掉 tickCount 會讓剛掉出的物品失去這段時間，遠離玩家的農場掉落物就不太會合併。
 * <p>
 * 所以優先只改掉落物本身的 age 欄位（Paper 1.20.5 起執行時使用 Mojang 名稱，欄位名稱固定為 age），
 * 找不到時才退回 setTicksLived。
 */
public final class ItemAge {

    private static Method getHandle;
    private static Field age;
    private static boolean failed;

    private ItemAge() {
    }

    public static void set(Item item, int value) {
        if (!failed) {
            try {
                if (age == null) {
                    getHandle = item.getClass().getMethod("getHandle");
                    Object handle = getHandle.invoke(item);
                    Field f = handle.getClass().getField("age");
                    if (f.getType() != int.class) throw new NoSuchFieldException("age is not int");
                    age = f;
                }
                age.setInt(getHandle.invoke(item), value);
                return;
            } catch (Throwable t) {
                failed = true;
                Logger.getLogger("DropTimer").warning("無法直接設定掉落物年齡，改用 setTicksLived（剛掉出的物品合併會比原版少）：" + t);
            }
        }
        item.setTicksLived(Math.max(1, value));
    }
}
