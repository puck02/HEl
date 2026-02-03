## 阶段2 完成总结 - 建议追踪系统

### 概览
- **目标**：为 AI 建议系统添加用户反馈追踪能力，使 AI 能够学习哪些建议有效。
- **完成时间**：2026-02-03
- **构建状态**：✅ 107 tasks successful

### 实现内容

#### 1. 数据库层 (Room v6 → v7)

**新增 Entity：AdviceTrackingEntity**
```kotlin
@Entity(
    tableName = "advice_tracking",
    foreignKeys = [...]
)
data class AdviceTrackingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val adviceId: Long?,
    val adviceText: String,           // 单条建议内容
    val category: String,             // sleep/exercise/diet
    val generatedDate: String,
    val userFeedback: UserFeedback?,  // helpful/not_helpful/executed/dismissed
    val feedbackAt: Long?,
    val effectivenessScore: Int?,     // 1-5 分，执行后的效果评分
    val notes: String?
)
```

**UserFeedback 枚举**
```kotlin
enum class UserFeedback {
    @SerialName("helpful") HELPFUL,
    @SerialName("not_helpful") NOT_HELPFUL,
    @SerialName("executed") EXECUTED,
    @SerialName("dismissed") DISMISSED
}
```

**新增 DAO 方法（10个）**
- `insertAdviceTracking()` / `insertAdviceTrackings()`
- `updateAdviceTracking()`
- `getTrackingsForEntry()` / `getTrackingsInDateRange()`
- `getTrackingsByFeedback()` / `getExecutedWithScore()`
- `getTrackingsByCategory()`
- `deleteTrackingsForEntry()` / `clearAllTrackings()`

#### 2. Repository 层

**AdviceTrackingRepository** (新建)
```kotlin
class AdviceTrackingRepository(private val dao: DailyReportDao, ...)

// 核心方法：
suspend fun saveAdviceAsTrackable(
    entryId: Long,
    adviceId: Long?,
    payload: AdvicePayload,
    generatedDate: String
): List<Long>
// → 将 AdvicePayload 分解为多条 tracking 记录（observations/actions/tomorrowFocus 各自独立）

suspend fun markAsHelpful(trackingId: Long)
suspend fun markAsNotHelpful(trackingId: Long)
suspend fun markAsExecuted(trackingId: Long, effectivenessScore: Int?)
suspend fun markAsDismissed(trackingId: Long)

suspend fun generateEffectivenessSummary(
    endDate: String,
    days: Int = 30,
    onlyScored: Boolean = false
): String
// → 生成反馈统计摘要："过去30天您标记为'有帮助'的建议共12条，执行后平均效果3.5分..."
```

#### 3. 集成层

**DailyAdviceCoordinator 改动**
- 构造函数新增 `trackingRepository: AdviceTrackingRepository` 参数
- `generateAdvice()` 完成后调用 `trackingRepository.saveAdviceAsTrackable()`，将建议拆分保存
- `buildEnhancedPrompt()` 预留 `effectivenessSummary` 参数（待阶段3启用）

**AppContainer 改动**
- 新增 `adviceTrackingRepository` 实例化
- 注入到 `adviceCoordinator`

**DailyReportViewModel 改动**
- 构造函数新增 `trackingRepository` 参数
- 新增三个公开方法：
  - `markAdviceHelpful(trackingId: Long)`
  - `markAdviceNotHelpful(trackingId: Long)`
  - `markAdviceExecuted(trackingId: Long, effectivenessScore: Int?)`

#### 4. 测试层修复
- `BackupManagerTest` 中的 `FakeDailyReportDao` 添加了 11 个 tracking 方法的 stub 实现
- 确保所有单元测试通过

### 设计决策

#### 数据结构设计
**为什么选择拆分式存储？**
- `DailyAdviceEntity` 保存完整的 `AdvicePayload` JSON
- `AdviceTrackingEntity` 存储拆解后的每一条独立建议（如"增加30分钟睡眠"）
- **优势**：用户可以单独评价每条建议，AI 可以学习到细粒度反馈（而不是"整批建议有用/无用"）

