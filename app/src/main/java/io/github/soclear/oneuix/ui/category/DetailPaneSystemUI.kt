package io.github.soclear.oneuix.ui.category

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.github.soclear.oneuix.R
import io.github.soclear.oneuix.data.ONE_UI_VERSION
import io.github.soclear.oneuix.data.PowerMenuAction
import io.github.soclear.oneuix.data.Preference
import io.github.soclear.oneuix.ui.SettingViewModel
import io.github.soclear.oneuix.ui.component.SelectItem
import io.github.soclear.oneuix.ui.component.SettingsGroup
import io.github.soclear.oneuix.ui.component.SettingsPane
import io.github.soclear.oneuix.ui.component.SwitchItem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val ESIM_ADAPTER_SIM_BOTH = 2

@Composable
fun DetailPaneSystemUI(
    uiState: Preference.SystemUI,
    onEvent: (SystemUIEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPane(modifier = modifier) {
        SettingsGroup(R.string.group_status_bar_layout) {
            Column {
                var padding by remember {
                    mutableFloatStateOf(uiState.statusBar.statusBarLeftPaddingDp)
                }
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.statusBarLeftPaddingDp_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.modifyStatusBarLeftPadding) {
                        "%.1fdp".format(padding)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.modifyStatusBarLeftPadding,
                    onCheckedChange = {
                        if (it && padding == 0f) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarLeftPadding(it))
                    }
                )
                AnimatedVisibility(expanded && uiState.statusBar.modifyStatusBarLeftPadding) {
                    Slider(
                        value = padding,
                        onValueChange = { padding = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        valueRange = 0f..100f,
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.StatusBar.StatusBarLeftPaddingDp(padding))
                        }
                    )
                }
            }
            Column {
                var padding by remember {
                    mutableFloatStateOf(uiState.statusBar.statusBarRightPaddingDp)
                }
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.statusBarRightPaddingDp_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.modifyStatusBarRightPadding) {
                        "%.1fdp".format(padding)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.modifyStatusBarRightPadding,
                    onCheckedChange = {
                        if (it && padding == 0f) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarRightPadding(it))
                    }
                )
                AnimatedVisibility(expanded && uiState.statusBar.modifyStatusBarRightPadding) {
                    Slider(
                        value = padding,
                        onValueChange = { padding = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        valueRange = 0f..100f,
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.StatusBar.StatusBarRightPaddingDp(padding))
                        }
                    )
                }
            }
        }
        SettingsGroup(R.string.group_status_bar_battery) {
            Column {
                var widthScale by remember {
                    mutableFloatStateOf(uiState.statusBar.batteryIconWidthScale)
                }
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.setBatteryIconWidthScale_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.setBatteryIconWidthScale) {
                        "%.2f".format(widthScale)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.setBatteryIconWidthScale,
                    onCheckedChange = {
                        if (it && widthScale == 0f) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetBatteryIconWidthScale(it))
                    }
                )
                AnimatedVisibility(expanded && uiState.statusBar.setBatteryIconWidthScale) {
                    Slider(
                        value = widthScale,
                        onValueChange = { widthScale = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        valueRange = 0.5f..2f,
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.StatusBar.BatteryIconWidthScale(widthScale))
                        }
                    )
                }
            }
            Column {
                var heightScale by remember {
                    mutableFloatStateOf(uiState.statusBar.batteryIconHeightScale)
                }
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.setBatteryIconHeightScale_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.setBatteryIconHeightScale) {
                        "%.2f".format(heightScale)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.setBatteryIconHeightScale,
                    onCheckedChange = {
                        if (it && heightScale == 0f) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetBatteryIconHeightScale(it))
                    }
                )
                AnimatedVisibility(expanded && uiState.statusBar.setBatteryIconHeightScale) {
                    Slider(
                        value = heightScale,
                        onValueChange = { heightScale = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        valueRange = 0.5f..2f,
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.StatusBar.BatteryIconHeightScale(heightScale))
                        }
                    )
                }
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    title = stringResource(id = R.string.hideBatteryPercentageSign_title),
                    summary = stringResource(id = R.string.hideBatteryPercentageSign_summary),
                    checked = uiState.statusBar.hideBatteryPercentageSign,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.HideBatteryPercentageSign(it))
                    }
                )
            }
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.battery),
                title = stringResource(id = R.string.hideBatteryIcon_title),
                summary = stringResource(id = R.string.hideBatteryIcon_summary),
                checked = uiState.statusBar.hideBatteryIcon,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.HideBatteryIcon(it))
                }
            )
            Column {
                var sizeDp by remember {
                    mutableFloatStateOf(uiState.statusBar.circleBatteryIconSizeDp)
                }
                var horizontalPaddingDp by remember {
                    mutableFloatStateOf(uiState.statusBar.circleBatteryIconHorizontalPaddingDp)
                }
                var verticalPaddingDp by remember {
                    mutableFloatStateOf(uiState.statusBar.circleBatteryIconVerticalPaddingDp)
                }

                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    title = stringResource(id = R.string.useCircleBatteryIcon_title),
                    summary = stringResource(id = R.string.useCircleBatteryIcon_summary),
                    checked = uiState.statusBar.useCircleBatteryIcon,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.UseCircleBatteryIcon(it))
                    }
                )
                AnimatedVisibility(uiState.statusBar.useCircleBatteryIcon) {
                    Column {
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(id = R.string.circleBatteryIconSizeDp_title))
                            },
                            supportingContent = { Text(text = "%.0fdp".format(sizeDp)) },
                            leadingContent = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.battery),
                                    contentDescription = null
                                )
                            }
                        )
                        Slider(
                            value = sizeDp,
                            onValueChange = { sizeDp = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = 8f..28f,
                            onValueChangeFinished = {
                                onEvent(SystemUIEvent.StatusBar.CircleBatteryIconSizeDp(sizeDp))
                            }
                        )
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(id = R.string.circleBatteryIconHorizontalPaddingDp_title))
                            },
                            supportingContent = { Text(text = "%.0fdp".format(horizontalPaddingDp)) },
                            leadingContent = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.battery),
                                    contentDescription = null
                                )
                            }
                        )
                        Slider(
                            value = horizontalPaddingDp,
                            onValueChange = { horizontalPaddingDp = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = 0f..8f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.CircleBatteryIconHorizontalPaddingDp(
                                        horizontalPaddingDp
                                    )
                                )
                            }
                        )
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(id = R.string.circleBatteryIconVerticalPaddingDp_title))
                            },
                            supportingContent = { Text(text = "%.0fdp".format(verticalPaddingDp)) },
                            leadingContent = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.battery),
                                    contentDescription = null
                                )
                            }
                        )
                        Slider(
                            value = verticalPaddingDp,
                            onValueChange = { verticalPaddingDp = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = 0f..8f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.CircleBatteryIconVerticalPaddingDp(
                                        verticalPaddingDp
                                    )
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            if (ONE_UI_VERSION >= 70000) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    title = stringResource(id = R.string.addBatteryLevelText_title),
                    summary = stringResource(id = R.string.addBatteryLevelText_summary),
                    checked = uiState.statusBar.addBatteryLevelText,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.AddBatteryLevelText(it))
                    }
                )
                AnimatedVisibility(uiState.statusBar.addBatteryLevelText) {
                    Column {
                        var percentSignScale by remember {
                            mutableFloatStateOf(uiState.statusBar.batteryLevelTextPercentSignScale)
                        }
                        var marginStartDp by remember {
                            mutableFloatStateOf(uiState.statusBar.batteryLevelTextMarginStartDp)
                        }
                        var textSizeScale by remember {
                            mutableFloatStateOf(uiState.statusBar.batteryLevelTextSizeScale)
                        }
                        var textOffsetDp by remember {
                            mutableFloatStateOf(uiState.statusBar.batteryLevelTextOffsetDp)
                        }

                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.battery),
                            title = stringResource(id = R.string.hideBatteryLevelTextPercentageSign_title),
                            checked = uiState.statusBar.hideBatteryLevelTextPercentageSign,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.StatusBar.HideBatteryLevelTextPercentageSign(it))
                            }
                        )
                        // 「%」だけを小さくする、旧Jailbreak Tweak風の表示にできる
                        AnimatedVisibility(!uiState.statusBar.hideBatteryLevelTextPercentageSign) {
                            Column {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = stringResource(
                                                id = R.string.batteryLevelTextPercentSignScale_title
                                            )
                                        )
                                    },
                                    supportingContent = {
                                        Text(text = "%.2fx".format(percentSignScale))
                                    },
                                    leadingContent = {
                                        Icon(
                                            ImageVector.vectorResource(id = R.drawable.format_size),
                                            contentDescription = null
                                        )
                                    }
                                )
                                Slider(
                                    value = percentSignScale,
                                    onValueChange = { percentSignScale = it },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    valueRange = 0.3f..1.5f,
                                    onValueChangeFinished = {
                                        onEvent(
                                            SystemUIEvent.StatusBar.BatteryLevelTextPercentSignScale(
                                                percentSignScale
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(
                                        id = R.string.batteryLevelTextSizeScale_title
                                    )
                                )
                            },
                            supportingContent = { Text(text = "%.2fx".format(textSizeScale)) },
                            leadingContent = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.format_size),
                                    contentDescription = null
                                )
                            }
                        )
                        Slider(
                            value = textSizeScale,
                            onValueChange = { textSizeScale = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = 0.5f..1.5f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.BatteryLevelTextSizeScale(textSizeScale)
                                )
                            }
                        )
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(
                                        id = R.string.batteryLevelTextMarginStartDp_title
                                    )
                                )
                            },
                            supportingContent = { Text(text = "%.1fdp".format(marginStartDp)) },
                            leadingContent = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.padding),
                                    contentDescription = null
                                )
                            }
                        )
                        Slider(
                            value = marginStartDp,
                            onValueChange = { marginStartDp = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = -8f..16f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.BatteryLevelTextMarginStartDp(
                                        marginStartDp
                                    )
                                )
                            }
                        )
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(
                                        id = R.string.batteryLevelTextOffsetDp_title
                                    )
                                )
                            },
                            supportingContent = { Text(text = "%.1fdp".format(textOffsetDp)) },
                            leadingContent = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.padding),
                                    contentDescription = null
                                )
                            }
                        )
                        Slider(
                            value = textOffsetDp,
                            onValueChange = { textOffsetDp = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = -8f..8f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.BatteryLevelTextOffsetDp(textOffsetDp)
                                )
                            }
                        )
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.battery),
                            title = stringResource(id = R.string.hideBatteryLevelTextChargingIcon_title),
                            summary = stringResource(id = R.string.hideBatteryLevelTextChargingIcon_summary),
                            checked = uiState.statusBar.hideBatteryLevelTextChargingIcon,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.StatusBar.HideBatteryLevelTextChargingIcon(it))
                            }
                        )
                    }
                }
            }
        }
        SettingsGroup(R.string.group_status_bar_network) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.net_speed),
                title = stringResource(id = R.string.supportRealTimeNetworkSpeed_title),
                summary = stringResource(id = R.string.supportRealTimeNetworkSpeed_summary),
                checked = uiState.statusBar.supportRealTimeNetworkSpeed,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.SupportRealTimeNetworkSpeed(it))
                }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.net_speed),
                title = stringResource(id = R.string.showSeparateUpDownNetworkSpeeds_title),
                summary = stringResource(id = R.string.showSeparateUpDownNetworkSpeeds_summary),
                checked = uiState.statusBar.showSeparateUpDownNetworkSpeeds,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.ShowSeparateUpDownNetworkSpeeds(it))
                }
            )
        }
        SettingsGroup(R.string.group_status_bar_clock) {
            Column {
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.setStatusBarClockFormat_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.setStatusBarClockFormat) {
                        uiState.statusBar.statusBarClockFormat
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.nest_clock_farsight_digital),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.setStatusBarClockFormat,
                    onCheckedChange = {
                        if (it && uiState.statusBar.statusBarClockFormat == "HH:mm") {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetStatusBarClockFormat(it))
                    }
                )

                AnimatedVisibility(expanded && uiState.statusBar.setStatusBarClockFormat) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        var tempDataTimeFormat by remember {
                            mutableStateOf(uiState.statusBar.statusBarClockFormat)
                        }
                        var label by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = tempDataTimeFormat,
                            onValueChange = { tempDataTimeFormat = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(text = label) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                var right = true
                                label = try {
                                    DateTimeFormatter
                                        .ofPattern(tempDataTimeFormat)
                                        .format(LocalDateTime.now())
                                } catch (_: Throwable) {
                                    right = false
                                    "error"
                                }
                                if (right) {
                                    onEvent(
                                        SystemUIEvent.StatusBar.StatusBarClockFormat(
                                            tempDataTimeFormat
                                        )
                                    )
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.confirm))
                        }
                    }
                }
            }
            Column {
                var expanded by rememberSaveable { mutableStateOf(false) }
                var dateTextScale by remember {
                    mutableFloatStateOf(uiState.statusBar.statusBarClockDateTextScale)
                }
                var dateOffsetDp by remember {
                    mutableFloatStateOf(uiState.statusBar.statusBarClockDateOffsetDp)
                }
                val dateLocale = rememberDateLocale(uiState.statusBar.statusBarClockDateLocale)

                SwitchItem(
                    title = stringResource(id = R.string.appendStatusBarClockDate_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.appendStatusBarClockDate) {
                        val preview = clockPreview(
                            timeFormat = uiState.statusBar.statusBarClockFormat,
                            dateFormat = uiState.statusBar.statusBarClockDateFormat,
                            separator = uiState.statusBar.statusBarClockDateSeparator,
                            dateBeforeTime = uiState.statusBar.statusBarClockDateBeforeTime,
                            locale = dateLocale,
                        )
                        // preview はユーザー入力由来なので、書式指定文字列としては使わない
                        preview + "   " + "%.2fx".format(dateTextScale)
                    } else {
                        stringResource(id = R.string.appendStatusBarClockDate_summary)
                    },
                    icon = ImageVector.vectorResource(id = R.drawable.today),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.appendStatusBarClockDate,
                    onCheckedChange = {
                        expanded = it
                        onEvent(SystemUIEvent.StatusBar.AppendStatusBarClockDate(it))
                    }
                )

                AnimatedVisibility(expanded && uiState.statusBar.appendStatusBarClockDate) {
                    Column {
                        // 日期格式。E=木, EEEE=木曜日, M/d(E)=8/14(木) など
                        ConfirmableTextField(
                            initialValue = uiState.statusBar.statusBarClockDateFormat,
                            label = stringResource(id = R.string.statusBarClockDateFormat_label),
                            preview = { input ->
                                runCatching {
                                    DateTimeFormatter
                                        .ofPattern(input, dateLocale)
                                        .format(LocalDateTime.now())
                                }.getOrNull()
                            },
                            onConfirm = {
                                onEvent(SystemUIEvent.StatusBar.StatusBarClockDateFormat(it))
                            }
                        )
                        // 時刻と日付の区切り文字。半角スペースや " · " など
                        ConfirmableTextField(
                            initialValue = uiState.statusBar.statusBarClockDateSeparator,
                            label = stringResource(id = R.string.statusBarClockDateSeparator_label),
                            preview = { "[$it]" },
                            onConfirm = {
                                onEvent(SystemUIEvent.StatusBar.StatusBarClockDateSeparator(it))
                            }
                        )
                        // 日付の言語。空欄ならシステム言語に追従
                        ConfirmableTextField(
                            initialValue = uiState.statusBar.statusBarClockDateLocale,
                            label = stringResource(id = R.string.statusBarClockDateLocale_label),
                            preview = { input ->
                                val locale = parseLocaleTag(input) ?: Locale.getDefault()
                                runCatching {
                                    DateTimeFormatter
                                        .ofPattern(
                                            uiState.statusBar.statusBarClockDateFormat,
                                            locale
                                        )
                                        .format(LocalDateTime.now())
                                }.getOrNull()
                            },
                            onConfirm = {
                                onEvent(SystemUIEvent.StatusBar.StatusBarClockDateLocale(it))
                            }
                        )
                        SwitchItem(
                            title = stringResource(id = R.string.statusBarClockDateBeforeTime_title),
                            summary = stringResource(id = R.string.statusBarClockDateBeforeTime_summary),
                            icon = ImageVector.vectorResource(id = R.drawable.tab_move),
                            checked = uiState.statusBar.statusBarClockDateBeforeTime,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.StatusBar.StatusBarClockDateBeforeTime(it))
                            }
                        )
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(
                                        id = R.string.statusBarClockDateTextScale_title
                                    )
                                )
                            },
                            supportingContent = { Text(text = "%.2fx".format(dateTextScale)) },
                            leadingContent = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.format_size),
                                    contentDescription = null
                                )
                            }
                        )
                        Slider(
                            value = dateTextScale,
                            onValueChange = { dateTextScale = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = 0.3f..1.5f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.StatusBarClockDateTextScale(dateTextScale)
                                )
                            }
                        )
                        // 日付は時刻と基線を共有するため、小さい文字だと下寄りに見える。その微調整
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(
                                        id = R.string.statusBarClockDateOffsetDp_title
                                    )
                                )
                            },
                            supportingContent = { Text(text = "%.1fdp".format(dateOffsetDp)) },
                            leadingContent = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.today),
                                    contentDescription = null
                                )
                            }
                        )
                        Slider(
                            value = dateOffsetDp,
                            onValueChange = { dateOffsetDp = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = -4f..4f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.StatusBarClockDateOffsetDp(dateOffsetDp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            Column {
                var scale by remember {
                    mutableFloatStateOf(uiState.statusBar.statusBarClockTextScale)
                }
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.setStatusBarClockTextScale_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.setStatusBarClockTextScale) {
                        "%.2fx".format(scale)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.format_size),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.setStatusBarClockTextScale,
                    onCheckedChange = {
                        if (it && scale == 1f) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetStatusBarClockTextScale(it))
                    }
                )
                AnimatedVisibility(expanded && uiState.statusBar.setStatusBarClockTextScale) {
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        valueRange = 0.5f..2.5f,
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.StatusBar.StatusBarClockTextScale(scale))
                        }
                    )
                }
            }
            SwitchItem(
                title = stringResource(id = R.string.updateStatusBarClockEverySecond_title),
                summary = stringResource(id = R.string.updateStatusBarClockEverySecond_summary),
                icon = ImageVector.vectorResource(id = R.drawable.nest_clock_farsight_digital),
                checked = uiState.statusBar.updateStatusBarClockEverySecond,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.UpdateStatusBarClockEverySecond(it))
                }
            )
        }
        SettingsGroup(R.string.group_status_bar_icons) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.folder_managed),
                title = stringResource(id = R.string.hideSecureFolderStatusBarIcon_title),
                checked = uiState.statusBar.hideSecureFolderStatusBarIcon,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.HideSecureFolderStatusBarIcon(it))
                }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.bluetooth),
                title = stringResource(id = R.string.restoreBluetoothStatusBarIcon_title),
                checked = uiState.statusBar.restoreBluetoothStatusBarIcon,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.RestoreBluetoothStatusBarIcon(it))
                }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                title = stringResource(id = R.string.physicalEsimAdapterWorkaround_title),
                summary = stringResource(id = R.string.physicalEsimAdapterWorkaround_summary),
                checked = uiState.statusBar.physicalEsimAdapterWorkaround,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.PhysicalEsimAdapterWorkaround(it))
                }
            )
            AnimatedVisibility(uiState.statusBar.physicalEsimAdapterWorkaround) {
                SelectItem(
                    icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                    title = stringResource(id = R.string.physicalEsimAdapterSimSlot_title),
                    entries = listOf(
                        stringResource(id = R.string.sim_slot_1),
                        stringResource(id = R.string.sim_slot_2),
                        stringResource(id = R.string.sim_slot_both)
                    ),
                    selectedIndex = uiState.statusBar.physicalEsimAdapterSimSlot.coerceIn(
                        0,
                        ESIM_ADAPTER_SIM_BOTH
                    ),
                    onSelectedIndexChange = {
                        onEvent(SystemUIEvent.StatusBar.PhysicalEsimAdapterSimSlot(it))
                    }
                )
            }
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
                title = stringResource(id = R.string.doubleTapStatusBarToSleep_title),
                checked = uiState.statusBar.doubleTapStatusBarToSleep,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.DoubleTapStatusBarToSleep(it))
                }
            )
            Column {
                var max by remember {
                    mutableIntStateOf(uiState.statusBar.statusBarMaxNotificationIcons)
                }
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.setStatusBarMaxNotificationIcons_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.modifyStatusBarMaxNotificationIcons) " $max" else null,
                    icon = ImageVector.vectorResource(id = R.drawable.notifications),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.modifyStatusBarMaxNotificationIcons,
                    onCheckedChange = {
                        if (it && max == 4) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarMaxNotificationIcons(it))
                    }
                )
                AnimatedVisibility(expanded && uiState.statusBar.modifyStatusBarMaxNotificationIcons) {
                    Slider(
                        value = max.toFloat(),
                        onValueChange = { max = it.roundToInt() },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        valueRange = 4f..20f,
                        steps = 15,
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.StatusBar.StatusBarMaxNotificationIcons(max))
                        }
                    )
                }
            }
            Column {
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.setCustomCarrierName_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.setCustomCarrierName) {
                        uiState.statusBar.customCarrierName
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.setCustomCarrierName,
                    onCheckedChange = {
                        if (it && uiState.statusBar.customCarrierName.isEmpty()) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetCustomCarrierName(it))
                    }
                )

                AnimatedVisibility(expanded && uiState.statusBar.setCustomCarrierName) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        var tempCustomCarrierName by remember {
                            mutableStateOf(uiState.statusBar.customCarrierName)
                        }

                        OutlinedTextField(
                            value = tempCustomCarrierName,
                            onValueChange = { tempCustomCarrierName = it },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onEvent(
                                    SystemUIEvent.StatusBar.CustomCarrierName(
                                        tempCustomCarrierName
                                    )
                                )
                            }
                        ) {
                            Text(text = stringResource(id = R.string.confirm))
                        }
                    }
                }
            }
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
                title = stringResource(id = R.string.hideLockscreenStatusBar_title),
                checked = uiState.statusBar.hideLockscreenStatusBar,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.HideLockscreenStatusBar(it))
                }
            )
        }
        SettingsGroup(R.string.qs) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
                title = stringResource(id = R.string.setQsClockMonospaced_title),
                checked = uiState.qs.setQsClockMonospaced,
                onCheckedChange = {
                    onEvent(SystemUIEvent.QS.SetQsClockMonospaced(it))
                }
            )
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                    title = stringResource(id = R.string.hideDeviceControlQsTile_title),
                    checked = uiState.qs.hideDeviceControlQsTile,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.HideDeviceControlQsTile(it))
                    }
                )
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                    title = stringResource(id = R.string.hideSmartViewQsTile_title),
                    checked = uiState.qs.hideSmartViewQsTile,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.HideSmartViewQsTile(it))
                    }
                )
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.five_g),
                    title = stringResource(id = R.string.turnOn5gQsTile_title),
                    summary = stringResource(id = R.string.turnOn5gQsTile_summary),
                    checked = uiState.qs.turnOn5gQsTile,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.TurnOn5gQsTile(it))
                    }
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                ListItem(
                    headlineContent = { Text(stringResource(id = R.string.turnOn5gQsTile_title)) },
                    leadingContent = {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.five_g),
                            stringResource(id = R.string.turnOn5gQsTile_title)
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.root5gQsTile_summary)) }
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            SettingsGroup(R.string.group_qs_hide) {
                if (ONE_UI_VERSION < 80500) {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                        title = stringResource(id = R.string.hideQsBarMediaPlayer_title),
                        checked = uiState.qs.hideQsBarMediaPlayer,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.HideQsBarMediaPlayer(it))
                        }
                    )
                }
                if (ONE_UI_VERSION < 80500) {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                        title = stringResource(id = R.string.hideQsBarNearbyDevicesAndDeviceControl_title),
                        checked = uiState.qs.hideQsBarNearbyDevicesAndDeviceControl,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.HideQsBarNearbyDevicesAndDeviceControl(it))
                        }
                    )
                }
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                    title = stringResource(id = R.string.hideQsBarSecurityFooter_title),
                    summary = stringResource(id = R.string.hideQsBarSecurityFooter_summary),
                    checked = uiState.qs.hideQsBarSecurityFooter,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.HideQsBarSecurityFooter(it))
                    }
                )
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                    title = stringResource(id = R.string.hideQsBarDataUsage_title),
                    checked = uiState.qs.hideQsBarDataUsage,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.HideQsBarDataUsage(it))
                    }
                )
                if (ONE_UI_VERSION < 80500) {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                        title = stringResource(id = R.string.hideQsBarSmartViewAndModes_title),
                        checked = uiState.qs.hideQsBarSmartViewAndModes,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.HideQsBarSmartViewAndModes(it))
                        }
                    )
                }
            }
            SettingsGroup(R.string.group_qs_panel) {
                if (ONE_UI_VERSION < 80500) {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.expand),
                        title = stringResource(id = R.string.alwaysExpandQsTileChunk_title),
                        checked = uiState.qs.alwaysExpandQsTileChunk,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.AlwaysExpandQsTileChunk(it))
                        }
                    )
                }
                if (ONE_UI_VERSION < 80500) {
                    SwitchItem(
                        title = stringResource(id = R.string.alwaysShowTimeDateOnQs_title),
                        icon = ImageVector.vectorResource(id = R.drawable.nest_clock_farsight_digital),
                        checked = uiState.qs.alwaysShowTimeDateOnQs,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.AlwaysShowTimeDateOnQs(it))
                        }
                    )
                }
                if (ONE_UI_VERSION < 80500) {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.light_mode),
                        title = stringResource(id = R.string.addBrightnessProgressToQsBar_title),
                        checked = uiState.qs.addBrightnessProgressToQsBar,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.AddBrightnessProgressToQsBar(it))
                        }
                    )
                }
                if (ONE_UI_VERSION < 80500) {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.music_note),
                        title = stringResource(id = R.string.addVolumeProgressToQsBar_title),
                        checked = uiState.qs.addVolumeProgressToQsBar,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.AddVolumeProgressToQsBar(it))
                        }
                    )
                }
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.today),
                    title = stringResource(id = R.string.showTraditionalChineseDateOnQS_title),
                    checked = uiState.qs.showTraditionalChineseDateOnQS,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.ShowTraditionalChineseDateOnQS(it))
                    }
                )
                Column {
                    var textSize by remember { mutableFloatStateOf(uiState.qs.qsClockTextSize) }
                    var expanded by rememberSaveable { mutableStateOf(false) }

                    SwitchItem(
                        title = stringResource(id = R.string.modifyQSClockTextSize_title),
                        modifier = Modifier.animateContentSize(),
                        summary = if (uiState.qs.modifyQSClockTextSize) {
                            " %.1fsp".format(textSize)
                        } else null,
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        clickable = true,
                        onClick = { expanded = !expanded },
                        checked = uiState.qs.modifyQSClockTextSize,
                        onCheckedChange = {
                            if (it && textSize == 32f) {
                                expanded = true
                            } else if (!it) {
                                expanded = false
                            }
                            onEvent(SystemUIEvent.QS.ModifyQSClockTextSize(it))
                        }
                    )

                    AnimatedVisibility(expanded && uiState.qs.modifyQSClockTextSize) {
                        Slider(
                            value = textSize,
                            onValueChange = { textSize = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = 15f..70f,
                            onValueChangeFinished = {
                                onEvent(SystemUIEvent.QS.QSClockTextSize(textSize))
                            }
                        )
                    }
                }
            }
            SettingsGroup(R.string.aod) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    title = stringResource(id = R.string.hideAODStatusBar_title),
                    checked = uiState.aod.hideAODStatusBar,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.AOD.HideAODStatusBar(it))
                    }
                )
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.today),
                    title = stringResource(id = R.string.aodLockSupportLunar_title),
                    checked = uiState.aod.aodLockSupportLunar,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.AOD.AODLockSupportLunar(it))
                    }
                )
            }
        }
        SettingsGroup(R.string.group_power_menu) {
            Column {
                var expanded by rememberSaveable { mutableStateOf(false) }
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.power_settings_new),
                    title = stringResource(id = R.string.customPowerMenu_title),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.other.customPowerMenu,
                    onCheckedChange = {
                        expanded = it
                        onEvent(SystemUIEvent.Other.CustomPowerMenu(it))
                    }
                )
                AnimatedVisibility(expanded && uiState.other.customPowerMenu) {
                    PowerMenuActionEditor(
                        actions = uiState.other.powerMenuActions,
                        onActionsChange = {
                            onEvent(SystemUIEvent.Other.PowerMenuActions(it))
                        }
                    )
                }
            }
        }
        SettingsGroup(R.string.group_notifications_sound) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.screenshot),
                title = stringResource(id = R.string.disableScreenshotCaptureSound_title),
                checked = uiState.other.disableScreenshotCaptureSound,
                onCheckedChange = {
                    onEvent(SystemUIEvent.Other.DisableScreenshotCaptureSound(it))
                }
            )
            Column {
                var expanded by rememberSaveable { mutableStateOf(false) }
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.music_note),
                    title = stringResource(id = R.string.hideOngoingActivityMedia_title),
                    summary = if (uiState.other.hideOngoingActivityMedia && uiState.other.hideOngoingActivityMediaPackages.isNotEmpty()) {
                        uiState.other.hideOngoingActivityMediaPackages
                    } else {
                        stringResource(id = R.string.hideOngoingActivityMedia_summary)
                    },
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.other.hideOngoingActivityMedia,
                    onCheckedChange = {
                        if (it && uiState.other.hideOngoingActivityMediaPackages.isEmpty()) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.Other.HideOngoingActivityMedia(it))
                    }
                )
                AnimatedVisibility(expanded && uiState.other.hideOngoingActivityMedia) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        var tempPackages by remember {
                            mutableStateOf(uiState.other.hideOngoingActivityMediaPackages)
                        }
                        OutlinedTextField(
                            value = tempPackages,
                            onValueChange = { tempPackages = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(text = stringResource(id = R.string.hideOngoingActivityMedia_packages_hint)) },
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onEvent(SystemUIEvent.Other.HideOngoingActivityMediaPackages(tempPackages))
                            }
                        ) {
                            Text(text = stringResource(id = R.string.confirm))
                        }
                    }
                }
            }
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.notifications),
                title = stringResource(id = R.string.disableNotificationGrouping_title),
                summary = stringResource(id = R.string.disableNotificationGrouping_summary),
                checked = uiState.other.disableNotificationGrouping,
                onCheckedChange = {
                    onEvent(SystemUIEvent.Other.DisableNotificationGrouping(it))
                }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.notifications),
                title = stringResource(id = R.string.autoExpandNotifications_title),
                checked = uiState.other.autoExpandNotifications,
                onCheckedChange = {
                    onEvent(SystemUIEvent.Other.AutoExpandNotifications(it))
                }
            )
        }
    }
}

