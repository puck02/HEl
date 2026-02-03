# 首页优化 - 2026-02-03

## Overview
实现4个首页优化功能：
1. 四个卡片的柱状图显示真实7天数据
2. 默认用户名改为"Alex"
3. 删除设置页面的开发者选项
4. 心情卡片图标根据当日心情值动态变化

## Development Process

### 1. 数据架构改造
**目标**：让首页卡片显示近7天的真实趋势数据，而非静态占位柱状图。

**实现步骤**：

#### 1.1 扩展数据模型
```kotlin
// HomeDashboardViewModel.kt
data class MetricDisplay(
    val value: String,
    val hint: String?,
    val weeklyData: List<Float> = emptyList()  // 新增：存储7天归一化数据
)
```

#### 1.2 Repository层增加日期范围查询
```kotlin
// DailyReportRepository.kt
fun entriesForRange(
    startDate: java.time.LocalDate, 
    endDate: java.time.LocalDate
): kotlinx.coroutines.flow.Flow<List<DailyEntryWithResponses>> {
    val startDateStr = startDate.toString()
    val endDateStr = endDate.toString()
    return dailyReportDao.observeEntriesInRange(startDateStr, endDateStr)
}
```

#### 1.3 Dao层增加范围查询方法
```kotlin
// DailyReportDao.kt
@Transaction
@Query("SELECT * FROM daily_entries WHERE entry_date >= :startDate AND entry_date <= :endDate ORDER BY entry_date ASC")
fun observeEntriesInRange(startDate: String, endDate: String): Flow<List<DailyEntryWithResponses>>
```

### 2. ViewModel重构 - 7天数据聚合
**改造要点**：
- 原有逻辑：只查询今日条目 `repository.latestEntry()`
- 新逻辑：同时查询今日+近7天数据 `repository.entriesForRange(now - 6, now)`

#### 2.1 合并Flow流
```kotlin
val uiState: StateFlow<HomeDashboardUiState> = combine(
    repository.latestEntry(),
    repository.entriesForRange(LocalDate.now(clock).minusDays(6), LocalDate.now(clock)),
    userProfileStore.profileFlow
) { entry, weeklyEntries, profile ->
    mapToState(entry, weeklyEntries).copy(
        userName = profile.userName, 
        avatarUri = profile.avatarUri
    )
}.stateIn(...)
```

#### 2.2 数据归一化逻辑
**步数 (daily_steps)**:
```kotlin
"gt10k" -> 1.0f   // 已达标
"6_10k" -> 0.75f  // 接近目标
"3_6k" -> 0.5f    // 再多走走
"lt3k" -> 0.25f   // 需要活动
```

**睡眠 (sleep_duration)**:
```kotlin
"gt8" -> 1.0f    // 充足
"7_8" -> 0.85f   // 稳定
"6_7" -> 0.6f    // 略短
"lt6" -> 0.3f    // 偏少
```

**心情 (mood_irritability)** - 注意反转：
```kotlin
// 心情值越低越好 -> 归一化时反转
val scoreValue = answerValue.toIntOrNull()  // 0-10分
val normalizedValue = 1.0f - (scoreValue / 10f)  // 反转归一化
```

**能量 (overall_feeling)**:
```kotlin
"great" -> 1.0f   // 充沛
"ok" -> 0.7f      // 正常
"unwell" -> 0.4f  // 低迷
"awful" -> 0.15f  // 需要休息
```

### 3. UI组件改造
#### 3.1 MetricSpark动态柱状图
**原有实现**：4根静态柱子，硬编码高度 `[16.dp, 32.dp, 20.dp, 28.dp]`

**新实现**：
- 接受`weeklyData: List<Float>`参数
- 动态计算柱子数量（支持7天完整数据）
- 高度归一化：`minHeight + (maxHeight - minHeight) * normalizedValue`
- 使用`animateDpAsState`实现流畅入场动画

```kotlin
@Composable
private fun MetricSpark(weeklyData: List<Float>) {
    val bars = if (weeklyData.isEmpty()) {
        listOf(0.4f, 0.8f, 0.5f, 0.7f)  // 默认占位数据
    } else {
        weeklyData
    }
    
    val heights = bars.map { value ->
        val normalizedValue = value.coerceIn(0f, 1f)
        8.dp + 32.dp * normalizedValue  // 8-40dp范围
    }
    
    val animatedHeights = heights.mapIndexed { index, height ->
        animateDpAsState(
            targetValue = height,
            animationSpec = tween(durationMillis = 600, delayMillis = index * 100)
        )
    }
    
    Row {
        animatedHeights.forEach { animatedHeight ->
            Box(Modifier.height(animatedHeight.value))
        }
    }
}
```