#### 反馈流程设计
```
[生成建议]
    ↓
[saveAdviceAsTrackable] 
    → 将 observations/actions/tomorrowFocus 拆分为独立 tracking 记录
    ↓
[用户交互]
    → 标记"有帮助" / "无帮助" / "已执行" + 效果评分
    ↓
[generateEffectivenessSummary]
    → 统计过去30天的反馈数据
    ↓
[buildEnhancedPrompt]（阶段3启用）
    → 将反馈摘要传给 AI："您过去标记为有帮助的建议多为'早睡30分钟'类，请继续此方向"
```

### 未实现部分（留待阶段3）

#### UI 层反馈界面
- **原因**：当前 `AdviceUiState` 仅包含 `AdvicePayload`，未暴露 tracking IDs
- **阶段3计划**：
  - 扩展 `AdviceUiState` 包含 `List<AdviceTrackingEntity>`
  - 为每条建议项添加 👍 / 👎 / ✅ 按钮
  - 创建效果评分对话框（1-5星评分）

#### 反馈数据回流到 AI Prompt
- **原因**：`effectivenessSummary` 参数已预留但尚未在 `buildEnhancedPrompt` 中实际使用
- **阶段3计划**：
  - 在生成建议前调用 `generateEffectivenessSummary()`
  - 将反馈摘要加入 system prompt：
    ```
    用户反馈历史：
    - 过去30天标记"有帮助"的建议共15条，平均执行效果4.2分
    - "增加睡眠时长"类建议被标记有帮助7次，效果4.5分
    - "运动频率提升"类建议被标记无帮助3次，效果2.1分
    
    请根据上述反馈调整今日建议策略。
    ```

### 验证检查清单

- [x] Room 数据库版本升级到 v7
- [x] AdviceTrackingEntity 成功创建并包含所有必需字段
- [x] DAO 方法签名与接口完全匹配（包括 `limit` 等默认参数）
- [x] AdviceTrackingRepository 完整实现（5个核心方法）
- [x] DailyAdviceCoordinator 集成 tracking 保存
- [x] AppContainer 依赖注入配置正确
- [x] DailyReportViewModel 暴露反馈 API
- [x] 单元测试 FakeDailyReportDao 覆盖所有新增方法
- [x] `./gradlew build` 成功（107 tasks）

### 下一步：阶段3 - 本地规则引擎 + AI分层

#### 目标
1. **本地规则引擎**：处理 70% 简单场景（sleep<6h → "早睡30分钟"）
2. **AI 分层调用**：
   - 本地规则先行（快速响应）
   - 复杂场景才调用 DeepSeek API（周期性波动、多指标关联）
3. **反馈闭环**：
   - `effectivenessSummary` 正式启用
   - AI 根据历史反馈调整建议策略
4. **UI 完善**：
   - 每条建议独立反馈按钮
   - 效果评分对话框

#### 预计工作量
- LocalAdvisorEngine.kt 创建（~150 行）
- DailyAdviceCoordinator 重构分层逻辑（~50 行改动）
- AdviceUiState 扩展 + UI 组件更新（~100 行）
- 测试验证

---

**代码变更统计**
- 新增文件：3 个（AdviceTrackingEntity, AdviceTrackingRepository, EnhancedSummaryModels）
- 修改文件：6 个（DailyReportDao, DailyReportDatabase, DailyAdviceCoordinator, AppContainer, DailyReportViewModel, BackupManagerTest）
- 新增代码：~600 行
- Database 版本：6 → 7
- Build 任务：107 tasks

**性能影响**
- 每次生成建议后额外执行 1 次批量插入（observations/actions/tomorrowFocus 共约 8-10 条记录）
- 查询效率：indexed by `entry_id`, `generated_date`, `category`
- 预计对用户无感知延迟

**安全考虑**
- `adviceText` 字段包含健康相关建议文本，本地存储未加密
- 生产环境需启用 Room `enableMultiInstanceInvalidation = false` 确保单进程访问

**迁移策略**
- Room `fallbackToDestructiveMigration()` 启用
- 测试用户从 v6 升级时，旧数据将**清空重建**（因未提供 Migration）
- 正式发布前需添加 Migration(6, 7) 保留历史建议数据
