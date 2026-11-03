package top.yogiczy.mytv.tv.ui.screen.settings.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.tv.ui.screen.settings.components.SettingsCategoryScreen
import top.yogiczy.mytv.tv.ui.screen.settings.components.SettingsListItem
import top.yogiczy.mytv.tv.ui.theme.MyTvTheme
import top.yogiczy.mytv.tv.ui.utils.rememberCanRequestPackageInstallsPermission
import top.yogiczy.mytv.tv.ui.utils.rememberReadExternalStoragePermission
import top.yogiczy.mytv.tv.ui.utils.resolveInstallSettingsIntent

@Composable
fun SettingsPermissionsScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
) {
    SettingsCategoryScreen(
        modifier = modifier,
        header = { Text("设置 / 权限") },
        onBackPressed = onBackPressed,
    ) { firstItemFocusRequester ->
        item {
            val (hasPermission, requestPermission) = rememberCanRequestPackageInstallsPermission()
            val context = LocalContext.current
            val settingsPageAvailable = remember(context) {
                resolveInstallSettingsIntent(context) != null
            }

            SettingsListItem(
                modifier = Modifier.focusRequester(firstItemFocusRequester),
                headlineContent = "应用内安装其他应用",
                supportingContent = when {
                    hasPermission -> "已开启，在线更新可直接安装"
                    !settingsPageAvailable -> "本机系统没有该设置页；安装时系统会弹窗询问是否允许"
                    else -> "未开启，点此进入系统设置开启（否则更新装不上）"
                },
                trailingContent = {
                    if (hasPermission) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Cancel, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                onSelect = {
                    requestPermission()
                },
            )
        }

        item {
            val (hasPermission, requestPermission) = rememberReadExternalStoragePermission()

            SettingsListItem(
                headlineContent = "读取外部存储/管理全部文件",
                supportingContent = if (hasPermission) "已开启"
                else "仅在用本地直播源文件时需要；网络源可不开启。点此进入系统设置",
                trailingContent = {
                    if (hasPermission) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Cancel, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                onSelect = {
                    requestPermission()
                },
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun SettingsPermissionsScreenPreview() {
    MyTvTheme {
        SettingsPermissionsScreen()
    }
}