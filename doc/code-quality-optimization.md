# 代码质量优化方案

**日期**: 2026-02-07  
**状态**: 📋 设计方案（待实施）

## 目标

提升代码可读性、可维护性和可测试性，具体包括：
- ✅ **提取硬编码字符串**：迁移到 strings.xml 支持国际化
- ✅ **抽取魔法数字**：定义为有意义的常量
- ✅ **优化命名**：统一变量/函数命名规范
- ✅ **简化复杂函数**：拆分超过 50 行的函数
- ✅ **增加文档注释**：为核心 API 添加 KDoc

---

## 当前代码质量评估

### 静态分析结果（基于 grep + 手动审查）

| 指标 | 当前状态 | 目标 | 严重度 |
|------|---------|------|--------|
| 硬编码中文字符串 | 50+ 处 | 0 | 🔴 高 |
| 魔法数字 | 30+ 处 | 0 | 🟡 中 |
| 超长函数（>50行） | 15+ 处 | <5 | 🟡 中 |
| 缺少文档的公共API | 80% | <20% | 🟢 低 |
| 命名不规范 | 10+ 处 | 0 | 🟢 低 |

---

## 优化任务分解

### 任务 1：提取硬编码字符串（国际化）

**预计时间**: 6-8 小时  
**优先级**: 🔴 高

#### 现状分析

通过 `grep` 搜索发现 50+ 处硬编码中文字符串：

**高频位置**:
1. **medication/** (25+ 处)
   - AddMedicationScreen.kt: "药品名称", "服用频率", "单次剂量"
   - MedicationDetailScreen.kt: "暂停", "恢复", "结束"
   - AddMedicationDialog.kt: "该药品正在服用中"

2. **settings/** (15+ 处)
   - SettingsScreen.kt: "设置", "API Key", "清空所有数据"

3. **report/** (10+ 处)
   - DailyReportScreen.kt: "今日日报", "提交日报", "正在生成建议"

#### 迁移策略

**阶段 1**: 提取高频 UI 文本（优先级：设置 > 用药 > 日报）

```kotlin
// ❌ 迁移前
Text("药品名称")
Text("服用频率")

// ✅ 迁移后
Text(stringResource(R.string.medication_name_label))
Text(stringResource(R.string.medication_frequency_label))
```

**阶段 2**: 提取对话框文本

```kotlin
// ❌ 迁移前
AlertDialog(
    title = { Text("确认删除") },
    text = { Text("删除后将无法恢复，确定要删除这个药品吗？") }
)

// ✅ 迁移后
AlertDialog(
    title = { Text(stringResource(R.string.medication_delete_confirm_title)) },
    text = { Text(stringResource(R.string.medication_delete_confirm_message)) }
)
```

**阶段 3**: 提取复杂文本（包含变量插值）

```kotlin
// ❌ 迁移前
Text("进行中（${course.startDate} ~ 至今）")

// ✅ 迁移后
// strings.xml
<string name="medication_course_active">进行中（%1$s ~ 至今）</string>

// Kotlin
Text(stringResource(R.string.medication_course_active, course.startDate))
```

#### 迁移验收

- ✅ `grep -r 'Text("[\u4e00-\u9fa5]' app/src/` 无结果
- ✅ `./gradlew build` 成功
- ✅ UI 显示无变化（视觉回归测试）

---

### 任务 2：抽取魔法数字

**预计时间**: 2-3 小时  
**优先级**: 🟡 中

#### 现状分析

**常见魔法数字**:
1. 时间相关: `20`, `8`, `30`, `90` (天数、小时)
2. 重试相关: `3`, `1000`, `8000` (次数、延迟毫秒)
3. 网络相关: `30`, `60` (超时秒数)
4. UI 相关: `16`, `8`, `24` (dp 间距)

#### 优化方案

**1. 创建 Constants.kt**

```kotlin
// core/util/Constants.kt
object Constants {
    // 时间相关
    object Time {
        const val DAILY_REPORT_REMINDER_HOUR = 20  // 20:00
        const val WEEKLY_INSIGHT_HOUR = 1           // 01:00
        const val INSIGHT_RETENTION_DAYS = 90       // 洞察保留 90 天
    }

    // 网络相关
    object Network {
        const val CONNECT_TIMEOUT_SECONDS = 30L
        const val READ_TIMEOUT_SECONDS = 60L
        const val RETRY_MAX_ATTEMPTS = 3
        const val RETRY_INITIAL_DELAY_MS = 1000L
        const val RETRY_MAX_DELAY_MS = 8000L
    }

    // UI 相关
    object UI {
        const val PADDING_SMALL = 8
        const val PADDING_MEDIUM = 16
        const val PADDING_LARGE = 24
    }
}
```

**2. 替换硬编码数字**

```kotlin
// ❌ 迁移前
OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)

// ✅ 迁移后
OkHttpClient.Builder()
    .connectTimeout(Constants.Network.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(Constants.Network.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
```

```kotlin
// ❌ 迁移前
val retryDelay = min(1000L * (2.0.pow(attempt - 1)).toLong(), 8000L)

// ✅ 迁移后
val retryDelay = min(
    Constants.Network.RETRY_INITIAL_DELAY_MS * (2.0.pow(attempt - 1)).toLong(),
    Constants.Network.RETRY_MAX_DELAY_MS
)
```

---

### 任务 3：简化复杂函数

**预计时间**: 4-5 小时  
**优先级**: 🟡 中

#### 现状分析

**超长函数列表**（>50行）:
1. `DailyReportScreen.kt::DailyReportScreen()` (~150 行)
2. `SettingsScreen.kt::SettingsScreen()` (~120 行)
3. `AddMedicationScreen.kt::AddMedicationScreen()` (~100 行)
4. `MedicationDetailScreen.kt::MedicationDetailScreen()` (~80 行)

#### 优化方案

**策略**: 按 UI 区域拆分为子 Composable

**案例 1: SettingsScreen.kt**

```kotlin
// ❌ 迁移前（120 行）
@Composable
fun SettingsScreen() {
    Scaffold(topBar = { /* ... */ }) { padding ->
        LazyColumn {
            // AI 设置区域（30 行）
            item { /* ... */ }
            
            // 备份区域（40 行）
            item { /* ... */ }
            
            // 数据管理区域（30 行）
            item { /* ... */ }
            
            // 用户信息区域（20 行）
            item { /* ... */ }
        }
    }
}