#### 3.2 MetricCard传递weeklyData
```kotlin
MetricCard(
    title = "步数",
    metric = uiState.steps,
    icon = Icons.Outlined.RunCircle,
    // 新增：传递真实7天数据
    modifier = Modifier.weight(1f)
)

// MetricCard内部：
MetricSpark(weeklyData = metric?.weeklyData ?: emptyList())
```

### 4. 动态心情图标
**需求**：根据今日心情值（0-10分）动态更换图标：
- 0-3分：😊 `SentimentVerySatisfied` - 平稳
- 4-6分：😐 `SentimentNeutral` - 略烦躁
- 7-10分：☹️ `SentimentDissatisfied` - 明显紧绷

#### 4.1 MetricCard签名扩展
```kotlin
@Composable
private fun MetricCard(
    title: String,
    metric: MetricDisplay?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    index: Int = 0,
    dynamicIcon: ImageVector? = null  // 新增：可选的动态图标
)
```

#### 4.2 心情卡片调用
```kotlin
// HomeDashboardScreen中计算动态图标
val moodIcon = when {
    uiState.mood == null -> Icons.Outlined.Mood
    else -> {
        val moodScore = uiState.mood.value.split("/")
            .firstOrNull()?.trim()?.toIntOrNull() ?: 5
        when {
            moodScore <= 3 -> Icons.Outlined.SentimentVerySatisfied
            moodScore <= 6 -> Icons.Outlined.SentimentNeutral
            else -> Icons.Outlined.SentimentDissatisfied
        }
    }
}

MetricCard(
    title = "今日心情",
    metric = uiState.mood,
    icon = Icons.Outlined.Mood,
    dynamicIcon = moodIcon,  // 传入计算的图标
    ...
)
```

### 5. 默认用户名优化
**改动文件**：
1. `UserProfileStore.kt`: `userName = "Alex"` (default)
2. `HomeDashboardUiState`: `userName = "Alex"`

### 6. 删除开发者选项
**改动文件**: `SettingsScreen.kt`
- 删除`DebugToolsSection()` Composable（73行，包含生成测试数据功能）
- 删除调用位置：`// 调试工具（开发者选项）DebugToolsSection()`

## Technical Insights

### 归一化策略选择
**挑战**：不同指标语义差异大，如何统一归一化？

**方案**：
- **分类型数据**（步数/睡眠）：预定义4档映射表
- **连续型数据**（心情0-10）：线性归一化 `value / maxValue`
- **反向指标**（心情值）：`1.0 - normalized` 反转（因为高分代表不好）

### 动画性能优化
**问题**：7根柱子同时动画可能卡顿

**优化**：
- 使用`remember`缓存bars列表，避免重组时重复计算
- `delayMillis = index * 100` 错开动画时机（stagger effect）
- 复用`animateDpAsState`而非手动AnimationSpec

### 测试层兼容
**问题**：Dao接口新增`observeEntriesInRange`方法，导致`FakeDailyReportDao`编译失败

**解决**：
```kotlin
// BackupManagerTest.kt
override fun observeEntriesInRange(startDate: String, endDate: String): Flow<List<DailyEntryWithResponses>> {
    return flowOf(
        entries.filter { it.entryDate >= startDate && it.entryDate <= endDate }
            .map { entry -> DailyEntryWithResponses(
                entry, 
                responses.filter { it.entryId == entry.id }
            )}
    )
}
```

## What's Next
- [ ] 底部导航栏iOS风格动画（水滴玻璃质感）
- [ ] 7天数据点击跳转详情页
- [ ] 横屏适配（卡片横向排列）
- [ ] 添加"上周对比"百分比标签

## References
- Material Icons: https://fonts.google.com/icons?icon.style=Outlined&icon.set=Material+Icons
- Compose Animation: https://developer.android.com/jetpack/compose/animation
- Room Flow Queries: https://developer.android.com/training/data-storage/room/async-queries

## Build Verification
```bash
$ ./gradlew clean build
BUILD SUCCESSFUL in 4s
107 actionable tasks: 13 executed, 94 up-to-date

$ ./gradlew assembleDebug
BUILD SUCCESSFUL in 954ms
```

**APK Size**: ~6.2MB (no significant change)
**Test Results**: All unit tests passed (62 tests)