/**
 * 入力欄と「確定」ボタンの組。
 *
 * [preview] が null 以外を返した場合のみ [onConfirm] を呼び、
 * 返ってきた文字列をラベルに表示して結果をその場で確認できるようにする。
 * null（＝不正な入力）の場合はラベルに error と表示し、設定は保存しない。
 */
@Composable
private fun ConfirmableTextField(
    initialValue: String,
    label: String,
    preview: (String) -> String?,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    var hint by remember { mutableStateOf(label) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(text = hint) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                val result = preview(text)
                hint = result ?: "error"
                if (result != null) {
                    onConfirm(text)
                }
            }
        ) {
            Text(text = stringResource(id = R.string.confirm))
        }
    }
}

/**
 * BCP 47 言語タグ（`ja` / `en-US` など）を [Locale] に変換する。
 * 空欄や解釈できない値の場合は null を返し、呼び出し側でシステム言語にフォールバックする。
 */
private fun parseLocaleTag(tag: String): Locale? {
    val trimmed = tag.trim()
    if (trimmed.isEmpty()) return null
    return runCatching {
        Locale.forLanguageTag(trimmed.replace('_', '-'))
    }.getOrNull()?.takeIf { it.language.isNotEmpty() }
}

@Composable
private fun rememberDateLocale(tag: String): Locale {
    val systemLocale = Locale.getDefault(Locale.Category.FORMAT)
    return remember(tag, systemLocale) { parseLocaleTag(tag) ?: systemLocale }
}

