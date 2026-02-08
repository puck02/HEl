# 特性模块化方案设计

**日期**: 2026-02-07  
**状态**: 📋 设计方案（待实施）

## 目标

将现有单体 `app` 模块拆分为多个独立的特性模块（Feature Modules），实现：
- ⚡ **并行编译**：不同特性模块可同时编译，加速构建时间
- 🔒 **依赖隔离**：防止特性间不当依赖，强制清晰架构
- 🧪 **独立测试**：特性模块可单独测试，不依赖完整应用
- 📦 **代码重用**：核心模块可被多个特性复用

---

## 目标模块结构

```
heldairy/
├── app/                          # 应用壳（导航、主题、Application）
│   ├── MainActivity.kt
│   ├── HElDairyApplication.kt
│   └── navigation/
│       └── MainNavHost.kt
│
├── feature/
│   ├── home/                     # 首页特性
│   │   ├── src/main/java/.../feature/home/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── HomeViewModel.kt
│   │   │   └── HomeRepository.kt
│   │   └── build.gradle.kts
│   │
│   ├── report/                   # 日报提交特性
│   │   ├── src/main/java/.../feature/report/
│   │   │   ├── DailyReportScreen.kt
│   │   │   ├── DailyReportViewModel.kt
│   │   │   └── DailyReportRepository.kt
│   │   └── build.gradle.kts
│   │
│   ├── insights/                 # 健康洞察特性
│   │   ├── src/main/java/.../feature/insights/
│   │   │   ├── InsightsScreen.kt
│   │   │   └── InsightsViewModel.kt
│   │   └── build.gradle.kts
│   │
│   ├── medication/               # 用药管理特性
│   │   ├── src/main/java/.../feature/medication/
│   │   │   ├── MedicationListScreen.kt
│   │   │   ├── MedicationDetailScreen.kt
│   │   │   └── MedicationViewModel.kt
│   │   └── build.gradle.kts
│   │
│   └── settings/                 # 设置特性
│       ├── src/main/java/.../feature/settings/
│       │   ├── SettingsScreen.kt
│       │   └── SettingsViewModel.kt
│       └── build.gradle.kts
│
├── core/
│   ├── database/                 # Room 数据库核心
│   │   ├── src/main/java/.../core/database/
│   │   │   ├── DailyReportDatabase.kt
│   │   │   ├── entities/
│   │   │   └── dao/
│   │   └── build.gradle.kts
│   │
│   ├── network/                  # 网络层核心
│   │   ├── src/main/java/.../core/network/
│   │   │   ├── DeepSeekApi.kt
│   │   │   ├── DeepSeekClient.kt
│   │   │   ├── NetworkMonitor.kt
│   │   │   └── RetryInterceptor.kt
│   │   └── build.gradle.kts
│   │
│   ├── data/                     # 数据层核心（Repositories + Coordinators）
│   │   ├── src/main/java/.../core/data/
│   │   │   ├── DailyReportRepository.kt
│   │   │   ├── DailyAdviceCoordinator.kt
│   │   │   └── InsightRepository.kt
│   │   └── build.gradle.kts
│   │
│   ├── preferences/              # 数据存储核心（DataStore + SecurePreferences）
│   │   ├── src/main/java/.../core/preferences/
│   │   │   ├── AiPreferencesStore.kt
│   │   │   └── SecurePreferencesStore.kt
│   │   └── build.gradle.kts
│   │
│   ├── ui/                       # UI 组件核心（主题、通用组件）
│   │   ├── src/main/java/.../core/ui/
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Theme.kt
│   │   │   │   └── Typography.kt
│   │   │   └── components/
│   │   │       ├── TimePicker.kt
│   │   │       └── LoadingIndicator.kt
│   │   └── build.gradle.kts
│   │
│   └── worker/                   # 后台任务核心（WorkManager Workers）
│       ├── src/main/java/.../core/worker/
│       │   ├── WeeklyInsightWorker.kt
│       │   └── WorkScheduler.kt
│       └── build.gradle.kts
│
└── settings.gradle.kts           # 模块注册
```

