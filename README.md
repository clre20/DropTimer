# DropTimer

DropTimer 是一款專為 Paper 伺服器設計的掉落物消失時間控制插件。可以用指令設定全伺服器、個別世界的掉落物消失時間，也能透過遊戲內介面替特定物品（支援完整 NBT 比對）設定專屬時間，從幾秒到 99 分 59 秒，或是永不消失。設定變更後會立即套用到地上已存在的掉落物，不需重啟伺服器。

## 功能特性

* **三層時間設定**：
  * 依照「物品規則 ＞ 世界設定 ＞ 全域設定」的順序決定每個掉落物的消失時間。
  * 每一層都可以設為 `never`（永不消失）。
  * 適用所有掉落來源：玩家丟出、方塊破壞、生物與玩家死亡、爆炸、投擲器、容器破壞、其他插件生成的掉落物等。
* **直覺的時間格式**：
  * 支援純秒數（`90`）或 `分:秒`（`1:30`），最長 `99:59`。
  * 輸入錯誤時（例如 `1:75`）會清楚提示，不會寫入錯誤設定。
* **NBT 物品規則（遊戲內介面）**：
  * 在介面中點擊自己背包裡的物品即可新增規則，插件會複製一份作為比對樣本，背包裡的物品不會被拿走。
  * 每條規則可選擇「完整 NBT」（名稱、附魔、Lore、耐久、容器內容都要相同）或「只比種類」。
  * 每條規則可限定套用的世界，並可調整優先順序。
  * 規則以 Paper 原生格式保存，伺服器升級版本後完整 NBT 不會遺失（1.21.11 建立的規則已實測可於 26.x 正常使用）。
* **即時重新計時**：
  * 修改任何設定後，已載入區塊中受影響的掉落物會從當下重新計時；未載入區塊的掉落物會在區塊載入時自動套用。
  * 只有套用設定真的改變的掉落物才會重新計時，其他掉落物維持原本進度。
* **超過原版時間也能精準計時**：
  * 設定時間超過原版 5 分鐘時，由插件接手倒數多出來的時間，最後再交回原版處理。
  * 倒數進度存在掉落物本身，區塊卸載、伺服器重啟後照樣接續；區塊未載入時不計時（與原版行為一致）。
* **介面安全防護**：
  * 介面全部以左鍵操作；右鍵、Shift、數字鍵、副手鍵、拖曳、雙擊、Q 丟棄與創造模式中鍵一律取消，防止物品被取出或複製。
  * 關閉介面（Esc）即取消，不會儲存未確認的修改。
* **相容原版行為**：
  * 只調整掉落物年齡，不影響 Paper 對剛生成實體的處理，遠離玩家的農場掉落物照常合併。
  * 讀取 `spigot.yml` 的 `item-despawn-rate`，伺服器自訂的原版消失門檻也能正確計算。
  * 偵測到 Paper `alt-item-despawn-rate` 開啟時會在主控台提示可能衝突。
* **設定檔保護**：
  * 設定檔版本更新時自動補上新設定並保留原有數值與註解，舊檔備份為 `config.yml.bak`。
  * `config.yml` 或 `items.yml` 格式損壞時，會自動備份為 `.broken-時間` 檔案並以預設值繼續運作，避免資料被覆蓋。
* **版本更新提示**：
  * 啟動時與輸入 `/dt` 時檢查 GitHub 最新版本，僅提示不自動下載。

## 指令與權限

本插件主指令為 `/dt`。

| 指令 | 說明 | 執行端 | 權限節點 | 預設 |
| :--- | :--- | :--- | :--- | :--- |
| `/dt` | 顯示版本、更新狀態與目前所在世界的消失時間 | 遊戲內 / 控制台 | `droptimer.use` | OP |
| `/dt help` | 顯示自己有權限使用的指令 | 遊戲內 / 控制台 | `droptimer.use` | OP |
| `/dt info` | 列出全域、各世界設定與物品規則數量 | 遊戲內 / 控制台 | `droptimer.use` | OP |
| `/dt set <時間>` | 設定全域消失時間（`default` = 恢復原版 5:00） | 遊戲內 / 控制台 | `droptimer.set` | OP |
| `/dt world <世界>` | 查看該世界的消失時間 | 遊戲內 / 控制台 | `droptimer.world` | OP |
| `/dt world <世界> set <時間>` | 設定該世界的消失時間（`default` = 改回跟隨全域） | 遊戲內 / 控制台 | `droptimer.world` | OP |
| `/dt item` | 開啟物品規則介面 | 僅限玩家 | `droptimer.item` | OP |
| `/dt item add <時間>` | 以主手物品（完整 NBT）快速新增規則；已有相同物品的規則則更新時間 | 僅限玩家 | `droptimer.item` | OP |
| `/dt reload` | 重新載入設定檔與物品規則 | 遊戲內 / 控制台 | `droptimer.reload` | OP |