/** 設定画面のサマリーに表示する、ステータスバー時計のプレビュー文字列。 */
private fun clockPreview(
    timeFormat: String,
    dateFormat: String,
    separator: String,
    dateBeforeTime: Boolean,
    locale: Locale,
): String {
    val now = LocalDateTime.now()
    fun render(pattern: String) = runCatching {
        DateTimeFormatter.ofPattern(pattern, locale).format(now)
    }.getOrDefault("?")

    val time = render(timeFormat)
    val date = render(dateFormat)
    return if (dateBeforeTime) "$date$separator$time" else "$time$separator$date"
}

@Composable
private fun PowerMenuActionEditor(
    actions: List<PowerMenuAction>,
    onActionsChange: (List<PowerMenuAction>) -> Unit,
) {
    val normalizedActions = PowerMenuAction.normalize(actions)
    Column {
        normalizedActions.forEachIndexed { index, action ->
            val visiblePosition = normalizedActions.take(index + 1).count { it.visible }
            ListItem(
                headlineContent = {
                    Text(text = stringResource(id = powerMenuActionTitle(action.name)))
                },
                supportingContent = {
                    Text(
                        text = if (action.visible) {
                            stringResource(
                                id = R.string.powerMenuActionVisible_summary,
                                visiblePosition
                            )
                        } else {
                            stringResource(id = R.string.powerMenuActionHidden_summary)
                        }
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            enabled = index > 0,
                            onClick = {
                                onActionsChange(normalizedActions.move(index, index - 1))
                            }
                        ) {
                            Text(text = stringResource(id = R.string.moveUp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            enabled = index < normalizedActions.lastIndex,
                            onClick = {
                                onActionsChange(normalizedActions.move(index, index + 1))
                            }
                        ) {
                            Text(text = stringResource(id = R.string.moveDown))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = action.visible,
                            onCheckedChange = { visible ->
                                onActionsChange(
                                    normalizedActions.mapIndexed { actionIndex, item ->
                                        if (actionIndex == index) item.copy(visible = visible) else item
                                    }
                                )
                            }
                        )
                    }
                }
            )
        }
    }
}

private fun List<PowerMenuAction>.move(fromIndex: Int, toIndex: Int): List<PowerMenuAction> =
    toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }

@StringRes
private fun powerMenuActionTitle(actionName: String): Int = when (actionName) {
    PowerMenuAction.POWER -> R.string.powerMenuAction_power
    PowerMenuAction.DATA_MODE -> R.string.powerMenuAction_dataMode
    PowerMenuAction.RESTART -> R.string.powerMenuAction_restart
    PowerMenuAction.SAFE_MODE -> R.string.powerMenuAction_safeMode
    PowerMenuAction.LOCK_DOWN_MODE -> R.string.powerMenuAction_lockDownMode
    PowerMenuAction.EMERGENCY_CALL -> R.string.powerMenuAction_emergencyCall
    PowerMenuAction.MEDICAL_INFO -> R.string.powerMenuAction_medicalInfo
    PowerMenuAction.SIDE_KEY_SETTINGS -> R.string.sideKeySettings
    PowerMenuAction.FORCE_RESTART_MESSAGE -> R.string.powerMenuAction_forceRestartMessage
    PowerMenuAction.RESTART_SYSTEMUI -> R.string.restartSystemUI
    PowerMenuAction.RESTART_RECOVERY -> R.string.restartRecovery
    PowerMenuAction.RESTART_DOWNLOAD -> R.string.restartDownload
    else -> R.string.other
}

sealed interface SystemUIEvent {
    sealed interface StatusBar : SystemUIEvent {
        @JvmInline
        value class ModifyStatusBarLeftPadding(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarLeftPaddingDp(val value: Float) : StatusBar

        @JvmInline
        value class ModifyStatusBarRightPadding(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarRightPaddingDp(val value: Float) : StatusBar

        @JvmInline
        value class SetBatteryIconWidthScale(val value: Boolean) : StatusBar

        @JvmInline
        value class BatteryIconWidthScale(val value: Float) : StatusBar

        @JvmInline
        value class SetBatteryIconHeightScale(val value: Boolean) : StatusBar

        @JvmInline
        value class BatteryIconHeightScale(val value: Float) : StatusBar

        @JvmInline
        value class HideBatteryPercentageSign(val value: Boolean) : StatusBar

        @JvmInline
        value class HideBatteryIcon(val value: Boolean) : StatusBar

        @JvmInline
        value class UseCircleBatteryIcon(val value: Boolean) : StatusBar

        @JvmInline
        value class CircleBatteryIconSizeDp(val value: Float) : StatusBar

        @JvmInline
        value class CircleBatteryIconHorizontalPaddingDp(val value: Float) : StatusBar

        @JvmInline
        value class CircleBatteryIconVerticalPaddingDp(val value: Float) : StatusBar

        @JvmInline
        value class AddBatteryLevelText(val value: Boolean) : StatusBar

        @JvmInline
        value class HideBatteryLevelTextPercentageSign(val value: Boolean) : StatusBar

        @JvmInline
        value class HideBatteryLevelTextChargingIcon(val value: Boolean) : StatusBar

        @JvmInline
        value class BatteryLevelTextPercentSignScale(val value: Float) : StatusBar

        @JvmInline
        value class BatteryLevelTextMarginStartDp(val value: Float) : StatusBar

        @JvmInline
        value class BatteryLevelTextSizeScale(val value: Float) : StatusBar

        @JvmInline
        value class BatteryLevelTextOffsetDp(val value: Float) : StatusBar

        @JvmInline
        value class SupportRealTimeNetworkSpeed(val value: Boolean) : StatusBar

        @JvmInline
        value class ShowSeparateUpDownNetworkSpeeds(val value: Boolean) : StatusBar

        @JvmInline
        value class SetStatusBarClockFormat(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarClockFormat(val value: String) : StatusBar

        @JvmInline
        value class AppendStatusBarClockDate(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarClockDateFormat(val value: String) : StatusBar

        @JvmInline
        value class StatusBarClockDateSeparator(val value: String) : StatusBar

        @JvmInline
        value class StatusBarClockDateBeforeTime(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarClockDateTextScale(val value: Float) : StatusBar

        @JvmInline
        value class StatusBarClockDateOffsetDp(val value: Float) : StatusBar

        @JvmInline
        value class StatusBarClockDateLocale(val value: String) : StatusBar

        @JvmInline
        value class SetStatusBarClockTextScale(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarClockTextScale(val value: Float) : StatusBar

        @JvmInline
        value class UpdateStatusBarClockEverySecond(val value: Boolean) : StatusBar

        @JvmInline
        value class HideSecureFolderStatusBarIcon(val value: Boolean) : StatusBar

        @JvmInline
        value class RestoreBluetoothStatusBarIcon(val value: Boolean) : StatusBar

        @JvmInline
        value class PhysicalEsimAdapterWorkaround(val value: Boolean) : StatusBar

        @JvmInline
        value class PhysicalEsimAdapterSimSlot(val value: Int) : StatusBar

        @JvmInline
        value class DoubleTapStatusBarToSleep(val value: Boolean) : StatusBar

        @JvmInline
        value class ModifyStatusBarMaxNotificationIcons(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarMaxNotificationIcons(val value: Int) : StatusBar

        @JvmInline
        value class SetCustomCarrierName(val value: Boolean) : StatusBar

        @JvmInline
        value class CustomCarrierName(val value: String) : StatusBar

        @JvmInline
        value class HideLockscreenStatusBar(val value: Boolean) : StatusBar
    }

    sealed interface QS : SystemUIEvent {
        @JvmInline
        value class SetQsClockMonospaced(val value: Boolean) : QS

        @JvmInline
        value class HideDeviceControlQsTile(val value: Boolean) : QS

        @JvmInline
        value class HideSmartViewQsTile(val value: Boolean) : QS

        @JvmInline
        value class TurnOn5gQsTile(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarMediaPlayer(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarNearbyDevicesAndDeviceControl(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarSecurityFooter(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarDataUsage(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarSmartViewAndModes(val value: Boolean) : QS

        @JvmInline
        value class AlwaysExpandQsTileChunk(val value: Boolean) : QS

        @JvmInline
        value class AlwaysShowTimeDateOnQs(val value: Boolean) : QS

        @JvmInline
        value class AddBrightnessProgressToQsBar(val value: Boolean) : QS

        @JvmInline
        value class AddVolumeProgressToQsBar(val value: Boolean) : QS

        @JvmInline
        value class ShowTraditionalChineseDateOnQS(val value: Boolean) : QS

        @JvmInline
        value class ModifyQSClockTextSize(val value: Boolean) : QS

        @JvmInline
        value class QSClockTextSize(val value: Float) : QS
    }

    sealed interface AOD : SystemUIEvent {
        @JvmInline
        value class HideAODStatusBar(val value: Boolean) : AOD

        @JvmInline
        value class AODLockSupportLunar(val value: Boolean) : AOD
    }

    sealed interface Other : SystemUIEvent {
        @JvmInline
        value class CustomPowerMenu(val value: Boolean) : Other

        @JvmInline
        value class PowerMenuActions(val value: List<PowerMenuAction>) : Other

        @JvmInline
        value class DisableScreenshotCaptureSound(val value: Boolean) : Other

        @JvmInline
        value class DisableNotificationGrouping(val value: Boolean) : Other

        @JvmInline
        value class AutoExpandNotifications(val value: Boolean) : Other

        @JvmInline
        value class HideOngoingActivityMedia(val value: Boolean) : Other

        @JvmInline
        value class HideOngoingActivityMediaPackages(val value: String) : Other
    }
}

fun SettingViewModel.onSystemUIEvent(event: SystemUIEvent) {
    when (event) {
        is SystemUIEvent.StatusBar -> onStatusBarEvent(event)
        is SystemUIEvent.QS -> onQSEvent(event)
        is SystemUIEvent.AOD -> onAODEvent(event)
        is SystemUIEvent.Other -> onOtherEvent(event)
    }
}

private fun SettingViewModel.onStatusBarEvent(event: SystemUIEvent.StatusBar) {
    updateData { preference ->
        when (event) {
            is SystemUIEvent.StatusBar.ModifyStatusBarLeftPadding -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            modifyStatusBarLeftPadding = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarLeftPaddingDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarLeftPaddingDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.ModifyStatusBarRightPadding -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            modifyStatusBarRightPadding = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarRightPaddingDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarRightPaddingDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetBatteryIconWidthScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setBatteryIconWidthScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.BatteryIconWidthScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            batteryIconWidthScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideLockscreenStatusBar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideLockscreenStatusBar = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetBatteryIconHeightScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setBatteryIconHeightScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.BatteryIconHeightScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            batteryIconHeightScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideBatteryPercentageSign -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideBatteryPercentageSign = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideBatteryIcon -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideBatteryIcon = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.UseCircleBatteryIcon -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            useCircleBatteryIcon = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.CircleBatteryIconSizeDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            circleBatteryIconSizeDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.CircleBatteryIconHorizontalPaddingDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            circleBatteryIconHorizontalPaddingDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.CircleBatteryIconVerticalPaddingDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            circleBatteryIconVerticalPaddingDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.AddBatteryLevelText -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            addBatteryLevelText = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideBatteryLevelTextPercentageSign -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideBatteryLevelTextPercentageSign = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideBatteryLevelTextChargingIcon -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideBatteryLevelTextChargingIcon = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.BatteryLevelTextPercentSignScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            batteryLevelTextPercentSignScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.BatteryLevelTextMarginStartDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            batteryLevelTextMarginStartDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.BatteryLevelTextSizeScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            batteryLevelTextSizeScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.BatteryLevelTextOffsetDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            batteryLevelTextOffsetDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SupportRealTimeNetworkSpeed -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            supportRealTimeNetworkSpeed = event.value
                        )
                    )
                )
            }


            is SystemUIEvent.StatusBar.ShowSeparateUpDownNetworkSpeeds -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            showSeparateUpDownNetworkSpeeds = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetStatusBarClockFormat -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setStatusBarClockFormat = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockFormat -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockFormat = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.AppendStatusBarClockDate -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            appendStatusBarClockDate = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockDateFormat -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockDateFormat = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockDateSeparator -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockDateSeparator = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockDateBeforeTime -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockDateBeforeTime = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockDateTextScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockDateTextScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockDateOffsetDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockDateOffsetDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockDateLocale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockDateLocale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetStatusBarClockTextScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setStatusBarClockTextScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockTextScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockTextScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.UpdateStatusBarClockEverySecond -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            updateStatusBarClockEverySecond = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideSecureFolderStatusBarIcon -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideSecureFolderStatusBarIcon = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.RestoreBluetoothStatusBarIcon -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            restoreBluetoothStatusBarIcon = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.PhysicalEsimAdapterWorkaround -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            physicalEsimAdapterWorkaround = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.PhysicalEsimAdapterSimSlot -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            physicalEsimAdapterSimSlot = event.value.coerceIn(
                                0,
                                ESIM_ADAPTER_SIM_BOTH
                            )
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.DoubleTapStatusBarToSleep -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            doubleTapStatusBarToSleep = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.ModifyStatusBarMaxNotificationIcons -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            modifyStatusBarMaxNotificationIcons = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarMaxNotificationIcons -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarMaxNotificationIcons = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetCustomCarrierName -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setCustomCarrierName = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.CustomCarrierName -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            customCarrierName = event.value
                        )
                    )
                )
            }
        }
    }
}

private fun SettingViewModel.onQSEvent(event: SystemUIEvent.QS) {
    updateData { preference ->
        when (event) {
            is SystemUIEvent.QS.SetQsClockMonospaced -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            setQsClockMonospaced = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideDeviceControlQsTile -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideDeviceControlQsTile = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideSmartViewQsTile -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideSmartViewQsTile = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.TurnOn5gQsTile -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            turnOn5gQsTile = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarMediaPlayer -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarMediaPlayer = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarNearbyDevicesAndDeviceControl -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarNearbyDevicesAndDeviceControl = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarSecurityFooter -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarSecurityFooter = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarDataUsage -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarDataUsage = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarSmartViewAndModes -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarSmartViewAndModes = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.AlwaysExpandQsTileChunk -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            alwaysExpandQsTileChunk = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.AlwaysShowTimeDateOnQs -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            alwaysShowTimeDateOnQs = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.AddBrightnessProgressToQsBar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            addBrightnessProgressToQsBar = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.AddVolumeProgressToQsBar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            addVolumeProgressToQsBar = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.ShowTraditionalChineseDateOnQS -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            showTraditionalChineseDateOnQS = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.ModifyQSClockTextSize -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            modifyQSClockTextSize = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.QSClockTextSize -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            qsClockTextSize = event.value
                        )
                    )
                )
            }
        }
    }
}

