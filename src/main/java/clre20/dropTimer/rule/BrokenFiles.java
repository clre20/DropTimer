package clre20.dropTimer.rule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 設定檔格式壞掉時的備份。
 */
public final class BrokenFiles {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private BrokenFiles() {
    }

    /** 複製成 檔名.broken-時間，回傳備份檔 */
    public static File backup(File file) {
        File out = new File(file.getParentFile(), file.getName() + ".broken-" + LocalDateTime.now().format(TIME));
        try {
            Files.copy(file.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
        return out;
    }
}
