package com.heldairy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius as GeometryCornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
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
import com.heldairy.feature.medication.AddMedicationViewModel
import com.heldairy.feature.medication.ui.AddMedicationRoute
import com.heldairy.feature.medication.ui.MedicationDetailRoute
import com.heldairy.feature.medication.ui.MedicationListRoute
import com.heldairy.feature.report.ui.DailyReportRoute
import com.heldairy.feature.settings.ThemeViewModel
import com.heldairy.feature.settings.ui.SettingsRoute
import com.heldairy.core.ui.randomDailyCarePrompt
import com.heldairy.core.ui.randomHomeCarePrompt
import com.heldairy.ui.theme.HElDairyTheme
import com.heldairy.ui.theme.Spacing
import com.heldairy.ui.theme.CornerRadius
import com.heldairy.ui.theme.Elevation
import com.heldairy.ui.theme.ElegantAnimations
import com.heldairy.ui.theme.IconSize
import com.heldairy.ui.theme.success
import com.heldairy.ui.theme.warning
import com.heldairy.ui.theme.info
import com.heldairy.ui.theme.accentPurple
import com.heldairy.ui.theme.accentIndigo
import com.heldairy.ui.theme.accentTeal
import com.heldairy.ui.theme.accentPeach
import com.heldairy.ui.theme.BowDecoration
import com.heldairy.ui.theme.SmallBowDecoration
import com.heldairy.ui.theme.KittyAvatar
import com.heldairy.ui.theme.KittyBackground
import com.heldairy.ui.theme.BackgroundTheme
import com.heldairy.ui.theme.StickerDecoration
import com.heldairy.ui.theme.accentPurple
import com.heldairy.ui.theme.accentIndigo
import com.heldairy.ui.theme.accentTeal
import com.heldairy.ui.theme.accentPeach
import androidx.compose.foundation.layout.offset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import coil.compose.AsyncImage

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
    @DrawableRes val iconRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HElDairyApp() {
    val tabs = listOf(
        ConciergeTab("首页", "生活管家陪伴你", R.drawable.bow),
        ConciergeTab("日报", "记录今日状态", R.drawable.cake01),
        ConciergeTab("洞察", "趋势与总结", R.drawable.cake02),
        ConciergeTab("用药", "药品与疗程", R.drawable.milkshake),
        ConciergeTab("设置", "偏好与导出", R.drawable.strawberry)
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
                IOSStyleNavigationBar(
                    tabs = tabs,
                    selectedIndex = selectedIndex,
                    onTabSelected = { selectedIndex = it }
                )
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
                3 -> {
                    var selectedMedId by rememberSaveable { mutableStateOf<Long?>(null) }
                    var showAddScreen by rememberSaveable { mutableStateOf(false) }
                    
                    val app = LocalContext.current.applicationContext as HElDairyApplication
                    
                    when {
                        selectedMedId != null -> {
                            // Add key to force recomposition when medId changes
                            key(selectedMedId) {
                                MedicationDetailRoute(
                                    medId = selectedMedId!!,
                                    paddingValues = innerPadding,
                                    onBack = { selectedMedId = null }
                                )
                            }
                        }
                        showAddScreen -> {
                            val addViewModel: AddMedicationViewModel = viewModel(
                                key = "addMedication",
                                factory = AddMedicationViewModel.factory(
                                    repository = app.appContainer.medicationRepository,
                                    nlpParser = app.appContainer.medicationNlpParser,
                                    preferencesStore = app.appContainer.aiPreferencesStore
                                )
                            )
                            AddMedicationRoute(
                                viewModel = addViewModel,
                                paddingValues = innerPadding,
                                onBack = { showAddScreen = false }
                            )
                        }
                        else -> {
                            MedicationListRoute(
                                paddingValues = innerPadding,
                                onMedClick = { medId -> selectedMedId = medId },
                                onAddClick = { showAddScreen = true }
                            )
                        }
                    }
                }
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
            Spacer(modifier = Modifier.weight(1f))
            // 大号 Kitty 头像
            KittyAvatar(size = 120.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "功能逐步上线中喵～生活管家会很快来到这里！",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))
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
private fun HeaderCard(
    uiState: HomeDashboardUiState,
    isDarkTheme: Boolean,
    carePrompt: String,
    onToggleTheme: () -> Unit
) {
    val now = LocalDateTime.now()
    val greeting = greetingForHour(now.hour)
    val dayLine = carePrompt
    val dateText = now.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))

    Box(modifier = Modifier.fillMaxWidth()) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
        modifier = Modifier
            .padding(horizontal = Spacing.M, vertical = Spacing.XXS)
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(CornerRadius.Medium)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(CornerRadius.Medium)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.S),
            modifier = Modifier.padding(Spacing.M)
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(IconSize.Hero)  // 使用统一 IconSize
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.avatarUri != null) {
                    AsyncImage(
                        model = uiState.avatarUri,
                        contentDescription = "头像",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Hello Kitty 默认头像
                    KittyAvatar(
                        size = IconSize.Hero
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.XXS),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dayLine,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$greeting，${uiState.userName}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 主题切换按钮带优雅缩放
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = ElegantAnimations.elegantSpring,
                label = "theme_button_scale"
            )
            
            IconButton(
                onClick = {
                    isPressed = true
                    onToggleTheme()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Outlined.WbSunny else Icons.Outlined.Nightlight,
                    contentDescription = if (isDarkTheme) "切换到日间" else "切换到夜间"
                )
            }
            
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    delay(150)
                    isPressed = false
                }
            }
        }
    }
    StickerDecoration(
        drawableRes = R.drawable.bow,
        size = 54.dp,
        rotation = 20f,
        alpha = 0.55f,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 20.dp, y = (-20).dp)
    )
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

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        KittyBackground(backgroundRes = BackgroundTheme.HOME) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(paddingValues)
                    .padding(top = Spacing.L, bottom = Spacing.M),
                verticalArrangement = Arrangement.spacedBy(Spacing.M)
            ) {
            HeaderCard(
                uiState = uiState,
                isDarkTheme = isDarkTheme,
                carePrompt = carePrompt,
                onToggleTheme = onToggleTheme
            )
            MetricGrid(uiState = uiState, onStartDaily = onStartDaily)
            InsightsCTA(hasEntry = uiState.hasTodayEntry, onStartDaily = onStartDaily, onOpenInsights = onOpenInsights)
            Spacer(modifier = Modifier.height(Spacing.XXS))
        }
        }
    }
}

