package top.yogiczy.mytv.core.data.utils

import top.yogiczy.mytv.core.data.entities.epgsource.EpgSource
import top.yogiczy.mytv.core.data.entities.epgsource.EpgSourceList
import top.yogiczy.mytv.core.data.entities.iptvsource.IptvSource
import top.yogiczy.mytv.core.data.entities.iptvsource.IptvSourceList

/**
 * 常量
 */
object Constants {
    /**
     * 应用 标题
     */
    const val APP_TITLE = "电视直播"

    /**
     * 应用 代码仓库
     */
    const val APP_REPO = "https://github.com/wnhhh/livetv-update"

    /**
     * 应用 说明
     */
    const val APP_DESC = "个人自用的电视直播应用"

    /**
     * 直播源
     */
    val IPTV_SOURCE_LIST = IptvSourceList(
        listOf(
            IptvSource(
                name = "我的直播源",
                url = "https://gh-proxy.com/raw.githubusercontent.com/wnhhh/iptv-list/main/iptv.txt",
            ),
        )
    )

    /**
     * 直播源缓存时间（毫秒）
     *
     * 缓存有效期内启动直接读取本地文件，不再下载和解析；
     * 需要立即拉取最新直播源时，使用「刷新订阅源」按钮强制刷新。
     */
    const val IPTV_SOURCE_CACHE_TIME = 1000 * 60 * 60 * 6L // 6小时

    /**
     * 节目单来源
     */
    val EPG_SOURCE_LIST = EpgSourceList(
        listOf(
            EpgSource(
                name = "默认节目单 fanmingming",
                url = "https://live.fanmingming.com/e.xml",
            ),
        )
    )

    /**
     * 节目单刷新时间阈值（小时）
     */
    const val EPG_REFRESH_TIME_THRESHOLD = 2 // 不到2点不刷新

    /**
     * 频道图标提供
     *
     * {name} 频道名称
     *
     * {name|lowercase} 转成小写
     *
     * {name|uppercase} 转成大写
     *
     */
    const val CHANNEL_LOGO_PROVIDER = "https://live.fanmingming.com/tv/{name|uppercase}.png"

    /**
     * GitHub 加速代理列表，按顺序尝试
     *
     * 单个代理限流、换域名或挂掉时，自动换下一个，最后再试直连。
     * 只保留实测可用的（2026-11 实测 gh-proxy/ghfast 返回 200 且支持 releases 附件；
     * ghproxy.net、ghproxy.cc、gh.llkk.cc、github.moeyy.xyz 均已超时，不要再加回来）。
     */
    val GITHUB_PROXIES = listOf(
        "https://gh-proxy.com/",
        "https://ghfast.top/",
    )

    /**
     * 主代理地址（保留给需要单地址的场景）
     */
    const val GITHUB_PROXY = "https://gh-proxy.com/"

    /**
     * 给原始地址生成一组候选 URL：依次套用各代理，最后附加直连地址兜底
     */
    fun buildProxyUrls(url: String): List<String> {
        if (url.isBlank()) return emptyList()
        if (GITHUB_PROXIES.any { url.startsWith(it) }) return listOf(url)

        return GITHUB_PROXIES.map { it + url } + url
    }

    /**
     * 更新清单所在仓库的 raw 目录
     *
     * 必须指向自己的仓库：原先指向上游作者，点「检查更新」会读到别人的版本、
     * 下载到别人的安装包（签名不同还会安装失败）。
     */
    private const val UPDATE_REPO_RAW =
        "https://raw.githubusercontent.com/wnhhh/livetv-update/main"

    /**
     * 各更新渠道对应的清单文件
     *
     * 存的是不带代理的原始地址，请求时由 [buildProxyUrls] 逐个代理尝试。
     */
    val GIT_RELEASE_LATEST_URL = mapOf(
        "stable" to "${UPDATE_REPO_RAW}/tv-stable.json",
        "beta" to "${UPDATE_REPO_RAW}/tv-beta.json",
        "dev" to "${UPDATE_REPO_RAW}/tv-dev.json",
    )

    /**
     * 网络请求重试次数
     *
     * 直播源拉取失败时会先重试，失败后回退本地缓存；
     * 重试次数过多会让启动长时间卡在加载界面，故限制为 3 次。
     */
    const val NETWORK_RETRY_COUNT = 3L

    /**
     * 网络请求重试间隔时间（毫秒）
     */
    const val NETWORK_RETRY_INTERVAL = 3000L

    /**
     * 播放器 userAgent
     */
    const val VIDEO_PLAYER_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; Android TV) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * 播放器加载超时
     */
    const val VIDEO_PLAYER_LOAD_TIMEOUT = 1000L * 15 // 15秒

    /**
     * 日志历史最大保留条数
     */
    const val LOG_HISTORY_MAX_SIZE = 100

    /**
     * 界面 临时频道界面显示时间
     */
    const val UI_TEMP_CHANNEL_SCREEN_SHOW_DURATION = 2000L // 2秒

    /**
     * 界面 超时未操作自动关闭界面
     */
    const val UI_SCREEN_AUTO_CLOSE_DELAY = 1000L * 15 // 15秒

    /**
     * 界面 时间显示前后范围
     */
    const val UI_TIME_SCREEN_SHOW_DURATION = 1000L * 30 // 前后30秒
}