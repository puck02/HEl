package com.heldairy.feature.medication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.heldairy.ui.theme.Spacing
import com.heldairy.ui.theme.CornerRadius
import com.heldairy.ui.theme.Elevation
import com.heldairy.ui.theme.success
import com.heldairy.ui.theme.KittyBackground
import com.heldairy.ui.theme.BackgroundTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heldairy.feature.medication.Med
import com.heldairy.feature.medication.MedFilterStatus
import com.heldairy.feature.medication.MedSortBy
import com.heldairy.feature.medication.MedicationListUiState
import com.heldairy.feature.medication.MedicationListViewModel
import com.heldairy.ui.theme.HElDairyTheme

@Composable
fun MedicationListRoute(
    paddingValues: PaddingValues,
    onMedClick: (Long) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        val previewState = MedicationListUiState(
            meds = listOf(
                Med(id = 1, name = "阿莫西林", hasActiveCourse = true),
                Med(id = 2, name = "布洛芬", hasActiveCourse = false),
                Med(id = 3, name = "维生素C", hasActiveCourse = true)
            ),
            displayedMeds = listOf(
                Med(id = 1, name = "阿莫西林", hasActiveCourse = true),
                Med(id = 2, name = "布洛芬", hasActiveCourse = false),
                Med(id = 3, name = "维生素C", hasActiveCourse = true)
            ),
            isLoading = false
        )
        MedicationListScreen(
            paddingValues = paddingValues,
            uiState = previewState,
            onMedClick = onMedClick,
            onAddMed = {},
            onSearchQueryChange = {},
            onFilterStatusChange = {},
            onSortByChange = {}
        )
        return
    }

    val viewModel: MedicationListViewModel = viewModel(factory = MedicationListViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    KittyBackground(backgroundRes = BackgroundTheme.MEDICATION) {
        MedicationListScreen(
            paddingValues = paddingValues,
            uiState = uiState,
            onMedClick = onMedClick,
            onAddMed = onAddClick,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onFilterStatusChange = viewModel::onFilterStatusChange,
            onSortByChange = viewModel::onSortByChange
        )
    }
}

@Composable
private fun MedicationListScreen(
    paddingValues: PaddingValues,
    uiState: MedicationListUiState,
    onMedClick: (Long) -> Unit,
    onAddMed: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterStatusChange: (MedFilterStatus) -> Unit,
    onSortByChange: (MedSortBy) -> Unit
) {
    Scaffold(
        modifier = Modifier.padding(paddingValues),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMed,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加药品")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 搜索框
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.M),
                placeholder = { Text("搜索药品名称或别名") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                },
                singleLine = true
            )

            Spacer(Modifier.height(Spacing.S))

            // 统计卡片
            MedicationStatsCard(
                stats = uiState.stats,
                modifier = Modifier.padding(horizontal = Spacing.M)
            )

            Spacer(Modifier.height(Spacing.S))

            // 筛选chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.M),
                horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
            ) {
                item {
                    FilterChip(
                        selected = uiState.filterStatus == MedFilterStatus.ALL,
                        onClick = { onFilterStatusChange(MedFilterStatus.ALL) },
                        label = { Text("全部") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterStatus == MedFilterStatus.ACTIVE,
                        onClick = { onFilterStatusChange(MedFilterStatus.ACTIVE) },
                        label = { Text("正在服用") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterStatus == MedFilterStatus.PAUSED,
                        onClick = { onFilterStatusChange(MedFilterStatus.PAUSED) },
                        label = { Text("已暂停") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterStatus == MedFilterStatus.ENDED,
                        onClick = { onFilterStatusChange(MedFilterStatus.ENDED) },
                        label = { Text("已结束") }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.M))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载中...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                uiState.displayedMeds.isEmpty() -> {
                    if (uiState.searchQuery.isNotBlank() || uiState.filterStatus != MedFilterStatus.ALL) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "无符合条件的药品",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        EmptyMedicationState()
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.M, vertical = Spacing.XS),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S)
                    ) {
                        items(
                            items = uiState.displayedMeds,
                            key = { it.id }  // 优化重组性能
                        ) { med ->
                            MedicationCard(
                                med = med,
                                onClick = { onMedClick(med.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMedicationState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.S)
        ) {
            Icon(
                imageVector = Icons.Outlined.Medication,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "还没有添加药品",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击右下角 + 按钮添加",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MedicationCard(
    med: Med,
    onClick: () -> Unit
) {
    val statusColor = if (med.hasActiveCourse) {
        MaterialTheme.colorScheme.success
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Low)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Medication,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.M))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = med.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (med.hasActiveCourse) "正在服用" else "已停用",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMedicationListScreen() {
    HElDairyTheme(darkTheme = false) {
        MedicationListRoute(paddingValues = PaddingValues(0.dp))
    }
}
