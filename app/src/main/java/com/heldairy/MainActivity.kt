package com.heldairy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.RunCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import android.os.Build
import com.heldairy.feature.home.HomeDashboardUiState
import com.heldairy.feature.home.HomeDashboardViewModel
import com.heldairy.feature.home.MetricDisplay
import com.heldairy.feature.insights.ui.InsightsRoute
import com.heldairy.feature.medication.ui.MedicationListRoute
import com.heldairy.feature.report.ui.DailyReportRoute
import com.heldairy.feature.settings.ThemeViewModel
import com.heldairy.feature.settings.ui.SettingsRoute
import com.heldairy.core.ui.randomDailyCarePrompt
import com.heldairy.core.ui.randomHomeCarePrompt
import com.heldairy.ui.theme.HElDairyTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { HElDairyApp() }
    }
}

data class ConciergeTab(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HElDairyApp() {
    val tabs = listOf(
        ConciergeTab("首页", "生活管家陪伴你", Icons.AutoMirrored.Outlined.Chat),
        ConciergeTab("日报", "记录今日状态", Icons.AutoMirrored.Outlined.EventNote),
        ConciergeTab("洞察", "趋势与总结", Icons.AutoMirrored.Outlined.TrendingUp),
        ConciergeTab("用药", "药品与疗程", Icons.Outlined.Medication),
        ConciergeTab("设置", "偏好与导出", Icons.Outlined.Settings)
    )

    val isPreview = LocalInspectionMode.current
    var previewDarkTheme by remember { mutableStateOf(false) }
    val themeViewModel: ThemeViewModel? = if (isPreview) null else viewModel(factory = ThemeViewModel.Factory)
    val isDarkTheme = if (isPreview) previewDarkTheme else themeViewModel!!.isDarkTheme.collectAsStateWithLifecycle().value
    val onToggleTheme: () -> Unit = {
        if (isPreview) {
            previewDarkTheme = !previewDarkTheme
        } else {
            themeViewModel?.toggleTheme()
        }
    }

    // Cache care prompts per app session so they stay stable across tab switches.
    val homeCarePrompt = rememberSaveable { randomHomeCarePrompt() }
    val dailyCarePrompt = rememberSaveable { randomDailyCarePrompt() }

    HElDairyTheme(darkTheme = isDarkTheme) {
        val view = LocalView.current
        if (!LocalInspectionMode.current) {
            SideEffect {
                val window = (view.context as Activity).window
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
            }
        }

        var selectedIndex by rememberSaveable { mutableStateOf(0) }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                0 -> HomeGreetingRoute(
                    paddingValues = innerPadding,
                    onStartDaily = { selectedIndex = 1 },
                    onOpenInsights = { selectedIndex = 2 },
                    carePrompt = homeCarePrompt,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
                1 -> DailyReportRoute(
                    paddingValues = innerPadding,
                    carePrompt = dailyCarePrompt
                )
                2 -> InsightsRoute(paddingValues = innerPadding)
                3 -> MedicationListRoute(paddingValues = innerPadding)
                4 -> SettingsRoute(paddingValues = innerPadding)
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
                .padding(24.dp)
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
    HElDairyTheme(darkTheme = false) {
        HElDairyApp()
    }
}

@Composable
private fun HeaderCard(isDarkTheme: Boolean, carePrompt: String, onToggleTheme: () -> Unit) {
    val now = LocalDateTime.now()
    val greeting = greetingForHour(now.hour)
    val dayLine = carePrompt
    val dateText = now.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))
    val shape = RoundedCornerShape(32.dp)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = shape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(800f, 600f)
                    ),
                    shape = shape
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Alex", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Text(text = dayLine, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$greeting，Alex", style = MaterialTheme.typography.headlineMedium)
                    Text(text = dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = onToggleTheme,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Outlined.WbSunny else Icons.Outlined.Nightlight,
                        contentDescription = if (isDarkTheme) "切换到日间" else "切换到夜间"
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeGreetingRoute(
    paddingValues: PaddingValues,
    onStartDaily: () -> Unit,
    onOpenInsights: () -> Unit,
    carePrompt: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
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
            onOpenInsights = onOpenInsights,
            carePrompt = carePrompt,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme
        )
        return
    }

    val viewModel: HomeDashboardViewModel = viewModel(factory = HomeDashboardViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeGreetingScreen(
        paddingValues = paddingValues,
        uiState = uiState,
        onStartDaily = onStartDaily,
        onOpenInsights = onOpenInsights,
        carePrompt = carePrompt,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme
    )
}

