package top.yogiczy.mytv.core.data.repositories.git.parser

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.yogiczy.mytv.core.data.entities.git.GitRelease
import top.yogiczy.mytv.core.data.utils.Globals

/**
 * github发行版解析
 */
class GithubGitReleaseParser : GitReleaseParser {
    override fun isSupport(url: String): Boolean {
        return url.contains("github.com")
    }

    override suspend fun parse(data: String): GitRelease {
        val json = Globals.json.parseToJsonElement(data).jsonObject

        return GitRelease(
            version = json.getValue("tag_name").jsonPrimitive.content.substring(1),
            // 保留原始地址：下载时由 Constants.buildProxyUrls 生成多代理候选
            downloadUrl = json.getValue("assets").jsonArray[0].jsonObject["browser_download_url"]!!.jsonPrimitive.content,
            description = json.getValue("body").jsonPrimitive.content,
        )
    }
}