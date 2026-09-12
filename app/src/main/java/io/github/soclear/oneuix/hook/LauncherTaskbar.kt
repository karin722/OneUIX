package io.github.soclear.oneuix.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.soclear.oneuix.data.Package

/**
 * Fold / タブレット限定になっているタスクバー（ 画面下端からスワイプで出るドック ）をバー型端末でも使えるようにする。
 *
 * One UI 8 / 8.5 のランチャー（ honeyspace ）を逆コンパイルして特定した、実在する 3 つの門番を開ける。
 * 詳しい根拠は docs/fold-taskbar-unlock.ja.md を参照。
 *
 * 1. [com.honeyspace.common.Rune] の `HOME_SUPPORT_TASKBAR`（ 大元 ）
 * 2. [com.honeyspace.ui.common.taskbar.TaskbarControllerImpl] の `_taskbarAvailable`
 * 3. [com.honeyspace.ui.common.util.TaskbarUtilImpl] の `taskbarEnabled`（ 設定の ON/OFF ）
 *
 * 端末の種別そのもの（ ModelFeature.isTabletModel など ）はあえて触らない。
 * あれを偽装するとレイアウト全体がタブレット扱いになって副作用が大きく、
 * 通常のタスクバーを出すだけならここまでで足りるため。
 */
object LauncherTaskbar {
    /**
     * Kotlin の `Rune.HOME_SUPPORT_TASKBAR` は companion のゲッター呼び出しにコンパイルされるので、
     * 参照側はすべてここを通る。値の出どころである静的フィールドではなくゲッターを差し替えるのは、
     * フィールドが static final で書き換えられないのと、`<clinit>` の実行タイミングに左右されないため。
     */
    private const val RUNE_COMPANION_CLASS = $$"com.honeyspace.common.Rune$Companion"

    private const val TASKBAR_CONTROLLER_IMPL_CLASS =
        "com.honeyspace.ui.common.taskbar.TaskbarControllerImpl"

    private const val TASKBAR_UTIL_IMPL_CLASS = "com.honeyspace.ui.common.util.TaskbarUtilImpl"

    private const val SEM_FLOATING_FEATURE_CLASS =
        "com.samsung.android.feature.SemFloatingFeature"

    /**
     * `Rune.HOME_SUPPORT_TASKBAR` の唯一の入力。
     * バー型端末では /system/etc/floating_feature.xml にこのキー自体が無く、
     * 存在しないキーの `getBoolean` は false を返すので、そこでタスクバーが閉じられている。
     */
    private const val TASKBAR_FLOATING_FEATURE_KEY =
        "SEC_FLOATING_FEATURE_LAUNCHER_SUPPORT_TASKBAR"

    fun unlockFoldTaskbar(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.LAUNCHER) return

        // Rune の <clinit> がフィーチャーを読む前に仕込みたいので、これを最初に呼ぶ。
        forceTaskbarFloatingFeature(loadPackageParam)
        forceRuneTaskbarSupport(loadPackageParam)
        forceTaskbarAvailable(loadPackageParam)
        forceTaskbarEnabled(loadPackageParam)
    }

    /**
     * 門番 1: `Rune.HOME_SUPPORT_TASKBAR`。
     *
     * ここが false だとタスクバーは存在ごと消える。具体的には
     * `TaskbarControllerImpl` のコンストラクタがイベント配線をまるごと飛ばし、
     * `initialize()` と `TaskbarVisibilityController.init()` が即 return し、
     * `TaskbarControllerProxyImpl.getTaskbarController()` が null を返してジェスチャも死ぬ。
     */
    private fun forceRuneTaskbarSupport(loadPackageParam: LoadPackageParam) {
        try {
            findAndHookMethod(
                RUNE_COMPANION_CLASS,
                loadPackageParam.classLoader,
                "getHOME_SUPPORT_TASKBAR",
                XC_MethodReplacement.returnConstant(true)
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    /**
     * 門番 1 の裏口。ゲッターだけでなく静的フィールドの値そのものも true にしておく。
     * 同じモジュール内からフィールドが直接読まれた場合の取りこぼしを防ぐための保険。
     */
    private fun forceTaskbarFloatingFeature(loadPackageParam: LoadPackageParam) {
        val callback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.args.firstOrNull() == TASKBAR_FLOATING_FEATURE_KEY) {
                    param.result = true
                }
            }
        }

        // getBoolean(String) と getBoolean(String, boolean) の両方がある。
        try {
            findAndHookMethod(
                SEM_FLOATING_FEATURE_CLASS,
                loadPackageParam.classLoader,
                "getBoolean",
                String::class.java,
                callback
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }

        try {
            findAndHookMethod(
                SEM_FLOATING_FEATURE_CLASS,
                loadPackageParam.classLoader,
                "getBoolean",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                callback
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    /**
     * 門番 2: `TaskbarControllerImpl._taskbarAvailable`。
     *
     * 初期値は Settings.Global の `sem_task_bar_available` から来ていて、
     * バー型端末では 0 が入っている。これが false のままだと
     * `TaskbarStyleInfo.isTaskbar` が false になり、タスクバーのビューが作られない。
     *
     * 値の流れは `_taskbarAvailable` → `getTaskbarStyleInfo(available, docked, hide)` の一本道なので、
     * 合流点であるこのメソッドの第 1 引数を true に差し替えるのが最小の介入になる。
     * 設定値そのものを書き換えないので、モジュールを切れば元に戻る。
     */
    private fun forceTaskbarAvailable(loadPackageParam: LoadPackageParam) {
        try {
            findAndHookMethod(
                TASKBAR_CONTROLLER_IMPL_CLASS,
                loadPackageParam.classLoader,
                "getTaskbarStyleInfo",
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[0] = true
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    /**
     * 門番 3: `TaskbarUtilImpl.taskbarEnabled`。
     *
     * 実体は `Rune.HOME_SUPPORT_TASKBAR && Settings.Global の task_bar == 1` で、
     * バー型端末では設定項目自体が作られないので既定値の 0 に落ちる。
     * ここはユーザー向けの ON/OFF なので、モジュールのスイッチで代替する形にして true に固定する。
     */
    private fun forceTaskbarEnabled(loadPackageParam: LoadPackageParam) {
        try {
            findAndHookMethod(
                TASKBAR_UTIL_IMPL_CLASS,
                loadPackageParam.classLoader,
                "getTaskbarEnabled",
                XC_MethodReplacement.returnConstant(true)
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }
}
