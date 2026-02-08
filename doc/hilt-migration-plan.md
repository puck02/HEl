# Hilt 依赖注入迁移方案

**日期**: 2026-02-07  
**状态**: 📋 设计方案（待实施）

## 目标

将当前手动 DI（AppContainer）迁移到 Hilt（Dagger 2 封装），实现：
- ✅ **编译时验证**：依赖关系在编译期检查，运行时不会因注入失败崩溃
- ✅ **减少样板代码**：不再手动传递依赖，由 Hilt 自动生成注入代码
- ✅ **生命周期管理**：自动管理单例/Activity/ViewModel 作用域
- ✅ **测试便利性**：支持依赖替换（Fake/Mock），无需修改生产代码

---

## 当前架构分析

### 现有 AppContainer 设计

```kotlin
// core/di/AppContainer.kt
class AppContainer(private val context: Context) {
    // Database
    val database: DailyReportDatabase by lazy {
        Room.databaseBuilder(/* ... */)
    }

    // Network
    val deepSeekApi: DeepSeekApi by lazy {
        Retrofit.Builder()/* ... */.build()
    }

    // Network Monitor
    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    // Preferences
    val aiPreferencesStore: AiPreferencesStore by lazy {
        AiPreferencesStore(context, securePreferencesStore)
    }

    // Repositories
    val dailyReportRepository: DailyReportRepository by lazy {
        DailyReportRepository(database.dailyReportDao(), database.questionResponseDao())
    }

    // Coordinators
    val dailyAdviceCoordinator: DailyAdviceCoordinator by lazy {
        DailyAdviceCoordinator(
            deepSeekClient, dailyReportRepository, 
            aiPreferencesStore, networkMonitor
        )
    }
}

// HElDairyApplication.kt
class HElDairyApplication : Application() {
    lateinit var appContainer: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

// ViewModel 中使用
class DailyReportViewModel(
    private val repository: DailyReportRepository,
    private val coordinator: DailyAdviceCoordinator
) : ViewModel()

// Activity 中传递
val appContainer = (application as HElDairyApplication).appContainer
val viewModel = DailyReportViewModel(
    appContainer.dailyReportRepository,
    appContainer.dailyAdviceCoordinator
)
```

**问题**:
1. ❌ 手动传递依赖繁琐（ViewModel 需要 2+ 参数）
2. ❌ 生命周期管理需手动控制（何时创建/销毁）
3. ❌ 测试时需替换整个 AppContainer
4. ❌ 无编译时验证（运行时才发现依赖缺失）

---

## Hilt 架构设计

### 目标依赖图

```
┌─────────────────────────────────────────────────┐
│         @HiltAndroidApp                         │
│         HElDairyApplication                     │
└──────────────────┬──────────────────────────────┘
                   │
    ┌──────────────┴──────────────┐
    │                             │
    ▼                             ▼
┌─────────────────────┐    ┌─────────────────────┐
│ @InstallIn          │    │ @InstallIn          │
│ SingletonComponent  │    │ ViewModelComponent  │
│                     │    │                     │
│ - Database          │    │ - ViewModels        │
│ - NetworkMonitor    │    │                     │
│ - DeepSeekApi       │    └─────────────────────┘
│ - Repositories      │
│ - Coordinators      │
└─────────────────────┘
```

### Hilt 组件作用域

| 组件 | 生命周期 | 适用对象 |
|------|---------|---------|
| `SingletonComponent` | Application 级别 | Database, NetworkMonitor, Repository |
| `ViewModelComponent` | ViewModel 级别 | ViewModel 依赖 |
| `ActivityComponent` | Activity 级别 | Activity 依赖（导航、主题） |

---

## 迁移步骤（分阶段）

### 阶段 1：基础配置（1-2 小时）

#### 1.1 添加 Hilt 依赖

```kotlin
// build.gradle.kts (Project)
plugins {
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

// build.gradle.kts (app)
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    
    // ViewModel 支持
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
}
```

#### 1.2 启用 Hilt

```kotlin
// HElDairyApplication.kt
@HiltAndroidApp  // ✅ 添加注解
class HElDairyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WorkScheduler.initialize(applicationContext)
    }
}

// MainActivity.kt
@AndroidEntryPoint  // ✅ 添加注解
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { /* ... */ }
    }
}
```

---

### 阶段 2：迁移数据层（2-3 小时）

#### 2.1 提供 Database

