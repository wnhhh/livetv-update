package top.yogiczy.mytv.tv.ui.screensold.quickop.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.ControlCamera
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yogiczy.mytv.core.data.entities.channel.Channel
import top.yogiczy.mytv.tv.ui.rememberChildPadding
import top.yogiczy.mytv.tv.ui.screen.settings.settingsVM
import top.yogiczy.mytv.tv.ui.screensold.videoplayer.VideoPlayerDisplayMode
import top.yogiczy.mytv.tv.ui.screensold.videoplayer.player.VideoPlayer
import top.yogiczy.mytv.tv.ui.theme.MyTvTheme
import top.yogiczy.mytv.tv.ui.utils.Configs
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunched

/**
 * 底部操作条
 *
 * 分两行：第一行放最常用的 5 项（频道列表 / 收藏 / 节目单 / 播放控制 / 设置），
 * 第二行放图像调节与系统类操作，避免高频项被挤到需要横向滚动才能找到的位置。
 */
@Composable
fun QuickOpBtnList(
    modifier: Modifier = Modifier,
    channelProvider: () -> Channel = { Channel.EMPTY },
    channelLineIdxProvider: () -> Int = { 0 },
    playerDisplayModeProvider: () -> VideoPlayerDisplayMode = { VideoPlayerDisplayMode.ORIGINAL },
    playerMetadataProvider: () -> VideoPlayer.Metadata = { VideoPlayer.Metadata() },
    onShowEpg: () -> Unit = {},
    onShowChannels: () -> Unit = {},
    onShowFavorites: () -> Unit = {},
    onShowChannelLine: () -> Unit = {},
    onShowVideoPlayerController: () -> Unit = {},
    onShowVideoPlayerDisplayMode: () -> Unit = {},
    onShowVideoTracks: () -> Unit = {},
    onShowAudioTracks: () -> Unit = {},
    onShowSubtitleTracks: () -> Unit = {},
    onShowMoreSettings: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onRefreshSource: () -> Unit = {},
    toDashboardScreen: () -> Unit = {},
    onUserAction: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val childPadding = rememberChildPadding()
    val primaryListState = rememberLazyListState()
    val secondaryListState = rememberLazyListState()
    val playerMetadata = playerMetadataProvider()

    // 任意一行滚动都视为用户有操作，重置自动关闭计时
    LaunchedEffect(primaryListState) {
        snapshotFlow { primaryListState.isScrollInProgress }.distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }
    LaunchedEffect(secondaryListState) {
        snapshotFlow { secondaryListState.isScrollInProgress }.distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LazyRow(
            state = primaryListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = childPadding.start, end = childPadding.end),
        ) {
            item {
                QuickOpBtn(
                    modifier = Modifier.focusOnLaunched(),
                    title = "频道列表",
                    imageVector = Icons.Outlined.LiveTv,
                    onSelect = onShowChannels,
                )
            }

            item {
                QuickOpBtn(
                    title = "收藏",
                    imageVector = Icons.Outlined.Star,
                    onSelect = onShowFavorites,
                )
            }

            item {
                QuickOpBtn(
                    title = "节目单",
                    imageVector = Icons.Outlined.Book,
                    onSelect = onShowEpg,
                )
            }

            item {
                QuickOpBtn(
                    title = "播放控制",
                    imageVector = Icons.Outlined.ControlCamera,
                    onSelect = onShowVideoPlayerController,
                )
            }

            item {
                QuickOpBtn(
                    title = "设置",
                    imageVector = Icons.Outlined.Settings,
                    onSelect = onShowMoreSettings,
                )
            }
        }

        LazyRow(
            state = secondaryListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = childPadding.start, end = childPadding.end),
        ) {
            item {
                val lineName by remember {
                    derivedStateOf {
                        channelProvider().lineList.getOrNull(channelLineIdxProvider())?.name
                            ?: "线路%d".format(channelLineIdxProvider() + 1)
                    }
                }

                QuickOpBtn(
                    title = lineName,
                    imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    onSelect = onShowChannelLine,
                )
            }

            item {
                QuickOpBtn(
                    title = playerDisplayModeProvider().label,
                    imageVector = Icons.Outlined.AspectRatio,
                    onSelect = onShowVideoPlayerDisplayMode,
                )
            }

            item {
                val settingsViewModel = settingsVM

                QuickOpBtn(
                    title = settingsVM.videoPlayerCore.label,
                    imageVector = Icons.Outlined.SlowMotionVideo,
                    onSelect = {
                        settingsViewModel.videoPlayerCore = when (settingsViewModel.videoPlayerCore) {
                            Configs.VideoPlayerCore.MEDIA3 -> Configs.VideoPlayerCore.IJK
                            Configs.VideoPlayerCore.IJK -> Configs.VideoPlayerCore.MEDIA3
                        }
                    },
                )
            }

            if (playerMetadata.videoTracks.isNotEmpty()) {
                item {
                    QuickOpBtn(
                        title = playerMetadataProvider().video?.shortLabel ?: "视轨",
                        imageVector = Icons.Outlined.Videocam,
                        onSelect = onShowVideoTracks,
                    )
                }
            }

            if (playerMetadata.audioTracks.isNotEmpty()) {
                item {
                    QuickOpBtn(
                        title = playerMetadataProvider().audio?.shortLabel ?: "音轨",
                        imageVector = Icons.Outlined.Audiotrack,
                        onSelect = onShowAudioTracks,
                    )
                }
            }

            if (playerMetadata.subtitleTracks.isNotEmpty()) {
                item {
                    QuickOpBtn(
                        title = playerMetadataProvider().subtitle?.shortLabel ?: "字幕",
                        imageVector = Icons.Outlined.Subtitles,
                        onSelect = onShowSubtitleTracks,
                    )
                }
            }

            item {
                QuickOpBtn(
                    title = "刷新订阅源",
                    imageVector = Icons.Outlined.Refresh,
                    onSelect = onRefreshSource,
                )
            }

            item {
                QuickOpBtn(
                    title = "清除缓存",
                    imageVector = Icons.Outlined.ClearAll,
                    onSelect = onClearCache,
                )
            }

            item {
                QuickOpBtn(
                    title = "主界面",
                    imageVector = Icons.Outlined.Home,
                    onSelect = toDashboardScreen,
                )
            }

            item {
                QuickOpBtn(
                    title = "返回",
                    imageVector = Icons.Outlined.ArrowBackIosNew,
                    onSelect = onDismissRequest,
                )
            }
        }
    }
}

@Preview
@Composable
private fun QuickOpBtnListPreview() {
    MyTvTheme {
        QuickOpBtnList()
    }
}
