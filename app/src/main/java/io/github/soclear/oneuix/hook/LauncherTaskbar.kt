package io.github.soclear.oneuix.hook

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.soclear.oneuix.data.Package
import io.github.soclear.oneuix.hook.util.HookConfig
import io.github.soclear.oneuix.hook.util.afterAttach
import io.github.soclear.oneuix.hook.util.getHookConfig
import io.github.soclear.oneuix.hook.util.longVersionCode
import kotlinx.serialization.Serializable
import org.luckypray.dexkit.DexKitBridge
import java.io.File

/**
 * Fold / タブレット限定になっているタスクバー（ 画面下端からスワイプで出るドック ）をバー型端末でも使えるようにする。
 *
 * One UI 8 / 8.5 のランチャー（ honeyspace ）を逆コンパイルして特定した門番を開ける。
 * 詳しい根拠は docs/fold-taskbar-unlock.ja.md を参照。
 *
 * 大きく 2 段構えになっている。
 *
 * 1. そもそもタスクバーを作らせないフラグ群（ [forceRuneTaskbarSupport] ほか ）
 * 2. 作られたあとに中身が破綻する分（ [fixAllAppsButtonTint] / [useTaskbarAwareLayoutPlan] ）
 *
 * 端末種別（ ModelFeature ）は原則として触らない。あれを丸ごと偽装すると
 * ホーム画面のグリッド既定値まで変わってしまうため、
 * [useTaskbarAwareLayoutPlan] では**タスクバーのレイアウトを組む瞬間だけ**に限定して差し替えている。
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

    private const val MODEL_FEATURE_COMPANION_CLASS =
        $$"com.honeyspace.ui.common.ModelFeature$Companion"

    private const val PARENT_TYPE_CLASS = "com.honeyspace.ui.common.entity.ParentType"

    /** [PARENT_TYPE_CLASS] のうち、タスクバー扱いになる列挙子。`ParentType.isTaskBar()` と同じ判定。 */
    private val TASKBAR_PARENT_TYPES = setOf("TASKBAR", "DEXTASKBAR")

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
        allowEditOnTaskbar(loadPackageParam)
        fixAllAppsButtonTint(loadPackageParam)
        useTaskbarAwareLayoutPlan(loadPackageParam)
    }

    // ------------------------------------------------------------------
    // 1. タスクバーを作らせている門番
    // ------------------------------------------------------------------

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

    /**
     * タスクバーの上でアイコンを掴めるようにする。
     *
     * `Rune.SUPPORT_EDIT_ON_TASKBAR` はビルド時から `false` 固定で、
     * 参照側はどこも `SUPPORT_EDIT_ON_TASKBAR || TaskbarUtil.editTaskbarHomeUpEnabled` という形をしている。
     * つまり本来は Good Lock の Home Up にある「タスクバーを編集」でしか true にならない。
     *
     * これが false のあいだ、タスクバーのホットシートはドロップ先として振る舞わないので、
     * タスクバーの上に落としたつもりのアイコンは下のホーム画面にすり抜けてしまう。
     */
    private fun allowEditOnTaskbar(loadPackageParam: LoadPackageParam) {
        try {
            findAndHookMethod(
                RUNE_COMPANION_CLASS,
                loadPackageParam.classLoader,
                "getSUPPORT_EDIT_ON_TASKBAR",
                XC_MethodReplacement.returnConstant(true)
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    // ------------------------------------------------------------------
    // 2. 作られたあとに破綻する分の手当て
    // ------------------------------------------------------------------

    /** 一度引いたら変わらないので覚えておく。0 は「見つからなかった」の意味で使う。 */
    private var allAppsIconId = -1
    private var allAppsIconLightId = 0
    private var allAppsIconDarkId = 0

    /**
     * タスクバーのアプリ一覧ボタンがライトモードでも白いままになるのを直す。
     *
     * `ic_all_apps` は `ic_all_apps_light`（ 白 ）と `ic_all_apps_dark`（ 黒 ）を重ねた layer-list で、
     * どちらを見せるかはレイヤーの alpha で決まる。タスクバーはその alpha を
     * ナビゲーションバーの darkIntensity から計算しているが、この値が入るのは
     * `TaskbarEvent.NavButtonsDarkIntensityChanged` を受け取ったときだけ。
     * バー型端末では SystemUI 側がタスクバーの存在を知らないのでこのイベントが一度も来ず、
     * 既定値の 0.0f のまま固定される。0.0f は「白いレイヤーだけ不透明」を意味するので、
     * ライトモードでも白いアイコンが出続ける。
     *
     * タスクバー側の合成処理は「 LayerDrawable でなければそのまま返す 」という作りなので、
     * layer-list ではなく現在のモードに合う 1 枚だけを返してやれば素通りする。
     * `ic_all_apps` を参照しているのはタスクバーのこのボタンだけなので、影響範囲も閉じている。
     */
    private fun fixAllAppsButtonTint(loadPackageParam: LoadPackageParam) {
        try {
            findAndHookMethod(
                Resources::class.java,
                "getDrawable",
                Int::class.javaPrimitiveType,
                Resources.Theme::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val resources = param.thisObject as? Resources ?: return
                        val requested = param.args[0] as? Int ?: return
                        if (requested != layerListId(resources)) return

                        val night = resources.configuration.uiMode and
                                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                        val replacement = if (night) {
                            lightIconId(resources)
                        } else {
                            darkIconId(resources)
                        }
                        if (replacement != 0) {
                            param.args[0] = replacement
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    private fun layerListId(resources: Resources): Int {
        if (allAppsIconId == -1) {
            allAppsIconId = resources.getIdentifier("ic_all_apps", "drawable", Package.LAUNCHER)
        }
        // 見つからなかった場合の 0 は、どの実リソース ID とも一致しないので実害がない。
        return allAppsIconId
    }

    private fun lightIconId(resources: Resources): Int {
        if (allAppsIconLightId == 0) {
            allAppsIconLightId =
                resources.getIdentifier("ic_all_apps_light", "drawable", Package.LAUNCHER)
        }
        return allAppsIconLightId
    }

    private fun darkIconId(resources: Resources): Int {
        if (allAppsIconDarkId == 0) {
            allAppsIconDarkId =
                resources.getIdentifier("ic_all_apps_dark", "drawable", Package.LAUNCHER)
        }
        return allAppsIconDarkId
    }

    // ------------------------------------------------------------------
    // タスクバーのレイアウト寸法
    // ------------------------------------------------------------------

    @Serializable
    data class TaskbarHookConfig(
        override val versionCode: Long,
        /** ホットシートのレイアウト計画クラス（ 難読化されているので毎バージョン探し直す ）。 */
        val layoutPlanClass: String,
    ) : HookConfig

    /**
     * レイアウト計画を組んでいる最中かどうか。コンストラクタの中だけで見たいので ThreadLocal。
     * ホットシートの生成は UI スレッドなので、これで十分に閉じる。
     */
    private val buildingTaskbarLayout = ThreadLocal.withInitial { false }

    /**
     * タスクバーのアイコンサイズが 0 になり、中身が見えず何も置けなくなるのを直す。
     *
     * ホットシートの寸法計算は端末種別で 3 つのクラスに分かれていて、
     * タスクバーの寸法を計算するコードは**タブレット系と Fold 系にしか書かれていない**。
     * バー型端末が使うクラスにはそもそも実装が無く、アイコンサイズは初期値の 0 のままになる。
     *
     * ```java
     * if (isTabletModel() || isDexSpace || (isMultiFoldModel() && (isTaskBar || isMainState))) {
     *     nVar = new v(...);       // タブレット系。狭い画面向けの分岐も持っている
     * } else if (isFoldModel() && (isTaskBar || isMainState)) {
     *     nVar = new b(...);       // Fold 系
     * } else {
     *     nVar = new a(...);       // バー型。タスクバーの寸法計算が無い
     * }
     * ```
     *
     * そこで**タスクバーのレイアウト計画を組む瞬間に限って** `isMultiFoldModel()` を true に見せ、
     * タブレット系の計算を使わせる。ホーム画面側のホットシートは `ParentType` が違うので影響を受けず、
     * `ModelFeature` を恒久的に書き換えないのでホーム画面のグリッド既定値も変わらない。
     *
     * 対象クラスは難読化されているため DexKit で探すが、コンストラクタの引数型は
     * すべて難読化されていないので一意に特定できる。結果は JSON にキャッシュされ、
     * ランチャーの更新後の初回起動だけ探し直す。
     */
    private fun useTaskbarAwareLayoutPlan(loadPackageParam: LoadPackageParam) {
        afterAttach {
            try {
                val config = getHookConfig(File(filesDir, "TaskbarHookConfig.json")) {
                    findLayoutPlanClass()
                } ?: return@afterAttach

                hookLayoutPlanConstructors(config.layoutPlanClass, loadPackageParam.classLoader)
                forceMultiFoldWhileBuildingTaskbar(loadPackageParam.classLoader)
            } catch (t: Throwable) {
                XposedBridge.log(t)
            }
        }
    }

    /**
     * レイアウト計画クラスを DexKit で探す。
     * 引数の並びが特徴的なので、コンストラクタの型だけで絞り込める。
     */
    private fun Context.findLayoutPlanClass(): TaskbarHookConfig? {
        System.loadLibrary("dexkit")
        DexKitBridge.create(classLoader, true).use { bridge ->
            val constructor = bridge.findMethod {
                matcher {
                    name = "<init>"
                    paramTypes(
                        "android.content.Context",
                        "int",
                        "android.graphics.Point",
                        "android.graphics.Point",
                        "com.honeyspace.common.interfaces.CombinedDexInfo",
                        PARENT_TYPE_CLASS,
                        "com.honeyspace.sdk.source.DeviceStatusSource",
                        "boolean",
                        "boolean",
                        "int",
                        "boolean",
                        "float",
                        // CommonSettingsDataSource$ItemSizeLevel。入れ子の列挙なので型名は指定しない。
                        null,
                        "boolean",
                        "com.honeyspace.common.interfaces.CoverSyncHelper",
                        "com.honeyspace.sdk.source.entity.UpdateWorkspaceItemStyleData",
                    )
                }
            }.singleOrNull() ?: return null

            return TaskbarHookConfig(
                versionCode = longVersionCode,
                layoutPlanClass = constructor.declaredClassName,
            )
        }
    }

    /** 対象クラスのコンストラクタに入っているあいだだけ目印を立てる。 */
    private fun hookLayoutPlanConstructors(className: String, classLoader: ClassLoader) {
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (isTaskbarLayout(param.args)) {
                    buildingTaskbarLayout.set(true)
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                // 例外で抜けた場合もここは通るので、立てっぱなしにはならない。
                buildingTaskbarLayout.set(false)
            }
        }

        findClass(className, classLoader).declaredConstructors.forEach { constructor ->
            try {
                XposedBridge.hookMethod(constructor, callback)
            } catch (t: Throwable) {
                XposedBridge.log(t)
            }
        }
    }

    /**
     * 引数の中から `ParentType` を型で拾って、タスクバー用かどうかを見る。
     * 位置で拾うと引数が 1 つ増えただけで壊れるので、型で探す。
     */
    private fun isTaskbarLayout(args: Array<Any?>): Boolean {
        val parentType = args.firstOrNull { it?.javaClass?.name == PARENT_TYPE_CLASS }
        return (parentType as? Enum<*>)?.name in TASKBAR_PARENT_TYPES
    }

    /** 目印が立っているあいだだけ、この端末を MultiFold だと答える。 */
    private fun forceMultiFoldWhileBuildingTaskbar(classLoader: ClassLoader) {
        try {
            findAndHookMethod(
                MODEL_FEATURE_COMPANION_CLASS,
                classLoader,
                "isMultiFoldModel",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (buildingTaskbarLayout.get()) {
                            param.result = true
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }
}
