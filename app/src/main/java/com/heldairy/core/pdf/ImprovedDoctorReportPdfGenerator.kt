package com.heldairy.core.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import com.heldairy.core.data.DoctorReportData
import com.heldairy.core.data.TrendFlag
import java.io.File
import java.io.FileOutputStream

/**
 * 改进的PDF医生报表生成器 v3.0
 * - 使用StaticLayout改善中文排版
 * - 添加颜色编码和可视化
 * - 支持自动分页（修复分页后canvas引用问题）
 * - 生成到临时文件用于预览
 * - 包含完整AI分析内容
 */
class ImprovedDoctorReportPdfGenerator(
    private val context: Context,
    private val reportData: DoctorReportData
) {
    // A4尺寸（点）
    private val pageWidth = 595
    private val pageHeight = 842
    
    // 边距和间距（使用8pt网格系统）
    private val marginHorizontal = 48f
    private val marginTop = 56f
    private val marginBottom = 56f
    private val sectionSpacing = 28f
    private val lineSpacing = 1.5f
    
    // 颜色定义
    private val colorPrimary = Color.parseColor("#1976D2")
    private val colorSuccess = Color.parseColor("#4CAF50")
    private val colorWarning = Color.parseColor("#FF9800")
    private val colorError = Color.parseColor("#F44336")
    private val colorGray900 = Color.parseColor("#212121")
    private val colorGray700 = Color.parseColor("#616161")
    private val colorGray300 = Color.parseColor("#E0E0E0")
    
    // TextPaint对象
    private val titlePaint = TextPaint().apply {
        color = colorPrimary
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    
    private val sectionHeaderPaint = TextPaint().apply {
        color = colorGray900
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    
    private val bodyPaint = TextPaint().apply {
        color = colorGray900
        textSize = 11f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }
    
    private val captionPaint = TextPaint().apply {
        color = colorGray700
        textSize = 10f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }
    
    private val boldBodyPaint = TextPaint().apply {
        color = colorGray900
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    
    private val linePaint = Paint().apply {
        color = colorGray300
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // 当前页面状态
    private var currentY = 0f
    private var currentPage: PdfDocument.Page? = null
    private var currentCanvas: Canvas? = null
    private var pageNumber = 1
    private lateinit var pdfDoc: PdfDocument
    
    /**
     * 获取当前可用的Canvas，如果不可用则抛出异常
     */
    private fun canvas(): Canvas = currentCanvas 
        ?: throw IllegalStateException("Canvas not available")
    
    /**
     * 生成PDF到临时文件
     */
    fun generateToTempFile(): File {
        val tempFile = File(context.cacheDir, "temp_doctor_report_${System.currentTimeMillis()}.pdf")
        pdfDoc = PdfDocument()
        
        try {
            pageNumber = 1
            startNewPage()
            drawReport()
            finishCurrentPage()
            
            FileOutputStream(tempFile).use { outputStream ->
                pdfDoc.writeTo(outputStream)
            }
            
            return tempFile
        } finally {
            pdfDoc.close()
        }
    }
    
    private fun startNewPage() {
        if (currentPage != null) {
            finishCurrentPage()
        }
        
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        currentPage = pdfDoc.startPage(pageInfo)
        currentCanvas = currentPage?.canvas
        currentY = marginTop
        pageNumber++
    }
    
    private fun finishCurrentPage() {
        currentPage?.let { page ->
            drawPageFooter(page.canvas, pageNumber - 1)
            pdfDoc.finishPage(page)
        }
        currentPage = null
        currentCanvas = null
    }
    
    /**
     * 确保有足够空间，否则分页
     */
    private fun ensureSpace(requiredHeight: Float) {
        if (currentY + requiredHeight > pageHeight - marginBottom) {
            startNewPage()
        }
    }
    
    private fun getContentWidth(): Float = pageWidth - 2 * marginHorizontal
    
    private fun drawReport() {
        // 1. 报表标题和信息
        drawReportInfo()
        currentY += sectionSpacing
        
        // 2. 数据完整度
        drawDataCompleteness()
        currentY += sectionSpacing
        
        // 3. 用药情况
        drawMedicationSummary()
        currentY += sectionSpacing
        
        // 4. 症状趋势
        drawSymptomTrends()
        currentY += sectionSpacing
        
        // 5. 生活方式
        drawLifestyleSummary()
        currentY += sectionSpacing
        
        // 6. AI健康洞察（完整内容）
        drawAiInsights()
        currentY += sectionSpacing
        
        // 7. 免责声明
        drawDisclaimer()
    }
    
    private fun drawReportInfo() {
        val c = canvas()
        
        // 标题
        c.drawText("健康数据报告", marginHorizontal, currentY, titlePaint)
        currentY += titlePaint.textSize + 8f
        
        // 副标题
        captionPaint.color = colorGray700
        c.drawText("本报告仅供医生参考，不构成医疗诊断依据", marginHorizontal, currentY, captionPaint)
        currentY += captionPaint.textSize + 16f
        
        // 装饰线
        linePaint.strokeWidth = 2f
        linePaint.color = colorPrimary
        c.drawLine(marginHorizontal, currentY, pageWidth - marginHorizontal, currentY, linePaint)
        currentY += 16f
        
        // 报表信息
        drawSectionHeader("报表信息")
        
        val info = reportData.patientInfo
        drawText("生成时间: ${info.reportGeneratedAt}", marginHorizontal + 16f)
        currentY += bodyPaint.textSize * lineSpacing
        drawText("数据范围: ${info.dataRangeStart} 至 ${info.dataRangeEnd} (${reportData.timeWindow}天)", marginHorizontal + 16f)
        currentY += bodyPaint.textSize * lineSpacing
    }
    
    private fun drawDataCompleteness() {
        ensureSpace(60f)
        drawSectionHeader("数据完整度")
        
        val completeness = reportData.dataCompleteness
        val percentage = (completeness.completionRate * 100).toInt()
        
        drawText("已填写 ${completeness.filledDays} / ${completeness.totalDays} 天 ($percentage%)", marginHorizontal + 16f)
        currentY += bodyPaint.textSize * lineSpacing + 8f
        
        // 进度条
        val c = canvas()
        val barWidth = getContentWidth() - 32f
        val barHeight = 12f
        val cornerRadius = 6f
        val x = marginHorizontal + 16f
        
        fillPaint.color = colorGray300
        c.drawRoundRect(RectF(x, currentY, x + barWidth, currentY + barHeight), cornerRadius, cornerRadius, fillPaint)
        
        val progressWidth = barWidth * completeness.completionRate.toFloat()
        fillPaint.color = when {
            completeness.completionRate >= 0.7 -> colorSuccess
            completeness.completionRate >= 0.5 -> colorWarning
            else -> colorError
        }
        c.drawRoundRect(RectF(x, currentY, x + progressWidth, currentY + barHeight), cornerRadius, cornerRadius, fillPaint)
        currentY += 20f
    }
    
    private fun drawMedicationSummary() {
        ensureSpace(60f)
        drawSectionHeader("用药情况")
        
        val summary = reportData.medicationSummary
        
        if (summary.activeMedications.isEmpty() && summary.events.isEmpty()) {
            drawText("当前无活跃用药记录", marginHorizontal + 16f)
            currentY += bodyPaint.textSize * lineSpacing
            return
        }
        
        // 当前用药
        if (summary.activeMedications.isNotEmpty()) {
            drawText("当前用药:", marginHorizontal + 16f, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing
            
            summary.activeMedications.forEach { med ->
                ensureSpace(20f)
                val dosageInfo = med.dosage?.let { " $it" } ?: ""
                val timeInfo = med.timeHints?.let { " ($it)" } ?: ""
                drawText("• ${med.name}$dosageInfo, ${med.frequency}$timeInfo", marginHorizontal + 32f)
                currentY += bodyPaint.textSize * lineSpacing
            }
            currentY += 8f
        }
        
        // 依从性统计
        ensureSpace(40f)
        drawText("用药依从性:", marginHorizontal + 16f, boldBodyPaint)
        currentY += bodyPaint.textSize * lineSpacing
        
        val adherence = summary.adherence
        drawText("按时服用: ${adherence.onTime}天, 有遗漏: ${adherence.missed}天, 无需用药: ${adherence.na}天", marginHorizontal + 32f)
        currentY += bodyPaint.textSize * lineSpacing
        
        // 用药事件记录
        if (summary.events.isNotEmpty()) {
            currentY += 8f
            ensureSpace(40f)
            drawText("用药变更记录:", marginHorizontal + 16f, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing + 4f
            
            val eventsByDate = summary.events.groupBy { it.date }
            eventsByDate.forEach { (date, events) ->
                ensureSpace(24f)
                drawText("[$date]", marginHorizontal + 24f, captionPaint)
                currentY += captionPaint.textSize * 1.3f
                
                events.forEach { event ->
                    ensureSpace(24f)
                    drawMultilineText("${event.time} - ${event.description}", marginHorizontal + 40f, (getContentWidth() - 56f).toInt())
                }
            }
        }
    }
    
    private fun drawSymptomTrends() {
        ensureSpace(60f)
        drawSectionHeader("症状趋势")
        
        if (reportData.symptomSummary.metrics.isEmpty()) {
            drawText("暂无症状数据", marginHorizontal + 16f)
            currentY += bodyPaint.textSize * lineSpacing
            return
        }
        
        val startX = marginHorizontal + 16f
        val colWidths = floatArrayOf(100f, 70f, 70f, 160f)
        
        // 表头
        ensureSpace(30f)
        var x = startX
        listOf("症状", "平均值", "最近值", "趋势").forEachIndexed { i, header ->
            canvas().drawText(header, x + 4f, currentY, boldBodyPaint)
            x += colWidths[i]
        }
        currentY += bodyPaint.textSize * lineSpacing + 4f
        
        // 分隔线
        linePaint.color = colorGray300
        linePaint.strokeWidth = 1f
        canvas().drawLine(startX, currentY, startX + colWidths.sum(), currentY, linePaint)
        currentY += 4f
        
        // 数据行
        reportData.symptomSummary.metrics.forEachIndexed { index, metric ->
            ensureSpace(24f)
            val c = canvas()
            
            // 斑马纹
            if (index % 2 == 1) {
                fillPaint.color = Color.parseColor("#F5F5F5")
                c.drawRect(startX, currentY - 2f, startX + colWidths.sum(), currentY + bodyPaint.textSize * lineSpacing + 2f, fillPaint)
            }
            
            // 症状名称
            c.drawText(metric.symptomName, startX + 4f, currentY, bodyPaint)
            
            // 平均值
            bodyPaint.color = getSeverityColor(metric.average.toFloat())
            c.drawText(String.format("%.1f", metric.average), startX + colWidths[0] + 4f, currentY, bodyPaint)
            bodyPaint.color = colorGray900
            
            // 最近值
            val latestValue = metric.latestValue ?: metric.average
            bodyPaint.color = getSeverityColor(latestValue.toFloat())
            c.drawText(String.format("%.1f", latestValue), startX + colWidths[0] + colWidths[1] + 4f, currentY, bodyPaint)
            bodyPaint.color = colorGray900
            
            // 趋势
            val (trendSymbol, trendColor) = when (metric.trend) {
                TrendFlag.rising -> "↑ ${metric.trendDescription}" to colorError
                TrendFlag.falling -> "↓ ${metric.trendDescription}" to colorSuccess
                TrendFlag.stable -> "→ ${metric.trendDescription}" to colorGray700
            }
            bodyPaint.color = trendColor
            c.drawText(trendSymbol, startX + colWidths[0] + colWidths[1] + colWidths[2] + 4f, currentY, bodyPaint)
            bodyPaint.color = colorGray900
            
            currentY += bodyPaint.textSize * lineSpacing + 4f
        }
    }
    
    private fun drawLifestyleSummary() {
        ensureSpace(60f)
        drawSectionHeader("生活方式")
        
        val lifestyle = reportData.lifestyleSummary
        
        if (lifestyle.sleepSummary.isNotBlank()) {
            ensureSpace(36f)
            drawText("睡眠时长:", marginHorizontal + 16f, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing
            drawText(lifestyle.sleepSummary, marginHorizontal + 32f)
            currentY += bodyPaint.textSize * lineSpacing + 6f
        }
        
        if (lifestyle.napSummary.isNotBlank()) {
            ensureSpace(36f)
            drawText("午睡情况:", marginHorizontal + 16f, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing
            drawText(lifestyle.napSummary, marginHorizontal + 32f)
            currentY += bodyPaint.textSize * lineSpacing + 6f
        }
        
        if (lifestyle.stepsSummary.isNotBlank()) {
            ensureSpace(36f)
            drawText("运动步数:", marginHorizontal + 16f, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing
            drawText(lifestyle.stepsSummary, marginHorizontal + 32f)
            currentY += bodyPaint.textSize * lineSpacing + 6f
        }
        
        if (lifestyle.chillExposureDays > 0) {
            ensureSpace(20f)
            drawText("受凉天数: ${lifestyle.chillExposureDays}天", marginHorizontal + 16f)
            currentY += bodyPaint.textSize * lineSpacing
        }
        
        if (!lifestyle.menstrualSummary.isNullOrBlank()) {
            ensureSpace(20f)
            drawText("经期状态: ${lifestyle.menstrualSummary}", marginHorizontal + 16f)
            currentY += bodyPaint.textSize * lineSpacing
        }
    }
    
    private fun drawAiInsights() {
        ensureSpace(80f)
        drawSectionHeader("AI健康洞察")
        
        val ai = reportData.aiInsightsSummary
        
        if (ai == null) {
            drawText("暂无AI分析（请确保已配置API密钥并生成周度洞察）", marginHorizontal + 16f)
            currentY += bodyPaint.textSize * lineSpacing
            return
        }
        
        // 完整显示周度洞察内容
        if (ai.weeklyInsights.isNotEmpty()) {
            drawText("健康趋势分析:", marginHorizontal + 16f, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing + 4f
            
            ai.weeklyInsights.forEach { insight ->
                ensureSpace(40f)
                drawMultilineText("• $insight", marginHorizontal + 24f, (getContentWidth() - 40f).toInt())
                currentY += 4f
            }
            currentY += 8f
        }
        
        // 建议关注
        if (ai.topSuggestions.isNotEmpty()) {
            ensureSpace(40f)
            drawText("重点建议:", marginHorizontal + 16f, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing + 4f
            
            ai.topSuggestions.forEach { suggestion ->
                ensureSpace(40f)
                drawMultilineText("• $suggestion", marginHorizontal + 24f, (getContentWidth() - 40f).toInt())
                currentY += 4f
            }
        }
        
        // 补充分析上下文
        currentY += 8f
        ensureSpace(60f)
        val contextAnalysis = buildContextAnalysis()
        drawMultilineText(contextAnalysis, marginHorizontal + 16f, (getContentWidth() - 32f).toInt())
    }
    
    /**
     * 构建上下文分析，结合长期趋势
     */
    private fun buildContextAnalysis(): String {
        val sb = StringBuilder()
        sb.append("【综合分析】")
        
        // 基于症状趋势
        val risingSymptoms = reportData.symptomSummary.metrics.filter { it.trend == TrendFlag.rising }
        val fallingSymptoms = reportData.symptomSummary.metrics.filter { it.trend == TrendFlag.falling }
        val highSeverity = reportData.symptomSummary.metrics.filter { it.average > 6.0 }
        
        if (highSeverity.isNotEmpty()) {
            sb.append("本周期内${highSeverity.joinToString("、") { it.symptomName }}症状较为严重（平均值>6），建议重点关注并与医生沟通。")
        }
        
        if (risingSymptoms.isNotEmpty()) {
            sb.append("${risingSymptoms.joinToString("、") { it.symptomName }}呈上升趋势，需要注意观察。")
        }
        
        if (fallingSymptoms.isNotEmpty()) {
            sb.append("${fallingSymptoms.joinToString("、") { it.symptomName }}有所改善，可继续保持当前调理方式。")
        }
        
        // 基于用药依从性
        val adherence = reportData.medicationSummary.adherence
        if (adherence.missed > adherence.onTime && adherence.onTime + adherence.missed > 0) {
            sb.append("用药依从性有待改善，建议设置提醒确保按时服药。")
        }
        
        // 基于数据完整度
        if (reportData.dataCompleteness.completionRate < 0.5) {
            sb.append("数据记录完整度较低(${(reportData.dataCompleteness.completionRate * 100).toInt()}%)，建议坚持每日记录以获得更准确的健康趋势分析。")
        }
        
        if (sb.toString() == "【综合分析】") {
            sb.append("本周期健康状况总体稳定，请继续保持良好的生活习惯和健康记录。")
        }
        
        return sb.toString()
    }
    
    private fun drawDisclaimer() {
        ensureSpace(50f)
        val c = canvas()
        
        linePaint.strokeWidth = 0.5f
        linePaint.color = colorGray300
        c.drawLine(marginHorizontal, currentY, pageWidth - marginHorizontal, currentY, linePaint)
        currentY += 12f
        
        val disclaimer = "⚠️ 本报告由健康日记APP生成，仅供医生参考，不构成医疗诊断依据。请以医生的专业判断为准。"
        drawMultilineText(disclaimer, marginHorizontal, getContentWidth().toInt(), captionPaint)
    }
    
    private fun drawPageFooter(canvas: Canvas, pageNum: Int) {
        val footerY = pageHeight - marginBottom / 2
        val footerText = "第 $pageNum 页"
        val textWidth = captionPaint.measureText(footerText)
        canvas.drawText(footerText, pageWidth / 2f - textWidth / 2f, footerY, captionPaint)
    }
    
    private fun drawSectionHeader(text: String) {
        canvas().drawText(text, marginHorizontal, currentY, sectionHeaderPaint)
        currentY += sectionHeaderPaint.textSize + 10f
    }
    
    private fun drawText(text: String, x: Float, paint: TextPaint = bodyPaint) {
        canvas().drawText(text, x, currentY, paint)
    }
    
    /**
     * 绘制多行文本，支持自动换行和分页
     */
    private fun drawMultilineText(text: String, x: Float, maxWidth: Int, paint: TextPaint = bodyPaint) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth).build()
        
        // 检查是否需要分页
        if (currentY + layout.height > pageHeight - marginBottom) {
            startNewPage()
        }
        
        val c = canvas()
        c.save()
        c.translate(x, currentY)
        layout.draw(c)
        c.restore()
        
        currentY += layout.height
    }
    
    private fun getSeverityColor(value: Float): Int = when {
        value < 3f -> colorSuccess
        value < 7f -> colorWarning
        else -> colorError
    }
}