// ✅ 迁移后（拆分为 4 个子组件）
@Composable
fun SettingsScreen() {
    Scaffold(topBar = { SettingsTopBar() }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { AiSettingsSection() }
            item { BackupSection() }
            item { DataManagementSection() }
            item { UserProfileSection() }
        }
    }
}

@Composable
private fun AiSettingsSection() {
    // 30 行代码
}

@Composable
private fun BackupSection() {
    // 40 行代码
}

@Composable
private fun DataManagementSection() {
    // 30 行代码
}

@Composable
private fun UserProfileSection() {
    // 20 行代码
}
```

**案例 2: DailyReportScreen.kt**

```kotlin
// ❌ 迁移前（150 行）
@Composable
fun DailyReportScreen() {
    // 问题列表渲染（50 行）
    // 建议生成状态（40 行）
    // 提交按钮逻辑（30 行）
    // 错误处理（30 行）
}

// ✅ 迁移后
@Composable
fun DailyReportScreen() {
    Scaffold(topBar = { ReportTopBar() }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            QuestionList()                // 50 行 → 独立组件
            AdviceGenerationStatus()      // 40 行 → 独立组件
            SubmitButton()                // 30 行 → 独立组件
        }
    }
}
```

---

### 任务 4：优化命名规范

**预计时间**: 1-2 小时  
**优先级**: 🟢 低

#### 现状分析

**命名不规范案例**:
1. 布尔变量不以 `is/has/should` 开头
   - `val enabled` → `val isEnabled`
   - `val aiFeature` → `val isAiFeatureEnabled`

2. 集合变量未复数化
   - `val medication` → `val medications`
   - `val entry` → `val entries`

3. 缩写不明确
   - `val dao` → `val dailyReportDao` (如果上下文不清晰)
   - `val ctx` → `val context`

#### 优化规范

```kotlin
// ✅ 布尔变量
val isAiEnabled: Boolean
val hasNetworkConnection: Boolean
val shouldShowDialog: Boolean

