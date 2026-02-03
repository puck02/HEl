# 医生报表功能实现 - 2026-02-03

## 概述
在"洞察"页面成功实现了"生成医生报表"功能，用户可以将健康数据导出为专业的PDF格式报表，供医生问诊参考。

## 实现内容

### 1. 数据模型层 (`DoctorReportModels.kt`)
创建了完整的报表数据结构：
- **DoctorReportData**: 顶层报表数据容器
- **PatientInfo**: 报表元信息（生成时间、数据范围、免责声明）
- **DataCompleteness**: 数据完整度统计
- **MedicationSummaryForReport**: 用药摘要（活跃用药列表 + 依从性）
- **SymptomSummaryForReport**: 症状趋势摘要（平均值、最新值、趋势箭头）
- **LifestyleSummaryForReport**: 生活方式摘要（睡眠、午睡、步数、受凉、经期）
- **AiInsightsSummaryForReport**: AI洞察摘要（可选）

### 2. 数据汇总层 (`DoctorReportRepository.kt`)
实现了数据聚合逻辑：
- 复用 `InsightCalculator` 计算7天/30天趋势
- 从 `MedicationRepository` 获取活跃用药课程
- 从 `InsightRepository` 提取周度AI洞察
- 格式化分布数据为人类可读文本（如"7-8小时 (70%)"）
- 中文化症状名称（headache_intensity → "头痛"）

### 3. PDF生成层 (`DoctorReportPdfGenerator.kt`)
使用Android原生打印框架实现：
- **技术选型**: `PrintDocumentAdapter` + `PdfDocument`
  - 优点：轻量、无外部依赖、系统级API
  - 缺点：需手动Canvas绘制（已实现）
- **报表结构**:
  - 标题："健康日记 - 医生报表"
  - 报表信息：生成时间、数据范围
  - 数据完整度：已填写天数/总天数
  - 用药情况：活跃用药列表（名称/剂量/频次）+ 依从性统计
  - 症状趋势：表格形式（症状名称、平均值、最近值、趋势↑↓→）
  - 生活方式：睡眠/午睡/步数/受凉/经期
  - AI洞察：关键观察 + 生活建议（可选）
  - 免责声明：页脚固定位置
- **排版细节**:
  - A4尺寸 (595x842 points)
  - 标题24pt粗体，章节18pt粗体，正文14pt
  - 50pt边距，20pt行高
  - 表格分隔线、章节间距优化

### 4. 导出助手 (`DoctorReportExporter.kt`)
封装打印流程：
- 调用系统 `PrintManager`
- 文件名格式：`健康报表_20260203`
- 用户可选择"保存为PDF"或"打印"

### 5. ViewModel层 (`InsightsViewModel.kt`)
扩展了洞察页ViewModel：
- 新增状态：`isGeneratingReport`, `reportGenerationSuccess`, `reportError`
- **generateDoctorReport()**: 异步生成报表，根据当前选中窗口（7天/30天）
- **clearReportStatus()**: 清除生成状态（用于关闭Toast）
- 依赖注入：通过AppContainer获取 `DoctorReportRepository`

### 6. UI层 (`InsightsScreen.kt`)
新增UI组件：
- **DoctorReportCard**: Material 3风格卡片
  - PDF图标 + 标题 + 说明文字
  - 状态提示：
    - 生成中：加载进度条
    - 成功：绿色背景 + "报表已生成，请选择保存位置"
    - 失败：红色背景 + 错误信息
  - 生成按钮：主色调、带图标
  - 免责说明：小号灰色文字
- 位置：插入在"时间窗口选择器"和"数据卡片"之间

### 7. 依赖注入 (`AppContainer.kt`)
集成到App容器：
```kotlin
override val doctorReportRepository: DoctorReportRepository = DoctorReportRepository(
    insightRepository = insightRepository,
    medicationRepository = medicationRepository
)
```

## 技术亮点

### 1. 离线优先
- 所有数据来自Room本地数据库
- PDF生成完全在设备端完成，无需网络
- 符合"本地优先"设计哲学

### 2. 数据复用
- 复用 `InsightCalculator` 的趋势计算逻辑
- 复用 `InsightWindow` 数据结构
- 复用用药管理模块的 `Med` 和 `MedCourse`
- 最小化重复代码

### 3. 医疗边界
- 报表首页显著免责声明："本报告仅供医生参考，不构成医疗诊断依据"
- 页脚固定免责文本
- 符合"生活管家"定位，不越界诊断