private fun SettingViewModel.onAODEvent(event: SystemUIEvent.AOD) {
    updateData { preference ->
        when (event) {
            is SystemUIEvent.AOD.HideAODStatusBar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        aod = preference.systemUI.aod.copy(
                            hideAODStatusBar = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.AOD.AODLockSupportLunar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        aod = preference.systemUI.aod.copy(
                            aodLockSupportLunar = event.value
                        )
                    )
                )
            }
        }
    }
}

private fun SettingViewModel.onOtherEvent(event: SystemUIEvent.Other) {
    updateData { preference ->
        when (event) {
            is SystemUIEvent.Other.CustomPowerMenu -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            customPowerMenu = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.Other.PowerMenuActions -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            powerMenuActions = PowerMenuAction.normalize(event.value)
                        )
                    )
                )
            }

            is SystemUIEvent.Other.DisableScreenshotCaptureSound -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            disableScreenshotCaptureSound = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.Other.DisableNotificationGrouping -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            disableNotificationGrouping = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.Other.AutoExpandNotifications -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            autoExpandNotifications = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.Other.HideOngoingActivityMedia -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            hideOngoingActivityMedia = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.Other.HideOngoingActivityMediaPackages -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            hideOngoingActivityMediaPackages = event.value
                        )
                    )
                )
            }
        }
    }
}
