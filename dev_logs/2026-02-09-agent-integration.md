# Agent 智能体集成 - Android 端改造

**日期**: 2026-02-09  
**版本**: v1.1-alpha  
**目标**: 将 HElDairy Android 客户端与 hel-agent 后端对接，实现智能体驱动的健康建议、追问、周报、药品解析等功能

---

## 一、架构概述

```
┌─────────────────────────────────────────────────────────────────────┐
│                        HElDairy Android                             │
│                                                                     │
│  ┌─────────────┐    ┌──────────────────┐    ┌────────────────────┐ │
│  │ UI Layer    │--->│ ViewModel        │--->│ Coordinator        │ │
│  │ (Compose)   │    │ (State Holder)   │    │ (Agent-First Path) │ │
│  └─────────────┘    └──────────────────┘    └────────────────────┘ │
│                                                     │               │
│                                            ┌────────▼──────────┐   │
│                                            │  AgentClient      │   │
│                                            │  (High-level API) │   │
│                                            └────────┬──────────┘   │
│                                                     │               │
│  ┌─────────────────────────────────────────────────▼──────────────┐│
│  │                     Network Layer                              ││
│  │  AgentApi (Retrofit) + AgentAuthInterceptor (OkHttp)           ││
│  └────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼ HTTPS
┌─────────────────────────────────────────────────────────────────────┐
│                        hel-agent Backend                            │
│           FastAPI + LangGraph + LiteLLM + PostgreSQL                │
└─────────────────────────────────────────────────────────────────────┘
```

**集成策略**: Agent-First with DeepSeek Fallback
- Coordinator 首先尝试 AgentClient
- 捕获 `AgentNotReadyException` → 回退到原有 DeepSeekClient
- 功能开关：`agentEnabled` (DataStore)

---

## 二、新增文件清单

### 1. Network / Agent 模块

| 文件 | 说明 |
|------|------|
| `core/network/agent/AgentModels.kt` | 请求/响应数据类：Auth, Chat, Health, Medication, Sync |
| `core/network/agent/AgentApi.kt` | Retrofit 接口：register, login, refreshToken, me, chat, dailyAdvice, followUp, weeklyInsight, medicationParseNlp, medicationInfoSummary, syncUpload, syncStatus, healthCheck |
| `core/network/agent/AgentAuthInterceptor.kt` | OkHttp Interceptor：自动注入 Bearer Token, 401 → refresh token → retry |
| `core/network/agent/AgentClient.kt` | 高阶封装：ensureReady(), fetchAdvice, fetchFollowUpQuestions, fetchWeeklyInsight, medicationParseNlp 等，抛出 `AgentNotReadyException` |

### 2. Preferences / DataStore

| 文件 | 说明 |
|------|------|
| `core/preferences/AgentPreferencesStore.kt` | Agent 配置持久化：serverUrl, accessToken, refreshToken, syncEnabled, lastSyncTimestamp, loggedInUsername, agentEnabled |

### 3. Data Sync

| 文件 | 说明 |
|------|------|
| `core/data/DataSyncManager.kt` | 增量同步管理：收集 HealthEntry, Medication, MedCourse → SyncUploadRequest → 上传 |
| `core/worker/DataSyncWorker.kt` | WorkManager CoroutineWorker：周期性(6h)数据同步 |

---

## 三、修改文件清单

### 1. Database / DAO

| 文件 | 修改内容 |
|------|----------|
| `core/database/MedicationDao.kt` | +`loadAllMedsSuspend()`, +`loadAllCoursesSuspend()` (供 DataSyncManager 使用) |

### 2. DI / AppContainer

| 文件 | 修改内容 |
|------|----------|
| `core/di/AppContainer.kt` | 接口: +`agentPreferencesStore`, +`agentClient`, +`dataSyncManager`<br>实现: +`AgentPreferencesStore`, +lazy `agentOkHttpClient`, +`rebuildAgentClient(serverUrl)`, 修改 Coordinator 构造传入 `agentClient` |

### 3. AI Coordinators (Agent-First 路径)

| 文件 | 修改内容 |
|------|----------|
| `core/data/DailyAdviceCoordinator.kt` | +`agentClient?: AgentClient` 构造参数<br>先尝试 `agentClient.fetchAdvice()` → 回退 DeepSeek |
| `core/data/AiFollowUpCoordinator.kt` | +`agentClient?: AgentClient` 构造参数<br>先尝试 `agentClient.fetchFollowUpQuestions()` → 回退 DeepSeek |
| `core/data/WeeklyInsightCoordinator.kt` | +`agentClient?: AgentClient` 构造参数<br>先尝试 `agentClient.fetchWeeklyInsight()` → 回退 DeepSeek |

### 4. Feature / Settings

| 文件 | 修改内容 |
|------|----------|
| `feature/settings/SettingsViewModel.kt` | 完全重写：<br>+Agent 状态字段 (serverUrl, username, password, isLoading, lastSyncTimestamp...)<br>+Agent 操作 (login, register, logout, saveUrl, syncNow, triggerSync)<br>+Factory 注入 AgentPreferencesStore & AppContainerImpl |
| `feature/settings/ui/SettingsScreen.kt` | +Agent 回调参数传递<br>+`AgentSection` Composable (服务器地址、登录/注册表单、同步开关、立即同步按钮) |