---

## 模块依赖关系

```
┌─────────────────────────────────────────────────┐
│                      :app                       │
│  (Navigation, Theme Setup, Application)         │
└──┬────┬─────┬──────┬────┬──────────────────────┘
   │    │     │      │    │
   ▼    ▼     ▼      ▼    ▼
┌──────────────────────────────────────────┐
│  :feature:home                           │
│  :feature:report                         │
│  :feature:insights                       │
│  :feature:medication                     │
│  :feature:settings                       │
└──┬────┬─────┬──────┬────┬───────────────┘
   │    │     │      │    │
   │    │     │      │    └──────┐
   │    │     │      └───────┐   │
   │    │     └──────────┐   │   │
   │    └────────────┐   │   │   │
   ▼                 ▼   ▼   ▼   ▼
┌─────────────────────────────────────────┐
│  :core:database                         │
│  :core:network                          │
│  :core:data                             │
│  :core:preferences                      │
│  :core:ui                               │
│  :core:worker                           │
└─────────────────────────────────────────┘
```

**依赖规则**:
- ✅ Feature 模块可依赖 Core 模块
- ✅ App 模块可依赖所有模块
- ❌ Feature 模块**不能**相互依赖
- ❌ Core 模块**不能**依赖 Feature 模块

---

## 迁移步骤（分阶段）

### 阶段 1：创建 Core 模块（基础设施）

**预计时间**: 4-6 小时

**任务清单**:
1. 创建 `:core:database` 模块
   - 移动 `core/database/` 包到新模块
   - 配置 build.gradle.kts（Room + KSP）
   - 验证编译通过

2. 创建 `:core:network` 模块
   - 移动 `core/network/` 包到新模块
   - 配置依赖（Retrofit + OkHttp）

3. 创建 `:core:data` 模块
   - 移动 Repository 和 Coordinator 类
   - 依赖 `:core:database` 和 `:core:network`

4. 创建 `:core:preferences` 模块
   - 移动 DataStore 和 SecurePreferences 类

5. 创建 `:core:ui` 模块
   - 移动主题文件（Theme.kt, Color.kt）
   - 移动通用组件（TimePicker, LoadingIndicator）

6. 创建 `:core:worker` 模块
   - 移动 WorkManager 相关类

**验证**: `./gradlew :core:database:build` 成功

---

### 阶段 2：创建 Feature 模块（核心功能）

**预计时间**: 6-8 小时

**任务清单**:
1. 创建 `:feature:home` 模块
   - 移动 `feature/home/` 包到新模块
   - 依赖 `:core:ui`, `:core:data`
   - 配置 Compose

2. 创建 `:feature:report` 模块
   - 移动日报相关 UI 和 ViewModel
   - 依赖 `:core:ui`, `:core:data`

3. 创建 `:feature:insights` 模块
   - 移动洞察相关代码

4. 创建 `:feature:medication` 模块
   - 移动用药管理代码

5. 创建 `:feature:settings` 模块
   - 移动设置相关代码

**验证**: 每个 feature 模块可单独编译

---

### 阶段 3：重构 App 模块（导航壳）

**预计时间**: 3-4 小时

**任务清单**:
1. 精简 `:app` 模块
   - 只保留 MainActivity, HElDairyApplication
   - 只保留导航配置（MainNavHost）

2. 更新 AppContainer
   - 迁移到 `:core:data` 或使用 Hilt（可选）

3. 更新依赖
   - `:app` 依赖所有 `:feature:*` 模块
   - 验证编译

**验证**: `./gradlew :app:assembleDebug` 成功

---

### 阶段 4：优化与清理（可选）

**预计时间**: 2-3 小时

**任务清单**:
1. 移除冗余依赖
   - 清理各模块 build.gradle.kts
   - 确保依赖最小化

