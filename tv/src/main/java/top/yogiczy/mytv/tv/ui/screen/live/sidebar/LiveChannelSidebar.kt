package top.yogiczy.mytv.tv.ui.screen.live.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import top.yogiczy.mytv.core.data.entities.channel.Channel
import top.yogiczy.mytv.core.data.entities.channel.ChannelGroupList
import top.yogiczy.mytv.core.data.entities.channel.ChannelList
import top.yogiczy.mytv.core.data.entities.epg.EpgList
import top.yogiczy.mytv.core.data.entities.epg.EpgList.Companion.recentProgramme
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents
import kotlin.math.max

private val GROUP_COLUMN_WIDTH = 130.dp
private val CHANNEL_COLUMN_WIDTH = 200.dp

/** 收藏分组在左列的名称 */
private const val FAVORITE_GROUP_NAME = "收藏"

/**
 * 直播侧边栏频道列表（两级）
 *
 * 左列分组、右列频道：进入时分组的焦点落在当前频道所属分组，
 * 右侧频道列随左列焦点变化自动切换，按 OK 选台并关闭。
 */
@Composable
fun LiveChannelSidebar(
    modifier: Modifier = Modifier,
    channelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    currentChannelProvider: () -> Channel = { Channel() },
    epgListProvider: () -> EpgList = { EpgList() },
    showEpgProgrammeProvider: () -> Boolean = { false },
    favoriteEnabledProvider: () -> Boolean = { false },
    favoriteChannelListProvider: () -> ChannelList = { ChannelList() },
    favoriteSwitchEnabledProvider: () -> Boolean = { false },
    onChannelFavoriteToggle: (Channel) -> Unit = {},
    onChannelSelected: (Channel) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val channelGroupList = channelGroupListProvider()
    val currentChannel = currentChannelProvider()
    val favoriteChannelList = favoriteChannelListProvider()
    val favoriteEnabled = favoriteEnabledProvider()

    // 收藏作为左列第一个分组；没有收藏时不插入，避免出现空列表导致焦点无处可落
    val groups = remember(channelGroupList, favoriteChannelList, favoriteEnabled) {
        val base = buildSidebarGroups(channelGroupList)

        if (favoriteEnabled && favoriteChannelList.isNotEmpty()) {
            listOf(SidebarGroup(name = FAVORITE_GROUP_NAME, channelList = ChannelList(favoriteChannelList))) + base
        } else {
            base
        }
    }

    val favoriteChannelNames = remember(favoriteChannelList) {
        favoriteChannelList.map { it.name }.toSet()
    }

    if (groups.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("暂无频道", color = Color.White.copy(alpha = 0.7f))
        }
        return
    }

    var groupIdx by rememberSaveable(groups.size) {
        // 与「上下键只在收藏内切换」保持一致：
        // 开关打开且当前频道已收藏时，打开列表直接停在收藏分组的那一条，
        // 而不是跑到它所属的原始分组里（否则上下的范围和打开列表的位置对不上）
        val favoriteGroupIdx = if (favoriteSwitchEnabledProvider()) {
            groups.indexOfFirst { group ->
                group.name == FAVORITE_GROUP_NAME && group.channelList.any { it.name == currentChannel.name }
            }
        } else {
            -1
        }

        val realGroupIdx = groups.indexOfFirst { group ->
            group.name != FAVORITE_GROUP_NAME && group.channelList.any { it.name == currentChannel.name }
        }

        val fallbackIdx = groups.indexOfFirst { group ->
            group.channelList.any { it.name == currentChannel.name }
        }

        val targetIdx = when {
            favoriteGroupIdx >= 0 -> favoriteGroupIdx
            realGroupIdx >= 0 -> realGroupIdx
            else -> fallbackIdx
        }

        mutableIntStateOf(targetIdx.coerceAtLeast(0))
    }

    val safeGroupIdx = groupIdx.coerceIn(0, groups.lastIndex)
    val currentGroup = groups[safeGroupIdx]
    val channelList = currentGroup.channelList

    // 列表项里直接调用 recentProgramme 会在每次滚动重组时对每个可见项重复计算，
    // 这里按「频道名 → 当前节目名」预先算一次；每 60 秒刷新，节目切档后能跟上。
    val showEpg = showEpgProgrammeProvider()
    val epgList = epgListProvider()

    var epgTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            epgTick++
        }
    }

    val epgTitleMap = remember(channelList, showEpg, epgTick, epgList.size) {
        if (!showEpg) {
            emptyMap()
        } else {
            channelList.associate { it.name to epgList.recentProgramme(it)?.now?.title }
        }
    }

    // 用名称浅比较，Channel.equals 会连线路列表一起比，频道多时开销明显
    val targetIdx = remember(channelList, currentChannel.name) {
        channelList.indexOfFirst { it.name == currentChannel.name }.coerceAtLeast(0)
    }

    val groupListState = rememberLazyListState(
        initialFirstVisibleItemIndex = max(0, safeGroupIdx - 3)
    )
    val channelListState = rememberLazyListState()
    val channelFocusRequester = remember { FocusRequester() }

    // 焦点没落到位之前，不允许左列的 onFocusChanged 反向修改分组
    var initialFocusSettled by remember { mutableStateOf(false) }
    var isFirstEntry by remember { mutableStateOf(true) }

    // 打开侧边栏时直接定位到正在播的那个频道，而不是只定位到它所属的分组
    LaunchedEffect(currentGroup) {
        if (isFirstEntry) {
            isFirstEntry = false

            channelListState.scrollToItem(max(0, targetIdx - 3))

            // LazyColumn 首帧没布局完时 requestFocus 会失败，必须等目标项真正进入可视区
            snapshotFlow {
                channelListState.layoutInfo.visibleItemsInfo.any { it.index == targetIdx }
            }.first { it }

            withFrameNanos { }
            runCatching { channelFocusRequester.requestFocus() }
            initialFocusSettled = true
        } else {
            // 用户主动切换分组时，频道列回到顶部
            channelListState.scrollToItem(0)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(GROUP_COLUMN_WIDTH)
                    .background(Color(0xF2121417)),
            ) {
                Text(
                    text = "全部频道",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 10.dp),
                )

                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = groupListState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(groups, key = { _, group -> group.name }) { idx, group ->
                        SidebarGroupItem(
                            name = group.name,
                            count = group.size,
                            isSelected = idx == safeGroupIdx,
                            onFocused = {
                                if (initialFocusSettled && groupIdx != idx) groupIdx = idx
                            },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(CHANNEL_COLUMN_WIDTH)
                    .background(Color(0xF2181B1F)),
            ) {
                Text(
                    text = "${currentGroup.name} · ${currentGroup.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 24.dp, bottom = 2.dp),
                )

                if (favoriteEnabledProvider()) {
                    Text(
                        text = "长按 OK 收藏 / 取消收藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = channelListState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(
                        channelList,
                        key = { _, channel -> "${channel.index}:${channel.name}" },
                    ) { idx, channel ->
                        SidebarChannelItem(
                            channelProvider = { channel },
                            isCurrent = channel.name == currentChannel.name,
                            epgTitle = epgTitleMap[channel.name],
                            isFavorite = favoriteChannelNames.contains(channel.name),
                            onFavoriteToggle = { onChannelFavoriteToggle(channel) },
                            focusRequester = if (idx == targetIdx) channelFocusRequester else null,
                            onSelected = { onChannelSelected(channel) },
                        )
                    }
                }
            }

            // 右侧留空，画面可见，点击可关闭
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .handleKeyEvents(onSelect = onClose)
            )
        }
    }
}