@Composable
private fun MetricGrid(uiState: HomeDashboardUiState, onStartDaily: () -> Unit) {
    val shouldRedirectToDaily = !uiState.hasTodayEntry
    Column(
        modifier = Modifier
            .padding(horizontal = Spacing.M)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.S)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "步数",
                metric = uiState.steps,
                icon = Icons.Outlined.RunCircle,
                modifier = Modifier.weight(1f),
                onClick = if (shouldRedirectToDaily || uiState.steps == null) onStartDaily else null,
                index = 0
            )
            MetricCard(
                title = "昨晚睡眠",
                metric = uiState.sleep,
                icon = Icons.Outlined.Nightlight,
                modifier = Modifier.weight(1f),
                onClick = if (shouldRedirectToDaily || uiState.sleep == null) onStartDaily else null,
                index = 1
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S), modifier = Modifier.fillMaxWidth()) {
            // 根据心情值动态改变图标
            val moodIcon = when {
                uiState.mood == null -> Icons.Outlined.Mood
                else -> {
                    val moodScore = uiState.mood.value.split("/").firstOrNull()?.trim()?.toIntOrNull() ?: 5
                    when {
                        moodScore <= 3 -> Icons.Outlined.SentimentVerySatisfied  // 平稳
                        moodScore <= 6 -> Icons.Outlined.SentimentNeutral       // 略烦躁
                        else -> Icons.Outlined.SentimentDissatisfied            // 明显紧绷
                    }
                }
            }
            
            MetricCard(
                title = "今日心情",
                metric = uiState.mood,
                icon = Icons.Outlined.Mood,
                dynamicIcon = moodIcon,
                modifier = Modifier.weight(1f),
                onClick = if (shouldRedirectToDaily || uiState.mood == null) onStartDaily else null,
                index = 2
            )
            MetricCard(
                title = "身体能量",
                metric = uiState.energy,
                icon = Icons.Outlined.Bolt,
                modifier = Modifier.weight(1f),
                onClick = if (shouldRedirectToDaily || uiState.energy == null) onStartDaily else null,
                index = 3
            )
        }
    }
}

