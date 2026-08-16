package io.github.soclear.oneuix.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** グループカードの角丸。Material 3 の大きめの角丸で「まとまり」を表現する。 */
private val GroupCornerRadius = 24.dp

/** ジャンプ先が画面の一番上に貼り付かないよう、少し上に余白を残してスクロールする。 */
private val JumpTopMargin = 12.dp

/** ハイライトを表示し続ける時間。目で追えるだけの長さがあれば十分。 */
private const val HIGHLIGHT_DURATION_MS = 2200L

/**
 * 設定項目の画面内での位置。検索結果から目的の項目までスクロールするために使う。
 *
 * キーは項目のタイトル文字列そのもの。各項目の呼び出し側に ID を振らせなくても
 * [settingAnchor] が自動で登録できるようにするための割り切りで、
 * 同じ画面内でタイトルが重複しない限り一意になる。
 *
 * 位置はスクロールのたびに更新されるため [androidx.compose.runtime.State] にはしていない。
 * 読み出すのは常にコンポジション外（スクロール実行時）なので、再コンポーズを起こす必要がない。
 */
class SettingAnchors {
    private val positions = HashMap<String, Float>()
    private var contentTop = 0f

    /** ハイライト中の項目のタイトル。描画フェーズでのみ読まれる。 */
    var highlighted by mutableStateOf<String?>(null)
        internal set

    internal fun putContentTop(y: Float) {
        contentTop = y
    }

    internal fun put(title: String, y: Float) {
        positions[title] = y
    }

    /**
     * スクロール領域の先頭を 0 とした [title] の位置。
     * 未表示（折りたたまれた詳細設定など）の項目は登録されていないため null を返す。
     */
    fun offsetOf(title: String): Float? = positions[title]?.let { it - contentTop }
}

/**
 * 検索結果から特定の設定項目へ移動する要求。
 * 画面遷移とスクロールが別のタイミングで起きるため、要求だけを先に立てておき、
 * 移動先の [SettingsPane] が表示されてから消化する。
 */
@Stable
class SettingJumpController {
    var target by mutableStateOf<String?>(null)
        private set

    fun request(title: String) {
        target = title
    }

    internal fun consume(title: String) {
        if (target == title) target = null
    }
}

val LocalSettingAnchors = staticCompositionLocalOf<SettingAnchors?> { null }
val LocalSettingJump = staticCompositionLocalOf<SettingJumpController?> { null }

/**
 * 設定項目に検索ジャンプ用の目印を付ける。
 *
 * 位置の登録はレイアウトフェーズ、ハイライトの描画は描画フェーズで行うため、
 * スクロール中に再コンポーズが走ることはない。
 */
@Composable
fun Modifier.settingAnchor(title: String): Modifier {
    val anchors = LocalSettingAnchors.current ?: return this
    val highlightColor = MaterialTheme.colorScheme.secondaryContainer
    return this
        .onGloballyPositioned { anchors.put(title, it.localToRoot(Offset.Zero).y) }
        .drawBehind {
            if (anchors.highlighted == title) drawRect(color = highlightColor)
        }
}

/**
 * 設定画面のスクロール領域。
 *
 * 検索から [SettingJumpController.target] が指定されていれば、該当項目まで
 * スクロールして一時的にハイライトする。
 */
@Composable
fun SettingsPane(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val anchors = remember { SettingAnchors() }
    val jump = LocalSettingJump.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val target = jump?.target

    LaunchedEffect(target) {
        if (jump == null || target == null) return@LaunchedEffect
        // 画面遷移直後はまだレイアウトが終わっておらず位置が分からないので、少しだけ待つ。
        var offset = anchors.offsetOf(target)
        var remainingFrames = 30
        while (offset == null && remainingFrames > 0) {
            delay(16)
            offset = anchors.offsetOf(target)
            remainingFrames--
        }
        // この画面に無い項目（＝遷移前の画面が残っている場合）は何もしない。
        // 要求を消化してしまうと、目的の画面が表示されたときにジャンプできなくなる。
        val itemOffset = offset ?: return@LaunchedEffect
        val topMargin = with(density) { JumpTopMargin.toPx() }
        scrollState.animateScrollTo((itemOffset - topMargin).coerceAtLeast(0f).roundToInt())
        anchors.highlighted = target
        delay(HIGHLIGHT_DURATION_MS)
        anchors.highlighted = null
        jump.consume(target)
    }

    CompositionLocalProvider(LocalSettingAnchors provides anchors) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .onGloballyPositioned { anchors.putContentTop(it.localToRoot(Offset.Zero).y) }
        ) {
            content()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 関連する設定項目をひとまとめにする角丸カード。
 *
 * @param titleRes カードの上に置く見出し。null の場合は見出しなしのカードになる
 */
@Composable
fun SettingsGroup(
    @StringRes titleRes: Int? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        titleRes?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 10.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(GroupCornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}