### 4. Material 3设计
- 复用项目主题色（success/warning/semanticError）
- 统一圆角半径（CornerRadius.Small/Medium）
- 统一间距（Spacing.S/M/XS）
- Icon使用 Material Icons（PictureAsPdf/CheckCircle/Close）

### 5. 错误处理
- PDF生成失败显示明确错误信息
- 用户可重试
- 不会因生成失败阻塞UI

## 用户体验流程

1. 用户进入"洞察"页面
2. 选择时间窗口（7天/30天）
3. 点击"生成医生报表"按钮
4. 系统显示"正在生成报表..."
5. 调起系统打印对话框
6. 用户选择"保存为PDF"，选择保存位置
7. 成功后显示绿色提示："报表已生成"
8. 用户可关闭提示或再次生成

## 报表内容示例

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
          健康日记 - 医生报表
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

报表信息
  生成时间：2026-02-03 14:30
  数据范围：2026-01-27 至 2026-02-03 (7天)

数据完整度
  已填写 6 / 7 天 (86%)

用药情况
  活跃用药：
    • 布洛芬 (400mg) - 每日2次 [早饭后、晚饭后]
    • 维生素C (100mg) - 每日1次 [早饭后]
  
  用药依从性：
    按时服用：5天，有遗漏：1天，无需用药：1天

症状趋势
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
症状名称    平均值   最近值    趋势
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
头痛        3.5      2.0       ↓ 下降
颈肩腰      5.2      6.0       ↑ 上升
胃部        2.1      2.0       → 稳定

生活方式
  睡眠时长：7-8小时 (70%)
  午睡时长：无午睡 (85%)
  日均步数：6000-10000步 (60%)
  受凉天数：1天

AI健康洞察（仅供参考）
  关键观察：
    • 颈肩腰不适呈上升趋势，可能与久坐相关
    • 睡眠质量较好，7-8小时占比70%
  
  生活建议：
    • 建议每小时站立活动5分钟，缓解颈肩压力
    • 保持当前睡眠节奏
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
本报告由健康日记APP生成，仅供医生参考，
不构成医疗诊断依据。
```

## 后续优化方向

### 短期优化
- [ ] 支持导出多种格式（PDF + CSV打包）
- [ ] 添加报表模板选择（简版/详版）
- [ ] 支持自定义日期范围（不限于7/30天）
- [ ] 添加打印预览功能

### 长期增强
- [ ] 图表可视化（折线图、环形图嵌入PDF）
- [ ] 多语言支持（中/英医生报表）
- [ ] 医生反馈模块（扫码上传，逆向导入医嘱）
- [ ] 报表分享二维码（加密链接，72小时有效）

## 文件清单

**新增文件**:
- `app/src/main/java/com/heldairy/core/data/DoctorReportModels.kt`
- `app/src/main/java/com/heldairy/core/data/DoctorReportRepository.kt`
- `app/src/main/java/com/heldairy/core/pdf/DoctorReportPdfGenerator.kt`
- `app/src/main/java/com/heldairy/core/pdf/DoctorReportExporter.kt`

**修改文件**:
- `app/src/main/java/com/heldairy/core/di/AppContainer.kt` (添加doctorReportRepository)
- `app/src/main/java/com/heldairy/feature/insights/InsightsViewModel.kt` (添加报表生成函数)
- `app/src/main/java/com/heldairy/feature/insights/ui/InsightsScreen.kt` (添加DoctorReportCard)

## 编译验证
```bash
./gradlew build
# BUILD SUCCESSFUL in 47s
# 107 actionable tasks: 40 executed, 67 up-to-date
```

## 符合要求检查

✅ **本地优先**: 无需网络，全部数据来自Room  
✅ **医疗边界**: 免责声明显著，不提供诊断  
✅ **Material 3**: 使用项目统一主题和组件  
✅ **生活管家语气**: 报表文案温和、支持性  
✅ **离线可用**: PDF生成无需联网  
✅ **数据隐私**: 文件仅保存到用户选择的位置，不自动上传  
✅ **Android 10+**: PrintDocumentAdapter API兼容Android 10+  

## 总结
成功实现了医生报表功能，用户可将7天或30天的健康数据（症状趋势、用药记录、生活方式）导出为结构化PDF，供医生问诊参考。技术实现干净、符合项目架构，无外部依赖，完全离线可用。