@Composable
private fun HomeGreetingScreen(
    paddingValues: PaddingValues,
    uiState: HomeDashboardUiState,
    onStartDaily: () -> Unit,
    onOpenInsights: () -> Unit,
    carePrompt: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val scrollState = rememberScrollState()
    val background = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.background
        ),
        center = Offset(x = 120f, y = 120f),
        radius = 1200f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(isDarkTheme = isDarkTheme, carePrompt = carePrompt, onToggleTheme = onToggleTheme)
            MetricGrid(uiState = uiState, onStartDaily = onStartDaily)
            InsightsCTA(hasEntry = uiState.hasTodayEntry, onStartDaily = onStartDaily, onOpenInsights = onOpenInsights)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun MetricGrid(uiState: HomeDashboardUiState, onStartDaily: () -> Unit) {
    val shouldRedirectToDaily = !uiState.hasTodayEntry
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "步数",
                metric = uiState.steps,
                icon = Icons.Outlined.RunCircle,
                modifier = Modifier.weight(1f),
                accent = Color(0xFF2EA27C),
                onClick = if (shouldRedirectToDaily || uiState.steps == null) onStartDaily else null
            )
            MetricCard(
                title = "昨晚睡眠",
                metric = uiState.sleep,
                icon = Icons.Outlined.Nightlight,
                modifier = Modifier.weight(1f),
                accent = Color(0xFF7C5BE9),
                onClick = if (shouldRedirectToDaily || uiState.sleep == null) onStartDaily else null
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "今日心情",
                metric = uiState.mood,
                icon = Icons.Outlined.Mood,
                modifier = Modifier.weight(1f),
                accent = Color(0xFFCC6A3A),
                onClick = if (shouldRedirectToDaily || uiState.mood == null) onStartDaily else null
            )
            MetricCard(
                title = "身体能量",
                metric = uiState.energy,
                icon = Icons.Outlined.Bolt,
                modifier = Modifier.weight(1f),
                accent = Color(0xFF3D7CE0),
                onClick = if (shouldRedirectToDaily || uiState.energy == null) onStartDaily else null
            )
        }
    }
}

@Composable
private fun InsightsCTA(hasEntry: Boolean, onStartDaily: () -> Unit, onOpenInsights: () -> Unit) {
    val accent = Color(0xFF7D6BEE)
    val shape = RoundedCornerShape(28.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gradient = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF1C1B2B),
                Color(0xFF222640),
                Color(0xFF1A1F32)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFE9E2FF),
                Color(0xFFF0EEFF),
                Color(0xFFE6ECFF)
            )
        )
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    gradient,
                    shape = shape
                )
                .shadow(elevation = 10.dp, shape = shape, ambientColor = accent.copy(alpha = 0.12f), spotColor = accent.copy(alpha = 0.10f))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "洞察", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "每周日更新 · 近 7 天 / 30 天趋势",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = if (hasEntry) onOpenInsights else onStartDaily,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = if (isDark) {
                        ButtonDefaults.buttonColors(containerColor = Color(0xFF292F47), contentColor = Color.White)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = accent)
                    }
                ) {
                    Text(text = if (hasEntry) "前往洞察" else "先完成今日日报", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    metric: MetricDisplay?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gradient = if (isDark) {
        when (title) {
            "步数" -> listOf(Color(0xFF0F1F18), Color(0xFF132820))
            "昨晚睡眠" -> listOf(Color(0xFF18142B), Color(0xFF201B37))
            "今日心情" -> listOf(Color(0xFF2A1B14), Color(0xFF331F1A))
            "身体能量" -> listOf(Color(0xFF0F2135), Color(0xFF132B46))
            else -> listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
        }
    } else {
        when (title) {
            "步数" -> listOf(Color(0xFFDFFBF0), Color(0xFFF5FFFA))
            "昨晚睡眠" -> listOf(Color(0xFFF0E9FF), Color(0xFFF8F4FF))
            "今日心情" -> listOf(Color(0xFFFFF0E0), Color(0xFFFFF8EE))
            "身体能量" -> listOf(Color(0xFFE8F3FF), Color(0xFFF7FBFF))
            else -> listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
        }
    }
    val shape = RoundedCornerShape(28.dp)
    val clickable = onClick != null

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier.shadow(elevation = 12.dp, shape = shape, ambientColor = accent.copy(alpha = 0.18f), spotColor = accent.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(gradient),
                    shape = shape
                )
                .heightIn(min = 190.dp)
                .clip(shape)
                .then(
                    if (clickable) Modifier.clickable(onClick = onClick!!) else Modifier
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent)
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                }
                MetricSpark(accent = accent)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (metric != null) {
                        Text(text = metric.value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = metric.hint ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(text = "点击记录", style = MaterialTheme.typography.bodyMedium, color = accent)
                        Text(text = "今日还未填写", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSpark(accent: Color) {
    val bars = listOf(18.dp, 32.dp, 22.dp, 28.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        bars.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(height)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        color = accent.copy(alpha = 0.65f + (index * 0.07f)),
                    )
            )
        }
    }
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "早上好"
    in 12..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}
