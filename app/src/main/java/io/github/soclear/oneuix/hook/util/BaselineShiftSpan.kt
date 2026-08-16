package io.github.soclear.oneuix.hook.util

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * 让被标记的文字相对基线垂直偏移。
 *
 * 状态栏时钟把时间与日期放在同一个 [android.widget.TextView] 里，二者共用同一条基线。
 * 日期通常比时间小一号，共用基线会让日期看起来偏下，这个 span 用来做微调。
 *
 * @param shiftPx 正值表示向上移动。Android 的 [TextPaint.baselineShift] 以向下为正，故取负号。
 */
class BaselineShiftSpan(private val shiftPx: Int) : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.baselineShift -= shiftPx
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        textPaint.baselineShift -= shiftPx
    }
}
