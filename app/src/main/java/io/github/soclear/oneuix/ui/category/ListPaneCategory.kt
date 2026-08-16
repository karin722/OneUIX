package io.github.soclear.oneuix.ui.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.github.soclear.oneuix.R
import io.github.soclear.oneuix.data.ONE_UI_VERSION
import io.github.soclear.oneuix.ui.SettingSearchEntry
import io.github.soclear.oneuix.ui.SettingSearchIndex

private val CardShape = RoundedCornerShape(24.dp)

/**
 * 検索用にあらかじめ解決しておいた 1 項目分の文字列。
 * 入力のたびに `Resources.getString` を呼ばずに済むよう、最初に一度だけ作る。
 */
private data class SearchableSetting(
    val entry: SettingSearchEntry,
    val title: String,
    /** 検索結果に出す所在地。見出しがある画面はその見出し、無ければ対象アプリ名。 */
    val subtitle: String,
    val haystack: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPaneCategory(
    categoryAppInfoList: List<CategoryAppInfo>,
    onItemClick: (Category) -> Unit,
    onSearchResultClick: (SettingSearchEntry, String) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var query by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    val searchableSettings = remember(context, categoryAppInfoList) {
        val resources = context.resources
        val labels = categoryAppInfoList.associate { it.category to it.label }
        SettingSearchIndex.map { entry ->
            val title = resources.getString(entry.titleRes)
            val summary = entry.summaryRes?.let(resources::getString).orEmpty()
            val subtitle = entry.groupRes?.let(resources::getString)
                ?: labels[entry.category].orEmpty()
            SearchableSetting(entry, title, subtitle, "$title\n$summary")
        }
    }
    val results = remember(query, searchableSettings) {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            emptyList()
        } else {
            searchableSettings.filter { it.haystack.contains(keyword, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                actions = { OverflowMenu(onBackup = onBackup, onRestore = onRestore) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "search") {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (query.isNotBlank()) {
                if (results.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(id = R.string.search_no_result),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
                        )
                    }
                } else {
                    items(results, key = { it.entry.titleRes }) { result ->
                        SearchResultItem(
                            result = result,
                            onClick = {
                                // 結果を開くときはキーボードを閉じる
                                focusManager.clearFocus()
                                onSearchResultClick(result.entry, result.title)
                            }
                        )
                    }
                }
                return@LazyColumn
            }

            item(key = "status") {
                StatusCard(modifier = Modifier.padding(horizontal = 16.dp))
            }
            items(categoryAppInfoList, key = { it.category.name }) { appInfo ->
                CategoryCard(
                    appInfo = appInfo,
                    onClick = { onItemClick(appInfo.category) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item(key = "about") {
                AboutCard(
                    onClick = { onItemClick(Category.About) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun OverflowMenu(onBackup: () -> Unit, onRestore: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            ImageVector.vectorResource(id = R.drawable.more_vert),
            contentDescription = stringResource(id = R.string.more_options)
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.backup_config)) },
            leadingIcon = {
                Icon(ImageVector.vectorResource(id = R.drawable.backup_save), null)
            },
            onClick = {
                expanded = false
                onBackup()
            }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.restore_config)) },
            leadingIcon = {
                Icon(ImageVector.vectorResource(id = R.drawable.restore_open), null)
            },
            onClick = {
                expanded = false
                onRestore()
            }
        )
    }
}

/**
 * 検索欄。
 *
 * 未使用時は入力欄を「置かない」。Compose は最初のフォーカス可能な要素に
 * 自動でフォーカスを渡すため、`OutlinedTextField` を常設すると起動しただけで
 * キーボードが開いてしまう。タップされてから入力欄に差し替える。
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    if (!active) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = modifier
                .fillMaxWidth()
                .clickable { onActiveChange(true) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = stringResource(id = R.string.search_settings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(text = stringResource(id = R.string.search_settings)) },
        leadingIcon = {
            Icon(ImageVector.vectorResource(id = R.drawable.search), contentDescription = null)
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    onQueryChange("")
                    focusManager.clearFocus()
                    onActiveChange(false)
                }
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.close),
                    contentDescription = stringResource(id = R.string.search_clear)
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun SearchResultItem(result: SearchableSetting, onClick: () -> Unit) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        ListItem(
            headlineContent = { Text(result.title) },
            supportingContent = { Text(result.subtitle) },
            trailingContent = {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.chevron_right),
                    contentDescription = null
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

/**
 * モジュールの状態カード。
 * この画面が出ている時点でモジュールは有効（無効なら `ModuleDisabledScreen` になる）なので、
 * 状態表示に加えて、変更を反映するための System UI 再起動をここにまとめている。
 */
@Composable
private fun StatusCard(modifier: Modifier = Modifier) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.module_enabled),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = oneUiVersionLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = {
                    runCatching {
                        Runtime.getRuntime().exec("su -c killall com.android.systemui")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.power_settings_new),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(id = R.string.restartSystemUI))
            }
        }
    }
}

@Composable
private fun oneUiVersionLabel(): String = if (ONE_UI_VERSION <= 0) {
    stringResource(id = R.string.about_unknown)
} else {
    stringResource(
        id = R.string.one_ui_version_format,
        "${ONE_UI_VERSION / 10000}.${ONE_UI_VERSION / 100 % 100}"
    )
}

@Composable
private fun CategoryCard(
    appInfo: CategoryAppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = appInfo.label,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            leadingContent = {
                Image(
                    bitmap = appInfo.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            },
            trailingContent = {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.chevron_right),
                    contentDescription = null
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

@Composable
private fun AboutCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.about_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = { Text(text = stringResource(id = R.string.about_summary)) },
            leadingContent = {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.info),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            },
            trailingContent = {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.chevron_right),
                    contentDescription = null
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}
