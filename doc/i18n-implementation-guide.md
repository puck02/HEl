# 国际化（i18n）实施指南

**日期**: 2026-02-07  
**状态**: ✅ 基础设施就绪，待代码迁移

## 完成的工作

### 1. 字符串资源文件创建

已创建双语资源文件：
- ✅ `app/src/main/res/values/strings.xml` - 中文（默认）
- ✅ `app/src/main/res/values-en/strings.xml` - 英文

**资源统计**:
- 总字符串数：120+
- 覆盖范围：底部导航、设置、用药、日报、对话框、错误消息等
- 格式化字符串：支持 `%1$s`, `%d` 等参数化

### 2. 硬编码字符串识别

**扫描结果**:
- 🔍 识别到 **50+ 处**硬编码中文字符串
- 🎯 主要分布在：
  - `feature/medication/ui/` - 用药相关 UI（25+ 处）
  - `feature/settings/ui/` - 设置页面（15+ 处）
  - `feature/report/ui/` - 日报页面（10+ 处）
  - androidTest/ - 测试文件（5处，可忽略）

---

## 迁移策略

### 阶段 A：高频界面优先（推荐先实施）

**优先级 P0** - 用户每日接触的核心界面：
1. ✅ 底部导航栏（4个 Tab）
2. ✅ 设置页面（API Key、备份、数据清理）
3. ⏳ 日报提交流程（Step 0-3）
4. ⏳ 用药管理（添加、编辑、疗程）

**预计工作量**: 4-6 小时

### 阶段 B：对话框与反馈（中期）

**优先级 P1** - 用户交互反馈：
1. ⏳ 确认对话框（删除、清空数据、结束疗程等）
2. ⏳ Toast/Snackbar 消息（成功/失败提示）
3. ⏳ 错误消息（网络、API、验证）

**预计工作量**: 2-3 小时

### 阶段 C：详情与说明（后期）

**优先级 P2** - 低频界面：
1. ⏳ 药品详情页面
2. ⏳ 洞察报告详情
3. ⏳ PDF 医生报告文案
4. ⏳ 调试页面

**预计工作量**: 2-3 小时

---

## 使用指南

### 在 Compose UI 中使用

**之前（硬编码）**:
```kotlin
Text("添加用药")
```

**之后（国际化）**:
```kotlin
Text(stringResource(R.string.medication_add))
```

### 在 ViewModel 中使用

需要 Context 或 Application：
```kotlin
class SettingsViewModel(
    private val app: Application
) : AndroidViewModel(app) {
    
    fun showMessage() {
        val message = app.getString(R.string.settings_api_key_updated)
        _events.emit(SettingsEvent.Snackbar(message))
    }
}
```

### 格式化字符串

**定义**:
```xml
<string name="medication_course_active">进行中（%1$s ~ 至今）</string>
```

**使用**:
```kotlin
val formatted = stringResource(
    R.string.medication_course_active, 
    course.startDate.toString()
)
```

### 复数形式（可选扩展）

创建 `plurals.xml`：
```xml
<plurals name="days_ago">
    <item quantity="one">%d 天前</item>
    <item quantity="other">%d 天前</item>
</plurals>
```

使用：
```kotlin
val text = resources.getQuantityString(R.plurals.days_ago, days, days)
```

---

## 示例迁移

### 示例 1：设置页面 - API Key 输入框

**迁移前**:
```kotlin
OutlinedTextField(
    value = state.apiKeyInput,
    onValueChange = onApiKeyChanged,
    placeholder = { Text("••••••••••••••••••••••••••••••••") },
    // ...
)
```

**迁移后**:
```kotlin
OutlinedTextField(
    value = state.apiKeyInput,
    onValueChange = onApiKeyChanged,
    placeholder = { Text(stringResource(R.string.settings_api_key_placeholder)) },
    // ...
)
```

### 示例 2：用药对话框 - 删除确认

**迁移前**:
```kotlin
AlertDialog(
    title = { Text("确认删除") },
    text = { Text("删除后将无法恢复，确定要删除这个药品吗？") },
    confirmButton = {
        Button(onClick = { /* ... */ }) {
            Text("删除")
        }
    },
    dismissButton = {
        TextButton(onClick = { /* ... */ }) {
            Text("取消")
        }
    }
)
```

**迁移后**:
```kotlin
AlertDialog(
    title = { Text(stringResource(R.string.medication_delete_confirm_title)) },
    text = { Text(stringResource(R.string.medication_delete_confirm_message)) },
    confirmButton = {
        Button(onClick = { /* ... */ }) {
            Text(stringResource(R.string.action_delete))
        }
    },
    dismissButton = {
        TextButton(onClick = { /* ... */ }) {
            Text(stringResource(R.string.action_cancel))
        }
    }
)
```

### 示例 3：Snackbar 消息

