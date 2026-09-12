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
| `com.honeyspace.common.Rune$Companion#getSUPPORT_EDIT_ON_TASKBAR()` | 常に true（ タスクバー上でアイコンを掴めるようにする ） |
| `Resources#getDrawable(int, Theme)` | `ic_all_apps` の要求を、現在のモードに合う 1 枚に差し替え |

設計上の判断を 2 点。

**静的フィールドではなくゲッターを差し替える。** `Rune.HOME_SUPPORT_TASKBAR` は `static final` で
リフレクションから書き換えられないうえ、値は `<clinit>` で計算される。ゲッターを置き換えれば
クラス初期化のタイミングに左右されない。`SemFloatingFeature` 側のフックは、
同一モジュール内からフィールドが直接読まれた場合の取りこぼしを防ぐ保険であり、
`Rune` の `<clinit>` より先に仕込むため最初に呼んでいる。

**システム設定は書き換えない。** `sem_task_bar_available` や `task_bar` に値を書けば恒久的に残ってしまう。
代わりに読み出しの合流点をフックしているので、モジュールのスイッチを切れば元の挙動に戻る。

## 解放したあとに出た不具合と、その原因

実機（ SM-S931Z / One UI 8.5 ）でタスクバーが出たあと、2 つの症状が出た。どちらも実機ログで裏を取っている。

### ライトモードでアプリ一覧ボタンが白いまま

`R.drawable.ic_all_apps` は 2 枚重ねの layer-list になっている。

```xml
<layer-list>
    <item android:drawable="@drawable/ic_all_apps_light"/>   <!-- 白いアイコン -->
    <item android:drawable="@drawable/ic_all_apps_dark"/>    <!-- 黒いアイコン -->
</layer-list>
```

タスクバーの Pot はこの 2 枚の alpha を振り分けて色を切り替えるが、その配分は
**ナビゲーションバーの darkIntensity** から計算している。

```java
// 非フローティング（ 通常のタスクバー ）の分岐
float f = isDexSpace ? dexDarkIntensity : navButtonsDarkIntensity;
layer0.setAlpha((int) (comp(f) * 255.0f));   // ic_all_apps_light
layer1.setAlpha((int) (f * 255.0f));         // ic_all_apps_dark
```

`navButtonsDarkIntensity` に値が入るのは `TaskbarEvent.NavButtonsDarkIntensityChanged` を
受け取ったときだけで、これは SystemUI から飛んでくる。バー型端末では SystemUI 側が
タスクバーの存在を知らない（ `sem_task_bar_available = 0` のまま ）ので**このイベントが一度も来ない**。
結果、フィールドは初期値の `0.0f` のままになり、`comp(0.0f) = 1.0f` で
白いレイヤーだけが不透明になる。実機ログにも `updateDarkIntensity:` は一度も出ていない。

なお、フローティング形状の分岐では同じ処理が `uiMode` を直接見ていて正しく切り替わる。
つまり通常形状のタスクバーだけが SystemUI 頼みになっている。

**対処**: 色を塗る側ではなく、渡す絵のほうを差し替えた。タスクバーの合成処理は
「 LayerDrawable でなければそのまま返す 」という早期 return を持っているので、
layer-list ではなく現在のモードに合う 1 枚（ 夜なら `ic_all_apps_light`、昼なら `ic_all_apps_dark` ）を
返せば、darkIntensity の計算ごと素通りする。`ic_all_apps` を参照しているのは
タスクバーのこのボタン 1 箇所だけなので、他への影響はない。

### タスクバーにアプリを追加できない

トーストは `exceed_hotseat_max_count`（ お気に入りとして追加するスペースがありません ）で、
これを出しているのは **`HotseatOnHomeDragOperator`**、つまりホーム画面側のドロップ処理だった。

```
HotseatPot@72235236: resolveDragOperator parentType = TASKBAR → HotseatOnTaskbarDragOperator
HotseatOnHomeDragOperator: handleDrop show maxCount toast        ← 実際に処理したのはこちら
HomeView: dispatchDragEvent ... fromHoney=WORKSPACE
```

タスクバー用の operator は正しく割り当たっているのに、受け取りを断って下のホーム画面に
すり抜けていた。その門番が `Rune.SUPPORT_EDIT_ON_TASKBAR` で、

```java
// HotseatOnTaskbarDragOperator
if (Rune.INSTANCE.getSUPPORT_EDIT_ON_TASKBAR() || taskbarUtil.getEditTaskbarHomeUpEnabled()) {
    // 通常のドラッグ処理
}
```

この定数は**ビルド時から false 固定**で、Fold でも false。本来は Good Lock の Home Up にある
「タスクバーを編集」で `editTaskbarHomeUpEnabled` 側を true にするのが Samsung の想定経路になっている。
参照している 10 箇所あまりがすべて `SUPPORT_EDIT_ON_TASKBAR || editTaskbarHomeUpEnabled` の形なので、
Rune 側を true にするのは Home Up と同じ分岐に乗るだけで済む。

**対処**: `Rune$Companion#getSUPPORT_EDIT_ON_TASKBAR()` を true にした。

### ドックの上限は 5 個で、ホーム画面と共有している（ 未対処 ）

上とは別に、**ドックに入る数そのもの**がバー型端末では 5 個に固定されている。

```java
// HotseatViewModel.K() — ドックの最大数
if (displayType == MAIN) {
    return (isHomeOnlySpace() || !coverMainSync) ? C() : getHotseatCount();
}
// C() = getHotseatCountForCover() ?: getHotseatCount()
```

バー型端末では `coverMainSync` が null なので `C()` に落ち、`hotseatCountForCover` の
**5** が返る（ `AbsDefaultPreferenceValue` で機種によらず 5 に設定されている ）。

タスクバーとホーム画面のドックは同じコンテナなので、この 5 枠は両者で共有される。
実機ログでも 3 つの別々のアプリをドックに入れようとしてすべて弾かれており、
単純に 5 枠が埋まっている状態だった。これは Fold でも同じ仕組みで、
タスクバー解放とは独立した One UI 本来の挙動なので、このモジュールでは触っていない。

上限を上げるなら `PreferenceDataSource#getHotseatCountForCover()` を null にして
`hotseatCount` 側へ落とす手があるが、**ホーム画面のドックの見た目も一緒に変わる**ため、
副作用の説明なしに入れるべきではないと判断した。

## 制限と既知のリスク

- One UI 8.5 / SM-S931Z の実機でタスクバーが出ることは確認済み（ ログに `rune=true` ）
- クラス名・メソッド名は One UI のバージョンで変わりうる。変わった場合はフックが黙って外れる
  （ 各フックは個別に try/catch していて、失敗は Xposed のログに出る ）
- フローティング形状のタスクバーと、最近アプリ画面でのタスクバー維持は対象外（ 上記 `ModelFeature` の項を参照 ）
- ドックの枠は 5 個のままで、ホーム画面のドックと共有している（ 上記の項を参照 ）
- アプリ一覧ボタンの色は、モード切り替え後にタスクバーが作り直されるまで反映されないことがある
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
