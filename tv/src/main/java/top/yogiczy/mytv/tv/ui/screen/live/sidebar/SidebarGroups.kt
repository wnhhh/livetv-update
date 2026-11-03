package top.yogiczy.mytv.tv.ui.screen.live.sidebar

import top.yogiczy.mytv.core.data.entities.channel.Channel
import top.yogiczy.mytv.core.data.entities.channel.ChannelGroupList
import top.yogiczy.mytv.core.data.entities.channel.ChannelList
import top.yogiczy.mytv.core.data.utils.ChannelRegion

/**
 * 侧边栏展示用的分组
 *
 * 只在界面上使用，不回写 ChannelGroupList，
 * 因此频道编号、收藏记录都不受影响。
 */
data class SidebarGroup(
    val name: String,
    val channelList: ChannelList,
) {
    val size: Int get() = channelList.size
}

/** 这些分组名视为「泛分组」，内部按地区再细分 */
private val GENERIC_GROUP_NAMES = setOf(
    "地方", "地方频道", "地方台", "地区", "城市", "省内", "国内",
    "国际", "国际频道", "海外", "国外", "港澳台", "港台", "其他", "综合",
)

/** 这些分组不参与按数量自动细分 */
private val NO_AUTO_SPLIT_NAMES = setOf(
    "央视", "CCTV", "卫视", "省级卫视", "卫视台", "上星频道", "上星",
    "数字", "付费", "高清", "4K", "体育", "影视",
)

/** 分组名里含有这些词时也不拆分，保证省级卫视保持原有分类 */
private val NO_SPLIT_KEYWORDS = listOf("卫视", "上星")

/** 单个地区至少有多少个频道才单独成组 */
private const val MIN_CHANNEL_PER_REGION = 3

/** 源里可用这些分隔符显式指定分组，例如 #genre#地方/广东 */
private val EXPLICIT_SEPARATORS = charArrayOf('/', '|', '·', '：', ':')

private fun explicitGroupName(rawName: String): String? {
    val idx = rawName.indexOfFirst { it in EXPLICIT_SEPARATORS }
    if (idx <= 0 || idx >= rawName.length - 1) return null

    return rawName.substring(idx + 1).trim().ifBlank { null }
}

/**
 * 把原始分组列表转换为侧边栏展示分组
 *
 * 规则：
 * 1. 分组名带分隔符（地方/广东）时直接采用分隔符后的名字，不做识别
 * 2. 泛分组（地方频道、国际频道等）按频道名识别省份 / 国家后再拆
 * 3. 识别不出的频道归入「其他」，不足 [MIN_CHANNEL_PER_REGION] 个的地区也并入「其他」
 * 4. 拆分后仍只有一组的，保持原样
 */
fun buildSidebarGroups(source: ChannelGroupList): List<SidebarGroup> {
    val result = mutableListOf<SidebarGroup>()

    source.forEach { group ->
        val explicitName = explicitGroupName(group.name)
        if (explicitName != null) {
            result += SidebarGroup(explicitName, group.channelList)
            return@forEach
        }

        val isProtectedGroup = group.name in NO_AUTO_SPLIT_NAMES ||
                NO_SPLIT_KEYWORDS.any { group.name.contains(it) }

        // 只对已知的泛分组（地方频道/国际频道等）做自动细分。
        // 不再按数量判断：合集类源里「电影频道」「央视频道」也有几十个台，
        // 一旦按数量触发就会被误拆成地区。
        val needSplit = !isProtectedGroup && group.name in GENERIC_GROUP_NAMES

        val splitResult = if (needSplit) splitByRegion(group.channelList) else null
        if (splitResult != null) {
            result += splitResult
        } else {
            result += SidebarGroup(group.name.ifBlank { "其他" }, group.channelList)
        }
    }

    // 合并同名分组（例如源里本来就有「广东」，自动识别又产生一个「广东」）
    return result.groupBy { it.name }.map { (name, groups) ->
        if (groups.size == 1) groups.first()
        else SidebarGroup(name, ChannelList(groups.flatMap { it.channelList }))
    }
}

private fun splitByRegion(channelList: ChannelList): List<SidebarGroup>? {
    if (channelList.isEmpty()) return null

    val buckets = linkedMapOf<String, MutableList<Channel>>()
    val others = mutableListOf<Channel>()

    channelList.forEach { channel ->
        val region = ChannelRegion.of(channel.name)
        if (region == null) others += channel
        else buckets.getOrPut(region) { mutableListOf() } += channel
    }

    buckets.forEach { (_, list) ->
        if (list.size < MIN_CHANNEL_PER_REGION) others += list
    }

    val kept = buckets.filterValues { it.size >= MIN_CHANNEL_PER_REGION }

    // 只识别出一个地区说明这个分组本身不杂，保持原样更有意义
    if (kept.size <= 1) return null

    val groups = kept.entries
        .sortedByDescending { it.value.size }
        .map { (name, list) -> SidebarGroup(name, ChannelList(list)) }
        .toMutableList()

    if (others.isNotEmpty()) {
        groups += SidebarGroup("其他", ChannelList(others))
    }

    return groups
}
