package io.github.soclear.oneuix.hook

import android.content.Context
import android.os.Build
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.soclear.oneuix.data.Package
import io.github.soclear.oneuix.hook.util.afterAttach
import io.github.soclear.oneuix.hook.util.longVersionCode
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Modifier

/**
 * Fold / タブレット限定になっているタスクバー（画面下端からスワイプで出るドック）関連。
 *
 * One UI 8 系のランチャーで**どこが門番なのかがまだ特定できていない**ため、
 * この機能は 2 つの仕事をする。
 *
 * 1. 「対応している」系のフラグを片っ端から true にする（当たれば解放される）
 * 2. **タスクバー関連の実名を Xposed のログに吐き出す**（当たらなかったときの手がかり）
 *
 * 2 が本体で、ここで得た実名を使って決め打ちのフックに書き直すのが次の工程。
 * ログの読み方は docs/fold-taskbar-unlock.ja.md を参照。
 */
object LauncherTaskbar {
    private const val LOG_PREFIX = "OneUIX taskbar|"

    /** タスクバーの対応可否を持っていそうなフィーチャーフラグ置き場。 */
    private val FEATURE_CLASSES = listOf(
        "com.honeyspace.sdk.Rune",
        "com.samsung.android.rune.CoreRune",
    )

    private const val SEM_FLOATING_FEATURE_CLASS =
        "com.samsung.android.feature.SemFloatingFeature"

    private val TASKBAR_NAME_REGEX = Regex("TASK_?BAR", RegexOption.IGNORE_CASE)

    /** 「対応している」側のフラグだけを対象にする。ユーザー設定の ON/OFF は含めない。 */
    private val CAPABILITY_NAME_REGEX = Regex("SUPPORT|AVAILABLE|ALLOW", RegexOption.IGNORE_CASE)

    /** 意味が反転しているフラグ（ 未対応・禁止・非表示 ）を誤って true にしないための除外。 */
    private val NEGATIVE_NAME_REGEX =
        Regex("NOT|UNSUPPORT|DISABLE|BLOCK|HIDE|REMOVE|RESTRICT", RegexOption.IGNORE_CASE)

    /** 診断で拾う範囲。端末種別による分岐も一緒に見たいので広めに取る。 */
    private val DIAGNOSTIC_NAME_REGEX =
        Regex("TASK_?BAR|DOCK|FOLD|TABLET|FLIP|DEX", RegexOption.IGNORE_CASE)

    /** ログが流れすぎないように 1 セクションあたりの行数を絞る。 */
    private const val MAX_LINES_PER_SECTION = 60

    fun unlockFoldTaskbar(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.LAUNCHER) return

