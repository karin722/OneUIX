package io.github.soclear.oneuix.hook.systemui

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.getBatteryLevel
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.getBatteryState
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.getStatusBarIconTint
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.getWarningColorForLevel
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.readFieldValue
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.resolveBatteryChargingIconId
import io.github.soclear.oneuix.hook.systemui.BatteryMeterViewReflection.shouldShowChargingIcon
import java.lang.ref.WeakReference

/**
 * LineageOS風の円形（リング）バッテリーアイコンを描画する。
 *
 * `Drawable.draw(Canvas)`自体はXposedでフックしない。`draw`はレンダリングのたびに非常に高頻度で
 * 呼ばれるため、ARTのJITコンパイルとXposedフックの相性問題でフックが失われることがあり、
 * 実際に「一瞬円形になった後に元のバー形状へ戻る」という症状が確認された。
 * 代わりにこのクラス自身をmBatteryIconViewへ`setImageDrawable`で差し替え、通常のポリモーフィズムで
 * `draw`を呼ばせる（Xposedフック不要になりJITの影響を受けない）。
 *
 * 状態（レベル・充電中・色）は自前でキャッシュせず、[draw]が呼ばれるたびに
 * Samsung純正の`SamsungBatteryMeterDrawable`インスタンスから都度読み直す。これにより、
 * どのタイミングで再描画がトリガーされても常に最新の状態が反映される。
 *
 * なお、再描画契機（`invalidateSelf`）はSamsung側からは飛んでこない。Samsung純正Drawableは
 * ImageViewから外れた時点でCallbackを失い、`postInvalidate()`（内部で`scheduleSelf`を使う）が
 * 無反応になるためである。[CircleBatteryIcon]側でSamsungの再描画契機をフックして中継している。
 */
internal class CircleBatteryDrawable(
    samsungDrawable: Any,
    batteryMeterView: Any,
    private val resources: Resources
) : Drawable() {
    private val samsungDrawableRef = WeakReference(samsungDrawable)
    private val batteryMeterViewRef = WeakReference(batteryMeterView)

    var sizePx: Int = 0
        private set

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val arcRect = RectF()

    private var chargingIcon: Drawable? = null
    private var chargingIconResolved = false

    fun updateSize(sizePx: Int) {
        if (this.sizePx == sizePx || sizePx <= 0) return
        this.sizePx = sizePx
        val strokeWidth = sizePx * STROKE_WIDTH_RATIO
        trackPaint.strokeWidth = strokeWidth
        levelPaint.strokeWidth = strokeWidth
        val inset = strokeWidth / 2f
        arcRect.set(inset, inset, sizePx - inset, sizePx - inset)
        setBounds(0, 0, sizePx, sizePx)
    }

    override fun getIntrinsicWidth(): Int = sizePx

    override fun getIntrinsicHeight(): Int = sizePx

    override fun draw(canvas: Canvas) {
        if (sizePx <= 0) return
        val samsungDrawable = samsungDrawableRef.get() ?: return
        val batteryState = getBatteryState(samsungDrawable) ?: return

        val level = resolveLevel(batteryState)
        val charging = shouldShowChargingIcon(batteryState)
        val tintColor = resolveTintColor(samsungDrawable, level)

        trackPaint.color = trackColorFor(tintColor)
        levelPaint.color = tintColor

        canvas.drawOval(arcRect, trackPaint)
        canvas.drawArc(arcRect, START_ANGLE, 360f * level / 100f, false, levelPaint)

        if (charging) drawChargingIcon(canvas, tintColor)
    }

    /**
     * アイコンの色を解決する。
     *
     * 通常時は`BatteryMeterView.mTextColor`（＝`DarkIconDispatcher.getTint()`の結果）を使う。
     * Samsungがバッテリーアイコン専用に持っている色（`iconTint` / `batteryLevelColor`）は
     * 他のステータスバーアイコンのグレーと微妙に異なるため、そちらは使わない。
     * 残量警告時（赤）だけはSamsungの判定色を尊重する。
     */
    private fun resolveTintColor(samsungDrawable: Any, level: Int): Int {
        getWarningColorForLevel(samsungDrawable, level)?.let { return it }
        batteryMeterViewRef.get()?.let { getStatusBarIconTint(it) }?.let { return it }
        return readFieldValue(samsungDrawable, listOf("iconTint")) as? Int ?: Color.WHITE
    }

    /**
     * バッテリー残量を解決する。`SamsungBatteryState`は初期化直後だと`level = -1`（未取得）の
     * ままなので、その場合は`BatteryMeterView.mLevel`（BatteryManagerから直接取得された値）に
     * フォールバックする。
     */
    private fun resolveLevel(batteryState: Any): Int {
        val isDirectPowerMode =
            readFieldValue(batteryState, listOf("isDirectPowerMode")) as? Boolean == true
        // Samsung純正のgetBatteryLevel()と同様、直挿し給電中は満充電扱いにする
        if (isDirectPowerMode) return 100

        val stateLevel = getBatteryLevel(batteryState) ?: UNKNOWN_LEVEL
        if (stateLevel in 0..100) return stateLevel

        val viewLevel = batteryMeterViewRef.get()
            ?.let { readFieldValue(it, listOf("mLevel", "level")) as? Int }
            ?: UNKNOWN_LEVEL
        return viewLevel.coerceIn(0, 100)
    }

    private fun drawChargingIcon(canvas: Canvas, tintColor: Int) {
        val icon = resolveChargingIcon(tintColor) ?: return
        val iconSize = (sizePx * CHARGING_ICON_SCALE).toInt().coerceAtLeast(1)
        val offset = (sizePx - iconSize) / 2
        icon.setBounds(offset, offset, offset + iconSize, offset + iconSize)
        icon.draw(canvas)
    }

    private fun resolveChargingIcon(tintColor: Int): Drawable? {
        if (!chargingIconResolved) {
            chargingIconResolved = true
            val id = resolveBatteryChargingIconId(resources)
            chargingIcon = if (id != 0) {
                @Suppress("DEPRECATION")
                resources.getDrawable(id, null)?.mutate()
            } else {
                null
            }
        }
        chargingIcon?.setTint(tintColor)
        return chargingIcon
    }

    private fun trackColorFor(color: Int): Int =
        Color.argb(TRACK_ALPHA, Color.red(color), Color.green(color), Color.blue(color))

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val STROKE_WIDTH_RATIO = 0.14f
        const val TRACK_ALPHA = 65
        const val CHARGING_ICON_SCALE = 0.5f

        /** 12時方向を起点にする。 */
        const val START_ANGLE = -90f
        const val UNKNOWN_LEVEL = -1
    }
}