2. 添加 Convention Plugins（推荐）
   - 创建 `build-logic/` 目录
   - 统一配置 Compose、Kotlin、KSP 版本

3. 优化编译配置
   - 启用 Gradle 配置缓存
   - 启用并行编译

---

## 模块配置模板

### :core:database/build.gradle.kts

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.heldairy.core.database"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

### :feature:home/build.gradle.kts

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.heldairy.feature.home"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core 依赖
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
}
```

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HElDairy"

include(":app")

// Core modules
include(":core:database")
include(":core:network")
include(":core:data")
include(":core:preferences")
include(":core:ui")
include(":core:worker")

// Feature modules
include(":feature:home")
include(":feature:report")
include(":feature:insights")
include(":feature:medication")
include(":feature:settings")
```

---

## 预期收益

### 编译速度

**当前（单体模块）**:
- 全量编译：~30 秒
- 增量编译：~8 秒
- 清理重编译：~30 秒

**模块化后**:
- 全量编译：~25 秒（并行编译）
- 增量编译：~3 秒（只编译改动模块）
- 清理重编译：~20 秒（缓存复用）

**改进幅度**: 增量编译提速 ~60%

### 代码组织

| 指标 | 单体模块 | 模块化后 |
|-----|---------|---------|
| 模块数 | 1 | 12 |
| 平均文件数/模块 | ~150 | ~15 |
| 依赖复杂度 | 高（隐式） | 低（显式） |
| 测试隔离性 | 差 | 优 |

### 团队协作

- ✅ **并行开发**: 不同成员可独立开发不同 feature
- ✅ **代码审查**: PR 范围更小，审查更容易
- ✅ **责任划分**: 每个 feature 有明确归属

---

## 风险与注意事项

### ⚠️ 潜在风险

1. **初期投入大**: 首次迁移需要 15-20 小时
2. **依赖管理复杂**: 版本同步需要工具支持
3. **构建配置重复**: 每个模块都有 build.gradle.kts

### ✅ 缓解措施

1. **分阶段迁移**: 先迁移 core，再迁移 feature，逐步验证
2. **使用 Version Catalog**: 统一管理依赖版本（Gradle 7.0+）
3. **Convention Plugins**: 抽取公共配置到插件

### 🚫 不适合场景

- 项目规模 < 10,000 行代码
- 团队规模 < 3 人
- 无 CI/CD 基础设施

---

## 替代方案

### 方案 A：按层拆分（简化版）

只拆分为 3 个模块：
```
:app        # UI 层
:domain     # 业务逻辑层
:data       # 数据层
```

**优点**: 迁移成本低（3-5 小时）  
**缺点**: 并行编译收益小

### 方案 B：保持单体 + 包结构优化

不拆分模块，优化包结构：
```
com.heldairy/
├── feature/
│   ├── home/
│   ├── report/
│   └── medication/
└── core/
    ├── database/
    ├── network/
    └── ui/
```

**优点**: 零迁移成本  
**缺点**: 无法强制依赖隔离

---

## 推荐决策

### 当前项目适合模块化吗？

**✅ 建议实施**，因为：
1. 代码规模已达 15,000+ 行（适中）
2. 功能边界清晰（5 个独立特性）
3. 有明确性能瓶颈（增量编译慢）

### 建议实施时机

1. **现在（Phase 3）**: 作为架构演进的一部分
2. **代码冻结期**: 功能稳定后集中重构
3. **团队扩展前**: 为协作开发铺路

### 建议实施范围

**最小方案**（8-10 小时）:
- 只拆分 `:core:database` + `:core:network` + `:core:ui`
- Feature 保留在 `:app` 中

**完整方案**（15-20 小时）:
- 按本文档完整拆分 12 个模块

---

## 参考资料

- [Android 模块化指南](https://developer.android.com/topic/modularization)
- [Now in Android 示例项目](https://github.com/android/nowinandroid)
- [Gradle 多模块配置](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Convention Plugins 最佳实践](https://docs.gradle.org/current/samples/sample_convention_plugins.html)
