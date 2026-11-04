package top.yogiczy.mytv.tv.ui.screen.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.ViewCozy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.tv.ui.rememberChildPadding
import top.yogiczy.mytv.tv.ui.screen.components.AppScreen
import top.yogiczy.mytv.tv.ui.theme.MyTvTheme
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents
import top.yogiczy.mytv.tv.ui.utils.saveRequestFocus

private data class DashboardModule(
    val imageVector: ImageVector,
    val title: String,
    val subtitle: String,
    val emphasized: Boolean = false,
    val onSelected: () -> Unit,
)

@Composable
fun DashboardModuleList(
    modifier: Modifier = Modifier,
    toLiveScreen: () -> Unit = {},
    toChannelsScreen: () -> Unit = {},
    toFavoritesScreen: () -> Unit = {},
    toSearchScreen: () -> Unit = {},
    toMultiViewScreen: () -> Unit = {},
    toPushScreen: () -> Unit = {},
    toSettingsScreen: () -> Unit = {},
    toAboutScreen: () -> Unit = {},
) {
    val childPadding = rememberChildPadding()

    val primaryModules = listOf(
        DashboardModule(
            Icons.Outlined.Tv, "直播", "开始观看直播", emphasized = true,
            onSelected = toLiveScreen,
        ),
        DashboardModule(
            Icons.Outlined.GridView, "全部频道", "按分组浏览频道", emphasized = true,
            onSelected = toChannelsScreen,
        ),
        DashboardModule(
            Icons.Outlined.FavoriteBorder, "收藏", "查看收藏的频道", emphasized = true,
            onSelected = toFavoritesScreen,
        ),
        DashboardModule(
            Icons.Outlined.Search, "搜索", "按频道名快速查找", emphasized = true,
            onSelected = toSearchScreen,
        ),
    )
    val secondaryModules = listOf(
        DashboardModule(
            Icons.Outlined.ViewCozy, "多屏同播", "同屏观看多路频道",
            onSelected = toMultiViewScreen,
        ),
        DashboardModule(
            Icons.Outlined.CloudUpload, "推送", "扫码用手机推送",
            onSelected = toPushScreen,
        ),
        DashboardModule(
            Icons.Outlined.Settings, "设置", "直播源与播放偏好",
            onSelected = toSettingsScreen,
        ),
        DashboardModule(
            Icons.Outlined.Info, "关于", "版本与项目信息",
            onSelected = toAboutScreen,
        ),
    )

    val primaryFocusRequesters = remember { List(primaryModules.size) { FocusRequester() } }
    val secondaryFocusRequesters = remember { List(secondaryModules.size) { FocusRequester() } }

    LaunchedEffect(Unit) {
        primaryFocusRequesters.first().saveRequestFocus()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "快捷入口",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = childPadding.start),
        )

        Column(
            modifier = Modifier.padding(start = childPadding.start, end = childPadding.end),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DashboardModuleRow(
                modules = primaryModules,
                focusRequesters = primaryFocusRequesters,
                downFocusRequesters = secondaryFocusRequesters,
                itemHeight = 96.dp,
            )

            DashboardModuleRow(
                modules = secondaryModules,
                focusRequesters = secondaryFocusRequesters,
                upFocusRequesters = primaryFocusRequesters,
                itemHeight = 80.dp,
            )
        }
    }
}

@Composable
private fun DashboardModuleRow(
    modules: List<DashboardModule>,
    focusRequesters: List<FocusRequester>,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 88.dp,
    upFocusRequesters: List<FocusRequester>? = null,
    downFocusRequesters: List<FocusRequester>? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        modules.forEachIndexed { index, module ->
            val wrapToLast: (() -> Unit)? = if (index == 0) {
                { focusRequesters.last().saveRequestFocus() }
            } else null
            val wrapToFirst: (() -> Unit)? = if (index == modules.lastIndex) {
                { focusRequesters.first().saveRequestFocus() }
            } else null
            val moveUp: (() -> Unit)? = upFocusRequesters?.let { requesters ->
                { requesters[index].saveRequestFocus() }
            }
            val moveDown: (() -> Unit)? = downFocusRequesters?.let { requesters ->
                { requesters[index].saveRequestFocus() }
            }

            DashboardModuleItem(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequesters[index])
                    .handleKeyEvents(
                        onLeft = wrapToLast,
                        onRight = wrapToFirst,
                        onUp = moveUp,
                        onDown = moveDown,
                    ),
                imageVector = module.imageVector,
                title = module.title,
                subtitle = module.subtitle,
                emphasized = module.emphasized,
                height = itemHeight,
                onSelected = module.onSelected,
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun DashboardModuleListPreview() {
    MyTvTheme {
        AppScreen {
            DashboardModuleList(
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }
    }
}