```kotlin
// core/di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): DailyReportDatabase {
        return Room.databaseBuilder(
            context,
            DailyReportDatabase::class.java,
            "daily_report_db"
        )
        .addMigrations(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9
        )
        .build()
    }

    @Provides
    fun provideDailyReportDao(
        database: DailyReportDatabase
    ): DailyReportDao = database.dailyReportDao()

    @Provides
    fun provideQuestionResponseDao(
        database: DailyReportDatabase
    ): QuestionResponseDao = database.questionResponseDao()

    @Provides
    fun provideMedicationDao(
        database: DailyReportDatabase
    ): MedicationDao = database.medicationDao()
}
```

#### 2.2 提供 Preferences

```kotlin
// core/di/PreferencesModule.kt
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideSecurePreferencesStore(
        @ApplicationContext context: Context
    ): SecurePreferencesStore {
        return SecurePreferencesStore(context)
    }

    @Provides
    @Singleton
    fun provideAiPreferencesStore(
        @ApplicationContext context: Context,
        secureStore: SecurePreferencesStore
    ): AiPreferencesStore {
        return AiPreferencesStore(context, secureStore)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesStore(
        @ApplicationContext context: Context
    ): UserPreferencesStore {
        return UserPreferencesStore(context)
    }
}
```

#### 2.3 提供 Network

```kotlin
// core/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor {
        return RetryInterceptor(maxRetries = 3)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        retryInterceptor: RetryInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(retryInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideDeepSeekApi(
        okHttpClient: OkHttpClient
    ): DeepSeekApi {
        return Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DeepSeekApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDeepSeekClient(
        api: DeepSeekApi,
        networkMonitor: NetworkMonitor
    ): DeepSeekClient {
        return DeepSeekClient(api, networkMonitor)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }
}
```

#### 2.4 提供 Repositories

```kotlin
// core/di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDailyReportRepository(
        dailyReportDao: DailyReportDao,
        questionResponseDao: QuestionResponseDao
    ): DailyReportRepository {
        return DailyReportRepository(dailyReportDao, questionResponseDao)
    }

    @Provides
    @Singleton
    fun provideInsightRepository(
        dailyReportDao: DailyReportDao
    ): InsightRepository {
        return InsightRepository(dailyReportDao)
    }

    @Provides
    @Singleton
    fun provideMedicationRepository(
        medicationDao: MedicationDao
    ): MedicationRepository {
        return MedicationRepository(medicationDao)
    }
}
```

#### 2.5 提供 Coordinators

```kotlin
// core/di/CoordinatorModule.kt
@Module
@InstallIn(SingletonComponent::class)
object CoordinatorModule {

    @Provides
    @Singleton
    fun provideDailyAdviceCoordinator(
        deepSeekClient: DeepSeekClient,
        dailyReportRepository: DailyReportRepository,
        aiPreferencesStore: AiPreferencesStore,
        networkMonitor: NetworkMonitor
    ): DailyAdviceCoordinator {
        return DailyAdviceCoordinator(
            deepSeekClient,
            dailyReportRepository,
            aiPreferencesStore,
            networkMonitor
        )
    }

    @Provides
    @Singleton
    fun provideBackupManager(
        dailyReportDao: DailyReportDao,
        questionResponseDao: QuestionResponseDao,
        medicationDao: MedicationDao
    ): BackupManager {
        return BackupManager(dailyReportDao, questionResponseDao, medicationDao)
    }
}
```

---

### 阶段 3：迁移 ViewModels（2-3 小时）

#### 3.1 HomeViewModel

**迁移前**:
```kotlin
class HomeViewModel(
    private val repository: DailyReportRepository,
    private val medicationRepository: MedicationRepository
) : ViewModel()
```

**迁移后**:
```kotlin
@HiltViewModel  // ✅ 添加注解
class HomeViewModel @Inject constructor(  // ✅ @Inject
    private val repository: DailyReportRepository,
    private val medicationRepository: MedicationRepository
) : ViewModel()
```

#### 3.2 DailyReportViewModel

```kotlin
@HiltViewModel
class DailyReportViewModel @Inject constructor(
    private val repository: DailyReportRepository,
    private val coordinator: DailyAdviceCoordinator
) : ViewModel()
```

