# 医生报表 PDF 生成功能重新设计

## 概述
已完成医生报表从丑陋的纯文本PDF到专业医疗报告的重新设计，采用预览-保存工作流。

## 主要改进

### 1. **专业视觉设计**
- **Material 3 配色方案**：使用系统主题色（Primary #1976D2, Success #4CAF50, Warning #FF9800, Error #F44336）
- **8点网格系统**：所有间距基于8px的倍数，保持一致性
- **严重度色彩编码**：
  - 0-3分：绿色（良好）
  - 3-7分：黄色（警告）
  - 7-10分：红色（严重）
- **斑马纹表格**：提高数据可读性
- **趋势指示器**：使用↑↓→符号显示症状趋势
- **数据完整度进度条**：可视化展示数据覆盖率

### 2. **预览-保存工作流**
用户体验流程：
1. 点击"生成报表预览"按钮
2. 系统生成PDF到临时文件
3. 使用PdfRenderer渲染为高清位图（1200px宽度）
4. 全屏预览界面显示所有页面
5. 用户可以：
   - 🔄 **重新生成**：使用相同日期范围重新生成
   - 📤 **分享**：通过系统分享菜单发送
   - 💾 **保存PDF**：选择存储位置保存（Storage Access Framework）
   - ❌ **取消**：关闭预览并删除临时文件

### 3. **技术架构**

#### 核心组件
- **ImprovedDoctorReportPdfGenerator.kt**
  - 使用`PdfDocument` + `Canvas` API生成专业PDF
  - `StaticLayout`处理CJK文本换行和对齐
  - 真实的`Typeface.BOLD`字体粗细（非伪粗体）
  - 完整的8个章节：标题、患者信息、数据完整度、用药依从性、症状趋势、生活方式、AI建议、免责声明

- **PdfPreviewScreen.kt**
  - Compose全屏UI，Material 3设计
  - 使用`PdfRenderer`将PDF页面转换为Bitmap
  - `LazyColumn`滚动查看所有页面
  - 底部操作栏固定显示操作按钮

- **InsightsViewModel.kt** (状态管理)
  - `generatePreview()` - 异步生成PDF到缓存目录
  - `createSavePdfIntent()` - 创建Storage Access Framework意图
  - `createShareIntent()` - 使用FileProvider创建分享意图
  - `completeSave(uri)` - 将临时文件复制到用户选择位置
  - `closePreview()` - 清理临时文件

- **InsightsScreen.kt** (UI集成)
  - `rememberLauncherForActivityResult` - 处理保存结果
  - 条件渲染：预览文件存在时显示`PdfPreviewScreen`，否则显示主界面

#### 数据模型
```kotlin
DoctorReportData(
  patientInfo: PatientInfo(
    reportGeneratedAt: LocalDateTime,
    dataRangeStart: LocalDate,
    dataRangeEnd: LocalDate
  ),
  medicationSummary: MedicationSummaryForReport(
    adherence: MedicationAdherenceSummary(
      onTime: Int,    // 按时服药天数
      missed: Int,    // 漏服天数
      na: Int         // 无需服药天数
    )
  ),
  symptomSummary: SymptomsSummaryForReport(
    metrics: List<SymptomMetricRow>(
      symptomName: String,
      avgValue: Float,
      trendFlag: TrendFlag,  // rising/falling/stable
      trendDescription: String?
    )
  ),
  lifestyleSummary: LifestyleSummaryForReport(
    sleepSummary: String,
    napSummary: String,
    stepsSummary: String
  ),
  aiInsightsSummary: AiInsightsSummaryForReport?(
    weeklyInsights: String
  )
)
```

### 4. **文件存储**
- **临时文件**：`context.cacheDir/doctor_report_yyyyMMddHHmmss.pdf`
- **FileProvider配置**：
  - Authority: `${applicationId}.fileprovider`
  - Paths: `@xml/file_paths` (cache + external_cache)
- **自动清理**：
  - 保存成功后删除临时文件
  - 取消预览时删除临时文件
  - 应用关闭时系统自动清理缓存目录

### 5. **权限与安全**
- **无需存储权限**：使用Storage Access Framework（用户主动选择位置）
- **安全分享**：通过FileProvider授予临时URI读取权限
- **数据隔离**：临时文件存储在应用私有缓存目录

## 使用方法

### 生成报表
1. 进入"洞察"标签页
2. 滚动到"医生报表"卡片
3. 选择日期范围（快速选择或自定义）
4. 点击"生成报表预览"
5. 等待PDF生成（显示加载动画）

### 预览报表
- 全屏预览界面自动打开
- 滚动查看所有页面
- 顶部显示总页数
- 底部操作栏提供按钮

### 保存报表
1. 点击"保存PDF"按钮
2. 系统文件选择器打开
3. 选择保存位置并确认
4. 保存成功后自动关闭预览

### 分享报表
1. 点击"分享"按钮
2. 系统分享菜单打开
3. 选择目标应用（微信、邮件等）
4. 确认发送

## 技术细节

### PDF渲染质量
- **分辨率**：1200px宽度，保持原始纵横比
- **颜色格式**：ARGB_8888（真彩色+透明度）
- **缩放**：Bitmap适配屏幕宽度，保持清晰

### 性能优化
- **异步生成**：使用`Dispatchers.IO`线程池
- **惰性加载**：LazyColumn按需渲染可见页面
- **内存管理**：使用`use {}`自动关闭PdfRenderer和FileDescriptor

### 错误处理
- 数据不足时显示友好错误消息
- 文件创建失败时提示用户
- 分享失败时显示错误原因
- 保存取消时保留临时文件（可重新操作）

## 已知限制
1. **API要求**：PdfRenderer需要API 21+（Android 5.0+）- 已满足（项目minSdk=26）
2. **趋势图**：sparkline功能已实现但暂未使用（数据模型缺少`trendData`字段）
3. **国际化**：当前所有文本硬编码中文

## 未来改进方向
1. **趋势图可视化**：扩展数据模型添加历史数据点，启用sparkline
2. **自定义模板**：允许用户选择报表样式（简洁/详细/图表型）
3. **批量导出**：支持导出多份报表为ZIP
4. **水印功能**：添加"仅供医生参考"水印
5. **打印预览**：集成PrintManager直接打印
6. **QR码**：生成验证码用于医生核验真实性

## 测试建议
1. **各种日期范围**：7天、30天、3个月、1年
2. **数据完整度**：完整数据 vs 部分缺失数据
3. **极端值**：所有症状10分 vs 所有0分
4. **长文本**：AI建议超长内容的换行处理
5. **分享目标**：测试微信、邮件、文件管理器等
6. **存储位置**：内部存储、SD卡、云盘应用

## 相关文件
- `/app/src/main/java/com/heldairy/core/pdf/ImprovedDoctorReportPdfGenerator.kt`
- `/app/src/main/java/com/heldairy/feature/insights/preview/PdfPreviewScreen.kt`
- `/app/src/main/java/com/heldairy/feature/insights/InsightsViewModel.kt`
- `/app/src/main/java/com/heldairy/feature/insights/ui/InsightsScreen.kt`
- `/app/src/main/res/xml/file_paths.xml`
- `/app/src/main/AndroidManifest.xml` (FileProvider配置)

## 参考资料
- [Android PdfDocument API](https://developer.android.com/reference/android/graphics/pdf/PdfDocument)
- [PdfRenderer API](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)
- [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- [FileProvider Guide](https://developer.android.com/reference/androidx/core/content/FileProvider)