### 5. Util / Worker

| 文件 | 修改内容 |
|------|----------|
| `core/util/Constants.kt` | +`AGENT_DEFAULT_BASE_URL = "http://10.0.2.2:8000/"` |
| `core/worker/WorkScheduler.kt` | +`scheduleDataSync(context)`, +`cancelDataSync(context)` |

---

## 四、关键实现细节

### 4.1 Token 刷新机制 (AgentAuthInterceptor)

```kotlin
override fun intercept(chain: Chain): Response {
    val request = chain.request()
    
    // Public paths 不加 token
    if (isPublicPath(request.url.encodedPath)) {
        return chain.proceed(request)
    }
    
    // 注入 access token
    val accessToken = runBlocking { agentPreferencesStore.getAccessToken() }
    val authedRequest = request.newBuilder()
        .header("Authorization", "Bearer $accessToken")
        .build()
    
    val response = chain.proceed(authedRequest)
    
    // 401 → 尝试 refresh
    if (response.code == 401) {
        response.close()
        val newToken = runBlocking { refreshTokenIfPossible() }
        if (newToken != null) {
            // 重试
            return chain.proceed(
                request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            )
        } else {
            // refresh 失败 → 登出
            runBlocking { agentPreferencesStore.clearLogin() }
        }
    }
    return response
}
```

### 4.2 Agent-First Fallback 模式 (DailyAdviceCoordinator)

```kotlin
// 1) 尝试 Agent
if (agentClient != null) {
    try {
        val agentAdvice = agentClient.fetchAdvice(todayAnswers, summary7d)
        val normalized = normalizeAgentAdvice(agentAdvice)
        adviceRepository.saveAdvice(normalized)
        return@withContext Result.success(normalized)
    } catch (_: AgentNotReadyException) {
        // Agent 未就绪 → 继续到 DeepSeek
    } catch (e: Exception) {
        Log.w(TAG, "Agent 调用失败，回退 DeepSeek", e)
    }
}

// 2) DeepSeek 原有逻辑
val response = deepSeekClient.chat(...)
```

### 4.3 增量数据同步 (DataSyncManager)

```kotlin
suspend fun syncNow(): Result<SyncResponse> {
    val settings = agentPreferencesStore.settingsFlow.first()
    if (!settings.isReady) return Result.failure(...)
    
    val lastSync = settings.lastSyncTimestamp
    
    // 只收集 createdAt > lastSync 的数据
    val entries = collectEntries(lastSync)
    val medications = collectMedications(lastSync)
    val courses = collectCourses(lastSync)
    
    val request = SyncUploadRequest(
        healthEntries = entries,
        medications = medications,
        medicationCourses = courses
    )
    
    val response = agentClient.syncUpload(request)
    agentPreferencesStore.updateLastSyncTimestamp(System.currentTimeMillis())
    return Result.success(response)
}
```

---

## 五、UI 新增截图说明

### AgentSection (SettingsScreen)

```
┌──────────────────────────────────────────┐
│ 🤖 智能体 Agent                          │
├──────────────────────────────────────────┤
│ [✓] 启用智能体                           │
├──────────────────────────────────────────┤
│ 服务器地址                               │
│ ┌───────────────────────────┐ ┌──────┐  │
│ │ http://your-server:8000   │ │ 保存 │  │
│ └───────────────────────────┘ └──────┘  │
├──────────────────────────────────────────┤
│ ┌────────────────────────────────────┐   │
│ │ ✅ 已登录: user@example.com        │   │
│ │                                    │   │
│ │ [✓] 自动同步健康数据               │   │
│ │                                    │   │
│ │ 上次同步: 02-09 14:30  [立即同步]  │   │
│ │                                    │   │
│ │ [ 退出登录 ]                       │   │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

---

## 六、测试要点

1. **Agent 连接测试**
   - [ ] 服务器地址保存 & 重建 Retrofit 实例
   - [ ] 注册 → 登录 → token 持久化
   - [ ] 401 → refresh token → 重试
   - [ ] refresh 过期 → 自动登出

2. **功能回退测试**
   - [ ] agentEnabled = false → 纯 DeepSeek 模式
   - [ ] Agent 服务不可达 → 回退 DeepSeek
   - [ ] Agent 返回错误 → 回退 DeepSeek

3. **数据同步测试**
   - [ ] 增量同步：只上传 lastSync 之后的数据
   - [ ] 立即同步按钮
   - [ ] 6h 周期同步 (WorkManager)

4. **UI 测试**
   - [ ] AgentSection 显示/隐藏
   - [ ] 登录/注册表单
   - [ ] 同步状态显示

---

## 七、后续工作

- [ ] 药品 NLP 解析集成 (MedicationViewModel)
- [ ] 药品信息摘要集成 (MedicationDetailScreen)
- [ ] 多语言支持 (strings.xml)
- [ ] Agent 连接状态指示器 (AppBar)
- [ ] 错误提示 Toast / Snackbar

---

## 八、相关文件索引

- hel-agent 后端: `../hel-agent/`
- API 文档: `../hel-agent/docs/api-reference.md`
- 部署指南: `../hel-agent/docs/deployment.md`
