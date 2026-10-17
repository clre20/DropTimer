package clre20.dropTimer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 時間格式工具。
 * <p>
 * 時間一律以「秒」表示，{@link #NEVER} 代表永不消失。
 */
public final class TimeUtil {

    /** 永不消失 */
    public static final int NEVER = -1;
    /** 原版預設 5:00 */
    public static final int VANILLA = 300;
    /** 最長 99:59 */
    public static final int MAX = 99 * 60 + 59;

    private static final Pattern SECONDS = Pattern.compile("\\d{1,4}");
    private static final Pattern MIN_SEC = Pattern.compile("(\\d{1,2}):(\\d{1,2})");

    private TimeUtil() {
    }

    /**
     * 解析時間。
     *
     * @param input       使用者輸入
     * @param allowNever  是否接受 never
     * @return 秒數、{@link #NEVER}，格式錯誤回傳 null
     */
    public static Integer parse(String input, boolean allowNever) {
        if (input == null) return null;
        String s = input.trim();
        if (allowNever && s.equalsIgnoreCase("never")) return NEVER;

        int total;
        Matcher m;
        if (SECONDS.matcher(s).matches()) {
            total = Integer.parseInt(s);
        } else if ((m = MIN_SEC.matcher(s)).matches()) {
            int min = Integer.parseInt(m.group(1));
            int sec = Integer.parseInt(m.group(2));
            if (sec > 59) return null;
            total = min * 60 + sec;
        } else {
            return null;
        }
        if (total < 1 || total > MAX) return null;
        return total;
    }

    /** 顯示用：分:秒，秒補兩位；永不消失顯示「永不消失」 */
    public static String format(int seconds) {
        if (seconds == NEVER) return "永不消失";
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    /** 寫入設定檔用：分:秒 或 never */
    public static String toConfig(int seconds) {
        if (seconds == NEVER) return "never";
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