> `droptimer.*` 包含以上全部權限。使用任何 `/dt` 指令都需要 `droptimer.use`。
>
> 時間格式：
> * `90`：90 秒
> * `1:30`：1 分 30 秒（最長 `99:59`）
> * `never`：永不消失

## 使用範例

### 1. 調整全伺服器的掉落物消失時間
將所有掉落物改為 2 分鐘後消失：
```bash
/dt set 2:00
```
恢復原版 5 分鐘：
```bash
/dt set default
```

### 2. 個別世界使用不同時間
地獄掉落物 30 秒消失、終界掉落物永不消失：
```bash
/dt world world_nether set 0:30
/dt world world_the_end set never
```
改回跟隨全域設定：
```bash
/dt world world_nether set default
```

### 3. 讓特定物品永不消失
1. 手持要保護的物品（例如自訂名稱的武器），執行：
   ```bash
   /dt item add never
   ```
2. 或執行 `/dt item` 開啟介面，點擊背包中的物品新增規則，再於規則設定中調整：
   * **消失時間**：點擊後在聊天室輸入（輸入「取消」返回介面）。
   * **比對方式**：在「完整 NBT」與「只比種類」之間切換。
   * **範圍**：選擇規則套用的世界，全部不勾即為所有世界。
   * **優先順序**：同一個掉落物同時符合多條規則時，完整 NBT 規則優先，同類型則排越前面越優先。


## 注意事項

* 僅支援 Paper，不支援 Spigot 與 Folia（在 Spigot 上會顯示提示並自動停用）。
* 永不消失的掉落物會一直留在地上，數量過多時可能造成伺服器卡頓。
* 永不消失、以及超過原版時間正在由插件倒數的掉落物，不會與相同掉落物合併（原版限制），交回原版後即可正常合併。
* 移除插件前，請先執行 `/dt set default` 並刪除設為 `never` 的世界設定與物品規則，否則正在由插件倒數的掉落物在移除後會變成永不消失。


# DropTimer

DropTimer is an item despawn timer plugin designed specifically for Paper (Minecraft 1.21.11, 26.1 – 26.3) servers. Set how long dropped items stay on the ground server-wide, per world, or per item through an in-game GUI with full NBT matching — anywhere from a few seconds to 99 minutes 59 seconds, or never. Changes apply instantly to items already on the ground, with no restart required.

## Features

* **Three-Tier Timer Settings**:
  * Each dropped item's lifetime is resolved in the order "Item Rules > World Settings > Global Setting".
  * Every tier can be set to `never` (items never despawn).
  * Works with every drop source: player drops, block breaking, mob and player deaths, explosions, droppers, broken containers, items spawned by other plugins, and more.
* **Intuitive Time Format**:
  * Accepts plain seconds (`90`) or `minutes:seconds` (`1:30`), up to `99:59`.
  * Invalid input (such as `1:75`) is clearly reported and never written to the config.
* **NBT Item Rules (In-Game GUI)**:
  * Click any item in your own inventory to create a rule; the plugin stores a copy as the match template and never takes the original item.
  * Each rule can match by "Full NBT" (name, enchantments, lore, durability, and container contents must all match) or by "Item Type Only".
  * Each rule can be limited to specific worlds and reordered by priority.
  * Rules are stored in Paper's native format, so full NBT survives server version upgrades (rules created on 1.21.11 are verified to work on 26.x).
* **Instant Re-Timing**:
  * After any setting changes, affected items in loaded chunks restart their timer immediately; items in unloaded chunks are updated automatically when their chunk loads.
  * Only items whose applied setting actually changed are re-timed; all others keep their progress.
* **Accurate Timing Beyond the Vanilla Limit**:
  * When the configured time exceeds vanilla's 5 minutes, the plugin counts down the extra time itself before handing the item back to vanilla.
  * Countdown progress is stored on the item itself and persists across chunk unloads and server restarts; like vanilla, items in unloaded chunks do not count down.
