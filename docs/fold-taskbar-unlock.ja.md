# Fold 専用タスクバー（ドック）の解放 — 現状と調査手順

One UI が Fold・タブレットにしか出さない「画面下端からスワイプすると出てくるドック
（タスクバー）」を、バー型端末でも使えるようにするための機能です。

**重要：One UI 8 系ではまだ完成していません。** 実機で ON にしてもタスクバーは出ません。
どこが門番なのかを特定できていないためで、現在この機能は
**解放の試行 ＋ 門番を突き止めるための診断ログ出力**という位置づけです。

## 何が分かっていないのか

最初の実装は「`TASKBAR` を含み、かつ `SUPPORT` / `AVAILABLE` / `ALLOW` を含む boolean を
true にする」という、**フラグ名を推測で当てにいったもの**でした。実機では何も起きなかったので、
この推測は外れています。考えられる原因は次のとおりです。

- フラグ名が推測と違う
- そもそも boolean フラグではなく、**画面の最小幅（ `sw600dp` ）やデバイスプロファイル**で
  分岐している（One UI のタスクバーはタブレット扱いの画面で出るため、こちらの可能性が高い）
- 表示可否とスワイプ判定（ジェスチャ登録）が別のゲートになっている

推測を重ねても当たらないので、**実名をログに出させて事実から決める**方針に切り替えました。

## 使い方（調査に協力する場合）

1. モジュール本体アプリ → **One UI ホーム（ランチャー）→ Fold 専用のタスクバー（ドック）を解放** を ON
2. LSPosed のスコープに One UI ホーム（ `com.sec.android.app.launcher` ）が入っていることを確認
3. ランチャーを再起動（ホーム画面に戻って別アプリを開き直す、または端末を再起動）
4. LSPosed マネージャー → ログ、または `adb logcat -d | grep "OneUIX taskbar|"` でログを取得
5. `---- diagnostics begin ----` から `---- diagnostics end ----` までを開発者に渡す

診断のために DexKit でランチャーの dex を走査するので、**ランチャーの起動が数秒遅くなることがあります**。

## ログの読み方

```
OneUIX taskbar| applied: none (対応フラグが 1 つも見つからなかった)
OneUIX taskbar| ---- diagnostics begin ----
OneUIX taskbar| device: SM-XXXX / sdk 36 / launcher 1400000000
OneUIX taskbar| screen: smallestScreenWidthDp=411 screenWidthDp=411 ... densityDpi=450
OneUIX taskbar| com.honeyspace.sdk.Rune: 42 boolean fields, showing 42
OneUIX taskbar|   SUPPORT_XXX = false
OneUIX taskbar| dexkit classes matching "taskbar": 37
OneUIX taskbar|   com.honeyspace.ui.honeypots.taskbar.XXX
OneUIX taskbar| ---- diagnostics end ----
```

| 行 | 何が分かるか |
|---|---|
| `applied:` | `none` なら対応フラグが 1 つも存在しない＝ゲートはフラグではない可能性が高い |
| `screen:` | `smallestScreenWidthDp` が 600 未満なら、タブレット判定で弾かれている線が濃い |
| `Rune:` の一覧 | タスクバー関連のフラグが**実在するか、実名は何か、現在値は何か** |
| `dexkit classes` | タスクバー実装のクラス名。ここからフックすべき実体を決める |

## 実装

`app/src/main/java/io/github/soclear/oneuix/hook/LauncherTaskbar.kt` 。ランチャープロセスでのみ動きます。

- `forceTaskbarCapability()` — `com.honeyspace.sdk.Rune` と（ランチャープロセス内の）
  `com.samsung.android.rune.CoreRune` について、静的 boolean フィールドと無引数 boolean ゲッターを
  走査し、「対応・利用可否」を意味する名前のものだけ true に固定する。
  フラグは静的初期化子で端末種別から計算されるため、
  `Class.forName(name, true, classLoader)` で `<clinit>` を先に走らせてから上書きする。
- `forceTaskbarFloatingFeature()` — `SemFloatingFeature.getBoolean()` の同条件のキーを true にする。
- `dumpDiagnostics()` — 端末情報・フラグ一覧・DexKit で拾ったタスクバー関連の実名をログに出す。

`ENABLED` 系を対象に含めていないのは、それが**ユーザー自身の ON/OFF 設定**である可能性が高く、
true 固定にすると本人がタスクバーを消せなくなるためです。

## 次の工程

診断ログで実名が判明したら、

1. 総当たりのヒューリスティックを**実名を決め打ちしたフック**に置き換える
2. `dumpDiagnostics()` の呼び出しを削除する（起動が遅くなる原因なので残さない）
3. このドキュメントを「動く機能」の説明に書き直す

画面の最小幅が原因だった場合は `smallestScreenWidthDp` をランチャープロセス内だけ大きく見せる
方向になりますが、**レイアウト全体がタブレット扱いになって副作用が出やすい**ので、
タスクバーの生成経路だけに効く最小のフックを探す必要があります。