**迁移前**:
```kotlin
viewModelScope.launch {
    _events.emit(SettingsEvent.Snackbar("API Key 已更新"))
}
```

**迁移后**:
```kotlin
viewModelScope.launch {
    val message = app.getString(R.string.settings_api_key_updated)
    _events.emit(SettingsEvent.Snackbar(message))
}
```

---

## 验证清单

### 编译时验证
- ✅ 所有 `R.string.*` 引用存在于 strings.xml
- ✅ 格式化字符串参数数量匹配

### 运行时验证
1. **切换语言测试**:
   - Settings → System → Languages → 添加 English → 选择为首选语言
   - 重启应用，验证所有文本显示英文

2. **格式化字符串测试**:
   - 验证日期、数字格式化正确
   - 验证 `%1$s` 参数正确替换

3. **回退测试**:
   - 如果某个字符串在 `values-en/` 中不存在，应回退到 `values/` 中文版本

---

## 工具与脚本

### 自动化查找硬编码字符串

```bash
# 查找所有 Text("中文") 模式
grep -r 'Text("[\u4e00-\u9fa5]' app/src/main/java/

# 查找所有 label = { Text("中文") } 模式
grep -r 'label = { Text("[\u4e00-\u9fa5]' app/src/main/java/

# 查找所有 placeholder = { Text("中文") } 模式
grep -r 'placeholder = { Text("[\u4e00-\u9fa5]' app/src/main/java/
```

### IDE 快捷操作

**Android Studio 快速提取字符串**:
1. 选中硬编码字符串
2. Alt + Enter (Windows/Linux) 或 Option + Enter (Mac)
3. 选择 "Extract string resource"
4. 输入资源名称（如 `medication_add`）

---

## 未来扩展

### 支持更多语言（可选）

创建新的资源目录：
- `values-zh-rTW/` - 繁体中文（台湾）
- `values-ja/` - 日语
- `values-ko/` - 韩语
- `values-es/` - 西班牙语

### 动态语言切换（应用内）

如果需要在应用内切换语言（不依赖系统设置）：

```kotlin
object LocaleHelper {
    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }
}

// 使用
LocaleHelper.setLocale(context, "en") // 切换到英文
```

### 字符串验证工具

创建单元测试验证字符串资源：

```kotlin
@Test
fun allStringsExistInBothLanguages() {
    val zhStrings = context.resources.getStringArray(R.array.all_string_keys)
    val enContext = context.createConfigurationContext(
        Configuration().apply { setLocale(Locale.ENGLISH) }
    )
    
    zhStrings.forEach { key ->
        val enValue = enContext.getString(key)
        assertNotNull("Missing English translation for $key", enValue)
    }
}
```

---

## 当前状态

- ✅ **基础设施**: strings.xml (中文 + 英文) 已创建
- ✅ **资源定义**: 120+ 常用字符串已定义
- ⏳ **代码迁移**: 0% 完成（待实施）
- ⏳ **测试验证**: 待迁移完成后测试

---

## 推荐实施顺序

1. **Week 1**: 迁移设置页面（高频+关键）
2. **Week 2**: 迁移用药管理（复杂度高）
3. **Week 3**: 迁移日报与洞察（核心流程）
4. **Week 4**: 验证与修正（全流程测试）

---

## 注意事项

### ⚠️ 避免的陷阱

1. **不要在 ViewModel 中直接使用 stringResource()**
   - ❌ `val text = stringResource(R.string.xxx)` （Compose 专用）
   - ✅ `val text = app.getString(R.string.xxx)` （需要 Application）

2. **注意 Context 生命周期**
   - ViewModel 应使用 `ApplicationContext` 而非 Activity Context

3. **格式化字符串顺序**
   - 使用 `%1$s`, `%2$s` 而非 `%s` （支持重排序）

4. **避免字符串拼接**
   - ❌ `"进行中（$date ~ 至今）"`
   - ✅ `getString(R.string.medication_course_active, date)`

### ✅ 最佳实践

1. **资源命名规范**:
   - 功能_组件_含义：`medication_add`, `settings_api_key_label`
   - 动作前缀：`action_save`, `error_network`

2. **保持简洁**:
   - 字符串 ID 不超过 50 字符
   - 避免过度细分（如 `button_save_medication` 可简化为 `action_save`）

3. **注释重要资源**:
   ```xml
   <!-- 用于医生报告中显示疗程状态，%1$s=开始日期 -->
   <string name="medication_course_active">进行中（%1$s ~ 至今）</string>
   ```

---

## 参考资料

- [Android 本地化指南](https://developer.android.com/guide/topics/resources/localization)
- [Compose 字符串资源](https://developer.android.com/jetpack/compose/resources#strings)
- [格式化字符串](https://developer.android.com/guide/topics/resources/string-resource#FormattingAndStyling)
- [语言和区域代码](https://developer.android.com/guide/topics/resources/providing-resources#AlternativeResources)
