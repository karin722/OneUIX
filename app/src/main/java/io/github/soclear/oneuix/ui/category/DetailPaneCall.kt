package io.github.soclear.oneuix.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import io.github.soclear.oneuix.R
import io.github.soclear.oneuix.data.Preference
import io.github.soclear.oneuix.ui.SettingViewModel
import io.github.soclear.oneuix.ui.component.SettingsGroup
import io.github.soclear.oneuix.ui.component.SettingsPane
import io.github.soclear.oneuix.ui.component.SwitchItem

@Composable
fun DetailPaneCall(
    uiState: Preference.Call,
    onEvent: (CallEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPane(modifier = modifier) {
        SettingsGroup(R.string.group_call_recording) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.call_recording),
                title = stringResource(id = R.string.supportVoiceCallRecording_title),
                checked = uiState.supportVoiceCallRecording,
                onCheckedChange = { onEvent(CallEvent.SupportVoiceCallRecording(it)) }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.call_recording),
                title = stringResource(id = R.string.preferRecordingButton_title),
                checked = uiState.preferRecordingButton,
                onCheckedChange = { onEvent(CallEvent.PreferRecordingButton(it)) }
            )
        }
        SettingsGroup(R.string.group_call_display) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.person_pin_circle),
                title = stringResource(id = R.string.showGeocodedLocationInRecentCall_title),
                checked = uiState.showGeocodedLocationInRecentCall,
                onCheckedChange = { onEvent(CallEvent.ShowGeocodedLocationInRecentCall(it)) }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.call_log),
                title = stringResource(id = R.string.isOpStyleCHN_title),
                checked = uiState.isOpStyleCHN,
                onCheckedChange = { onEvent(CallEvent.IsOpStyleCHN(it)) }
            )
        }
        SettingsGroup(R.string.group_call_link) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
                title = stringResource(id = R.string.supportCallAndTextOnOtherDevices_title),
                summary = stringResource(id = R.string.supportCallAndTextOnOtherDevices_summary),
                checked = uiState.supportCallAndTextOnOtherDevices,
                onCheckedChange = { onEvent(CallEvent.SupportCallAndTextOnOtherDevices(it)) }
            )
        }
    }
}


sealed interface CallEvent {
    @JvmInline
    value class SupportVoiceCallRecording(val value: Boolean) : CallEvent

    @JvmInline
    value class PreferRecordingButton(val value: Boolean) : CallEvent

    @JvmInline
    value class ShowGeocodedLocationInRecentCall(val value: Boolean) : CallEvent

    @JvmInline
    value class IsOpStyleCHN(val value: Boolean) : CallEvent

    @JvmInline
    value class SupportCallAndTextOnOtherDevices(val value: Boolean) : CallEvent
}

fun SettingViewModel.onCallEvent(event: CallEvent) {
    updateData {
        when (event) {
            is CallEvent.SupportVoiceCallRecording -> {
                it.copy(call = it.call.copy(supportVoiceCallRecording = event.value))
            }

            is CallEvent.PreferRecordingButton -> {
                it.copy(call = it.call.copy(preferRecordingButton = event.value))
            }

            is CallEvent.ShowGeocodedLocationInRecentCall -> {
                it.copy(call = it.call.copy(showGeocodedLocationInRecentCall = event.value))
            }

            is CallEvent.IsOpStyleCHN -> {
                it.copy(call = it.call.copy(isOpStyleCHN = event.value))
            }

            is CallEvent.SupportCallAndTextOnOtherDevices -> {
                it.copy(call = it.call.copy(supportCallAndTextOnOtherDevices = event.value))
            }
        }
    }
}