        afterAttach {
            val applied = FEATURE_CLASSES.flatMap { className ->
                forceTaskbarCapability(className, loadPackageParam.classLoader)
            }

            forceTaskbarFloatingFeature(loadPackageParam)

            if (applied.isEmpty()) {
                log("applied: none (対応フラグが 1 つも見つからなかった)")
            } else {
                log("applied: ${applied.joinToString()}")
            }

            dumpDiagnostics(this, loadPackageParam.classLoader)
        }
    }

    private fun log(message: String) = XposedBridge.log("$LOG_PREFIX $message")

    // ------------------------------------------------------------------
    // 解放の試行
    // ------------------------------------------------------------------

    private fun isTaskbarCapabilityName(name: String): Boolean =
        TASKBAR_NAME_REGEX.containsMatchIn(name) &&
                CAPABILITY_NAME_REGEX.containsMatchIn(name) &&
                !NEGATIVE_NAME_REGEX.containsMatchIn(name)

    /**
     * [className] の中からタスクバー対応フラグを探して true に固定し、
     * 実際に書き換えたメンバー名を返す。
     */
    private fun forceTaskbarCapability(className: String, classLoader: ClassLoader): List<String> {
        // フラグは静的初期化子で端末種別から計算されるので、
        // 先に <clinit> を走らせてから上書きしないと計算結果で戻される。
        val clazz = try {
            Class.forName(className, true, classLoader)
        } catch (_: Throwable) {
            return emptyList()
        }

        val applied = mutableListOf<String>()

        clazz.declaredFields.forEach { field ->
            if (!Modifier.isStatic(field.modifiers)) return@forEach
            if (field.type != Boolean::class.javaPrimitiveType) return@forEach
            if (!isTaskbarCapabilityName(field.name)) return@forEach
            try {
                field.isAccessible = true
                if (field.getBoolean(null)) return@forEach
                field.setBoolean(null, true)
                applied += "${clazz.simpleName}.${field.name}"
            } catch (t: Throwable) {
                // val （ final ）で書き換えられないものはゲッター側のフックに任せる。
                XposedBridge.log(t)
            }
        }

        // Kotlin の val はフィールドが private final になり、公開されるのはゲッターだけ。
        clazz.declaredMethods.forEach { method ->
            if (method.parameterTypes.isNotEmpty()) return@forEach
            if (method.returnType != Boolean::class.javaPrimitiveType) return@forEach
            if (!isTaskbarCapabilityName(method.name)) return@forEach
            try {
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                applied += "${clazz.simpleName}#${method.name}()"
            } catch (t: Throwable) {
                XposedBridge.log(t)
            }
        }

        return applied
    }

    /** CSC の隠しフィーチャー側でも弾かれることがあるので、タスクバー関連のキーだけ通す。 */
    private fun forceTaskbarFloatingFeature(loadPackageParam: LoadPackageParam) {
        val callback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val key = param.args.firstOrNull() as? String ?: return
                if (isTaskbarCapabilityName(key)) {
                    param.result = true
                }
            }
        }

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

    // ------------------------------------------------------------------
    // 診断ダンプ
    // ------------------------------------------------------------------

    /**
     * タスクバーがどこで止められているのかを突き止めるための材料をログに出す。
     * 決め打ちのフックに書き直せたら、この呼び出しごと消す。
     */
    private fun dumpDiagnostics(context: Context, classLoader: ClassLoader) {
        log("---- diagnostics begin ----")
        try {
            dumpDeviceInfo(context)
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }

        FEATURE_CLASSES.forEach { className ->
            try {
                dumpBooleanMembers(className, classLoader)
            } catch (t: Throwable) {
                XposedBridge.log(t)
            }
        }

        try {
            dumpDexEntries(classLoader)
        } catch (t: Throwable) {
            // DexKit が失敗してもフラグ側の情報は残したいので、握って続行する。
            log("dexkit: failed (${t.javaClass.simpleName}: ${t.message})")
        }
        log("---- diagnostics end ----")
    }

    /**
     * 端末の素性。One UI のタスクバーは boolean フラグではなく
     * 画面の最小幅（ sw600dp ）で分かれている可能性があるので、必ず一緒に見る。
     */
    private fun dumpDeviceInfo(context: Context) {
        val configuration = context.resources.configuration
        log("device: ${Build.MODEL} / sdk ${Build.VERSION.SDK_INT} / launcher ${context.longVersionCode}")
        log(
            "screen: smallestScreenWidthDp=${configuration.smallestScreenWidthDp}" +
                    " screenWidthDp=${configuration.screenWidthDp}" +
                    " screenHeightDp=${configuration.screenHeightDp}" +
                    " densityDpi=${configuration.densityDpi}"
        )
    }

    /** フィーチャーフラグ置き場の boolean を、現在値つきで一覧にする。 */
    private fun dumpBooleanMembers(className: String, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName(className, true, classLoader)
        } catch (_: Throwable) {
            log("$className: not found")
            return
        }

        // Rune のように小さいクラスは全部、CoreRune のように巨大なものは関連しそうな名前だけ。
        val fields = clazz.declaredFields
            .filter { Modifier.isStatic(it.modifiers) }
            .filter { it.type == Boolean::class.javaPrimitiveType }
        val interesting = fields
            .filter { fields.size <= MAX_LINES_PER_SECTION || DIAGNOSTIC_NAME_REGEX.containsMatchIn(it.name) }
            .take(MAX_LINES_PER_SECTION)

        log("$className: ${fields.size} boolean fields, showing ${interesting.size}")
        interesting.forEach { field ->
            val value = try {
                field.isAccessible = true
                field.getBoolean(null).toString()
            } catch (t: Throwable) {
                "<${t.javaClass.simpleName}>"
            }
            log("  ${field.name} = $value")
        }

        clazz.declaredMethods
            .filter { it.parameterTypes.isEmpty() }
            .filter { it.returnType == Boolean::class.javaPrimitiveType }
            .filter { DIAGNOSTIC_NAME_REGEX.containsMatchIn(it.name) }
            .take(MAX_LINES_PER_SECTION)
            .forEach { log("  ${it.name}() -> boolean") }
    }

    /**
     * ランチャーの dex からタスクバー関連の実名を拾う。
     * 名前を推測するのをやめるための材料なので、クラス名とメソッド名だけ出せば足りる。
     */
    private fun dumpDexEntries(classLoader: ClassLoader) {
        System.loadLibrary("dexkit")
        DexKitBridge.create(classLoader, true).use { bridge ->
            val classes = bridge.findClass {
                matcher { className("taskbar", StringMatchType.Contains, true) }
            }
            log("dexkit classes matching \"taskbar\": ${classes.size}")
            classes.take(MAX_LINES_PER_SECTION).forEach { log("  ${it.name}") }

            val methods = bridge.findMethod {
                matcher {
                    name("taskbar", StringMatchType.Contains, true)
                    returnType = "boolean"
                }
            }
            log("dexkit boolean methods matching \"taskbar\": ${methods.size}")
            methods.take(MAX_LINES_PER_SECTION).forEach {
                log("  ${it.toDexMethod().serialize()}")
            }
        }
    }
}
