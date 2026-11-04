package top.yogiczy.mytv.core.util.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {
    /**
     * 调起系统安装器
     *
     * @return 是否成功调起。部分机型即使未授予「安装未知应用」也能调起，
     *         具体是否允许安装由系统安装器在安装时询问，因此这里不判断权限。
     */
    fun installApk(context: Context, filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists()) return false

        return runCatching {
            val apkFile = prepareApkFile(context, file)

            val uri =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context, context.packageName + ".FileProvider", apkFile
                    )
                }
                else Uri.fromFile(apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                setDataAndType(uri, "application/vnd.android.package-archive")
            }

            context.startActivity(installIntent)
            true
        }.getOrDefault(false)
    }

    /**
     * 确保 apk 位于 FileProvider 放行的 cacheDir 内
     *
     * 下载目录本身就是 cacheDir，直接复用即可。原先无条件 readBytes + writeBytes
     * 会把整个安装包（约 17MB）读进堆内存再写一份，低内存电视容易失败。
     */
    @SuppressLint("SetWorldReadable")
    private fun prepareApkFile(context: Context, file: File): File {
        val cacheDir = context.cacheDir

        if (runCatching { file.canonicalPath.startsWith(cacheDir.canonicalPath) }.getOrDefault(false)) {
            // 已在 cacheDir 内，直接交给 FileProvider
            file.setReadable(true, false)
            return file
        }

        // 少数入口（例如浏览器上传）会把包放到其它目录，这时才流式复制一份
        val target = File(cacheDir, file.name)

        if (!target.exists() || target.length() != file.length()) {
            file.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }

        // 解决 Android6 无法解析安装包
        target.setReadable(true, false)
        return target
    }
}
