package com.heldairy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.RunCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heldairy.feature.home.HomeDashboardUiState
import com.heldairy.feature.home.HomeDashboardViewModel
import com.heldairy.feature.home.MetricDisplay
import com.heldairy.feature.report.ui.DailyReportRoute
import com.heldairy.feature.settings.ThemeViewModel
import com.heldairy.feature.settings.ui.SettingsRoute
import com.heldairy.ui.theme.HElDairyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HElDairyApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HElDairyApp() {
    val isDarkTheme = if (LocalInspectionMode.current) {
        false
    } else {
        val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
        val dark by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()
        dark
    }

    HElDairyTheme(darkTheme = isDarkTheme, dynamicColor = false) {
        val tabs = remember {
            listOf(
                ConciergeTab(
                    title = "主页",
                    description = "欢迎来到生活管家，准备开始今日的关怀对话。",
                    icon = Icons.AutoMirrored.Outlined.Chat
                ),
                ConciergeTab(
                    title = "日报",
                    description = "今日的基础问题会在这里依序出现。",
                    icon = Icons.AutoMirrored.Outlined.EventNote
                ),
                ConciergeTab(
                    title = "洞察",
                    description = "在这里将展示近期趋势与提示。",
                    icon = Icons.Outlined.Assessment
                ),
                ConciergeTab(
                    title = "设置",
                    description = "管理 API Key、偏好与导出工具。",
                    icon = Icons.Outlined.Settings
                )
            )
        }
        var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

        Scaffold(
            topBar = {
                if (selectedIndex != 0) {
                    CenterAlignedTopAppBar(
                        title = { Text(text = tabs[selectedIndex].title) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (selectedIndex) {
                0 -> HomeGreetingRoute(paddingValues = innerPadding) { selectedIndex = 1 }
                1 -> DailyReportRoute(paddingValues = innerPadding)
                3 -> SettingsRoute(paddingValues = innerPadding)
                else -> TabContent(
                    paddingValues = innerPadding,
                    content = tabs[selectedIndex].description
                )
            }
        }
    }
}

@Composable
private fun TabContent(paddingValues: PaddingValues, content: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "功能逐步上线中，生活管家会很快来到这里。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHElDairyApp() {
    HElDairyTheme {
        HElDairyApp()
    }
}

data class ConciergeTab(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun HomeGreetingRoute(paddingValues: PaddingValues, onStartDaily: () -> Unit) {
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        var previewDarkTheme by remember { mutableStateOf(false) }
        val previewState = HomeDashboardUiState(
            hasTodayEntry = true,
            steps = MetricDisplay(value = "8,240", hint = "已达标"),
            sleep = MetricDisplay(value = "7 h 20 m", hint = "稳定"),
            mood = MetricDisplay(value = "3 / 10", hint = "平稳"),
            energy = MetricDisplay(value = "很好", hint = "充沛")
        )
        HomeGreetingScreen(
            paddingValues = paddingValues,
            uiState = previewState,
            onStartDaily = onStartDaily,
            isDarkTheme = previewDarkTheme,
            onToggleTheme = { previewDarkTheme = !previewDarkTheme }
        )
        return
    }

    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
    val viewModel: HomeDashboardViewModel = viewModel(factory = HomeDashboardViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeState by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()
    HomeGreetingScreen(
        paddingValues = paddingValues,
        uiState = uiState,
        onStartDaily = onStartDaily,
        isDarkTheme = themeState,
        onToggleTheme = { themeViewModel.toggleTheme() }
    )
}

@Composable
private fun HomeGreetingScreen(
    paddingValues: PaddingValues,
    uiState: HomeDashboardUiState,
    onStartDaily: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Alex", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text(text = "今天也一起守护健康", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "晚安好，Alex", style = MaterialTheme.typography.headlineSmall)
                        Text(text = "周三 · 10月24日", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.WbSunny else Icons.Outlined.Nightlight,
                            contentDescription = if (isDarkTheme) "切换到日间" else "切换到夜间"
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "步数",
                metric = uiState.steps,
                icon = Icons.Outlined.RunCircle,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "昨晚睡眠",
                metric = uiState.sleep,
                icon = Icons.Outlined.Nightlight,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "今日心情",
                metric = uiState.mood,
                icon = Icons.Outlined.Assessment,
                modifier = Modifier.weight(1f),
                useGradient = true
            )
            MetricCard(
                title = "身体能量",
                metric = uiState.energy,
                icon = Icons.Outlined.Assessment,
                modifier = Modifier.weight(1f),
                useGradient = true
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "夜间洞察", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = if (uiState.hasTodayEntry) {
                        "今日基础记录已保存，稍后会根据数据生成洞察与建议。"
                    } else {
                        "完成今日日报后，这里会为你准备睡眠与恢复建议。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onStartDaily,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = if (uiState.hasTodayEntry) "查看日报" else "开始填写今日日报")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricCard(
    title: String,
    metric: MetricDisplay?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    useGradient: Boolean = false
) {
    val gradient = if (useGradient) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            )
        )
    } else {
        null
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (gradient != null) Modifier.background(gradient) else Modifier)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = title, style = MaterialTheme.typography.labelLarge)
            }
            if (metric != null) {
                Text(text = metric.value, style = MaterialTheme.typography.headlineSmall)
                metric.hint?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "点击记录",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "今日还未填写",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
