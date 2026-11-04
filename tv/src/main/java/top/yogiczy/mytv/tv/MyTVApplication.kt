package top.yogiczy.mytv.tv

import android.app.Application
import android.content.Intent
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import top.yogiczy.mytv.core.data.AppData
import top.yogiczy.mytv.core.data.entities.channel.Channel
import top.yogiczy.mytv.core.data.utils.Constants
import top.yogiczy.mytv.tv.ui.screen.Screens
import top.yogiczy.mytv.tv.ui.utils.Configs
import kotlin.system.exitProcess

class MyTVApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        crashHandle()
        AppData.init(applicationContext)
        initDefaultConfigs()
    }

    /**
     * 内置默认配置仅在首次启动时注入一次
     *
     * 之前每次冷启动都无条件覆盖，会把用户改过的直播源、播放记录等设置重置，
     * 并且把直播源缓存时间强制写成 0，导致每次启动都要重新下载解析整个直播源。
     */
    private fun initDefaultConfigs() {
        if (Configs.appFirstInitDone) return

        Configs.iptvSourceCurrent = Constants.IPTV_SOURCE_LIST.first()
        Configs.iptvSourceCacheTime = Constants.IPTV_SOURCE_CACHE_TIME
        Configs.iptvChannelLastPlay = Channel.EMPTY
        Configs.appStartupScreen = Screens.Live.name
        Configs.appAgreementAgreed = true
        Configs.epgEnable = false
        Configs.appFirstInitDone = true
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader(this).newBuilder()
            .logger(DebugLogger())
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .build()
            }
            .build()
    }

    private fun crashHandle() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            throwable.printStackTrace()

            val intent = Intent(this, CrashHandlerActivity::class.java).apply {
                putExtra("error_message", throwable.message)
                putExtra("error_stacktrace", throwable.stackTraceToString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)

            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        }
    }
}
