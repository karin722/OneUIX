# Fold 専用タスクバー（ドック）の解放

Samsung のランチャー `com.sec.android.app.launcher`（ honeyspace ）は、画面下端からスワイプで出るタスクバーを
Fold とタブレットだけの機能にしている。この文書は、バー型端末でそれを開けるために**何がどこで塞いでいるのか**と、
モジュールがそれをどう開けているのかを記録する。

## 調査方法

推測ではなく実機の APK を読んだ。

```
adb shell pm path com.sec.android.app.launcher
adb pull /system/priv-app/TouchWizHome_2017/TouchWizHome_2017.apk
jadx -d out --no-debug-info --show-bad-code TouchWizHome_2017.apk
```

確認した環境は **SM-S931Z（ Galaxy S25、バー型 ）/ One UI 8.5（ 80500 ）/ Android 16**、
ランチャーは単一 APK（ split なし、5 dex、約 59 MB ）。画面は `sw384dp`。

## 特定した門番

タスクバーは boolean フラグ 1 個ではなく、**3 段の判定**で閉じられていた。

### 1. `com.honeyspace.common.Rune.HOME_SUPPORT_TASKBAR` — 大元

```java
// com.honeyspace.common.Rune の <clinit>
HOME_SUPPORT_TASKBAR = semFloatingFeature.getBoolean("SEC_FLOATING_FEATURE_LAUNCHER_SUPPORT_TASKBAR");
```

`SEC_FLOATING_FEATURE_LAUNCHER_SUPPORT_TASKBAR` は **SM-S931Z の `/system/etc/floating_feature.xml` に存在しない**。
存在しないキーの `getBoolean` は false を返すので、この時点で固定的に false になる。

これが false のとき、タスクバーは表示されないのではなく**存在ごと消える**。

| 場所 | false のときの挙動 |
| --- | --- |
| `TaskbarControllerImpl.<init>` | イベント配線のブロックをまるごと飛ばす |
| `TaskbarControllerImpl.initialize()` | 即 return。タスクバーの Pot が作られない |
| `TaskbarVisibilityController.<init>` / `.init()` | `TaskbarState` を 0 にして即 return |
| `TaskbarControllerProxyImpl.getTaskbarController()` | null を返す。スワイプのヒント描画が死ぬ |
| `GestureInputHandler` | タスクバー向けのジェスチャ処理が無効 |
| `TaskbarUtilImpl.getTaskbarEnabled()` | 常に false |

参照側は表示系もジェスチャ系も**すべて `Rune.INSTANCE.getHOME_SUPPORT_TASKBAR()`**、
つまり `com.honeyspace.common.Rune$Companion#getHOME_SUPPORT_TASKBAR()` を通る。入口が 1 つなのでフックしやすい。

### 2. `TaskbarControllerImpl._taskbarAvailable`

```java
// TaskbarControllerImpl の <init>
Integer num = (Integer) globalSettingsDataSource.get(globalSettingKeys.getTASK_BAR_AVAILABLE()).getValue();
this._taskbarAvailable = StateFlowKt.MutableStateFlow(num != null && num.intValue() == 1);
```

`TASK_BAR_AVAILABLE` の実体は **Settings.Global の `sem_task_bar_available`** で、実機の値は `0`。

この値は次の一本道で効いてくる。

```java
getTaskbarStyleInfo() =
    getTaskbarStyleInfo(_taskbarAvailable && !isEasySpace && HOME_SUPPORT_TASKBAR, docked, hide);

getTaskbarStyleInfo(available, docked, hide) =
    new TaskbarStyleInfo((available || docked) && !hide, ...);
```

門番 1 を開けても `available` が false なら `TaskbarStyleInfo.isTaskbar` が false になり、ビューは作られない。

### 3. `TaskbarUtilImpl.getTaskbarEnabled()`

```java
public boolean getTaskbarEnabled() {
    Integer num;
    return Rune.INSTANCE.getHOME_SUPPORT_TASKBAR()
        && (num = (Integer) this.globalSettingsDataSource.get(GlobalSettingKeys.INSTANCE.getTASK_BAR_ENABLED()).getValue()) != null
        && num.intValue() == 1;
}
```

`TASK_BAR_ENABLED` の実体は **Settings.Global の `task_bar`**（ 既定値 0 ）。
これは本来「設定アプリのタスクバー ON/OFF」に対応するが、門番 1 が閉じている端末ではその設定項目自体が作られないため、
値は未設定のまま既定の 0 に落ちる。

参考までに、関連する設定キーの実名は以下のとおり（ いずれも `Settings.Global` ）。

