# Firebase Crashlytics 集成指南

## 概述

Firebase Crashlytics 是 Google 提供的免费崩溃报告工具，可捕获生产环境中的异常并提供详细的堆栈跟踪和设备信息。

## 为什么暂未集成？

考虑到以下因素，我们将 Crashlytics 集成作为**可选步骤**：
1. 需要 Google Services 依赖（增加 ~2MB APK 大小）
2. 需要 Firebase 项目配置和 `google-services.json` 文件
3. 当前项目无其他 Firebase 依赖，保持轻量化

## 集成步骤（如需启用）

### 1. 创建 Firebase 项目
1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 创建新项目或使用现有项目
3. 添加 Android 应用（包名：`com.heldairy`）
4. 下载 `google-services.json` 并放置到 `app/` 目录

### 2. 修改项目配置

**build.gradle.kts (项目根目录)**:
```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.16" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
    // 新增：Google Services 和 Crashlytics
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}
```

**app/build.gradle.kts**:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    // 新增：应用插件
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

dependencies {
    // ... 现有依赖 ...
    
    // Firebase BOM（统一版本管理）
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx") // 可选但推荐
}
```

### 3. 初始化 Crashlytics

**HElDairyApplication.kt**:
```kotlin
import com.google.firebase.crashlytics.FirebaseCrashlytics

class HElDairyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 启用 Crashlytics 数据收集
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        // 设置用户标识符（可选，用于追踪用户）
        val userId = appContainer.userProfileStore.profileFlow.first().userName
        FirebaseCrashlytics.getInstance().setUserId(userId.hashCode().toString())
        
        // ... 其他初始化代码 ...
    }
}
```

### 4. 手动记录非致命错误

在关键 API 调用失败时记录：

```kotlin
try {
    val advice = deepSeekClient.fetchAdvice(...)
} catch (e: Exception) {
    // 记录到 Crashlytics
    FirebaseCrashlytics.getInstance().recordException(e)
    // 继续处理...
}
```

### 5. 添加自定义键值对

帮助诊断问题：

```kotlin
FirebaseCrashlytics.getInstance().apply {
    setCustomKey("ai_enabled", aiSettings.aiEnabled)
    setCustomKey("last_report_date", entry.entryDate)
    setCustomKey("network_type", networkMonitor.isCurrentlyConnected())
}
```

### 6. 测试崩溃报告

强制触发崩溃（仅用于测试）：

```kotlin
// 在 Debug 屏幕添加测试按钮
Button(onClick = {
    throw RuntimeException("Test Crash")
}) {
    Text("触发测试崩溃")
}
```

## 数据隐私考虑

### 默认收集的信息
- 设备型号、Android 版本
- 应用版本、崩溃堆栈跟踪
- 崩溃发生前的用户操作日志（最后10个事件）

### 不会收集的信息
- 用户健康数据（日报内容、症状记录）
- API Key（不在日志中打印）
- 个人身份信息（姓名、邮箱等）

### 关闭 Crashlytics（如用户要求）

在设置页面添加开关：

```kotlin
// Settings Screen
Switch(
    checked = crashlyticsEnabled,
    onCheckedChange = { enabled ->
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(enabled)
        // 存储用户偏好
    }
)
```

## 替代方案

如果不使用 Firebase，可考虑：
1. **ACRA** - 开源崩溃报告库（支持自托管后端）
2. **Sentry** - 商业方案（免费层 5000 events/月）
3. **本地日志收集** - 写入 `getFilesDir()` + 导出功能

## 估算影响

### APK 大小增加
- Firebase Crashlytics SDK: ~150KB
- Firebase 核心库: ~1.8MB
- Google Services 依赖: ~200KB
- **总计**: ~2.15MB

### 性能影响
- 启动延迟: <5ms（异步初始化）
- 内存占用: ~3MB（后台监控）
- CPU 开销: 可忽略（仅在崩溃时活跃）

### 构建时间增加
- 首次: +10秒（下载依赖）
- 后续: +2秒（Google Services 插件处理）

## 推荐时机

建议在以下场景启用：
- ✅ 应用发布 Beta 测试版本
- ✅ 公开发布 Play Store 后
- ✅ 收到多个用户崩溃反馈但无法复现
- ❌ 当前开发阶段（本地调试已足够）

## 当前状态

**未集成** - 项目保持轻量化，依赖 Android Studio Logcat 和用户反馈诊断问题。

如需启用，按照上述步骤操作即可，代码架构已预留扩展空间。
