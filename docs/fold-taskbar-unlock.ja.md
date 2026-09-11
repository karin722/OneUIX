# Fold 専用タスクバー（ドック）の解放

One UI が Fold・タブレットにしか出さない画面下端のタスクバー（ドック）を、
バー型端末のランチャーでも選べるようにする機能です。

「使えるようにする」までがこの機能の仕事で、表示の ON/OFF 自体は解放後に
**ホーム画面設定 → タスクバー** でユーザーが切り替えます。

## 設定場所

モジュール本体アプリ → **One UI ホーム（ランチャー）→ Fold 専用のタスクバー（ドック）を解放**

ON にしたあとはランチャー（ `com.sec.android.app.launcher` ）を再起動してください。
LSPosed のスコープにランチャーが入っていないと効きません。

## 実装

`hook/Launcher.kt` の `unlockFoldTaskbar()` 。ランチャープロセスでのみ動きます。

### なぜフラグ名を決め打ちしないのか

ランチャー（ honeyspace ）は「この端末はタスクバーに対応しているか」を
`com.honeyspace.sdk.Rune` などのフィーチャーフラグで判定していますが、
その**メンバー名は One UI のバージョンで揺れます**。
1 つの名前を決め打ちすると、別バージョンでは黙って何もしないフックになってしまいます。

そこで名前ではなく**意味**で拾う方針にしました。

| 条件 | 正規表現 | 意図 |
|---|---|---|
| 対象 | `TASK_?BAR` | タスクバー関連のメンバーだけに絞る |
| 対象 | `SUPPORT` / `AVAILABLE` / `ALLOW` | 「対応・利用可否」を表すものだけ |
| 除外 | `NOT` / `UNSUPPORT` / `DISABLE` / `BLOCK` / `HIDE` / `REMOVE` / `RESTRICT` | 意味が反転しているフラグを誤って true にしない |

`ENABLED` 系を対象に含めていないのは、それが**ユーザー自身の ON/OFF 設定**である
可能性が高く、true 固定にすると本人がタスクバーを消せなくなるためです。
解放するのは「対応しているか」だけに留めています。

### 書き換える場所

1. **静的ブーリアンフィールド** — `com.honeyspace.sdk.Rune` と、
   ランチャープロセス内の `com.samsung.android.rune.CoreRune` 。
2. **引数なしの boolean ゲッター** — Kotlin の `val` はフィールドが private final になり、
   公開されるのはゲッターだけなので、こちらもフックして `true` を返させます。
3. **`SemFloatingFeature.getBoolean()`** — CSC 側の隠しフィーチャーで弾かれる場合に備えて、
   上と同じ条件に合致するキーだけ `true` を返します。

フラグは静的初期化子で端末種別から計算されるため、
`Class.forName(name, true, classLoader)` で **`<clinit>` を先に走らせてから**上書きします。
先に書き換えると、あとから走る初期化で計算結果に戻されてしまいます。

適用タイミングは `afterAttach`（ `Application.attach` 直後 ）。
ランチャーの UI が組み立てられる前で、かつクラスがロードできる状態です。

### 効いたかどうかの確認

書き換えたメンバー名を Xposed のログに 1 行だけ出します。

```
OneUIX unlockFoldTaskbar: Rune.SUPPORT_TASKBAR, CoreRune.SUPPORT_TASK_BAR
```

1 つも該当しなかった場合はこうなります。フラグ名が変わったか、
すでに対応端末だったかのどちらかです。

```
OneUIX unlockFoldTaskbar: no taskbar capability member matched
```

## 注意

- Fold・タブレット向けに作られた UI をバー型端末で動かすので、
  タスクバーの高さや余白が画面幅に対して不自然になることがあります。
- One UI のバージョンによっては、解放してもホーム画面設定に項目が出ないことがあります。
  その場合は上記のログを添えて報告してください。