| 定数 | キー | 既定値 |
| --- | --- | --- |
| `TASK_BAR_ENABLED` | `task_bar` | 0 |
| `TASK_BAR_AVAILABLE` | `sem_task_bar_available` | 0 |
| `TASK_BAR_TYPE` | `taskbar_style_type` | 1 |
| `TASK_BAR_RECENT_ENABLED` | `taskbar_recent_apps_enabled` | 0 |

## あえて触っていないもの

`com.honeyspace.ui.common.ModelFeature` にも端末種別の判定がある。

```java
static {
    isTabletModel     = Rune.INSTANCE.getSUPPORT_TABLET_TYPE();
    isMultiFoldModel  = Rune.INSTANCE.getSUPPORT_MULTI_FOLDABLE_HOME();
    isBarModel        = !(isTabletModel || isFoldModel || isMultiFoldModel);
}
```

これは次を左右する。

- `FloatingTaskbarShowCheckerImpl.isShowing()` — **フローティング**タスクバーの表示可否
- `TaskbarControllerImpl.isTabletOrMultiFoldModel` — `updateTouchRect()` の早期 return、最近アプリ画面でのタスクバー維持
- `TaskbarVisibilityController.maintainTaskbarInRecent`

通常のタスクバーを出すだけなら**これらは不要**で、しかも偽装するとレイアウト全体がタブレット扱いになり副作用が大きい。
そのためモジュールでは触っていない。フローティング形状や最近アプリ画面での挙動まで必要になった場合の拡張点として記録しておく。

同様に、`smallestScreenWidthDp` を偽装する方向も採っていない。この機能は sw600dp では分岐していないため必要がない。

## 実装

`app/src/main/java/io/github/soclear/oneuix/hook/LauncherTaskbar.kt`。
名前はすべて実名決め打ちで、DexKit による探索も総当たりのヒューリスティックも使わない。

| フック対象 | 内容 |
| --- | --- |
| `SemFloatingFeature#getBoolean(String[, boolean])` | キーが `SEC_FLOATING_FEATURE_LAUNCHER_SUPPORT_TASKBAR` のとき true |
| `com.honeyspace.common.Rune$Companion#getHOME_SUPPORT_TASKBAR()` | 常に true |
| `TaskbarControllerImpl#getTaskbarStyleInfo(boolean, boolean, boolean)` | 第 1 引数 `available` を true に差し替え |
| `TaskbarUtilImpl#getTaskbarEnabled()` | 常に true |

設計上の判断を 2 点。

**静的フィールドではなくゲッターを差し替える。** `Rune.HOME_SUPPORT_TASKBAR` は `static final` で
リフレクションから書き換えられないうえ、値は `<clinit>` で計算される。ゲッターを置き換えれば
クラス初期化のタイミングに左右されない。`SemFloatingFeature` 側のフックは、
同一モジュール内からフィールドが直接読まれた場合の取りこぼしを防ぐ保険であり、
`Rune` の `<clinit>` より先に仕込むため最初に呼んでいる。

**システム設定は書き換えない。** `sem_task_bar_available` や `task_bar` に値を書けば恒久的に残ってしまう。
代わりに読み出しの合流点をフックしているので、モジュールのスイッチを切れば元の挙動に戻る。

## 制限と既知のリスク

- 確認済みなのは One UI 8.5 / SM-S931Z の APK 解析まで。**実機での動作確認は未実施**
- クラス名・メソッド名は One UI のバージョンで変わりうる。変わった場合はフックが黙って外れる
  （ 各フックは個別に try/catch していて、失敗は Xposed のログに出る ）
- フローティング形状のタスクバーと、最近アプリ画面でのタスクバー維持は対象外（ 上記 `ModelFeature` の項を参照 ）
- 設定アプリ側のタスクバー関連メニューは、システム側が `sem_task_bar_available = 0` のままなので出てこない可能性がある

## 動作確認の手順

1. モジュール APK をインストールする
2. LSPosed でモジュールを有効にし、**スコープに「ホーム画面（ `com.sec.android.app.launcher` ）」を追加**する
3. OneUIX の設定で「Fold 専用のタスクバー（ドック）を解放」を ON にする
4. ランチャーを再起動する（ 設定 → アプリ → ホーム画面 → 強制停止、または端末を再起動 ）
5. 画面下端から上にゆっくりスワイプしてタスクバーが出るか確認する

うまくいかない場合は、次のログに手がかりが出る。

```
adb logcat -d | grep -i -E "LSPosed|TaskbarControllerImpl|TaskbarVisibilityController"
```

特に `create TaskbarStyleInfo: taskbarEnabled=...(available=..., easy=..., rune=...)` という行が
`TaskbarControllerImpl` から出ていて、3 つの門番がそれぞれどうなっているかをそのまま読める。
