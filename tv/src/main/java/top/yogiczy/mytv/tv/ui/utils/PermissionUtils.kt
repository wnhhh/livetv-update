package top.yogiczy.mytv.tv.ui.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import top.yogiczy.mytv.tv.ui.material.Snackbar
import top.yogiczy.mytv.tv.ui.material.SnackbarType

/**
 * 是否已允许「应用内安装其他应用」
 *
 * Android 8 以下无需该权限，视为始终允许。
 */
fun Context.canInstallPackages(): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) true
    else packageManager.canRequestPackageInstalls()

/**
 * 是否已获得存储权限
 *
 * Android 11+ 判断「所有文件访问」，旧版本判断运行时读外部存储权限。
 */
fun Context.hasExternalStoragePermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    } else true

/**
 * 找出本机可用的「安装未知应用」设置页
 *
 * 不同 ROM 实现的页面不一样：小米电视只认带包名参数的页面，部分机型连该页面都没有，
 * 但安装时系统仍会自己弹确认框，所以逐级尝试，全部不可用才返回 null。
 */
fun resolveInstallSettingsIntent(context: Context): Intent? {
    val packageUri = Uri.parse("package:${context.packageName}")

    val candidates = listOf(
        // 标准做法：直接打开本应用的「安装未知应用」开关页
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri),
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES),
        // 退化：应用详情页，里面通常有权限入口
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri),
        Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),
        Intent(Settings.ACTION_SECURITY_SETTINGS),
    )

    return candidates.firstOrNull { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            context.packageManager.resolveActivity(intent, 0) != null
        }.getOrDefault(false)
    }
}

@Composable
fun rememberCanRequestPackageInstallsPermission(): Pair<Boolean, () -> Unit> {

    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(context.canInstallPackages()) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            hasPermission = context.canInstallPackages()

            if (!hasPermission) {
                Snackbar.show("未授予 应用内安装其他应用 权限", type = SnackbarType.ERROR)
            }
        }

    fun requestPermission() {
        if (hasPermission) return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            hasPermission = true
        } else {
            hasPermission = context.canInstallPackages()

            if (!hasPermission) {
                val intent = resolveInstallSettingsIntent(context)

                if (intent != null) {
                    runCatching { launcher.launch(intent) }
                        .onFailure {
                            Snackbar.show(
                                "打开系统设置页失败，可在系统设置中手动开启「安装未知应用」",
                                type = SnackbarType.ERROR,
                            )
                        }
                } else {
                    // 小米等部分机型没有该设置页，安装时系统会自行询问
                    Snackbar.show(
                        "本机系统没有该设置页；点「立即更新」安装时，系统会弹窗询问是否允许安装",
                        duration = 8_000,
                    )
                }
            }
        }
    }

    // 从系统设置返回后重新判断，否则授权状态不会更新
    RefreshPermissionOnResume { hasPermission = context.canInstallPackages() }

    return Pair(hasPermission, ::requestPermission)
}

@Composable
fun rememberReadExternalStoragePermission(): Pair<Boolean, () -> Unit> {

    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(context.hasExternalStoragePermission()) }

    val launcherAfterR =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            hasPermission = context.hasExternalStoragePermission()

            if (!hasPermission) {
                Snackbar.show("未授予 管理全部文件 权限", type = SnackbarType.ERROR)
            }
        }

    val launcherAfterM = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        hasPermission = context.hasExternalStoragePermission()

        if (!hasPermission) {
            Snackbar.show("未授予 读取外部存储 权限", type = SnackbarType.ERROR)
        }
    }

    fun requestPermission() {
        if (hasPermission) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val intent =
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                launcherAfterR.launch(intent)
            }.onFailure {
                Snackbar.show(
                    "无法找到对应的设置项，请手动授予 管理全部文件 权限",
                    type = SnackbarType.ERROR,
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            launcherAfterM.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            hasPermission = true
        }
    }

    RefreshPermissionOnResume { hasPermission = context.hasExternalStoragePermission() }

    return Pair(hasPermission, ::requestPermission)
}

/**
 * 页面回到前台时刷新权限状态
 *
 * 这两类权限只能在系统设置页开启，返回应用时必须重新读取，否则界面会一直显示未授予。
 */
@Composable
private fun RefreshPermissionOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * 首次启动检查关键权限，缺失时提示一次
 *
 * 电视系统不会在启动时弹权限请求；且「应用内安装其他应用」「所有文件访问」属于特殊权限，
 * 系统不允许应用直接弹窗授予，只能由用户到系统设置里手动开启，应用侧只能做提示引导。
 */
@Composable
fun rememberPermissionGuideOnce() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (Configs.appPermissionGuideShown) return@LaunchedEffect

        val missing = buildList {
            if (!context.canInstallPackages()) add("应用内安装其他应用")
            if (!context.hasExternalStoragePermission()) add("所有文件访问")
        }

        if (missing.isEmpty()) return@LaunchedEffect

        // 只提示一次，避免每次启动都打扰
        Configs.appPermissionGuideShown = true

        Snackbar.show(
            "缺少权限：${missing.joinToString("、")}；请按菜单键 → 设置 → 权限 开启",
            duration = 10_000,
        )
    }
}