* **GUI Protection**:
  * All GUIs use left-click only; right-click, Shift-click, number keys, the offhand key, dragging, double-clicking, Q-dropping, and creative middle-click are all cancelled to prevent item removal or duplication.
  * Closing a GUI (Esc) cancels the edit; unconfirmed changes are never saved.
* **Vanilla-Friendly Behavior**:
  * Only the item's age is adjusted, leaving Paper's handling of newly spawned entities untouched, so drops from farms far away from players still merge normally.
  * Reads `item-despawn-rate` from `spigot.yml`, so custom vanilla despawn thresholds are calculated correctly.
  * Warns in the console when Paper's `alt-item-despawn-rate` is enabled, since it may conflict.
* **Config File Protection**:
  * When the config version changes, new options are added automatically while existing values and comments are kept; the old file is backed up as `config.yml.bak`.
  * If `config.yml` or `items.yml` becomes malformed, it is automatically backed up as a `.broken-<time>` file and the plugin keeps running with defaults, so your data is never overwritten.
* **Update Notifications**:
  * Checks GitHub for the latest release on startup and when running `/dt`; it only notifies and never downloads automatically.

## Commands & Permissions

The primary command for this plugin is `/dt`.

| Command | Description | Executor | Permission Node | Default |
| :--- | :--- | :--- | :--- | :--- |
| `/dt` | Show version, update status, and the despawn time of your current world | In-game / Console | `droptimer.use` | OP |
| `/dt help` | Show the commands you have permission to use | In-game / Console | `droptimer.use` | OP |
| `/dt info` | List the global setting, per-world settings, and the number of item rules | In-game / Console | `droptimer.use` | OP |
| `/dt set <time>` | Set the global despawn time (`default` = restore vanilla 5:00) | In-game / Console | `droptimer.set` | OP |
| `/dt world <world>` | View the despawn time of a world | In-game / Console | `droptimer.world` | OP |
| `/dt world <world> set <time>` | Set the despawn time of a world (`default` = follow the global setting) | In-game / Console | `droptimer.world` | OP |
| `/dt item` | Open the item rules GUI | Player only | `droptimer.item` | OP |
| `/dt item add <time>` | Quickly add a Full NBT rule for the item in your main hand; updates the time if a rule for the same item exists | Player only | `droptimer.item` | OP |
| `/dt reload` | Reload configuration files and item rules | In-game / Console | `droptimer.reload` | OP |

> `droptimer.*` grants all of the above. Every `/dt` command requires `droptimer.use`.
>
> Time Format:
> * `90`: 90 seconds
> * `1:30`: 1 minute 30 seconds (up to `99:59`)
> * `never`: never despawn

## Usage Examples

### 1. Change the Server-Wide Despawn Time
Make all dropped items despawn after 2 minutes:
```bash
/dt set 2:00
```
Restore vanilla's 5 minutes:
```bash
/dt set default
```

### 2. Use Different Times per World
Nether drops despawn after 30 seconds, End drops never despawn:
```bash
/dt world world_nether set 0:30
/dt world world_the_end set never
```
Return to the global setting:
```bash
/dt world world_nether set default
```

### 3. Make a Specific Item Never Despawn
1. Hold the item you want to protect (for example, a custom-named weapon) and run:
   ```bash
   /dt item add never
   ```
2. Or run `/dt item` to open the GUI, click an item in your inventory to create a rule, then adjust it in the rule editor:
   * **Despawn Time**: Click and type the time in chat (type `取消` to return to the GUI; configurable).
   * **Match Mode**: Toggle between "Full NBT" and "Item Type Only".
   * **Worlds**: Choose which worlds the rule applies to; leaving all unchecked means every world.
   * **Priority**: When an item matches multiple rules, Full NBT rules win first; among rules of the same mode, the one listed earlier wins.


Item rules are stored in `plugins/DropTimer/items.yml`; editing them through the `/dt item` GUI is recommended.

## Notes

* Paper only; Spigot and Folia are not supported (on Spigot the plugin shows a notice and disables itself).
* Items set to never despawn stay on the ground forever and may cause lag in large numbers.
* Items that never despawn, and items currently in the plugin's extended countdown, do not merge with identical items (a vanilla limitation); they merge normally once handed back to vanilla.
* Before removing the plugin, run `/dt set default` and delete any world settings or item rules set to `never`; otherwise items in the plugin's extended countdown will never despawn after removal.
