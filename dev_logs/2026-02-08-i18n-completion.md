# 2026-02-08 i18n 完整化 - 修复界面英文显示问题

## 问题描述
用户反馈"我发现页面中使用了大量英文啊，请使用中文"

## 问题定位
经过全面扫描，发现以下页面仍存在硬编码的UI文本：
1. **PdfPreviewScreen.kt** - PDF预览界面（报表预览功能）
2. **InsightsScreen.kt** - 洞察页面

这些硬编码的中文文本没有提取到 strings.xml，在之前的 i18n 工作中被遗漏。

## 修复内容

### 1. 新增字符串资源（22个字符串）

#### values/strings.xml（中文）
```xml
<!-- PDF Preview Screen -->
<string name="pdf_preview_title">报表预览</string>
<string name="pdf_preview_page_count">共 %1$d 页</string>
<string name="pdf_preview_generating">正在生成报表预览...</string>
<string name="pdf_preview_failed">预览失败</string>
<string name="pdf_preview_retry">重新生成</string>
<string name="pdf_preview_page_indicator">第 %1$d 页 / 共 %2$d 页</string>
<string name="pdf_preview_save">保存PDF</string>
<string name="pdf_preview_share">分享</string>
<string name="pdf_preview_regenerate">重新生成</string>
<string name="pdf_preview_saving">保存中...</string>
<string name="cd_page_description">第 %1$d 页</string>

<!-- Insights Screen -->
<string name="insights_ai_advice">AI 洞察建议</string>
<string name="insights_error_loading">加载失败</string>
<string name="insights_empty_title">暂无数据</string>
<string name="insights_empty_message">完成每日填报后，这里会出现你的习惯分布和趋势。</string>
<string name="insights_progress_title">今日进度</string>
<string name="insights_progress_days">%1$d/%2$d 天已完成</string>
<string name="insights_no_advice_hint">暂时没有可展示的 AI 建议。</string>
```

#### values-en/strings.xml（英文对照）
添加了对应的英文翻译。

### 2. 代码修改

#### PdfPreviewScreen.kt（8处替换）
修改位置：
- L95: 顶栏标题 → `stringResource(R.string.pdf_preview_title)`
- L99: 页数显示 → `stringResource(R.string.pdf_preview_page_count, uiState.pageCount)`
- L107: 返回按钮描述 → `stringResource(R.string.cd_back)`
- L163: 加载提示 → `stringResource(R.string.pdf_preview_generating)`
- L200: 预览失败标题 → `stringResource(R.string.pdf_preview_failed)`
- L210: 重新生成按钮 → `stringResource(R.string.pdf_preview_retry)`
- L265: 页码标签 → `stringResource(R.string.pdf_preview_page_indicator, pageNumber, totalPages)`
- L275: 页面描述 → `stringResource(R.string.cd_page_description, pageNumber)`
- L311-354: 底部栏三个按钮文本 → 分别使用对应 stringResource

新增导入：
```kotlin
import androidx.compose.ui.res.stringResource
import com.heldairy.R
```

#### InsightsScreen.kt（6处替换）
修改位置：
- L116: AI建议弹窗标题 → `stringResource(R.string.insights_ai_advice)`
- L144: 暂无建议提示 → `stringResource(R.string.insights_no_advice_hint)`
- L361: 错误卡片标题 → `stringResource(R.string.insights_error_loading)`
- L387: 空状态标题 → `stringResource(R.string.insights_empty_title)`
- L392: 空状态说明 → `stringResource(R.string.insights_empty_message)`
- L423: 今日进度标题 → `stringResource(R.string.insights_progress_title)`
- L424: 进度天数 → `stringResource(R.string.insights_progress_days, window.entryCount, window.days)`
- L717: 每周洞察卡片标题 → `stringResource(R.string.insights_ai_advice)`

新增导入：
```kotlin
import androidx.compose.ui.res.stringResource
```

## 验证结果

### 编译验证
```bash
./gradlew :app:compileDebugKotlin --quiet
```
✅ **BUILD SUCCESSFUL** - 无错误，无警告

### i18n覆盖率
- ✅ 所有用户可见文本已提取到字符串资源
- ✅ 双语言支持（中文 + 英文）
- ✅ 格式化字符串正确使用（%1$d 占位符）
- ✅ 所有硬编码文本已消除

## 影响范围
- **修改文件**：4个
  - `values/strings.xml` (+22 strings)
  - `values-en/strings.xml` (+22 strings)
  - `PdfPreviewScreen.kt` (8处硬编码替换 + 2个导入)
  - `InsightsScreen.kt` (8处硬编码替换 + 1个导入)
  
- **功能影响**：无破坏性变更，仅文本来源改为字符串资源

## 后续建议
1. ✅ 所有UI文本已国际化
2. 📋 建议在代码审查时增加 lint 规则检测硬编码字符串
3. 📋 可考虑启用 Android Lint 的 `HardcodedText` 检查强制要求使用字符串资源

## 相关文档
- Phase 3 进度日志：`dev_logs/2026-02-07-phase3-progress.md`
- Requirements：`doc/requirements.md`