#### 3.3 SettingsViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aiPreferencesStore: AiPreferencesStore,
    private val userPreferencesStore: UserPreferencesStore,
    private val backupManager: BackupManager
) : ViewModel()
```

#### 3.4 在 Compose 中使用

**迁移前**:
```kotlin
val appContainer = (LocalContext.current.applicationContext as HElDairyApplication).appContainer
val viewModel = remember {
    HomeViewModel(appContainer.dailyReportRepository, appContainer.medicationRepository)
}
```

**迁移后**:
```kotlin
val viewModel: HomeViewModel = hiltViewModel()  // ✅ 自动注入
```

---

### 阶段 4：清理与优化（1-2 小时）

#### 4.1 删除 AppContainer

```diff
- // core/di/AppContainer.kt
- class AppContainer(private val context: Context) { /* ... */ }

// HElDairyApplication.kt
@HiltAndroidApp
class HElDairyApplication : Application() {
-   lateinit var appContainer: AppContainer
    
    override fun onCreate() {
        super.onCreate()
-       appContainer = AppContainer(this)
    }
}
```

#### 4.2 更新所有 Screens

```kotlin
// feature/home/ui/HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()  // ✅ 统一使用 hiltViewModel()
) {
    /* ... */
}

// feature/settings/ui/SettingsScreen.kt
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    /* ... */
}
```

#### 4.3 更新测试

```kotlin
// androidTest/.../HomeScreenTest.kt
@HiltAndroidTest
class HomeScreenTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun testHomeScreen() {
        composeTestRule.setContent {
            HomeScreen()  // ViewModel 自动注入
        }
    }
}
```

---

## 迁移对比

### 代码简化度

**迁移前（AppContainer）**:
```kotlin
// 定义依赖（AppContainer.kt 300+ 行）
class AppContainer(context: Context) {
    val database by lazy { /* 10 行配置 */ }
    val deepSeekApi by lazy { /* 15 行配置 */ }
    val repository by lazy { /* 5 行配置 */ }
    val coordinator by lazy { /* 10 行配置 */ }
}

// 使用依赖（每个 ViewModel）
class HomeViewModel(
    repository: DailyReportRepository,
    medicationRepository: MedicationRepository
) : ViewModel()

val appContainer = (app as HElDairyApplication).appContainer
val viewModel = HomeViewModel(
    appContainer.dailyReportRepository,
    appContainer.medicationRepository
)
```

**迁移后（Hilt）**:
```kotlin
// 定义依赖（Module 文件，按功能拆分 5 个文件共 250 行）
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) = /* ... */
}

// 使用依赖（自动注入）
@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: DailyReportRepository,
    medicationRepository: MedicationRepository
) : ViewModel()

val viewModel: HomeViewModel = hiltViewModel()  // ✨ 一行搞定
```

**代码量对比**:
| 指标 | AppContainer | Hilt |
|------|-------------|------|
| 配置代码行数 | 300+ | 250 |
| ViewModel 实例化 | 5-10 行 | 1 行 |
| 测试替换依赖 | 需修改 AppContainer | @TestInstallIn 无侵入 |

---

### 性能影响

| 指标 | AppContainer | Hilt |
|------|-------------|------|
| 编译时间 | 基准 | +5-10% |
| APK 大小 | 基准 | +50KB（Dagger 运行时） |
| 启动时间 | 基准 | 无差异（编译时生成） |
| 运行时性能 | 基准 | 无差异 |

**⚠️ 注意**: Hilt 增加编译时间是因为代码生成，但运行时无开销。

---

## 测试支持

### 替换依赖（Fake/Mock）

**迁移前（AppContainer）**:
```kotlin
// 需要创建 FakeAppContainer
class FakeAppContainer(context: Context) : AppContainer(context) {
    override val dailyReportRepository = FakeDailyReportRepository()
    override val deepSeekClient = FakeDeepSeekClient()
}

// 测试中手动替换
(app as HElDairyApplication).appContainer = FakeAppContainer(context)
```

**迁移后（Hilt）**:
```kotlin
// 定义 Fake Module
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object FakeRepositoryModule {
    @Provides
    @Singleton
    fun provideFakeDailyReportRepository() = FakeDailyReportRepository()
}

// 测试中自动使用
@HiltAndroidTest
class HomeScreenTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)
    // FakeDailyReportRepository 自动注入，无需手动替换
}
```

---

## 最佳实践

### 1. 按功能拆分 Module

```
core/di/
├── DatabaseModule.kt       # 数据库相关
├── NetworkModule.kt        # 网络相关
├── PreferencesModule.kt    # 存储相关
├── RepositoryModule.kt     # 仓库相关
└── CoordinatorModule.kt    # 协调器相关
```

### 2. 使用作用域限定生命周期

```kotlin
@Provides
@Singleton  // ✅ Application 级别单例
fun provideDatabase(@ApplicationContext context: Context) = /* ... */

