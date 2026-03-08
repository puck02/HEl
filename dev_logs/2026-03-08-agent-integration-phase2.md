# 2026-03-08 Agent 对接实施记录（Phase 2）

## 本阶段目标
1. Android 注册流程强制 email 必填（与后端契约一致）
2. 用药接口模型切换到后端结构（Agent 协议）
3. 用药能力接入 Agent-First，保留 DeepSeek fallback
4. 执行编译验证并记录结果

## 已完成改造

### A. 注册 email 必填链路（Settings）
- 修改 `SettingsViewModel`：
  - 新增 `onAgentEmailChanged`
  - `SettingsUiState` 增加 `agentEmailInput`
  - `agentRegister()` 改为校验用户名/邮箱/密码
  - 增加邮箱格式校验（`android.util.Patterns.EMAIL_ADDRESS`）
  - 调用 `client.register(..., email = email, ...)`
- 修改 `SettingsScreen`：
  - `SettingsRoute -> SettingsScreen -> AgentSection` 增加 `onAgentEmailChanged` 参数传递
  - Agent 未登录表单新增邮箱输入框

### B. Agent 网络模型对齐后端 Medication 契约
- 修改 `AgentModels.kt`：
  - `AgentRegisterRequest.email` 从 nullable 改为必填 `String`
  - `AgentMedNlpParseRequest` 增加 `current_meds`、`active_courses_summary`
  - `AgentMedNlpParseResponse` 改为后端结构：`mentioned_meds/actions/questions`
  - `AgentMedInfoSummaryRequest` 改为 `{ text, med_name }`
  - `AgentMedInfoSummaryResponse` 改为 `name_candidates/dosage_summary/cautions_summary/adverse_summary`

### C. AgentClient 逻辑对齐
- 修改 `AgentClient.register(...)`：email 改为必填参数
- 修改 `parseMedicationNlp(...)`：
  - 解析后端 `actions` / `mentioned_meds`
  - 映射为应用已有 `MedicationNlpResult`
  - 支持 `frequencyText|frequency_text`、`doseText|dose_text`、`timeHints|time_hints`
- 修改 `fetchMedInfoSummary(...)`：
  - 组装 `text` 请求后端
  - 把 `dosage/cautions/adverse` 组合为应用展示字符串

### D. 用药业务接入 Agent-First（保留 fallback）
- 修改 `AddMedicationViewModel`：
  - 构造函数新增 `agentClient: AgentClient?`
  - `parseNaturalInput()` 先尝试 Agent 解析，失败后回落到 DeepSeek（原路径）
- 修改 `MedicationDetailViewModel`：
  - 构造函数新增 `agentClient: AgentClient?`
  - `generateInfoSummary()` 先尝试 Agent，失败后回落 DeepSeek
- 修改工厂调用：
  - `MainActivity` 创建 `AddMedicationViewModel` 时注入 `app.appContainer.agentClient`
  - `MedicationDetailViewModel.factory` 注入 `agentClient`

## 变更文件清单
- `app/src/main/java/com/heldairy/feature/settings/SettingsViewModel.kt`
- `app/src/main/java/com/heldairy/feature/settings/ui/SettingsScreen.kt`
- `app/src/main/java/com/heldairy/core/network/agent/AgentModels.kt`
- `app/src/main/java/com/heldairy/core/network/agent/AgentClient.kt`
- `app/src/main/java/com/heldairy/feature/medication/AddMedicationViewModel.kt`
- `app/src/main/java/com/heldairy/feature/medication/MedicationDetailViewModel.kt`
- `app/src/main/java/com/heldairy/MainActivity.kt`

## 验证结果
- IDE 文件级错误检查：上述改动文件均 `No errors found`
- Gradle 编译：执行 `./gradlew :app:compileDebugKotlin --no-daemon` 时失败
  - 失败点：`Task :app:mergeDebugResources`
  - 报错：`strings.xml: Premature end of file`

## 阻塞分析
- 本地检查显示 `strings.xml` 文件大小正常（14KB+），但从终端 `head` 读取时偶发无输出。
- 结合当前项目位于 OneDrive 路径，判断为云盘同步/文件按需下载导致的间歇性读取异常，而非本次代码改动引入的编译错误。

## 下一步建议（Phase 2.1）
1. 先把 `HEl` 项目临时复制到本地非云盘目录（如 `/tmp/HEl-local`）执行一次完整编译验证
2. 继续推进 Sync v2（push/pull/cursor/conflict/tombstone）协议落地
3. 增补 Android 对应契约测试：
   - 注册邮箱必填校验
   - medication 新响应映射测试
   - Agent-First + fallback 行为测试
