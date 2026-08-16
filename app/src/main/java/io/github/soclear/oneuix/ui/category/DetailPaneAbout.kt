package io.github.soclear.oneuix.ui.category

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import io.github.soclear.oneuix.BuildConfig
import io.github.soclear.oneuix.R
import io.github.soclear.oneuix.data.ONE_UI_VERSION
import io.github.soclear.oneuix.ui.component.SettingsGroup
import io.github.soclear.oneuix.ui.component.SettingsPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val REPOSITORY_URL = "https://github.com/SoClear/OneUIX"
private const val RELEASES_URL = "https://github.com/SoClear/OneUIX/releases/latest"
private const val LSPOSED_REPOSITORY_URL =
    "https://github.com/Xposed-Modules-Repo/io.github.soclear.oneuix"
private const val DEVELOPER_URL = "https://github.com/SoClear"
private const val CONTRIBUTOR_URL = "https://github.com/karin722"
private const val LICENSE_URL = "https://github.com/SoClear/OneUIX/blob/main/LICENSE.txt"

@Composable
fun DetailPaneAbout(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val openLink: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    SettingsPane(modifier = modifier) {
        AppHeader()
        SettingsGroup(R.string.about_group_version) {
            InfoItem(
                icon = ImageVector.vectorResource(id = R.drawable.info),
                title = stringResource(id = R.string.about_app_version),
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            )
            InfoItem(
                icon = ImageVector.vectorResource(id = R.drawable.logo_dev),
                title = stringResource(id = R.string.about_one_ui_version),
                value = formatOneUiVersion()
            )
            InfoItem(
                icon = ImageVector.vectorResource(id = R.drawable.apps),
                title = stringResource(id = R.string.about_android_version),
                value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            )
            InfoItem(
                icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
                title = stringResource(id = R.string.about_device),
                value = Build.MODEL
            )
        }
        SettingsGroup(R.string.about_group_links) {
            LinkItem(
                icon = ImageVector.vectorResource(id = R.drawable.code_brackets),
                title = stringResource(id = R.string.about_repository),
                summary = REPOSITORY_URL,
                onClick = { openLink(REPOSITORY_URL) }
            )
            LinkItem(
                icon = ImageVector.vectorResource(id = R.drawable.restore_open),
                title = stringResource(id = R.string.about_releases),
                summary = stringResource(id = R.string.about_releases_summary),
                onClick = { openLink(RELEASES_URL) }
            )
            LinkItem(
                icon = ImageVector.vectorResource(id = R.drawable.apk_document),
                title = stringResource(id = R.string.about_lsposed_repository),
                summary = stringResource(id = R.string.about_lsposed_repository_summary),
                onClick = { openLink(LSPOSED_REPOSITORY_URL) }
            )
        }
        SettingsGroup(R.string.about_group_credits) {
            LinkItem(
                icon = ImageVector.vectorResource(id = R.drawable.logo_dev),
                title = stringResource(id = R.string.about_developer),
                summary = "SoClear",
                avatar = rememberGitHubAvatar("SoClear"),
                onClick = { openLink(DEVELOPER_URL) }
            )
            LinkItem(
                icon = ImageVector.vectorResource(id = R.drawable.logo_dev),
                title = stringResource(id = R.string.about_contributor),
                summary = "karin722",
                avatar = rememberGitHubAvatar("karin722"),
                onClick = { openLink(CONTRIBUTOR_URL) }
            )
            LinkItem(
                icon = ImageVector.vectorResource(id = R.drawable.folder_managed),
                title = stringResource(id = R.string.about_license),
                summary = stringResource(id = R.string.about_license_summary),
                onClick = { openLink(LICENSE_URL) }
            )
        }
    }
}

/** アプリアイコンと名前。アダプティブアイコンをそのまま使いたいので PackageManager から読む。 */
@Composable
private fun AppHeader() {
    val context = LocalContext.current
    val icon = remember(context) {
        context.packageManager.getApplicationIcon(context.packageName)
            .toBitmap()
            .asImageBitmap()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(id = R.string.xposed_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun InfoItem(icon: ImageVector, title: String, value: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value) },
        leadingContent = { Icon(icon, title) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * @param avatar null でなければアイコンの代わりに丸く切り抜いて表示する
 */
@Composable
private fun LinkItem(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    avatar: ImageBitmap? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = {
            if (avatar == null) {
                Icon(icon, title)
            } else {
                Image(
                    bitmap = avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        },
        trailingContent = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.open_in_new),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

/** 一度読めたアバターは使い回す。画面を開き直すたびに取りに行かないため。 */
private val avatarCache = mutableMapOf<String, ImageBitmap>()

/**
 * GitHub のアバター画像。取得できない場合は null のままで、呼び出し側はアイコンを出す。
 * 画像ライブラリを足すほどの用途ではないので、ここだけ自前で読む。
 */
@Composable
private fun rememberGitHubAvatar(user: String): ImageBitmap? {
    var avatar by remember(user) { mutableStateOf(avatarCache[user]) }
    LaunchedEffect(user) {
        if (avatar != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) { loadGitHubAvatar(user) } ?: return@LaunchedEffect
        avatarCache[user] = loaded
        avatar = loaded
    }
    return avatar
}

private fun loadGitHubAvatar(user: String): ImageBitmap? = runCatching {
    val connection = URL("https://github.com/$user.png?size=160")
        .openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    } finally {
        connection.disconnect()
    }
}.getOrNull()

/**
 * `ro.build.version.oneui` は 70000 のような整数なので `7.0` に整形する。
 * 取得できない（＝One UI ではない）場合は不明として扱う。
 */
@Composable
private fun formatOneUiVersion(): String {
    if (ONE_UI_VERSION <= 0) return stringResource(id = R.string.about_unknown)
    return "${ONE_UI_VERSION / 10000}.${ONE_UI_VERSION / 100 % 100}"
}