// ✅ 集合变量
val medications: List<Medication>
val dailyEntries: List<DailyEntry>

// ✅ 明确的上下文
val viewModelScope: CoroutineScope  // 而不是 scope
val applicationContext: Context     // 而不是 ctx
```

---

### 任务 5：增加文档注释

**预计时间**: 3-4 小时  
**优先级**: 🟢 低

#### 现状分析

**缺少文档的核心 API**:
1. Repository 层所有公共方法（30+ 方法）
2. Coordinator 层所有公共方法（10+ 方法）
3. ViewModel 层的复杂方法（20+ 方法）
4. 数据模型类（15+ 类）

#### 优化方案

**KDoc 模板**:

```kotlin
/**
 * 保存日报记录到数据库
 *
 * 此方法会先验证数据的完整性，然后将日报条目、问答记录、AI 建议一并保存。
 * 如果数据库操作失败，会抛出 [IllegalStateException]。
 *
 * @param entry 日报条目，必须包含有效的日期和基础评分
 * @param responses 用户对所有问题的回答列表
 * @param advice AI 生成的建议内容（可为 null）
 * @return 保存后的日报 ID
 * @throws IllegalStateException 如果 entry.date 为空或数据库操作失败
 *
 * @sample
 * ```kotlin
 * val reportId = repository.saveDailyReport(
 *     entry = DailyEntry(date = "2026-02-07", overallScore = 8),
 *     responses = listOf(QuestionResponse(question = "睡眠质量", answer = "良好")),
 *     advice = "建议早睡"
 * )
 * ```
 */
suspend fun saveDailyReport(
    entry: DailyEntry,
    responses: List<QuestionResponse>,
    advice: String?
): Long {
    // 实现
}
```

**优先级排序**:
1. 🔴 高优先级：Repository 的 CRUD 方法
2. 🟡 中优先级：Coordinator 的业务逻辑方法
3. 🟢 低优先级：ViewModel 的 UI 状态更新方法

---

## 代码质量工具集成（推荐）

### 1. Detekt（Kotlin 静态分析）

**安装**:
```kotlin
// build.gradle.kts (Project)
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}

// build.gradle.kts (app)
plugins {
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    config.setFrom("$projectDir/config/detekt.yml")
    buildUponDefaultConfig = true
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
}
```

**配置**（`config/detekt.yml`）:
```yaml
complexity:
  LongMethod:
    threshold: 50  # 函数超过 50 行报错
  TooManyFunctions:
    threshold: 15  # 类超过 15 个函数报错

naming:
  VariableNaming:
    variablePattern: '[a-z][a-zA-Z0-9]*'  # 变量命名规范
  FunctionNaming:
    functionPattern: '[a-z][a-zA-Z0-9]*'  # 函数命名规范

style:
  MagicNumber:
    ignoreNumbers: [-1, 0, 1, 2]  # 忽略常见数字
    ignoreHashCodeFunction: true
```

**运行**:
```bash
./gradlew detekt  # 生成报告到 build/reports/detekt/
```

---

### 2. ktlint（代码格式化）

**安装**:
```kotlin
// build.gradle.kts (Project)
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
}