@Provides  // ❌ 不加 @Singleton，每次创建新实例
fun provideRandomGenerator() = Random()
```

### 3. 避免在 Module 中持有状态

```kotlin
// ❌ 错误：Module 不应持有可变状态
@Module
object BadModule {
    private var counter = 0  // ❌
    
    @Provides
    fun provideCounter() = counter++
}

// ✅ 正确：状态放在 Repository/ViewModel
@Module
object GoodModule {
    @Provides
    @Singleton
    fun provideCounterRepository() = CounterRepository()
}
```

### 4. 接口注入（可选，推荐）

```kotlin
// 定义接口
interface DailyReportRepository {
    suspend fun saveDailyReport(entry: DailyEntry)
}

// 实现类
class DailyReportRepositoryImpl @Inject constructor(
    private val dao: DailyReportDao
) : DailyReportRepository {
    override suspend fun saveDailyReport(entry: DailyEntry) = dao.insertEntry(entry)
}

// Module 绑定
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDailyReportRepository(
        impl: DailyReportRepositoryImpl
    ): DailyReportRepository
}

// 使用接口注入
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DailyReportRepository  // ✅ 接口，便于测试替换
) : ViewModel()
```

---

## 风险与注意事项

### ⚠️ 潜在风险

1. **学习曲线**: 团队需熟悉 Hilt/Dagger 概念
2. **编译时间增加**: 代码生成需额外时间（+5-10%）
3. **APK 增大**: Dagger 运行时库 +50KB
4. **迁移风险**: 一次性迁移可能引入 bug

### ✅ 缓解措施

1. **分阶段迁移**: 先迁移数据层，验证通过后再迁移 ViewModel
2. **保留 AppContainer**: 迁移期间两套方案并存，逐步切换
3. **增加测试覆盖**: 迁移前为核心逻辑补充单元测试
4. **代码审查**: 关注依赖注入的正确性

### 🚫 不适合场景

- 项目规模 < 5 个 ViewModel
- 团队对 DI 概念陌生且无学习意愿
- 编译时间已成为瓶颈（Hilt 会加剧）

---

## 推荐决策

### 当前项目适合 Hilt 吗？

**✅ 建议实施**，因为：
1. ViewModel 数量已达 8+（HomeViewModel, DailyReportViewModel, SettingsViewModel, MedicationViewModel, etc.）
2. 依赖关系复杂（Coordinator 依赖 4+ 参数）
3. 有测试需求（需要 Fake 替换）

### 建议实施时机

1. **模块化后**: 配合模块化一起做（共用 Hilt 配置）
2. **功能稳定期**: 避免在开发新特性时迁移
3. **测试覆盖率 > 60%**: 确保迁移不破坏功能

### 建议实施范围

**最小方案**（4-5 小时）:
- 只迁移 ViewModel 层
- AppContainer 保留作为 Module 的配置源

**完整方案**（6-8 小时）:
- 全面迁移，删除 AppContainer
- 按功能拆分 5 个 Module
- 补充测试支持（FakeModule）

---

## 迁移验收标准

### 功能验收

- ✅ 所有 ViewModel 成功注入
- ✅ 应用启动无崩溃
- ✅ 日报提交流程正常
- ✅ 用药管理功能正常
- ✅ 设置页面正常

### 代码质量验收

- ✅ `./gradlew build` 成功
- ✅ 无 Hilt 编译警告
- ✅ AppContainer.kt 已删除
- ✅ 所有 ViewModel 使用 `@HiltViewModel`
- ✅ 所有 Screen 使用 `hiltViewModel()`

### 性能验收

- ✅ 编译时间增加 < 15%
- ✅ APK 大小增加 < 100KB
- ✅ 启动时间无明显变化（< 50ms）

---

## 参考资料

- [Hilt 官方文档](https://developer.android.com/training/dependency-injection/hilt-android)
- [Hilt ViewModel 集成](https://developer.android.com/training/dependency-injection/hilt-jetpack)
- [Hilt 测试指南](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Dagger 性能优化](https://dagger.dev/dev-guide/performance)
