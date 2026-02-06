package com.heldairy.feature.medication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heldairy.ui.theme.Spacing
import com.heldairy.ui.theme.success
import com.heldairy.ui.theme.StickerDecoration
import com.heldairy.R

data class MedicationStats(
    val total: Int,
    val active: Int,
    val paused: Int,
    val ended: Int
)

@Composable
fun MedicationStatsCard(
    stats: MedicationStats,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.M),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = {
                    Icon(
                        Icons.Outlined.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                label = "全部",
                value = stats.total.toString()
            )
            
            StatItem(
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.success,
                        modifier = Modifier.size(32.dp)
                    )
                },
                label = "正在服用",
                value = stats.active.toString()
            )
            
            StatItem(
                icon = {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                label = "已暂停",
                value = stats.paused.toString()
            )
        }
    }
    StickerDecoration(
        drawableRes = R.drawable.milkshake,
        size = 46.dp,
        rotation = 15f,
        alpha = 0.5f,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 16.dp, y = (-16).dp)
    )
    }
}

@Composable
private fun StatItem(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon()
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
