package clre20.dropTimer.update;

import clre20.dropTimer.DropTimer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 檢查 GitHub 最新 Release，只提示不下載。結果快取 10 分鐘。
 */
public final class UpdateChecker {

    public enum State { UNKNOWN, LATEST, OUTDATED, FAILED }

    private static final String API = "https://api.github.com/repos/clre20/DropTimer/releases/latest";
    private static final long CACHE_MS = 10 * 60 * 1000L;
    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    private final DropTimer plugin;
    private final String current;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile State state = State.UNKNOWN;
    private volatile String latest;
    private volatile long checkedAt;

    public UpdateChecker(DropTimer plugin) {
        this.plugin = plugin;
        this.current = plugin.getPluginMeta().getVersion();
    }

    /** 快取過期才會真的去查；log = 查完有新版時寫到主控台 */
    public void checkAsync(boolean log) {
        if (!plugin.config().isUpdateCheckerEnabled()) return;
        if (System.currentTimeMillis() - checkedAt < CACHE_MS && state != State.UNKNOWN) return;
        if (!running.compareAndSet(false, true)) return;

        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(API))
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "DropTimer/" + current)
                        .GET().build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                Matcher m = TAG.matcher(res.body());
                if (res.statusCode() != 200 || !m.find()) {
                    state = State.FAILED;
                } else {
                    latest = m.group(1).replaceFirst("^[vV]", "");
                    state = compare(latest, current) > 0 ? State.OUTDATED : State.LATEST;
                    if (log && state == State.OUTDATED) {
                        plugin.getLogger().info("有新版本 " + latest + "（目前 " + current + "）：https://github.com/clre20/DropTimer/releases");
                    }
                }
            } catch (Exception e) {
                state = State.FAILED;
            } finally {
                checkedAt = System.currentTimeMillis();
                running.set(false);
            }
        });
    }

    /** 比較版本號，a &gt; b 回傳正數 */
    static int compare(String a, String b) {
        String[] x = a.split("[^0-9]+"), y = b.split("[^0-9]+");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0;
            int q = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    public String statusLine() {
        if (!plugin.config().isUpdateCheckerEnabled()) return "&7版本檢查已關閉";
        return switch (state) {
            case UNKNOWN -> "&7正在檢查更新…";
            case LATEST -> "&a目前為最新版本";
            case OUTDATED -> "&e有新版本 &f" + latest + "&e：&fgithub.com/clre20/DropTimer/releases";
            case FAILED -> "&7無法檢查更新";
        };
    }
}
