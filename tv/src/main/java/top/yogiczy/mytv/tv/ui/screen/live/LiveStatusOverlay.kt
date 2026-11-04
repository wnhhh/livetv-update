package top.yogiczy.mytv.tv.ui.screen.live

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.tv.ui.material.CircularProgressIndicator
import top.yogiczy.mytv.tv.ui.screen.main.MainUiState
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunched
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents

/**
 * 直播页状态浮层
 *
 * 直播源尚未就绪时覆盖在画面上，替代原来那张整屏的加载页，
 * 就绪后立即消失，不再有一次页面跳转。
 */
@Composable
fun LiveStatusOverlay(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onRetry: () -> Unit = {},
    toSettingsScreen: () -> Unit = {},
) {
    val isError = uiState is MainUiState.Error

    val message = when (uiState) {
        is MainUiState.Loading -> uiState.message ?: "正在加载直播源"
        is MainUiState.Error -> uiState.message ?: "无法加载直播源"
        is MainUiState.Ready -> return
    }

    Box(
        modifier = modifier
            .focusable()
            .focusOnLaunched()
            .handleKeyEvents(
                onSelect = onRetry,
                onSettings = toSettingsScreen,
                onLongSelect = toSettingsScreen,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 64.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isError) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.15f),
                        strokeWidth = 2.5.dp,
                    )
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isError) MaterialTheme.colorScheme.error else Color.White,
                )
            }

            Text(
                text = if (isError) "按 OK 重试 · 按菜单键进入设置"
                else "首次加载完成后，之后启动将直接进入播放",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.9f),
            )
        }
    }
}