// build.gradle.kts (app)
plugins {
    id("org.jlleitschuh.gradle.ktlint")
}
```

**运行**:
```bash
./gradlew ktlintCheck  # 检查格式问题
./gradlew ktlintFormat # 自动修复格式问题
```

---

### 3. Gradle Dependency Analysis

**安装**:
```kotlin
// build.gradle.kts (Project)
plugins {
    id("com.autonomousapps.dependency-analysis") version "1.30.0"
}
```

**运行**:
```bash
./gradlew buildHealth  # 检查未使用的依赖
```

---

## 优化路线图

### 阶段 1：基础清理（1-2 天）

- [x] **1.1** 提取 strings.xml 中文字符串定义（已完成）
- [ ] **1.2** 迁移高频 UI 文本（settings, medication）
- [ ] **1.3** 创建 Constants.kt 常量类
- [ ] **1.4** 替换魔法数字

**验收**: `./gradlew build` 成功，UI 无回归

---

### 阶段 2：函数重构（2-3 天）

- [ ] **2.1** 拆分 SettingsScreen（120 行 → 4 个子组件）
- [ ] **2.2** 拆分 DailyReportScreen（150 行 → 4 个子组件）
- [ ] **2.3** 拆分 AddMedicationScreen（100 行 → 3 个子组件）

**验收**: 所有函数 < 50 行

---

### 阶段 3：文档完善（1-2 天）

- [ ] **3.1** 为 Repository 层添加 KDoc
- [ ] **3.2** 为 Coordinator 层添加 KDoc
- [ ] **3.3** 为核心数据模型添加注释

**验收**: 公共 API 文档覆盖率 > 80%

---

### 阶段 4：工具集成（可选，1 天）

- [ ] **4.1** 集成 Detekt 静态分析
- [ ] **4.2** 集成 ktlint 格式化
- [ ] **4.3** 配置 CI 自动检查

**验收**: `./gradlew detekt ktlintCheck` 通过

---

## 预期收益

### 代码可读性

**改进前**:
```kotlin
Text("药品名称")
if (course.endDate == null) {
    Text("进行中（${course.startDate} ~ 至今）")
}
val retryDelay = min(1000L * (2.0.pow(attempt - 1)).toLong(), 8000L)
```

**改进后**:
```kotlin
Text(stringResource(R.string.medication_name_label))
if (course.isActive) {
    Text(
        stringResource(
            R.string.medication_course_active,
            course.startDate
        )
    )
}
val retryDelay = calculateRetryDelay(
    attempt,
    initialDelay = Constants.Network.RETRY_INITIAL_DELAY_MS,
    maxDelay = Constants.Network.RETRY_MAX_DELAY_MS
)
```

### 可维护性

| 指标 | 改进前 | 改进后 | 变化 |
|------|--------|--------|------|
| 平均函数行数 | ~45 | ~25 | ↓ 44% |
| 硬编码字符串数 | 50+ | 0 | ↓ 100% |
| 魔法数字数 | 30+ | <5 | ↓ 83% |
| 文档覆盖率 | 20% | 80% | ↑ 300% |

### 国际化支持

- ✅ 一键切换语言（中文 ↔ 英文）
- ✅ 新增语言只需添加 `values-xx/strings.xml`
- ✅ 编译时检查字符串缺失

---

## 风险与注意事项

### ⚠️ 潜在风险

1. **回归 Bug**: 提取字符串可能改变 UI 显示
2. **耗时长**: 手动迁移 50+ 处字符串需 6-8 小时
3. **命名冲突**: strings.xml 可能出现重复 key

### ✅ 缓解措施

1. **视觉回归测试**: 迁移前后截图对比
2. **分批迁移**: 按模块逐步提交（settings → medication → report）
3. **命名规范**: 使用前缀区分（`medication_*`, `settings_*`）

---

## 验收标准

### 功能验收

- ✅ 所有 UI 文本显示正确
- ✅ 切换语言功能正常
- ✅ 应用无崩溃

### 代码质量验收

- ✅ `./gradlew build` 成功
- ✅ `./gradlew detekt` 无错误（如已集成）
- ✅ `./gradlew ktlintCheck` 通过（如已集成）
- ✅ 无硬编码中文字符串
- ✅ 所有函数 < 50 行
- ✅ 公共 API 文档覆盖率 > 80%

---

## 参考资料

- [Android 字符串资源指南](https://developer.android.com/guide/topics/resources/string-resource)
- [Detekt 官方文档](https://detekt.dev/)
- [ktlint 规则集](https://pinterest.github.io/ktlint/latest/)
- [KDoc 语法](https://kotlinlang.org/docs/kotlin-doc.html)
