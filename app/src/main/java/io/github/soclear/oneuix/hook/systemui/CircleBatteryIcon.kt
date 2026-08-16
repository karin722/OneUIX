package io.github.soclear.oneuix.hook.systemui

import android.widget.ImageView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.soclear.oneuix.data.Package
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.getBatterySamsungDrawable
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.readFieldValue
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.resolveBatteryMeterView
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * ステータスバーのバッテリーアイコンを、Samsung純正の縦長バー描画からLineageOS風の円形リング描画に変更する。
 *
 * `SamsungBatteryMeterDrawable.draw(Canvas)`自体をXposedでフックする方式は、`draw`が
 * レンダリングのたびに非常に高頻度で呼ばれるため、ARTのJITコンパイルとの相性問題でフックが
 * 失われ「一瞬円形になった後に元のバー形状へ戻る」症状が発生することを実機で確認した。
 * そこで[CircleBatteryDrawable]を`mBatteryIconView`へ`setImageDrawable`で差し替える方式を採る。
 *
 * ただしImageViewのdrawableを差し替えると、Samsung純正Drawableは`Drawable.Callback`を失う。
 * Samsungの再描画は`postInvalidate()`（内部で`scheduleSelf`を使う）に依存しているため、
 * これが無反応になり「初期状態のまま二度と更新されない」＝残量が常に未取得値のまま、という
 * 症状を引き起こす。そのためSamsung側の再描画契機を[relayTargets]経由で自前Drawableに中継する。
 *
 * ここでフックするのはいずれも低頻度にしか呼ばれない箇所のみで、`draw`は一切フックしない。
 */
internal object CircleBatteryIcon {
    /** Samsung純正Drawableのインスタンス -> 対応する自前Drawable。再描画契機の中継に使う。 */
    private val relayTargets: MutableMap<Any, CircleBatteryDrawable> =
        Collections.synchronizedMap(WeakHashMap())

    fun apply(
        loadPackageParam: XC_LoadPackage.LoadPackageParam,
        sizeDp: Float,
        horizontalPaddingDp: Float,
        verticalPaddingDp: Float
    ) {
        if (loadPackageParam.packageName != Package.SYSTEMUI) return

        val batteryMeterViewCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                syncBatteryIcon(param.thisObject, sizeDp, horizontalPaddingDp, verticalPaddingDp)
            }
        }

        try {
            XposedBridge.hookAllConstructors(
                XposedHelpers.findClass(
                    "com.android.systemui.battery.BatteryMeterView",
                    loadPackageParam.classLoader
                ),
                batteryMeterViewCallback
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }

        listOf(
            "onBatteryLevelChanged",
            "updatePercentText",
            "updateShowPercent",
            "scaleBatteryMeterViewsLegacy",
            "updateColors",
            // ライト/ダーク切り替えに追従する。Samsungはここでアイコン専用のアルファを掛けてくる
            "onDarkChanged",
            "onDarkChangedLegacy"
        ).forEach { methodName ->
            hookAllMethodsSafely(
                loadPackageParam,
                "com.android.systemui.battery.BatteryMeterView",
                methodName,
                batteryMeterViewCallback
            )
        }

        hookAllMethodsSafely(
            loadPackageParam,
            $$"com.android.systemui.battery.BatteryMeterViewController$3",
            "onBatteryLevelChanged",
            batteryMeterViewCallback
        )

        // Samsung純正Drawableの再描画契機を自前Drawableへ中継する。
        // ImageViewから外れた純正DrawableはCallbackを失い、これらの呼び出しが画面に反映されないため。
        val relayCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                relayTargets[param.thisObject]?.invalidateSelf()
            }
        }
        listOf("postInvalidate", "onBatteryLevelChanged", "resizeDrawable").forEach { methodName ->
            hookAllMethodsSafely(
                loadPackageParam,
                "com.android.systemui.battery.SamsungBatteryMeterDrawable",
                methodName,
                relayCallback
            )
        }
    }

    private fun hookAllMethodsSafely(
        loadPackageParam: XC_LoadPackage.LoadPackageParam,
        className: String,
        methodName: String,
        callback: XC_MethodHook
    ) {
        try {
            XposedBridge.hookAllMethods(
                XposedHelpers.findClass(className, loadPackageParam.classLoader),
                methodName,
                callback
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    private fun syncBatteryIcon(
        instance: Any,
        sizeDp: Float,
        horizontalPaddingDp: Float,
        verticalPaddingDp: Float
    ) {
        val batteryMeterView = resolveBatteryMeterView(instance) ?: return
        val iconView = readFieldValue(
            batteryMeterView,
            listOf("mBatteryIconView", "batteryIconView")
        ) as? ImageView ?: return
        val samsungDrawable = getBatterySamsungDrawable(batteryMeterView) ?: return

        val density = iconView.resources.displayMetrics.density
        val sizePx = (sizeDp * density).roundToInt().coerceAtLeast(1)
        val horizontalPaddingPx = (horizontalPaddingDp * density).roundToInt().coerceAtLeast(0)
        val verticalPaddingPx = (verticalPaddingDp * density).roundToInt().coerceAtLeast(0)

        val drawable = relayTargets.getOrPut(samsungDrawable) {
            CircleBatteryDrawable(samsungDrawable, batteryMeterView, iconView.resources)
        }
        // Samsung側のコードが後からmBatteryIconViewのdrawableを再設定するケースに備え、毎回描画対象を確認する
        if (iconView.drawable !== drawable) {
            iconView.setImageDrawable(drawable)
        }
        // Samsungはバッテリーアイコンにだけ専用のアルファ（status_bar_battery_*_mode_alpha、
        // 0.9や0.7）を掛けるため、他のステータスバーアイコンより薄いグレーに見えてしまう。
        // 不透明に戻して色を完全に一致させる。
        if (iconView.alpha != 1f) iconView.alpha = 1f

        drawable.updateSize(sizePx)
        drawable.invalidateSelf()

        applyIconLayout(iconView, sizePx, horizontalPaddingPx, verticalPaddingPx)
    }

    private fun applyIconLayout(
        iconView: ImageView,
        sizePx: Int,
        horizontalPaddingPx: Int,
        verticalPaddingPx: Int
    ) {
        val params = iconView.layoutParams
        if (params != null && (params.width != sizePx || params.height != sizePx)) {
            params.width = sizePx
            params.height = sizePx
            iconView.layoutParams = params
        }
        if (iconView.paddingStart != horizontalPaddingPx ||
            iconView.paddingTop != verticalPaddingPx ||
            iconView.paddingEnd != horizontalPaddingPx ||
            iconView.paddingBottom != verticalPaddingPx
        ) {
            iconView.setPaddingRelative(
                horizontalPaddingPx,
                verticalPaddingPx,
                horizontalPaddingPx,
                verticalPaddingPx
            )
        }
    }
}
