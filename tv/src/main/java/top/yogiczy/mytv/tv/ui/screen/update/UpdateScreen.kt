package top.yogiczy.mytv.tv.ui.screen.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yogiczy.mytv.core.data.entities.git.GitRelease
import top.yogiczy.mytv.core.data.utils.Globals
import top.yogiczy.mytv.core.util.utils.ApkInstaller
import top.yogiczy.mytv.tv.BuildConfig
import top.yogiczy.mytv.tv.ui.material.Snackbar
import top.yogiczy.mytv.tv.ui.material.SnackbarType
import top.yogiczy.mytv.tv.ui.screen.components.AppScreen
import top.yogiczy.mytv.tv.ui.screen.settings.settingsVM
import top.yogiczy.mytv.tv.ui.theme.MyTvTheme
import top.yogiczy.mytv.tv.ui.theme.SAFE_AREA_HORIZONTAL_PADDING
import top.yogiczy.mytv.tv.ui.tooling.PreviewWithLayoutGrids
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunched
import top.yogiczy.mytv.tv.ui.utils.gridColumns
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents
import top.yogiczy.mytv.tv.ui.utils.rememberCanRequestPackageInstallsPermission
import java.io.File

@Composable
fun UpdateScreen(
    modifier: Modifier = Modifier,
    updateViewModel: UpdateViewModel = updateVM,
    currentVersion: String = BuildConfig.VERSION_NAME,
    updateChannel: String = settingsVM.updateChannel,
    onBackPressed: () -> Unit = {},
) {
    val latestFile by lazy { File(Globals.cacheDir, "latest.apk") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val latestRelease = updateViewModel.latestRelease
    val isChecking = updateViewModel.isChecking

    val (hasPermission, requestPermission) = rememberCanRequestPackageInstallsPermission()

    // 进入页面即发起一次真实检查，否则本页只会显示「最新版本 v0.0.0」
    LaunchedEffect(Unit) {
        updateViewModel.checkUpdate(currentVersion, updateChannel, force = true)
    }

    // 不再把「已授予安装权限」当作安装前置条件：
    // 小米等部分机型不实现 canRequestPackageInstalls，但系统安装器会在安装时自行弹确认框
    LaunchedEffect(updateViewModel.updateDownloaded) {
        if (!updateViewModel.updateDownloaded) return@LaunchedEffect

        val installed = withContext(Dispatchers.IO) {
            ApkInstaller.installApk(context, latestFile.path)
        }

        if (!installed) {
            Snackbar.show(
                "无法调起安装器，请到 设置 → 权限 检查「应用内安装其他应用」",
                type = SnackbarType.ERROR,
            )
            requestPermission()
        }
    }

    AppScreen(
        modifier = modifier,
        onBackPressed = onBackPressed,
        canBack = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(SAFE_AREA_HORIZONTAL_PADDING.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(2.gridColumns()),
            ) {
                Column(
                    modifier = Modifier.width(5.gridColumns()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "当前版本: v$currentVersion",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Text(
                        when {
                            isChecking -> "最新版本: 检查中…"
                            updateViewModel.checkError.isNotBlank() -> "最新版本: 获取失败"
                            else -> "最新版本: v${latestRelease.version}"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Text(
                        "更新通道: ${
                            when (updateChannel) {
                                "stable" -> "稳定版"
                                "beta" -> "测试版"
                                "dev" -> "开发版"
                                else -> updateChannel
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (!hasPermission) {
                        Text(
                            "安装权限: 系统设置中未开启（部分机型安装时由系统询问）",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    LazyColumn {
                        item {
                            Text(
                                if (updateViewModel.checkError.isNotBlank()) {
                                    updateViewModel.checkError
                                } else {
                                    latestRelease.description
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                if (isChecking) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UpdateActionBtn(
                            modifier = Modifier.focusOnLaunched(),
                            title = "检查中，请稍候",
                        )

                        UpdateActionBtn(
                            title = "返回",
                            onSelected = onBackPressed,
                        )
                    }
                } else if (updateViewModel.isUpdateAvailable) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (updateViewModel.isUpdating) {
                            UpdateActionBtn(
                                modifier = Modifier.focusOnLaunched(),
                                title = "更新中，请勿关闭页面",
                            )
                        } else {
                            UpdateActionBtn(
                                modifier = Modifier.focusOnLaunched(),
                                title = "立即更新",
                                onSelected = {
                                    // 已经下载过就直接装，避免重复下载 17MB
                                    if (updateViewModel.updateDownloaded && latestFile.exists()) {
                                        coroutineScope.launch {
                                            val installed = withContext(Dispatchers.IO) {
                                                ApkInstaller.installApk(context, latestFile.path)
                                            }
                                            if (!installed) {
                                                Snackbar.show(
                                                    "无法调起安装器，请到 设置 → 权限 检查「应用内安装其他应用」",
                                                    type = SnackbarType.ERROR,
                                                )
                                                requestPermission()
                                            }
                                        }
                                    } else {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            updateViewModel.downloadAndUpdate(latestFile)
                                        }
                                    }
                                },
                            )
                        }

                        UpdateActionBtn(
                            title = "忽略",
                            onSelected = onBackPressed,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UpdateActionBtn(
                            modifier = Modifier.focusOnLaunched(),
                            title = "重新检查",
                            onSelected = {
                                coroutineScope.launch {
                                    updateViewModel.checkUpdate(
                                        currentVersion = currentVersion,
                                        channel = updateChannel,
                                        force = true,
                                    )
                                }
                            },
                        )

                        UpdateActionBtn(
                            title = "当前为最新版本",
                            onSelected = onBackPressed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateActionBtn(
    modifier: Modifier = Modifier,
    title: String,
    onSelected: () -> Unit = {},
) {
    ListItem(
        modifier = modifier
            .width(4.gridColumns())
            .handleKeyEvents(onSelect = onSelected),
        onClick = { },
        selected = false,
        headlineContent = { Text(title) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(0.1f),
        ),
    )
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun UpdateScreenPreview() {
    MyTvTheme {
        UpdateScreen(
            updateViewModel = UpdateViewModel(
                debugLatestRelease = GitRelease(
                    version = "9.0.0",
                    description = " 移除自定义直播源界面获取直播源信息，可能导致部分低内存设备OOM\r\n\r\n"
                        .repeat(20),
                )
            )
        )

        PreviewWithLayoutGrids { }
    }
}