package io.github.soclear.oneuix.hook.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.view.View
import de.robv.android.xposed.XposedHelpers
import io.github.soclear.oneuix.data.Package
import java.lang.reflect.Field

internal object BatteryMeterViewReflection {
    private const val STATUS_BAR_CHARGING_ICON = "stat_sys_battery_charging"
    private const val FALLBACK_CHARGING_ICON = "ic_icon_charging"
    private const val UNRESOLVED_CHARGING_ICON_ID = -1

    private var batteryChargingIconId = UNRESOLVED_CHARGING_ICON_ID

    fun resolveBatteryMeterView(instance: Any): Any? {
        if (instance is View) return instance
        val controller = readFieldValue(instance, listOf($$"this$0")) ?: return null
        return readFieldValue(controller, listOf("mView"))
    }

    fun getBatterySamsungDrawable(batteryMeterView: Any): Any? = readFieldValue(
        batteryMeterView,
        listOf("mSamsungDrawable", "samsungDrawable")
    )

    fun getBatteryState(samsungDrawable: Any): Any? = readFieldValue(
        samsungDrawable,
        listOf("batteryState", "mBatteryState")
    )

    fun isBatteryCharging(batteryMeterView: Any): Boolean {
        val meterCharging = readFieldValue(
            batteryMeterView,
            listOf("mCharging", "charging")
        ) as? Boolean == true
        if (meterCharging) return true

        val batteryState = getBatterySamsungDrawable(batteryMeterView)?.let(::getBatteryState) ?: return false
        return readFieldValue(batteryState, listOf("charging")) as? Boolean == true ||
                readFieldValue(batteryState, listOf("isDirectPowerMode")) as? Boolean == true
    }

    fun getBatteryLevel(batteryState: Any): Int? =
        readFieldValue(batteryState, listOf("level", "mLevel")) as? Int

    fun isChargingState(batteryState: Any): Boolean =
        readFieldValue(batteryState, listOf("charging")) as? Boolean == true ||
            readFieldValue(batteryState, listOf("isDirectPowerMode")) as? Boolean == true

    /**
     * 充電アイコンを表示すべきかの判定。満充電・充電保護中などを除外するSamsung純正の判定
     * `SamsungBatteryState.shouldShowChargingIcon()` をそのまま使い、失敗時のみ素朴な判定に落とす。
     */
    fun shouldShowChargingIcon(batteryState: Any): Boolean = runCatching {
        XposedHelpers.callMethod(batteryState, "shouldShowChargingIcon") as? Boolean
    }.getOrNull() ?: isChargingState(batteryState)

    fun getBatteryContext(samsungDrawable: Any): Context? =
        readFieldValue(samsungDrawable, listOf("context", "mContext")) as? Context

    fun getColorForLevel(samsungDrawable: Any, level: Int): Int? = runCatching {
        XposedHelpers.callMethod(samsungDrawable, "getColorForLevel", level) as? Int
    }.getOrNull()

    /**
     * 他のステータスバーアイコンと完全に同じ色を返す。
     *
     * Samsungはバッテリーアイコンにだけ専用の色（`status_bar_battery_frame_*_color`）を使うため、
     * 他のアイコンのグレーと微妙にズレる。`BatteryMeterView.mTextColor` には
     * `DarkIconDispatcher.getTint()` の結果、つまり他アイコンと同一のティントが入っているので、
     * こちらを使えばズレなく揃う。
     */
    fun getStatusBarIconTint(batteryMeterView: Any): Int? =
        readFieldValue(batteryMeterView, listOf("mTextColor", "textColor")) as? Int

    /**
     * 残量警告色（電池残量が少ない時の赤など）が適用されている場合はその色を返す。
     * 通常時はSamsungの`batteryLevelColor`がそのまま返るため、その場合はnullを返す。
     */
    fun getWarningColorForLevel(samsungDrawable: Any, level: Int): Int? {
        val color = getColorForLevel(samsungDrawable, level) ?: return null
        val normalColor = readFieldValue(samsungDrawable, listOf("batteryLevelColor")) as? Int
        return color.takeIf { it != normalColor }
    }

    @SuppressLint("DiscouragedApi")
    fun resolveBatteryChargingIconId(resources: Resources): Int {
        if (batteryChargingIconId != UNRESOLVED_CHARGING_ICON_ID) return batteryChargingIconId
        batteryChargingIconId = resources.getIdentifier(
            STATUS_BAR_CHARGING_ICON,
            "drawable",
            Package.SYSTEMUI
        ).takeIf { it != 0 } ?: resources.getIdentifier(
            FALLBACK_CHARGING_ICON,
            "drawable",
            Package.SYSTEMUI
        )
        return batteryChargingIconId
    }

    fun readFieldValue(instance: Any, names: List<String>): Any? =
        findField(instance, names)?.let { field -> runCatching { field.get(instance) }.getOrNull() }

    fun findField(instance: Any, names: List<String>): Field? {
        names.forEach { name ->
            XposedHelpers.findFieldIfExists(instance.javaClass, name)?.let { return it }
        }
        return null
    }
}
