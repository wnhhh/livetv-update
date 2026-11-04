package top.yogiczy.mytv.tv.ui.screen.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.tv.ui.theme.MyTvTheme
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents

@Composable
fun DashboardModuleItem(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    title: String,
    subtitle: String? = null,
    tag: String? = null,
    emphasized: Boolean = false,
    height: Dp = 88.dp,
    onSelected: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val titleStyle = if (emphasized) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.titleMedium
    }

    val containerColor by animateColorAsState(
        targetValue = if (isFocused) colorScheme.onSurface
        else colorScheme.onSurface.copy(alpha = 0.1f),
        label = "moduleContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isFocused) colorScheme.surface else colorScheme.onSurface,
        label = "moduleContent",
    )
    val iconContainerColor by animateColorAsState(
        targetValue = when {
            isFocused -> colorScheme.surface.copy(alpha = 0.14f)
            emphasized -> colorScheme.primary.copy(alpha = 0.18f)
            else -> colorScheme.onSurface.copy(alpha = 0.08f)
        },
        label = "moduleIconContainer",
    )
    val iconTint by animateColorAsState(
        targetValue = when {
            isFocused -> colorScheme.surface
            emphasized -> colorScheme.primary
            else -> colorScheme.onSurface.copy(alpha = 0.85f)
        },
        label = "moduleIcon",
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        label = "moduleScale",
    )

    Box(
        modifier = modifier
            .height(height)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .handleKeyEvents(onSelect = onSelected)
            .focusable()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = titleStyle.copy(fontWeight = FontWeight.Medium),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        tag?.let { nnTag ->
            Text(
                text = nnTag,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.shapes.medium,
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun DashboardModuleItemPreview() {
    MyTvTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DashboardModuleItem(
                modifier = Modifier.width(196.dp),
                imageVector = Icons.Outlined.Tv,
                title = "直播",
                subtitle = "开始观看直播",
                emphasized = true,
                height = 96.dp,
            )

            DashboardModuleItem(
                modifier = Modifier.width(196.dp),
                imageVector = Icons.Outlined.Settings,
                title = "设置",
                subtitle = "直播源与播放偏好",
                tag = "NEW",
                height = 80.dp,
            )
        }
    }
}
