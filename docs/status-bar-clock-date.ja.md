# ステータスバー時計への日付併記（日付のみ別フォントサイズ）

One UI 8.5 / Android 16 向け。OneUIX の既存機能「ステータスバーの日時フォーマットを設定」を拡張し、
**時刻部分のフォントサイズはそのままで、日付部分だけ任意の倍率に縮小・拡大**できるようにしたものです。

`HH:mm EEEE` を 1 本のパターンで指定すると全体が同じ字号になってしまうため、
時刻と日付を別々のパターンとして持ち、日付側にだけ `RelativeSizeSpan` を掛ける実装にしています。

## 設定場所

モジュール本体アプリ → **System UI → ステータスバー**

| 設定 | 既定値 | 説明 |
|---|---|---|
| ステータスバーの日時フォーマットを設定 | OFF / `HH:mm` | **時刻部分**のパターン。この機能を ON にするのが前提 |
| ステータスバーの時計に日付を併記 | OFF | 日付併記のマスタースイッチ |
| 日付パターン | `EEEE` | `E` → 金、`EEEE` → 金曜日、`M/d(E)` → 8/14(金) など自由入力 |
| 時刻と日付の区切り文字 | 半角スペース | ` · `、`  `（全角スペース）、空文字なども可 |
| 日付の言語 | 空欄 | 空欄でシステム言語。`ja` / `en-US` などの BCP 47 タグで固定も可能 |
| 日付を時刻の前に表示 | OFF | ON で `金曜日 12:34` の並びになる |
| 日付のフォントサイズ | `0.75x` | 時刻に対する倍率。0.30x〜1.50x |

入力欄の「確定」を押すと、ラベル部分に**その場でプレビュー**が出ます。
パターンが不正な場合は `error` と表示され、その値は保存されません。

設定変更後は **SystemUI の再起動**（または再起動）で反映されます。

## `HH:mm EEEE` を再現する設定

- 時刻パターン: `HH:mm`
- 日付併記: ON
- 日付パターン: `EEEE`
- 区切り文字: 半角スペース 1 個
- 日付のフォントサイズ: 好みの倍率（`0.70`〜`0.80` あたりが収まりが良い）

## 実装

### 追加ファイル

- `app/src/main/java/io/github/soclear/oneuix/hook/util/ClockTextFormatter.kt`

  時刻・日付それぞれの `DateTimeFormatter` を保持し、`CharSequence` を組み立てるクラス。

  - 日付部分（＋区切り文字）にのみ `RelativeSizeSpan(scale)` を
    `SPAN_EXCLUSIVE_EXCLUSIVE` で適用。時刻部分には一切 span を付けないので、
    システム側の時計字号・`textAppearance` はそのまま活きます。
  - 区切り文字を日付と同じ span に含めているため、日付を小さくすると
    時刻との間隔も等比で詰まり、見た目が破綻しません。
  - 倍率が `1.0` のときは span を張らず素の `String` を返す（無駄なオーバーヘッド回避）。
  - `DateTimeFormatter` はインスタンス内にキャッシュし、
    **システム言語が変わったときだけ**作り直します（毎分パースし直さない）。
  - 不正なパターン・不正なロケールはすべて既定値へフォールバックし、
    ステータスバーが空になったり SystemUI が落ちたりしないようにしています。

### 変更ファイル

- `hook/systemui/StatusBar.kt`

  `setStatusBarClockFormat` を時刻／日付の 2 パターン受け取りに変更。
  内部の `setStatusBarClockText` は `() -> String` から `() -> CharSequence` になり、
  span 付きテキストをそのまま `TextView.text` へ渡します。
  `contentDescription` には span を含まない `toString()` を設定（読み上げ対策）。
  テキスト生成中に例外が出てもフックが SystemUI を巻き込まないよう `try/catch` を追加しています。

  フック対象は既存実装と同じ
  `com.android.systemui.statusbar.policy.QSClockIndicatorView#notifyTimeChanged` で、
  これは One UI 8.5 系のクラス構成に対応した OneUIX 側の既存ロジックをそのまま使っています。

- `data/Preference.kt` — 設定項目 6 個を追加（`kotlinx.serialization` のデフォルト値付きなので、
  既存の `preference.json` をそのまま読み込めます＝**後方互換あり**）
- `ui/category/DetailPaneSystemUI.kt` — 設定 UI、イベント、リデューサ
- `hook/Main.kt` — 新シグネチャへの配線
- `res/values/strings.xml`, `values-zh`, **`values-ja`（新規）** — 文言
- `app/build.gradle.kts` — `localeFilters` に `ja` を追加

## 既知の注意点

- **横幅**: `EEEE`（金曜日）＋長いキャリア名＋通知アイコン多数が重なると、
  ステータスバーの表示領域が足りず切れることがあります。
  その場合は `E` に変える、倍率を下げる、OneUIX の
  「ステータスバーの左右パディングを変更」で余白を詰める、のいずれかで回避できます。
- **更新タイミング**: 日付は時計と同じタイミング（既定では毎分）で再描画されます。
  「ステータスバーの時計を毎秒更新」を併用しても問題ありません。
- 対象は**ステータスバーの時計**のみです。ロック画面時計・AOD・クイック設定の時計は別フックのため変わりません。

## 動作確認

`ClockTextFormatter` のロジックは Android スタブを用いた 25 項目のチェックで検証済みです
（文言・span 範囲・ロケール切替時のキャッシュ再構築・不正入力時のフォールバックなど）。
ビルド用の Android SDK / Maven リポジトリがこの作業環境から参照できなかったため、
APK のコンパイルは各自の環境で行ってください。

### ビルド方法

このリポジトリには既に `.github/workflows/ci.yml`（push で debug APK を自動ビルド）が入っています。

```bash
# 方法 A: GitHub にフォークして push すると Actions が APK を生成
git push origin main   # → Actions の artifact から APK をダウンロード

# 方法 B: ローカル（JDK 25 / Android SDK 37 が必要）
./gradlew assembleDebug
# 出力: app/build/outputs/apk/debug/app-debug.apk
```