@Composable
private fun InsightsCTA(hasEntry: Boolean, onStartDaily: () -> Unit, onOpenInsights: () -> Unit) {
    // 按钮缩放动画
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = ElegantAnimations.elegantSpring,
        label = "button_scale"
    )
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)  // 使用半透明背景
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Low),  // 添加微妙阴影
        modifier = Modifier
            .padding(horizontal = Spacing.M)
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.S),
            modifier = Modifier.padding(Spacing.M)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "洞察",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                StickerDecoration(
                    drawableRes = R.drawable.kitty01,
                    size = 36.dp,
                    rotation = -8f,
                    alpha = 0.7f,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            }
            Text(
                text = "每周日更新 · 近 7 天 / 30 天趋势",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    isPressed = true
                    if (hasEntry) onOpenInsights() else onStartDaily()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(buttonScale),
                shape = RoundedCornerShape(CornerRadius.XLarge),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (hasEntry) "前往洞察" else "先完成今日日报",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    delay(100)
                    isPressed = false
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
    onClick: (() -> Unit)? = null,
    index: Int = 0,
    dynamicIcon: ImageVector? = null  // 新增：允许传入动态图标
) {
    val clickable = onClick != null
    
    // 使用动态图标或默认图标
    val displayIcon = dynamicIcon ?: icon
    
    // 每个卡片使用不同的 Accent 颜色
    val accentColor = when (index) {
        0 -> MaterialTheme.colorScheme.accentPurple   // 睡眠
        1 -> MaterialTheme.colorScheme.accentTeal     // 情绪
        2 -> MaterialTheme.colorScheme.accentIndigo   // 症状
        3 -> MaterialTheme.colorScheme.accentPeach    // 能量
        else -> MaterialTheme.colorScheme.primary
    }
    
    // Staggered入场动画 - 优雅延迟
    var isVisible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = ElegantAnimations.smoothEntry,
        label = "card_alpha_$index"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = ElegantAnimations.smoothEntry,
        label = "card_offset_$index"
    )
    
    LaunchedEffect(Unit) {
        delay(index * 60L)  // 略微加快 stagger
        isVisible = true
    }
    
    // 按压缩放动画
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ElegantAnimations.elegantSpring,
        label = "card_scale_$index"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Elevation.Low,
            pressedElevation = Elevation.None
        ),
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 180.dp)
                .then(
                    if (clickable) Modifier.clickable {
                        isPressed = true
                        onClick!!()
                    } else Modifier
                )
        ) {
            // 装饰性背景圆形（右上角）
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.06f)
            ) {
                val circleRadius = size.width * 0.5f
                val center = Offset(
                    x = size.width * 1.1f,
                    y = -size.height * 0.2f
                )
                drawCircle(
                    color = accentColor,
                    radius = circleRadius,
                    center = center
                )
            }
            
            // Hello Kitty 蝴蝶结装饰（右上角）
            SmallBowDecoration(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.S)
                    .alpha(0.3f),
                color = accentColor
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.S),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.M)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
                ) {
                    // 使用 Accent 颜色的图标
                    Icon(
                        imageVector = displayIcon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.Large),
                        tint = accentColor
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                MetricSpark(weeklyData = metric?.weeklyData ?: emptyList())
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.XXS)) {
                    if (metric != null) {
                        Text(
                            text = metric.value,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = metric.hint ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        StickerDecoration(
                            drawableRes = R.drawable.cake01,
                            size = 48.dp,
                            rotation = -10f,
                            alpha = 0.55f,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(Spacing.XS))
                        Text(
                            text = "点击记录喵～",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
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
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun MetricSpark(weeklyData: List<Float>) {
    // 如果没有数据，使用默认占位符
    val bars = if (weeklyData.isEmpty()) {
        listOf(0.4f, 0.8f, 0.5f, 0.7f)
    } else {
        weeklyData
    }
    
    // 将数据归一化到dp高度 (最大40dp，最小8dp)
    val maxHeight = 40.dp
    val minHeight = 8.dp
    val heights = bars.map { value ->
        val normalizedValue = value.coerceIn(0f, 1f)
        minHeight + (maxHeight - minHeight) * normalizedValue
    }
    
    val animatedHeights = heights.mapIndexed { index, height ->
        animateDpAsState(
            targetValue = height,
            animationSpec = tween(durationMillis = 600, delayMillis = index * 100),
            label = "spark_height_$index"
        )
    }
    
    val animatedAlphas = bars.mapIndexed { index, _ ->
        animateFloatAsState(
            targetValue = (0.5f + (index * 0.08f)).coerceIn(0.5f, 1.0f),  // 确保alpha在合法范围内
            animationSpec = tween(durationMillis = 600, delayMillis = index * 100),
            label = "spark_alpha_$index"
        )
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.XXS),
        verticalAlignment = Alignment.Bottom
    ) {
        animatedHeights.forEachIndexed { index, animatedHeight ->
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(animatedHeight.value)
                    .clip(RoundedCornerShape(CornerRadius.Small))
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = animatedAlphas[index].value
                        )
                    )
            )
        }
    }
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "喵～早上好"
    in 12..13 -> "喵～中午好"
    in 14..17 -> "喵～下午好"
    else -> "喵～晚上好"
}

