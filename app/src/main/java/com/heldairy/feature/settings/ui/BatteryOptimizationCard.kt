package com.heldairy.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.heldairy.ui.theme.CornerRadius
import com.heldairy.ui.theme.Spacing

/**
 * 电池优化白名单引导卡片
 * 
 * 如果应用未被加入电池优化白名单，显示引导卡片。
 * 确保提醒功能在应用关闭后仍能正常工作。
 */
@Composable
fun BatteryOptimizationCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isIgnoring by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

    // 如果已经忽略电池优化，不显示卡片
    if (isIgnoring) return

    val manufacturerHint = remember { BatteryOptimizationHelper.getManufacturerHint() }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.M),
            verticalArrangement = Arrangement.spacedBy(Spacing.S)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.S)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "提醒功能优化",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "为确保日报提醒和用药提醒在应用关闭后仍能准时通知，请关闭电池优化限制。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (manufacturerHint != null) {
                Text(
                    text = manufacturerHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                    // 稍后重新检查状态（用户可能从系统设置返回）
                    isIgnoring = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("去设置")
            }
        }
    }
}
