package io.github.soclear.oneuix.hook.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.hookAllMethods
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.XposedHelpers.findClassIfExists
import de.robv.android.xposed.XposedHelpers.getIntField
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.soclear.oneuix.data.ONE_UI_VERSION
import io.github.soclear.oneuix.data.Package
import io.github.soclear.oneuix.hook.util.ClockTextFormatter
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

object StatusBar {
    /** [LinearLayout.LayoutParams.gravity] の「未指定」。 */
    private const val UNSPECIFIED_GRAVITY = -1

    /**
     * 追跡できた時計ビュー。
     *
     * One UI ではクラス名が `Clock` ではなく `QSClockIndicatorView` なので、
     * ビュー階層をクラス名で探すのは当てにならない。
     * [setStatusBarClockFormat] と同じコントローラーから実体を受け取る。
     *
     * 同じクラスがクイック設定パネル側の大きな時計にも使われるため、
     * 1 つに決め打ちせず候補として集め、電池アイコンと同じ画面のものを選ぶ。
     */
    private val statusBarClocks: MutableSet<TextView> =
        Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))

    /** 状態バーの時計に合わせた字形をすでに適用した電量テキスト。 */
    private val styledBatteryLevelTexts: MutableSet<TextView> =
        Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))

    fun setStatusBarPaddingDp(loadPackageParam: LoadPackageParam, left: Float?, right: Float?) {
        if (loadPackageParam.packageName != io.github.soclear.oneuix.data.Package.SYSTEMUI ||
            left == null && right == null
        ) {
            return
        }
        try {
            val clazz = findClass(
                "com.android.systemui.statusbar.phone.IndicatorGardenAlgorithmCenterCutout",
                loadPackageParam.classLoader
            )
            if (left != null) {
                findAndHookMethod(
                    clazz,
                    "calculateLeftPadding",
                    object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Int {
                            val inputProperties =
                                getObjectField(param.thisObject, "inputProperties")
                            val density = getObjectField(inputProperties, "density") as Float
                            return (left * density).roundToInt()
                        }
                    }
                )
            }
            if (right != null) {
                findAndHookMethod(
                    clazz,
                    "calculateRightPadding",
                    object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Int {
                            val inputProperties =
                                getObjectField(param.thisObject, "inputProperties")
                            val density = getObjectField(inputProperties, "density") as Float
                            return (right * density).roundToInt()
                        }
                    }
                )
            }
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun setBatteryIconScale(
        loadPackageParam: LoadPackageParam,
        widthScale: Float?,
        heightScale: Float?
    ) {
        if (loadPackageParam.packageName != io.github.soclear.oneuix.data.Package.SYSTEMUI || widthScale == null && heightScale == null) return
        try {
            findAndHookMethod(
                "com.android.systemui.battery.BatteryMeterView",
                loadPackageParam.classLoader,
                "scaleBatteryMeterViewsLegacy",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val mBatteryIconView =
                            getObjectField(param.thisObject, "mBatteryIconView") as ImageView
                        mBatteryIconView.layoutParams = mBatteryIconView.layoutParams.apply {
                            if (widthScale != null) {
                                width = (width * widthScale).roundToInt()
                            }
                            if (heightScale != null) {
                                height = (height * heightScale).roundToInt()
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun hideBatteryPercentageSign(resparam: InitPackageResourcesParam) {
        if (resparam.packageName != io.github.soclear.oneuix.data.Package.SYSTEMUI ||
            Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            return
        }
        val batterMeterFormat = "status_bar_settings_${
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) "uniform_"
            else ""
        }battery_meter_format"
        resparam.res.setReplacement(Package.SYSTEMUI, "string", batterMeterFormat, "%d")
    }

    fun updateStatusBarClockEverySecond(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.SYSTEMUI) return
        // 每秒更新
        findAndHookMethod(
            "com.android.systemui.statusbar.policy.QSClockQuickStarHelper",
            loadPackageParam.classLoader,
            "updateSecondsClockHandler",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val mSecondsHandler = getObjectField(param.thisObject, "mSecondsHandler")
                    if (mSecondsHandler != null) return
                    val looper = Looper.myLooper() ?: return
                    val handler = Handler(looper)
                    setObjectField(param.thisObject, "mSecondsHandler", handler)
                    val mSecondTick = getObjectField(param.thisObject, "mSecondTick") as Runnable
                    handler.post(mSecondTick)
                }
            }
        )

        // 数字字体等宽
        findAndHookMethod(
            "com.android.systemui.statusbar.policy.QSClockIndicatorViewController",
            loadPackageParam.classLoader,
            "onViewAttached",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val clockTextView = getObjectField(param.thisObject, "view") as TextView
                    clockTextView.fontFeatureSettings = "tnum"
                }
            }
        )
    }

    fun setStatusBarClockTextScale(loadPackageParam: LoadPackageParam, scale: Float) {
        if (loadPackageParam.packageName != Package.SYSTEMUI) return
        // The controller restores the system baseline size before this callback, so scaling
        // remains stable across density and font-scale changes instead of accumulating.
        val callback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val clockView = getObjectField(param.thisObject, "view") as TextView
                clockView.setTextSize(TypedValue.COMPLEX_UNIT_PX, clockView.textSize * scale)
            }
        }
        try {
            findAndHookMethod(
                "com.android.systemui.statusbar.policy.QSClockIndicatorViewController",
                loadPackageParam.classLoader,
                "onDensityOrFontScaleChanged",
                callback
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    /**
     * 设置状态栏时钟的时间与日期格式。
     *
     * 时间部分沿用系统时钟原本的字号，日期部分通过 [ClockTextFormatter] 单独缩放，
     * 因此调整 [dateTextScale] 只会改变日期（例如 `EEEE` → 星期几）的大小。
     *
     * @param timeFormat 时间部分的 `DateTimeFormatter` 模式，例如 `HH:mm`
     * @param dateFormat 日期部分的模式，例如 `E` / `EEEE` / `M/d(E)`。为 null 或空则不显示日期
     * @param dateSeparator 时间与日期之间的分隔符，会与日期一同缩放
     * @param dateBeforeTime 为 true 时日期显示在时间之前
     * @param dateTextScale 日期部分相对时间部分的字号倍率，1f 表示不缩放
     * @param localeTag 日期使用的语言标签（BCP 47，例如 `ja` / `en-US`）。为空则跟随系统语言
     * @param dateOffsetDp 日期部分相对基线的垂直偏移（dp），正值向上。时间部分不受影响
     */
    fun setStatusBarClockFormat(
        loadPackageParam: LoadPackageParam,
        timeFormat: String,
        dateFormat: String?,
        dateSeparator: String = " ",
        dateBeforeTime: Boolean = false,
        dateTextScale: Float = 1f,
        localeTag: String = "",
        dateOffsetDp: Float = 0f,
    ) {
        if (loadPackageParam.packageName != Package.SYSTEMUI) return
        val clockTextFormatter = ClockTextFormatter(
            timePattern = timeFormat,
            datePattern = dateFormat,
            separator = dateSeparator,
            dateBeforeTime = dateBeforeTime,
            dateTextScale = dateTextScale,
            localeTag = localeTag,
        )
        setStatusBarClockText(loadPackageParam) { clockTextView ->
            // 时钟所在的 TextView 才能拿到正确的屏幕密度，因此在这里把 dp 换算成 px
            val offsetPx = (dateOffsetDp * clockTextView.resources.displayMetrics.density)
                .roundToInt()
            clockTextFormatter.format(dateBaselineShiftPx = offsetPx)
        }
    }

    private fun setStatusBarClockText(
        loadPackageParam: LoadPackageParam,
        block: (TextView) -> CharSequence
    ) {
        if (loadPackageParam.packageName != Package.SYSTEMUI) return
        val callback = object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                val clockTextView = param.thisObject as TextView
                val dateTime = try {
                    block(clockTextView)
                } catch (t: Throwable) {
                    XposedBridge.log(t)
                    return null
                }
                clockTextView.text = dateTime
                // contentDescription 供无障碍朗读，使用不带 span 的纯文本
                clockTextView.contentDescription = dateTime.toString()
                return null
            }
        }
        try {
            findAndHookMethod(
                "com.android.systemui.statusbar.policy.QSClockIndicatorView",
                loadPackageParam.classLoader,
                "notifyTimeChanged",
                "com.android.systemui.statusbar.policy.QSClockBellSound",
                callback
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun hideSecureFolderStatusBarIcon(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.SYSTEMUI) return
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (param.args[0] == "managed_profile") {
                    param.result = null
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                findAndHookMethod(
                    "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl",
                    loadPackageParam.classLoader,
                    "setIcon",
                    String::class.java,
                    "com.android.systemui.statusbar.phone.StatusBarIconHolder",
                    callback
                )
            } else {
                findAndHookMethod(
                    "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
                    loadPackageParam.classLoader,
                    "setIcon",
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    CharSequence::class.java,
                    callback
                )
            }
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun restoreBluetoothStatusBarIcon(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.SYSTEMUI) return
        try {
            findAndHookMethod(
                "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl",
                loadPackageParam.classLoader,
                "hideBySimplification",
                "com.android.systemui.statusbar.phone.ui.IconManager",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val slot = param.args[1] as? String ?: return
                        if (slot == "bluetooth" || slot == "bluetooth_connected") {
                            param.result = false
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun doubleTapStatusBarToSleep(loadPackageParam: LoadPackageParam) {
        val callback = object : XC_MethodHook() {
            var lastTapTime = 0L

            override fun beforeHookedMethod(param: MethodHookParam) {
                val event = param.args[0] as MotionEvent
                if (event.action != MotionEvent.ACTION_DOWN) {
                    return
                }
                val currentTime = System.nanoTime()
                val interval = currentTime - lastTapTime
                if (interval in 40_000_000L..300_000_000L) {
                    lastTapTime = 0L
                    val view = param.thisObject as View
                    lockScreen(view.context)
                    param.result = true
                } else {
                    lastTapTime = currentTime
                }
            }

            fun lockScreen(context: Context) {
                val powerManager = context.getSystemService(PowerManager::class.java)
                callMethod(powerManager, "goToSleep", SystemClock.uptimeMillis())
            }
        }
        try {
            findAndHookMethod(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                loadPackageParam.classLoader,
                "onTouchEvent",
                MotionEvent::class.java,
                callback
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun hideLockscreenStatusBar(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != Package.SYSTEMUI) return
        try {
            findAndHookMethod(
                "com.android.systemui.statusbar.phone.KeyguardStatusBarView",
                loadPackageParam.classLoader,
                "setVisibility",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[0] = View.GONE
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun setCustomCarrierName(loadPackageParam: LoadPackageParam, carrierName: String) {
        if (loadPackageParam.packageName != Package.SYSTEMUI) return
        try {
            findAndHookMethod(
                "com.android.keyguard.CarrierTextManager",
                loadPackageParam.classLoader,
                "postToCallback",
                $$"com.android.keyguard.CarrierTextManager$CarrierTextCallbackInfo",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val carrierTextCallbackInfo = param.args[0] ?: return
                        runCatching { setObjectField(carrierTextCallbackInfo, "carrierText", carrierName) }
                        runCatching { setObjectField(carrierTextCallbackInfo, "carrierTextShort", carrierName) }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    /**
     * 电池图标右侧显示电量文字。
     *
     * @param hidePercentSign 为 true 时只显示数字，不显示 `%`
     * @param hideChargingIcon 为 true 时充电中也不显示 ⚡ 标记
     * @param percentSignScale `%` 相对数字部分的字号倍率。1f 表示与数字同样大小
     */
    fun addBatteryLevelText(
        loadPackageParam: LoadPackageParam,
        hidePercentSign: Boolean,
        hideChargingIcon: Boolean,
        percentSignScale: Float = 1f,
        marginStartDp: Float = 4f,
        textSizeScale: Float = 1f,
        verticalOffsetDp: Float = 0f,
    ) {
        if (loadPackageParam.packageName != Package.SYSTEMUI || ONE_UI_VERSION < 70000) return
        trackStatusBarClock(loadPackageParam)
        val batteryMeterViewClass = findClassIfExists(
            "com.android.systemui.battery.BatteryMeterView",
            loadPackageParam.classLoader
        ) ?: return

        val viewId = View.generateViewId()

        try {
            findAndHookMethod(
                batteryMeterViewClass,
                "scaleBatteryMeterViewsLegacy",
                object : XC_MethodHook() {
                    @SuppressLint("SetTextI18n")
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val batteryMeterView = param.thisObject as ViewGroup
                            var textView = batteryMeterView.findViewById<TextView>(viewId)
                            if (textView == null) {
                                textView = TextView(batteryMeterView.context).apply {
                                    id = viewId
                                    gravity = Gravity.CENTER
                                }
                                batteryMeterView.addView(
                                    textView, LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                )
                            }
                            layOutBatteryLevelText(
                                batteryMeterView, textView, marginStartDp, verticalOffsetDp
                            )
                            moveAfterBatteryIcon(batteryMeterView, textView)
                            applyStatusBarTextAppearance(
                                batteryMeterView, textView, textSizeScale
                            )
                            val level = getIntField(batteryMeterView, "mLevel")
                            val isCharging = callMethod(batteryMeterView, "isCharging") as Boolean
                            val suffix = if (isCharging && !hideChargingIcon) "\u26A1\uFE0E" else ""
                            textView.text = buildBatteryLevelText(
                                level = level,
                                hidePercentSign = hidePercentSign,
                                percentSignScale = percentSignScale,
                                suffix = suffix
                            )
                            textView.setTextColor(getIntField(batteryMeterView, "mTextColor"))
                        } catch (t: Throwable) {
                            XposedBridge.log(t)
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }

        try {
            hookAllMethods(batteryMeterViewClass, "updateColors", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val view = param.thisObject as ViewGroup
                        val textView = view.findViewById<TextView>(viewId) ?: return
                        textView.setTextColor(getIntField(view, "mTextColor"))
                    } catch (t: Throwable) {
                        XposedBridge.log(t)
                    }
                }
            })
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    /**
     * 电量文字必须位于电池图标的右侧。
     *
     * `BatteryMeterView` 是水平 [LinearLayout]，所以「图标之后的下一个位置」就是图标右侧。
     * 系统会在配置变化等时机重新添加自己的子视图，导致我们的文字被挤到左边，
     * 因此每次刷新都重新确认一次顺序，位置不对时才移动，避免无谓的重新布局。
     */
    private fun moveAfterBatteryIcon(batteryMeterView: ViewGroup, textView: TextView) {
        val iconView = runCatching {
            getObjectField(batteryMeterView, "mBatteryIconView") as? View
        }.getOrNull()
        val targetIndex = if (iconView != null && iconView.parent === batteryMeterView) {
            batteryMeterView.indexOfChild(iconView) + 1
        } else {
            batteryMeterView.childCount
        }
        val currentIndex = batteryMeterView.indexOfChild(textView)
        if (currentIndex == targetIndex) return

        val layoutParams = textView.layoutParams
        batteryMeterView.removeView(textView)
        // 移除后索引会前移一位，所以原本在文字之后的目标位置要相应修正。
        val insertIndex = if (currentIndex in 0..<targetIndex) targetIndex - 1 else targetIndex
        batteryMeterView.addView(
            textView,
            insertIndex.coerceIn(0, batteryMeterView.childCount),
            layoutParams ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    /**
     * 電量テキストの配置。
     *
     * 上下位置は、同じ [LinearLayout] に並んでいる純正の電量テキスト
     * （`mBatteryPercentView`）の配置をそのまま真似るのが確実。時計と揃うように
     * Samsung 側が調整済みだからで、これを写せば時計とも高さが揃う。
     * 純正のビューが無い場合だけ、水平 [LinearLayout] の既定（上寄せ）を打ち消して
     * 上下中央に置く。
     *
     * @param marginStartDp アイコンとの間隔。アイコン側の余白が広い機種で詰められるよう負の値も許す
     * @param verticalOffsetDp 微調整。正の値で上へ。レイアウトを動かさない translationY で効かせる
     */
    private fun layOutBatteryLevelText(
        batteryMeterView: ViewGroup,
        textView: TextView,
        marginStartDp: Float,
        verticalOffsetDp: Float,
    ) {
        val density = textView.resources.displayMetrics.density
        val percentView = runCatching {
            getObjectField(batteryMeterView, "mBatteryPercentView") as? TextView
        }.getOrNull()
        val referenceParams = percentView?.layoutParams as? LinearLayout.LayoutParams

        val params = textView.layoutParams as? LinearLayout.LayoutParams ?: return
        params.gravity = referenceParams?.gravity?.takeIf { it != UNSPECIFIED_GRAVITY }
            ?: Gravity.CENTER_VERTICAL
        params.height = referenceParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
        params.marginStart = (marginStartDp * density).roundToInt()
        textView.layoutParams = params

        percentView?.let {
            textView.includeFontPadding = it.includeFontPadding
            textView.setPaddingRelative(0, it.paddingTop, 0, it.paddingBottom)
        }
        textView.translationY = -verticalOffsetDp * density
    }

    /**
     * ステータスバーの時計ビューを覚えておく。
     * [setStatusBarClockText] と同じ `QSClockIndicatorView` 系を対象にしているので、
     * 時計の書式変更の設定が無効でも実体を取得できる。
     */
    private fun trackStatusBarClock(loadPackageParam: LoadPackageParam) {
        val rememberClock = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val view = param.thisObject as? TextView
                    ?: runCatching { getObjectField(param.thisObject, "view") as? TextView }
                        .getOrNull()
                    ?: return
                statusBarClocks.add(view)
            }
        }
        runCatching {
            hookAllMethods(
                findClass(
                    "com.android.systemui.statusbar.policy.QSClockIndicatorViewController",
                    loadPackageParam.classLoader
                ),
                "onViewAttached",
                rememberClock
            )
        }
        runCatching {
            hookAllMethods(
                findClass(
                    "com.android.systemui.statusbar.policy.QSClockIndicatorView",
                    loadPackageParam.classLoader
                ),
                "notifyTimeChanged",
                rememberClock
            )
        }
    }

    /**
     * 电量文字使用与状态栏时钟完全相同的字形。
     *
     * 新建的 [TextView] 默认是系统的常规字重，而 One UI 的状态栏时钟使用更粗的字重，
     * 所以不复制字形的话电量文字看起来会明显偏细。
     *
     * 时钟优先取 [trackStatusBarClock] 抓到的实例。One UI 的状态栏时钟类名是
     * `QSClockIndicatorView`，按类名在视图树里找是靠不住的。
     *
     * 字号则以系统自带的电量文字（`mBatteryPercentView`）为准。
     * 时钟在快捷设置面板里会放大，直接照搬会明显偏大。
     */
    private fun applyStatusBarTextAppearance(
        batteryMeterView: ViewGroup,
        textView: TextView,
        textSizeScale: Float,
    ) {
        // ビュー階層をたどる処理なので、同じ TextView に対しては一度だけ実行する。
        // フォントやテーマが変わったときは status bar ごと作り直されるため、取りこぼしはない。
        if (textView in styledBatteryLevelTexts) return
        val percentView = runCatching {
            getObjectField(batteryMeterView, "mBatteryPercentView") as? TextView
        }.getOrNull()
        // まだアタッチされていない等で見つからない場合は、次の更新でもう一度試す。
        val typefaceSource = findTrackedClock(batteryMeterView)
            ?: findStatusBarClock(batteryMeterView)
            ?: percentView
            ?: return
        val sizeSource = percentView ?: typefaceSource
        XposedBridge.log(
            "OneUIX: battery level text typeface from " + typefaceSource.javaClass.name +
                ", size from " + sizeSource.javaClass.name
        )
        textView.typeface = typefaceSource.typeface
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizeSource.textSize * textSizeScale)
        textView.letterSpacing = typefaceSource.letterSpacing
        textView.fontFeatureSettings = typefaceSource.fontFeatureSettings
        styledBatteryLevelTexts.add(textView)
    }

    /**
     * ステータスバーの時計を、電池ビューから上へたどって探す予備の手段。
     * [statusBarClock] が取れていないときだけ使う。
     */
    /** 電池アイコンと同じ画面に出ている時計。見つからなければ最後に拾ったもの。 */
    private fun findTrackedClock(batteryMeterView: View): TextView? {
        val root = batteryMeterView.rootView
        synchronized(statusBarClocks) {
            return statusBarClocks.firstOrNull { it.rootView === root }
                ?: statusBarClocks.lastOrNull()
        }
    }

    private fun findStatusBarClock(batteryMeterView: ViewGroup): TextView? {
        var child: View = batteryMeterView
        while (true) {
            val parent = child.parent as? ViewGroup ?: return null
            findClockInChildren(parent, skip = child)?.let { return it }
            child = parent
        }
    }

    private fun findClockInChildren(parent: ViewGroup, skip: View?): TextView? {
        for (index in 0..<parent.childCount) {
            val child = parent.getChildAt(index)
            if (child === skip) continue
            if (child is TextView && child.javaClass.simpleName.contains("Clock")) return child
            if (child is ViewGroup) findClockInChildren(child, skip = null)?.let { return it }
        }
        return null
    }

    /**
     * 电量文字。`%` 通过 [RelativeSizeSpan] 单独缩放，因此不会影响数字部分的字号。
     * 这样可以复现旧越狱插件那种「数字大、百分号小」的排版。
     */
    private fun buildBatteryLevelText(
        level: Int,
        hidePercentSign: Boolean,
        percentSignScale: Float,
        suffix: String,
    ): CharSequence {
        if (hidePercentSign) return "$level$suffix"
        if (percentSignScale == 1f) return "$level%$suffix"

        val builder = SpannableStringBuilder()
        builder.append(level.toString())
        val start = builder.length
        builder.append('%')
        builder.setSpan(
            RelativeSizeSpan(percentSignScale),
            start,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(suffix)
        return builder
    }
}
