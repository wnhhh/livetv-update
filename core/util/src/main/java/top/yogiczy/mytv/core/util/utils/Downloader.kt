package top.yogiczy.mytv.core.util.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import java.io.File
import java.io.FileOutputStream

object Downloader {
    /**
     * 依次尝试多个下载地址
     *
     * 某个加速代理失效时自动换下一个；失败可能已写入半截文件，删掉后重试，
     * 避免把不完整的安装包交给系统安装器。
     */
    suspend fun downloadToAny(
        urls: List<String>,
        filePath: String,
        onProgressCb: ((Int) -> Unit)?,
    ) = withContext(Dispatchers.IO) {
        if (urls.isEmpty()) throw Exception("没有可用的下载地址")

        var lastError: Exception? = null

        urls.forEachIndexed { index, url ->
            try {
                downloadTo(url, filePath, onProgressCb)
                return@withContext
            } catch (ex: Exception) {
                lastError = ex
                runCatching { File(filePath).delete() }
                println("下载失败（第 ${index + 1} 个地址），换下一个：${ex.message}")
            }
        }

        throw lastError ?: Exception("下载文件失败")
    }

    suspend fun downloadTo(url: String, filePath: String, onProgressCb: ((Int) -> Unit)?) =
        withContext(Dispatchers.IO) {
            val interceptor = Interceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(DownloadResponseBody(originalResponse, onProgressCb)).build()
            }

            val client = OkHttpClient.Builder().addNetworkInterceptor(interceptor).build()
            val request = okhttp3.Request.Builder().url(url).build()

            try {
                with(client.newCall(request).execute()) {
                    if (!isSuccessful) throw Exception("下载文件失败: $code")

                    val file = File(filePath)
                    FileOutputStream(file).use { fos -> fos.write(body!!.bytes()) }
                }
            } catch (ex: Exception) {
                throw Exception("下载文件失败，请检查网络连接", ex)
            }
        }

    private class DownloadResponseBody(
        private val originalResponse: okhttp3.Response,
        private val onProgressCb: ((Int) -> Unit)?,
    ) : okhttp3.ResponseBody() {
        override fun contentLength() = originalResponse.body!!.contentLength()

        override fun contentType() = originalResponse.body?.contentType()

        override fun source(): BufferedSource {
            return object : ForwardingSource(originalResponse.body!!.source()) {
                var totalBytesRead = 0L

                override fun read(sink: okio.Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                    val progress = (totalBytesRead * 100 / contentLength()).toInt()
                    CoroutineScope(Dispatchers.IO).launch {
                        onProgressCb?.invoke(progress)
                    }
                    return bytesRead
                }
            }.buffer()
        }
    }
}