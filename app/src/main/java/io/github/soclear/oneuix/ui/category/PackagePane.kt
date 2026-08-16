package io.github.soclear.oneuix.ui.category

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.soclear.oneuix.ui.component.SettingsGroup
import io.github.soclear.oneuix.ui.component.SettingsPane

/**
 * アプリ 1 つ分の設定画面。
 *
 * 項目数が少ないので見出しは付けず、まとめて 1 枚のカードに収める。
 * [SettingsPane] を通すことで、検索結果からこの画面の項目へジャンプできるようになる。
 */
@Composable
internal fun PackagePane(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsPane(modifier = modifier) {
        SettingsGroup(content = content)
    }
}
