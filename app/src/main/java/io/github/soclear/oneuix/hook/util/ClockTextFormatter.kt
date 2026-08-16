package io.github.soclear.oneuix.hook.util

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 状态栏时钟文本生成器。
 *
 * 时间部分与日期部分分别使用各自的 [DateTimeFormatter] 模式，
 * 日期部分通过 [RelativeSizeSpan] 单独缩放字号，因此不会影响时间部分的字号。
 *
 * 该类会缓存 [DateTimeFormatter]，并在系统语言（或用户指定的语言）变化时自动重建，
 * 避免每分钟都重新解析模式字符串。
 */
class ClockTextFormatter(
    timePattern: String,
    datePattern: String?,
    separator: String = " ",
    private val dateBeforeTime: Boolean = false,
    dateTextScale: Float = 1f,
    localeTag: String = "",
) {
    private val timePattern: String = timePattern.ifBlank { DEFAULT_TIME_PATTERN }
    private val datePattern: String? = datePattern?.takeIf { it.isNotBlank() }
    private val separator: String = if (this.datePattern == null) "" else separator
    private val dateTextScale: Float =
        dateTextScale.takeIf { it.isFinite() && it in MIN_SCALE..MAX_SCALE } ?: 1f

    /** 用户显式指定的语言。为 null 时跟随系统语言。 */
    private val fixedLocale: Locale? = parseLocale(localeTag)

    @Volatile
    private var cachedLocale: Locale? = null

    @Volatile
    private var timeFormatter: DateTimeFormatter = fallbackFormatter(DEFAULT_TIME_PATTERN, null)

    @Volatile
    private var dateFormatter: DateTimeFormatter? = null

    /** 时间与日期模式是否都无法解析。用于外部快速判断配置是否有效。 */
    val isTimePatternValid: Boolean = isPatternValid(this.timePattern)

    val isDatePatternValid: Boolean = this.datePattern?.let { isPatternValid(it) } != false

    /**
     * 生成状态栏时钟文本。
     *
     * 当日期部分存在且需要缩放或垂直微调时，返回带 span 的 [Spanned]；
     * 否则返回普通字符串，避免无谓的 span 开销。
     *
     * @param dateBaselineShiftPx 日期部分相对基线的垂直偏移，正值向上。时间部分不受影响
     */
    fun format(
        dateTime: LocalDateTime = LocalDateTime.now(),
        dateBaselineShiftPx: Int = 0,
    ): CharSequence {
        ensureFormatters()

        val time = runCatching { timeFormatter.format(dateTime) }.getOrDefault("")
        val formatter = dateFormatter ?: return time
        val date = runCatching { formatter.format(dateTime) }.getOrDefault("")
        if (date.isEmpty()) return time

        // 分隔符与日期一同缩放，这样时间和日期之间的间距会随日期字号等比变化，视觉上更协调。
        val scaledPart = if (dateBeforeTime) date + separator else separator + date
        if (dateTextScale == 1f && dateBaselineShiftPx == 0) {
            return if (dateBeforeTime) scaledPart + time else time + scaledPart
        }

        val builder = SpannableStringBuilder()
        val start: Int
        if (dateBeforeTime) {
            start = 0
            builder.append(scaledPart)
            builder.append(time)
        } else {
            builder.append(time)
            start = builder.length
            builder.append(scaledPart)
        }
        val end = start + scaledPart.length

        if (dateTextScale != 1f) {
            builder.setSpan(
                RelativeSizeSpan(dateTextScale),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (dateBaselineShiftPx != 0) {
            builder.setSpan(
                BaselineShiftSpan(dateBaselineShiftPx),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return builder
    }

    private fun ensureFormatters() {
        val locale = fixedLocale ?: Locale.getDefault(Locale.Category.FORMAT)
        if (locale == cachedLocale) return
        synchronized(this) {
            if (locale == cachedLocale) return
            timeFormatter = fallbackFormatter(timePattern, locale, DEFAULT_TIME_PATTERN)
            dateFormatter = datePattern?.let { fallbackFormatter(it, locale, DEFAULT_DATE_PATTERN) }
            cachedLocale = locale
        }
    }

    private companion object {
        const val DEFAULT_TIME_PATTERN = "HH:mm"
        const val DEFAULT_DATE_PATTERN = "EEEE"

        /** 缩放比例的允许区间，与设置界面的滑块范围保持一致。 */
        const val MIN_SCALE = 0.2f
        const val MAX_SCALE = 3f

        fun isPatternValid(pattern: String): Boolean = runCatching {
            DateTimeFormatter.ofPattern(pattern).format(LocalDateTime.now())
        }.isSuccess

        fun fallbackFormatter(
            pattern: String,
            locale: Locale?,
            fallbackPattern: String = pattern,
        ): DateTimeFormatter {
            fun build(p: String) = if (locale == null) {
                DateTimeFormatter.ofPattern(p)
            } else {
                DateTimeFormatter.ofPattern(p, locale)
            }
            return runCatching { build(pattern) }
                .recoverCatching { build(fallbackPattern) }
                .getOrElse { DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN) }
        }

        fun parseLocale(tag: String): Locale? {
            val trimmed = tag.trim()
            if (trimmed.isEmpty()) return null
            return runCatching {
                Locale.forLanguageTag(trimmed.replace('_', '-'))
            }.getOrNull()?.takeIf { it.language.isNotEmpty() }
        }
    }
}
