package top.yogiczy.mytv.core.data.repositories.git

import top.yogiczy.mytv.core.data.entities.git.GitRelease
import top.yogiczy.mytv.core.data.network.request
import top.yogiczy.mytv.core.data.repositories.git.parser.GitReleaseParser
import top.yogiczy.mytv.core.data.utils.Loggable

/**
 * git数据获取
 */
class GitRepository : Loggable("GitRepository") {

    /**
     * 获取最新发行版
     */
    suspend fun latestRelease(url: String): GitRelease {
        log.d("获取最新发行版: $url")

        try {
            val parser = GitReleaseParser.instances.first { it.isSupport(url) }
            return url.request { body -> parser.parse(body.string()) }!!
        } catch (ex: Exception) {
            log.e("获取最新发行版失败", ex)
            throw Exception("获取最新发行版失败，请检查网络连接", ex)
        }
    }

    /**
     * 依次尝试多个地址获取最新发行版
     *
     * 某个加速代理限流、换域名或失效时自动换下一个，避免更新地址单点故障。
     */
    suspend fun latestReleaseWithFallback(urls: List<String>): GitRelease {
        if (urls.isEmpty()) throw Exception("没有可用的更新地址")

        var lastError: Exception? = null

        urls.forEachIndexed { index, url ->
            try {
                return latestRelease(url)
            } catch (ex: Exception) {
                log.w("第 ${index + 1} 个地址获取清单失败，换下一个")
                lastError = ex
            }
        }

        throw lastError ?: Exception("获取最新发行版失败")
    }
}