@Composable
private fun IOSStyleNavigationBar(
    tabs: List<ConciergeTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // 计算indicator位置和宽度
    val itemWidth = remember { mutableStateOf(0f) }
    val targetOffset = if (itemWidth.value > 0) selectedIndex * itemWidth.value else 0f
    
    // 使用Spring动画实现流畅的indicator移动
    val indicatorOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicator_offset"
    )
    
    // indicator宽度动画 - 移动时略微拉伸
    val indicatorWidth by androidx.compose.animation.core.animateFloatAsState(
        targetValue = itemWidth.value * 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicator_width"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
    ) {
        // 使用Canvas绘制水滴形状的indicator
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val tabCount = tabs.size
            val calculatedItemWidth = canvasWidth / tabCount
            
            // 更新itemWidth用于offset计算
            itemWidth.value = calculatedItemWidth
            
            // 绘制iOS风格的水滴indicator
            val indicatorCenterX = indicatorOffset + calculatedItemWidth / 2
            val indicatorY = canvasHeight * 0.3f
            val indicatorHeight = 36.dp.toPx()
            
            // 绘制圆角矩形作为indicator背景（带模糊效果）
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.15f),
                topLeft = Offset(
                    x = indicatorCenterX - indicatorWidth / 2,
                    y = indicatorY
                ),
                size = Size(indicatorWidth, indicatorHeight),
                cornerRadius = GeometryCornerRadius(
                    x = indicatorHeight / 2,
                    y = indicatorHeight / 2
                )
            )
        }
        
        // Tab items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedIndex == index
                
                // 选中状态的缩放动画
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "tab_scale_$index"
                )
                
                // 选中状态的alpha动画
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.5f,
                    animationSpec = tween(durationMillis = 200),
                    label = "tab_alpha_$index"
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = tab.iconRes),
                        contentDescription = tab.title,
                        modifier = Modifier.size(28.dp),
                        alpha = if (isSelected) 1f else 0.5f
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
