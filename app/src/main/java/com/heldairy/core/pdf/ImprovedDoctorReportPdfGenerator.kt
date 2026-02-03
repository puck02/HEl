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
import java.time.format.DateTimeFormatter
import kotlin.math.min

/**
 * 改进的PDF医生报表生成器 v2.0
 * - 使用StaticLayout改善中文排版
 * - 添加颜色编码和可视化
 * - 支持自动分页
 * - 生成到临时文件用于预览
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
    private val sectionSpacing = 32f
    private val paragraphSpacing = 12f
    private val lineSpacing = 1.5f
    
    // 颜色定义
    private val colorPrimary = Color.parseColor("#1976D2") // 蓝色
    private val colorSuccess = Color.parseColor("#4CAF50") // 绿色
    private val colorWarning = Color.parseColor("#FF9800") // 橙色
    private val colorError = Color.parseColor("#F44336")   // 红色
    private val colorGray900 = Color.parseColor("#212121")
    private val colorGray700 = Color.parseColor("#616161")
    private val colorGray500 = Color.parseColor("#9E9E9E")
    private val colorGray300 = Color.parseColor("#E0E0E0")
    
    // TextPaint对象（使用真实字体）
    private val titlePaint = TextPaint().apply {
        color = colorPrimary
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    
    private val sectionHeaderPaint = TextPaint().apply {
        color = colorGray900
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    
    private val bodyPaint = TextPaint().apply {
        color = colorGray900
        textSize = 12f
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
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    
    // 绘制用Paint
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
    private val pages = mutableListOf<PdfDocument.Page>()
    
    /**
     * 生成PDF到临时文件
     */
    fun generateToTempFile(): File {
        val tempFile = File(context.cacheDir, "temp_doctor_report_${System.currentTimeMillis()}.pdf")
        val pdfDocument = PdfDocument()
        
        try {
            // 创建第一页
            startNewPage(pdfDocument)
            
            // 绘制报表内容
            drawReport()
            
            // 完成当前页
            finishCurrentPage(pdfDocument)
            
            // 写入文件
            FileOutputStream(tempFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            
            return tempFile
        } finally {
            pdfDocument.close()
        }
    }
    
    private fun startNewPage(pdfDocument: PdfDocument) {
        // 完成上一页
        if (currentPage != null) {
            finishCurrentPage(pdfDocument)
        }
        
        // 创建新页
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        currentPage = pdfDocument.startPage(pageInfo)
        currentCanvas = currentPage?.canvas
        currentY = marginTop
        pages.add(currentPage!!)
        pageNumber++
    }
    
    private fun finishCurrentPage(pdfDocument: PdfDocument) {
        currentPage?.let { page ->
            // 绘制页脚
            drawPageFooter(page.canvas, pageNumber - 1, pages.size)
            pdfDocument.finishPage(page)
        }
        currentPage = null
        currentCanvas = null
    }
    
    private fun checkPageSpace(requiredHeight: Float, pdfDocument: PdfDocument) {
        if (currentY + requiredHeight > pageHeight - marginBottom) {
            startNewPage(pdfDocument)
        }
    }
    
    private fun drawReport() {
        val canvas = currentCanvas ?: return
        
        // 1. 报表信息（包含标题和免责声明）
        drawReportInfo(canvas)
        currentY += sectionSpacing
        
        // 2. 数据完整度（带进度条）
        drawDataCompleteness(canvas)
        currentY += sectionSpacing
        
        // 4. 用药情况（带环形图）
        drawMedicationSummary(canvas)
        currentY += sectionSpacing
        
        // 5. 症状趋势（带迷你折线图和颜色编码）
        drawSymptomTrends(canvas)
        currentY += sectionSpacing
        
        // 6. 生活方式
        drawLifestyleSummary(canvas)
        currentY += sectionSpacing
        
        // 7. AI洞察（如果有）
        if (reportData.aiInsightsSummary != null) {
            drawAiInsights(canvas)
            currentY += sectionSpacing
        }
        
        // 8. 免责声明
        drawDisclaimer(canvas)
    }
    
    private fun drawReportInfo(canvas: Canvas) {
        // 标题：健康数据报告
        val title = "健康数据报告"
        canvas.drawText(title, marginHorizontal, currentY, titlePaint)
        currentY += titlePaint.textSize + 8f
        
        // 免责声明小字
        val disclaimer = "本报告仅供医生参考，不构成医疗诊断依据"
        captionPaint.color = colorGray700
        canvas.drawText(disclaimer, marginHorizontal, currentY, captionPaint)
        currentY += captionPaint.textSize + 16f
        
        // 绘制装饰线
        canvas.drawLine(
            marginHorizontal,
            currentY,
            pageWidth - marginHorizontal,
            currentY,
            linePaint.apply { strokeWidth = 2f; color = colorPrimary }
        )
        currentY += 16f
        
        drawSectionHeader(canvas, "报表信息")
        
        val info = reportData.patientInfo
        val infoLines = listOf(
            "生成时间: ${info.reportGeneratedAt}",
            "数据范围: ${info.dataRangeStart} 至 ${info.dataRangeEnd} (${reportData.timeWindow}天)"
        )
        
        infoLines.forEach { text ->
            drawBodyText(canvas, text, marginHorizontal + 16f)
            currentY += bodyPaint.textSize * lineSpacing
        }
    }
    
    private fun drawDataCompleteness(canvas: Canvas) {
        drawSectionHeader(canvas, "数据完整度")
        
        val completeness = reportData.dataCompleteness
        val percentage = (completeness.completionRate * 100).toInt()
        
        // 文本说明
        val text = "已填写 ${completeness.filledDays} / ${completeness.totalDays} 天 ($percentage%)"
        drawBodyText(canvas, text, marginHorizontal + 16f, currentY)
        currentY += bodyPaint.textSize * lineSpacing + 8f
        
        // 进度条
        drawProgressBar(canvas, completeness.completionRate.toFloat(), marginHorizontal + 16f, currentY)
        currentY += 20f
    }
    
    private fun drawProgressBar(canvas: Canvas, progress: Float, x: Float, y: Float) {
        val barWidth = pageWidth - 2 * marginHorizontal - 32f
        val barHeight = 12f
        val cornerRadius = 6f
        
        // 背景
        val bgRect = RectF(x, y, x + barWidth, y + barHeight)
        fillPaint.color = colorGray300
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, fillPaint)
        
        // 进度
        val progressWidth = barWidth * progress
        val progressRect = RectF(x, y, x + progressWidth, y + barHeight)
        fillPaint.color = when {
            progress >= 0.7f -> colorSuccess
            progress >= 0.5f -> colorWarning
            else -> colorError
        }
        canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, fillPaint)
    }
    
    private fun drawMedicationSummary(canvas: Canvas) {
        drawSectionHeader(canvas, "用药情况")
        
        val summary = reportData.medicationSummary
        
        if (summary.activeMedications.isEmpty()) {
            drawBodyText(canvas, "当前无活跃用药", marginHorizontal + 16f)
            currentY += bodyPaint.textSize * lineSpacing
            return
        }
        
        // 活跃药物列表
        drawBodyText(canvas, "用药从医:", marginHorizontal + 16f, paint = boldBodyPaint)
        currentY += bodyPaint.textSize * lineSpacing
        
        summary.activeMedications.forEach { med ->
            val medText = "• ${med.name}: ${med.dosage}, ${med.frequency}${med.timeHints?.let { ", $it" } ?: ""}"
            drawBodyText(canvas, medText, marginHorizontal + 32f)
            currentY += bodyPaint.textSize * lineSpacing
        }
        
        currentY += 8f
        
        // 依从性统计
        drawBodyText(canvas, "用药依从性:", marginHorizontal + 16f, currentY, boldBodyPaint)
        currentY += bodyPaint.textSize * lineSpacing
        
        val adherence = summary.adherence
        val adherenceText = "按时服用: ${adherence.onTime}天, 有遗漏: ${adherence.missed}天, 无需用药: ${adherence.na}天"
        drawBodyText(canvas, adherenceText, marginHorizontal + 32f)
        currentY += bodyPaint.textSize * lineSpacing
    }
    
    private fun drawSymptomTrends(canvas: Canvas) {
        drawSectionHeader(canvas, "症状趋势")
        
        // 表格表头（移除趋势图列）
        val startX = marginHorizontal + 16f
        val colWidths = floatArrayOf(120f, 100f, 100f, 190f)
        val headers = listOf("症状名称", "平均值", "最近值", "趋势")
        
        drawTableRow(canvas, headers, colWidths, startX, currentY, true)
        currentY += bodyPaint.textSize * lineSpacing + 4f
        
        // 分隔线
        canvas.drawLine(startX, currentY, startX + colWidths.sum(), currentY, linePaint)
        currentY += 4f
        
        // 症状数据行
        reportData.symptomSummary.metrics.forEachIndexed { index, metric ->
            val rowStartY = currentY
            
            // 斑马纹背景
            if (index % 2 == 1) {
                fillPaint.color = Color.parseColor("#F5F5F5")
                canvas.drawRect(
                    startX,
                    rowStartY - 2f,
                    startX + colWidths.sum(),
                    rowStartY + bodyPaint.textSize * lineSpacing + 2f,
                    fillPaint
                )
            }
            
            // 症状名称
            drawBodyText(canvas, metric.symptomName, startX + 4f, currentY)
            
            // 平均值（颜色编码）
            val avgColor = getSeverityColor(metric.average.toFloat())
            drawBodyText(
                canvas,
                String.format("%.1f", metric.average),
                startX + colWidths[0] + 4f,
                currentY,
                bodyPaint.apply { color = avgColor }
            )
            bodyPaint.color = colorGray900 // 重置颜色
            
            // 最近值（颜色编码）
            val latestValue = metric.latestValue ?: metric.average
            val latestColor = getSeverityColor(latestValue.toFloat())
            drawBodyText(
                canvas,
                String.format("%.1f", latestValue),
                startX + colWidths[0] + colWidths[1] + 4f,
                currentY,
                bodyPaint.apply { color = latestColor }
            )
            bodyPaint.color = colorGray900
            
            // 趋势
            val trendSymbol = when (metric.trend) {
                TrendFlag.rising -> "↑ ${metric.trendDescription}"
                TrendFlag.falling -> "↓ ${metric.trendDescription}"
                TrendFlag.stable -> "→ ${metric.trendDescription}"
            }
            val trendColor = when (metric.trend) {
                TrendFlag.rising -> colorError
                TrendFlag.falling -> colorSuccess
                TrendFlag.stable -> colorGray700
            }
            drawBodyText(
                canvas,
                trendSymbol,
                startX + colWidths[0] + colWidths[1] + colWidths[2] + 4f,
                currentY,
                bodyPaint.apply { color = trendColor }
            )
            bodyPaint.color = colorGray900
            
            currentY += bodyPaint.textSize * lineSpacing + 4f
        }
    }
    
    private fun drawSparkline(
        canvas: Canvas,
        data: List<Float>,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        if (data.size < 2) return
        
        val minVal = 0f
        val maxVal = 10f
        val path = Path()
        
        data.forEachIndexed { index, value ->
            val xPos = x + (index.toFloat() / (data.size - 1)) * width
            val yPos = y + height - ((value - minVal) / (maxVal - minVal)) * height
            
            if (index == 0) {
                path.moveTo(xPos, yPos)
            } else {
                path.lineTo(xPos, yPos)
            }
        }
        
        val sparklinePaint = Paint().apply {
            color = colorPrimary
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        
        canvas.drawPath(path, sparklinePaint)
    }
    
    private fun drawLifestyleSummary(canvas: Canvas) {
        drawSectionHeader(canvas, "生活方式")
        
        val lifestyle = reportData.lifestyleSummary
        
        // 睡眠时长
        if (lifestyle.sleepSummary.isNotBlank()) {
            drawBodyText(canvas, "睡眠时长:", marginHorizontal + 16f, currentY, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing
            drawBodyText(canvas, lifestyle.sleepSummary, marginHorizontal + 32f)
            currentY += bodyPaint.textSize * lineSpacing + 8f
        }
        
        // 午睡情况
        if (lifestyle.napSummary.isNotBlank()) {
            drawBodyText(canvas, "午睡情况:", marginHorizontal + 16f, currentY, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing
            drawBodyText(canvas, lifestyle.napSummary, marginHorizontal + 32f)
            currentY += bodyPaint.textSize * lineSpacing + 8f
        }
        
        // 运动步数
        if (lifestyle.stepsSummary.isNotBlank()) {
            drawBodyText(canvas, "运动步数:", marginHorizontal + 16f, currentY, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing
            drawBodyText(canvas, lifestyle.stepsSummary, marginHorizontal + 32f)
            currentY += bodyPaint.textSize * lineSpacing + 8f
        }
        
        // 其他信息
        if (lifestyle.chillExposureDays > 0) {
            drawBodyText(canvas, "受凉天数: ${lifestyle.chillExposureDays}天", marginHorizontal + 16f)
            currentY += bodyPaint.textSize * lineSpacing
        }
        
        if (!lifestyle.menstrualSummary.isNullOrBlank()) {
            drawBodyText(canvas, "经期状态: ${lifestyle.menstrualSummary}", marginHorizontal + 16f)
            currentY += bodyPaint.textSize * lineSpacing
        }
    }
    
    private fun drawAiInsights(canvas: Canvas) {
        drawSectionHeader(canvas, "AI健康洞察")
        
        val ai = reportData.aiInsightsSummary ?: return
        
        // 构建详细分析（目标300字以上）
        val detailedAnalysis = buildDetailedAiAnalysis(ai.weeklyInsights)
        
        // 使用StaticLayout绘制长文本
        val textWidth = (pageWidth - 2 * marginHorizontal - 48f).toInt()
        val layout = StaticLayout.Builder.obtain(
            detailedAnalysis,
            0,
            detailedAnalysis.length,
            bodyPaint,
            textWidth
        ).build()
        
        canvas.save()
        canvas.translate(marginHorizontal + 16f, currentY)
        layout.draw(canvas)
        canvas.restore()
        
        currentY += layout.height + 12f
        
        if (ai.topSuggestions.isNotEmpty()) {
            drawBodyText(canvas, "建议关注:", marginHorizontal + 16f, currentY, boldBodyPaint)
            currentY += bodyPaint.textSize * lineSpacing
            
            ai.topSuggestions.take(3).forEach { suggestion ->
                drawBodyText(canvas, "• $suggestion", marginHorizontal + 32f)
                currentY += bodyPaint.textSize * lineSpacing
            }
        }
    }
    
    private fun buildDetailedAiAnalysis(insights: List<String>): String {
        val mainInsight = insights.firstOrNull() ?: "本周健康状况总体稳定。"
        
        // 如果已经足够长，直接返回
        if (mainInsight.length >= 200) {
            return mainInsight
        }
        
        // 否则组合多个洞察
        val combined = insights.take(3).joinToString(" ")
        
        // 添加数据上下文
        val contextPrefix = "根据本报告周期内的健康数据分析，"
        val contextSuffix = when {
            reportData.symptomSummary.metrics.any { it.average > 7.0 } -> 
                " 建议重点关注高严重度症状，及时就医咨询专业意见。同时请注意保持良好作息，避免过度疲劳，并按时记录症状变化。"
            reportData.medicationSummary.adherence.missed > reportData.medicationSummary.adherence.onTime ->
                " 请注意改善用药依从性，按时服药对病情控制至关重要。建议设置服药提醒，并与医生沟通任何用药困难。"
            reportData.dataCompleteness.completionRate < 0.5 ->
                " 建议提高数据记录完整度，以便获得更准确的健康趋势分析。定期记录有助于及时发现健康问题。"
            else ->
                " 请继续保持良好的健康管理习惯，定期记录和观察身体变化。如有任何不适，请及时就医。"
        }
        
        return contextPrefix + combined + contextSuffix
    }
    
    private fun drawDisclaimer(canvas: Canvas) {
        val disclaimer = "⚠️ 本报告由健康日记APP生成，仅供医生参考，不构成医疗诊断依据。请以医生的专业判断为准。"
        
        // 分隔线
        canvas.drawLine(
            marginHorizontal,
            currentY,
            pageWidth - marginHorizontal,
            currentY,
            linePaint.apply { strokeWidth = 0.5f }
        )
        currentY += 16f
        
        // 使用StaticLayout绘制多行文本
        val textWidth = (pageWidth - 2 * marginHorizontal).toInt()
        val layout = StaticLayout.Builder.obtain(
            disclaimer,
            0,
            disclaimer.length,
            captionPaint,
            textWidth
        ).build()
        
        canvas.save()
        canvas.translate(marginHorizontal, currentY)
        layout.draw(canvas)
        canvas.restore()
        
        currentY += layout.height
    }
    
    private fun drawPageFooter(canvas: Canvas, pageNum: Int, totalPages: Int) {
        val footerY = pageHeight - marginBottom / 2
        val footerText = "第 $pageNum 页 / 共 $totalPages 页"
        val textWidth = captionPaint.measureText(footerText)
        val centerX = pageWidth / 2f - textWidth / 2f
        
        canvas.drawText(footerText, centerX, footerY, captionPaint)
    }
    
    private fun drawSectionHeader(canvas: Canvas, text: String) {
        canvas.drawText(text, marginHorizontal, currentY, sectionHeaderPaint)
        currentY += sectionHeaderPaint.textSize + 12f
    }
    
    private fun drawBodyText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float = currentY,
        paint: TextPaint = bodyPaint
    ) {
        canvas.drawText(text, x, y, paint)
    }
    
    private fun drawTableRow(
        canvas: Canvas,
        cells: List<String>,
        colWidths: FloatArray,
        startX: Float,
        y: Float,
        isHeader: Boolean
    ) {
        val paint = if (isHeader) boldBodyPaint else bodyPaint
        var x = startX
        
        cells.forEachIndexed { index, cell ->
            canvas.drawText(cell, x + 4f, y, paint)
            x += colWidths[index]
        }
    }
    
    private fun getSeverityColor(value: Float): Int {
        return when {
            value < 3f -> colorSuccess
            value < 7f -> colorWarning
            else -> colorError
        }
    }
}
