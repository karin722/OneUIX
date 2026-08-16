package io.github.soclear.oneuix.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import io.github.soclear.oneuix.R
import io.github.soclear.oneuix.ui.category.Category
import io.github.soclear.oneuix.ui.category.CategoryAppInfo
import io.github.soclear.oneuix.ui.category.DetailPaneAbout
import io.github.soclear.oneuix.ui.category.DetailPaneAndroid
import io.github.soclear.oneuix.ui.category.DetailPaneCall
import io.github.soclear.oneuix.ui.category.DetailPaneCamera
import io.github.soclear.oneuix.ui.category.DetailPaneOther
import io.github.soclear.oneuix.ui.category.DetailPaneSettings
import io.github.soclear.oneuix.ui.category.DetailPaneSystemUI
import io.github.soclear.oneuix.ui.category.ListPaneCategory
import io.github.soclear.oneuix.ui.category.onAndroidEvent
import io.github.soclear.oneuix.ui.category.onCallEvent
import io.github.soclear.oneuix.ui.category.onCameraEvent
import io.github.soclear.oneuix.ui.category.onOtherEvent
import io.github.soclear.oneuix.ui.category.onSettingsEvent
import io.github.soclear.oneuix.ui.category.onSystemUIEvent
import io.github.soclear.oneuix.ui.component.LocalSettingJump
import io.github.soclear.oneuix.ui.component.SettingJumpController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingScreen(viewModel: SettingViewModel, modifier: Modifier = Modifier) {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Category>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val categoryAppInfoList by viewModel.categoryAppInfoList.collectAsStateWithLifecycle()
    val preference by viewModel.preference.collectAsStateWithLifecycle()
    val jumpController = remember { SettingJumpController() }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    viewModel.backupTo(stream)
                }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    try {
                        viewModel.restoreFrom(stream)
                    } catch (_: Throwable) {
                        Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalSettingJump provides jumpController) {
        NavigableListDetailPaneScaffold(
            navigator = scaffoldNavigator,
            listPane = {
                AnimatedPane {
                    ListPaneCategory(
                        categoryAppInfoList = categoryAppInfoList,
                        onItemClick = { category ->
                            scope.launch {
                                scaffoldNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    category
                                )
                            }
                        },
                        onSearchResultClick = { entry, title ->
                            // 遷移先の画面が表示されてから、この項目までスクロールさせる。
                            jumpController.request(title)
                            scope.launch {
                                scaffoldNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    entry.category
                                )
                            }
                        },
                        onBackup = {
                            val name = "OneUIX_backup_${
                                LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            }.json"
                            backupLauncher.launch(name)
                        },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json")) }
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    scaffoldNavigator.currentDestination?.contentKey?.let { category ->
                        DetailScaffold(
                            title = categoryLabel(category, categoryAppInfoList),
                            showBack = scaffoldNavigator.canNavigateBack(),
                            onBack = { scope.launch { scaffoldNavigator.navigateBack() } },
                            canReset = category.hasPreferences,
                            onReset = { viewModel.resetCategory(category) }
                        ) { contentModifier ->
                            when (category) {
                                Category.Android -> DetailPaneAndroid(
                                    uiState = preference.android,
                                    onEvent = viewModel::onAndroidEvent,
                                    modifier = contentModifier
                                )

                                Category.SystemUI -> DetailPaneSystemUI(
                                    uiState = preference.systemUI,
                                    onEvent = viewModel::onSystemUIEvent,
                                    modifier = contentModifier
                                )

                                Category.Settings -> DetailPaneSettings(
                                    uiState = preference.settings,
                                    onEvent = viewModel::onSettingsEvent,
                                    modifier = contentModifier
                                )

                                Category.Call -> DetailPaneCall(
                                    uiState = preference.call,
                                    onEvent = viewModel::onCallEvent,
                                    modifier = contentModifier
                                )

                                Category.Camera -> DetailPaneCamera(
                                    uiState = preference.camera,
                                    onEvent = viewModel::onCameraEvent,
                                    modifier = contentModifier
                                )

                                Category.Other -> DetailPaneOther(
                                    uiState = preference.other,
                                    onEvent = viewModel::onOtherEvent,
                                    modifier = contentModifier
                                )

                                Category.About -> DetailPaneAbout(modifier = contentModifier)
                            }
                        }
                    }
                }
            },
            modifier = modifier.fillMaxSize(),
        )
    }
}

/**
 * 詳細ペインの外枠。タイトル・戻る・リセットをここにまとめ、
 * 各カテゴリの画面は設定項目の中身だけに集中できるようにしている。
 *
 * @param canReset false の場合はリセットを出さない（「One UI X について」など）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScaffold(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    canReset: Boolean,
    onReset: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scrollBehavior =
        TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var confirmingReset by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                ImageVector.vectorResource(id = R.drawable.arrow_back),
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    }
                },
                actions = {
                    if (canReset) {
                        IconButton(onClick = { confirmingReset = true }) {
                            Icon(
                                ImageVector.vectorResource(id = R.drawable.reset_settings),
                                contentDescription = stringResource(id = R.string.reset_category)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }

    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            title = { Text(text = stringResource(id = R.string.reset_category)) },
            text = { Text(text = stringResource(id = R.string.reset_category_message, title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingReset = false
                        onReset()
                    }
                ) {
                    Text(text = stringResource(id = R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReset = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

/** 詳細ペインのタイトル。設定を持つカテゴリは対象アプリの表示名をそのまま使う。 */
@Composable
private fun categoryLabel(
    category: Category,
    categoryAppInfoList: List<CategoryAppInfo>
): String = if (category == Category.About) {
    stringResource(id = R.string.about_title)
} else {
    categoryAppInfoList.firstOrNull { it.category == category }?.label.orEmpty()
}

@Composable
fun ModuleDisabledScreen(
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.module_disabled_tip))
        Button(
            onClick = onClickClose,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(text = stringResource(R.string.close))
        }
    }
}
