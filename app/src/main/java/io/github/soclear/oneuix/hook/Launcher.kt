package io.github.soclear.oneuix.hook

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findAndHookConstructor
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.soclear.oneuix.data.Package
import io.github.soclear.oneuix.hook.util.afterAttach
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import kotlin.math.roundToInt


object Launcher {
    fun showMemoryUsageInRecents(loadPackageParam: LoadPackageParam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            showMemoryUsageInRecentsTargetSdk36(loadPackageParam)
        } else {
            showMemoryUsageInRecentsTargetSdk35(loadPackageParam)
        }
    }

    fun showMemoryUsageInRecentsTargetSdk36(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.LAUNCHER ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA
        ) return

        var memoryTextView: TextView? = null
        var activityManager: ActivityManager? = null
        var activityRef: WeakReference<Activity>? = null

        findAndHookMethod(
            "com.android.quickstep.RecentsActivity",
            loadPackageParam.classLoader,
            "onCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    if (activityManager == null) {
                        activityManager = activity.getSystemService(ActivityManager::class.java)
                    }
                    (memoryTextView?.parent as? ViewGroup)?.removeView(memoryTextView)
                    val decorView = activity.window.decorView as? FrameLayout ?: return
                    memoryTextView = TextView(activity).apply {
                        setTextColor(Color.WHITE)
                        typeface = Typeface.MONOSPACE
                        setShadowLayer(5f, 0f, 0f, Color.BLACK)
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                            bottomMargin = dpToPx(0, decorView)
                        }
                    }
                    decorView.addView(memoryTextView)
                    activityRef = WeakReference(activity)
                }

                fun dpToPx(dp: Int, view: View): Int {
                    return (dp * view.resources.displayMetrics.density).roundToInt()
                }
            })


        val updateTextViewCallback = object : XC_MethodHook() {
            var lastUpdateTime = 0L
            val memoryInfo = ActivityManager.MemoryInfo()
            val swapTotal by lazy {
                File("/proc/meminfo").readLines()
                    .first { it.startsWith("SwapTotal:") }
                    .split(Regex("\\s+"))[1].toLong() * 1024
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val currentTime = System.nanoTime()
                if (currentTime - lastUpdateTime < 300_000_000L) {
                    return
                }
                lastUpdateTime = currentTime

                val memoryText = getMemoryText()
                activityRef?.get()?.runOnUiThread {
                    memoryTextView?.text = memoryText
                }
            }

            fun getMemoryText(): String {
                activityManager?.getMemoryInfo(memoryInfo) ?: return ""
                val leftText = formatMemory(memoryInfo.availMem, memoryInfo.totalMem)
                val swapFree = File("/proc/meminfo").readLines()
                    .first { it.startsWith("SwapFree:") }
                    .split(Regex("\\s+"))[1].toLong() * 1024
                val rightText = formatMemory(swapFree, swapTotal)
                return "$leftText   $rightText"
            }

            fun formatMemory(freeBytes: Long, totalBytes: Long): String {
                val freeBytesDouble = freeBytes.toDouble()
                val totalBytesDouble = totalBytes.toDouble()
                val divisor = 1024 * 1024 * 1024
                val freeGB = freeBytesDouble / divisor
                val freePercentage = (freeBytesDouble / totalBytes * 100).roundToInt()
                val usedGB = (totalBytesDouble - freeBytesDouble) / divisor
                val usedPercentage = 100 - freePercentage
                val totalGB = totalBytesDouble / divisor
                return "%.2fG %d%% %.2fG %d%% %.2fG".format(
                    freeGB,
                    freePercentage,
                    usedGB,
                    usedPercentage,
                    totalGB
                )
            }
        }

        try {
            findAndHookMethod(
                "com.android.quickstep.RecentsActivity",
                loadPackageParam.classLoader,
                "onResume",
                updateTextViewCallback
            )

            findAndHookMethod(
                $$"com.android.systemui.shared.system.TaskStackChangeListeners$Impl",
                loadPackageParam.classLoader,
                "onTaskRemoved",
                Int::class.javaPrimitiveType,
                updateTextViewCallback
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun showMemoryUsageInRecentsTargetSdk35(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.LAUNCHER ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM
        ) return

        var leftTextView: TextView? = null
        var rightTextView: TextView? = null
        var activityRef: WeakReference<Activity>? = null
        var activityManager: ActivityManager? = null
        val constraintSetConstructor = findClass(
            "androidx.constraintlayout.widget.ConstraintSet",
            loadPackageParam.classLoader
        ).getConstructor()
        val constraintLayoutLayoutParamsConstructor = findClass(
            $$"androidx.constraintlayout.widget.ConstraintLayout$LayoutParams",
            loadPackageParam.classLoader
        ).getConstructor(
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )

        val getViewCallback = object : XC_MethodHook() {
            @SuppressLint("ResourceType")
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    if (callMethod(param.thisObject, "getTAG") != "TaskListPot") return
                    leftTextView?.let { (it.parent as ViewGroup).removeView(it) }
                    rightTextView?.let { (it.parent as ViewGroup).removeView(it) }
                    val view = param.result as View
                    val buttonId = view.resources.getIdentifier("clear_all", "id", Package.LAUNCHER)
                    val clearAllButton: Button = view.findViewById(buttonId)
                    val constraintLayout = clearAllButton.parent as ViewGroup

                    leftTextView = TextView(constraintLayout.context).apply {
                        id = View.generateViewId()
                        setTextColor(Color.WHITE)
                        setTypeface(Typeface.MONOSPACE)
                        fontFeatureSettings = "'tnum' 1, 'pnum' 0"

                    }

                    rightTextView = TextView(constraintLayout.context).apply {
                        id = View.generateViewId()
                        setTextColor(Color.WHITE)
                        setTypeface(Typeface.MONOSPACE)
                        fontFeatureSettings = "'tnum' 1, 'pnum' 0"
                    }

                    addTextView(
                        leftTextView,
                        true,
                        clearAllButton,
                        constraintLayout,
                        constraintSetConstructor,
                        constraintLayoutLayoutParamsConstructor
                    )

                    addTextView(
                        rightTextView,
                        false,
                        clearAllButton,
                        constraintLayout,
                        constraintSetConstructor,
                        constraintLayoutLayoutParamsConstructor
                    )
                } catch (t: Throwable) {
                    XposedBridge.log(t)
                }
            }

            fun addTextView(
                textView: TextView,
                isLeft: Boolean,
                clearAllButton: Button,
                constraintLayout: ViewGroup,
                constraintSetConstructor: Constructor<*>,
                constraintLayoutLayoutParamsConstructor: Constructor<*>,
            ) {
                val initialParams = constraintLayoutLayoutParamsConstructor.newInstance(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )

                callMethod(constraintLayout, "addView", textView, initialParams)

                val constraintSet = constraintSetConstructor.newInstance()
                callMethod(constraintSet, "clone", constraintLayout)

                val top = 3
                val bottom = 4
                val start = 6
                val end = 7
                val parentId = 0
                // 通用垂直约束：顶部和底部与按钮对齐，实现垂直居中
                callMethod(constraintSet, "connect", textView.id, top, clearAllButton.id, top)
                callMethod(constraintSet, "connect", textView.id, bottom, clearAllButton.id, bottom)
                if (isLeft) {
                    // 左侧 TextView
                    callMethod(constraintSet, "connect", textView.id, end, clearAllButton.id, start)
                    callMethod(constraintSet, "connect", textView.id, start, parentId, start)
                } else {
                    // 右侧 TextView
                    callMethod(constraintSet, "connect", textView.id, start, clearAllButton.id, end)
                    callMethod(constraintSet, "connect", textView.id, end, parentId, end)
                }

                // 应用约束
                callMethod(constraintSet, "applyTo", constraintLayout)
            }
        }

        try {
            findAndHookMethod(
                "com.honeyspace.common.entity.HoneyPot",
                loadPackageParam.classLoader,
                "getView",
                getViewCallback
            )

            findAndHookMethod(
                "com.android.quickstep.RecentsActivity",
                loadPackageParam.classLoader,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as Activity
                        activityRef = WeakReference(activity)
                        activityManager = activity.getSystemService(ActivityManager::class.java)
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }


        val updateTextViewCallback = object : XC_MethodHook() {
            var lastUpdateTime = 0L
            val memoryInfo = ActivityManager.MemoryInfo()
            val swapTotal by lazy {
                File("/proc/meminfo").readLines()
                    .first { it.startsWith("SwapTotal:") }
                    .split(Regex("\\s+"))[1].toLong() * 1024
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val currentTime = System.nanoTime()
                if (currentTime - lastUpdateTime < 300_000_000L) {
                    return
                }
                lastUpdateTime = currentTime

                val memoryText = getMemoryText()
                activityRef?.get()?.runOnUiThread {
                    leftTextView?.text = memoryText.first
                    rightTextView?.text = memoryText.second
                }
            }

            fun getMemoryText(): Pair<String, String> {
                activityManager?.getMemoryInfo(memoryInfo) ?: return "" to ""
                val leftText = formatMemory(memoryInfo.availMem, memoryInfo.totalMem)
                val swapFree = File("/proc/meminfo").readLines()
                    .first { it.startsWith("SwapFree:") }
                    .split(Regex("\\s+"))[1].toLong() * 1024
                val rightText = formatMemory(swapFree, swapTotal)
                return leftText to rightText
            }

            fun formatMemory(freeBytes: Long, totalBytes: Long): String {
                val freeBytesDouble = freeBytes.toDouble()
                val totalBytesDouble = totalBytes.toDouble()
                val divisor = 1024 * 1024 * 1024
                val freeGB = freeBytesDouble / divisor
                val freePercentage = (freeBytesDouble / totalBytes * 100).roundToInt()
                val usedGB = (totalBytesDouble - freeBytesDouble) / divisor
                val usedPercentage = 100 - freePercentage
                val totalGB = totalBytesDouble / divisor
                return """
                    %5.2fGB%3d%%
                    %5.2fGB%3d%%
                    %5.2fGB
                """.trimIndent().format(
                    freeGB, freePercentage,
                    usedGB, usedPercentage,
                    totalGB
                ).trimIndent()
            }
        }

        try {
            findAndHookMethod(
                "com.android.quickstep.RecentsActivity",
                loadPackageParam.classLoader,
                "onResume",
                updateTextViewCallback
            )

            findAndHookMethod(
                $$"com.android.systemui.shared.system.TaskStackChangeListeners$Impl",
                loadPackageParam.classLoader,
                "onTaskRemoved",
                Int::class.javaPrimitiveType,
                updateTextViewCallback
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun removeShortcutBadge(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.LAUNCHER) return
        try {
            // 阻止工作资料（ WORK_APP ）/安全文件夹（ SECURE_FOLDER ）/即时应用（ INSTANT_APP ）/中国可卸载应用（ CHINA_REMOVABLE ）等角标
            findAndHookMethod(
                "com.honeyspace.ui.common.iconview.AppShortcutBadgeCreator",
                loadPackageParam.classLoader,
                "create",
                Context::class.java,
                "com.honeyspace.sdk.source.entity.ComponentKey",
                XC_MethodReplacement.returnConstant(null)
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }

        try {
            // 阻止深度快捷方式右下角的小图标
            findAndHookMethod(
                "com.honeyspace.ui.common.iconview.DeepShortcutIconSupplier",
                loadPackageParam.classLoader,
                "drawSmallIcon",
                android.graphics.Canvas::class.java,
                android.content.pm.ShortcutInfo::class.java,
                Boolean::class.javaPrimitiveType,
                XC_MethodReplacement.DO_NOTHING
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun hideAppsSearchBar(loadPackageParam: LoadPackageParam) {
        try {
            findAndHookConstructor(
                "com.honeyspace.ui.honeypots.appscreen.presentation.AppsSearchBar",
                loadPackageParam.classLoader,
                Context::class.java,
                AttributeSet::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val searchBar = param.thisObject as View
                        searchBar.addOnAttachStateChangeListener(object :
                            View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: View) {
                                v.visibility = View.GONE
                            }

                            override fun onViewDetachedFromWindow(v: View) {}
                        })
                        // 监听该 View 自身布局变化，防止 Data Binding 将 visibility 重置为 VISIBLE
                        searchBar.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                            if (view.visibility != View.GONE) {
                                view.visibility = View.GONE
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    /**
     * Fold / タブレット限定になっているタスクバー（画面下端のドック）を、
     * 通常のバー型端末でも選べるようにする。
     *
     * ランチャー（ honeyspace ）は「この端末はタスクバーに対応しているか」を
     * `com.honeyspace.sdk.Rune` などのフラグで判定しているが、そのメンバー名は One UI のバージョンで揺れる。
     * そこで名前を決め打ちせず、「対応・利用可否」を表すメンバーだけを総当たりで拾って
     * true に固定する。ユーザー自身の ON/OFF （ホーム画面設定のタスクバー）には触れないので、
     * 解放したあとの表示・非表示は本人が切り替えられる。
     */
    fun unlockFoldTaskbar(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.LAUNCHER) return

        afterAttach {
            val applied = TASKBAR_FEATURE_CLASSES.flatMap { className ->
                forceTaskbarCapability(className, loadPackageParam.classLoader)
            }

            forceTaskbarFloatingFeature(loadPackageParam)

            if (applied.isEmpty()) {
                // 名前が変わった／既に true だった場合。ログだけ残して何もしない。
                XposedBridge.log("OneUIX unlockFoldTaskbar: no taskbar capability member matched")
            } else {
                XposedBridge.log("OneUIX unlockFoldTaskbar: ${applied.joinToString()}")
            }
        }
    }

    /** タスクバーの対応可否を持っているフィーチャーフラグ置き場。 */
    private val TASKBAR_FEATURE_CLASSES = listOf(
        "com.honeyspace.sdk.Rune",
        "com.samsung.android.rune.CoreRune",
    )

    private val TASKBAR_NAME_REGEX = Regex("TASK_?BAR", RegexOption.IGNORE_CASE)

    /** 「対応している」側のフラグだけを対象にする。ユーザー設定の ON/OFF は含めない。 */
    private val CAPABILITY_NAME_REGEX = Regex("SUPPORT|AVAILABLE|ALLOW", RegexOption.IGNORE_CASE)

    /** 意味が反転しているフラグ（ 未対応・禁止・非表示 ）を誤って true にしないための除外。 */
    private val NEGATIVE_NAME_REGEX =
        Regex("NOT|UNSUPPORT|DISABLE|BLOCK|HIDE|REMOVE|RESTRICT", RegexOption.IGNORE_CASE)

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
        } catch (t: Throwable) {
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

    private const val SEM_FLOATING_FEATURE_CLASS =
        "com.samsung.android.feature.SemFloatingFeature"
}