@Composable
private fun SidebarGroupItem(
    modifier: Modifier = Modifier,
    name: String,
    count: Int,
    isSelected: Boolean,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }

    val background = when {
        isFocused -> Color.White
        isSelected -> Color.White.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val titleColor = if (isFocused) Color(0xF2121417) else Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            )
            .onFocusChanged {
                isFocused = it.isFocused || it.hasFocus
                // 焦点进入即切换右侧频道列，不需要额外按 OK
                if (it.isFocused || it.hasFocus) onFocused()
            }
            .focusable()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isFocused) Color(0xF2121417).copy(alpha = 0.6f)
            else Color.White.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun SidebarChannelItem(
    modifier: Modifier = Modifier,
    channelProvider: () -> Channel = { Channel() },
    isCurrent: Boolean = false,
    isFavorite: Boolean = false,
    epgTitle: String? = null,
    focusRequester: FocusRequester? = null,
    onSelected: () -> Unit = {},
    onFavoriteToggle: () -> Unit = {},
) {
    val channel = channelProvider()

    var isFocused by remember { mutableStateOf(false) }

    val background = when {
        isFocused -> Color.White
        isCurrent -> Color.White.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val titleColor = if (isFocused) Color(0xF2121417) else Color.White
    val secondaryColor =
        if (isFocused) Color(0xF2121417).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.55f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .focusable()
            .handleKeyEvents(onSelect = onSelected, onLongSelect = onFavoriteToggle)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = channel.no,
            style = MaterialTheme.typography.labelMedium,
            color = secondaryColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(22.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (isFocused) Modifier.basicMarquee() else Modifier,
            )

            if (!epgTitle.isNullOrBlank()) {
                Text(
                    text = epgTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isFavorite) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = "已收藏",
                modifier = Modifier.size(14.dp),
                tint = if (isFocused) Color(0xF2121417) else MaterialTheme.colorScheme.primary,
            )
        }

        if (isCurrent) {
            Text(
                text = "播放中",
                style = MaterialTheme.typography.labelSmall,
                color = if (isFocused) Color(0xF2121417) else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
