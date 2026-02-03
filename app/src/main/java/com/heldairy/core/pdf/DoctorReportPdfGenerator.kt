package com.heldairy.core.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.heldairy.core.data.DoctorReportData
import java.io.FileOutputStream

/**
 * PDF医生报表生成器
 * 使用Android打印框架生成格式化的PDF文档
 */
class DoctorReportPdfGenerator(
    private val context: Context,
    private val reportData: DoctorReportData
) : PrintDocumentAdapter() {

    private var pageHeight = 0
    private var pageWidth = 0
    private val marginLeft = 50f
    private val marginTop = 50f
    private val marginRight = 50f
    private val lineHeight = 20f

    // Paint objects
    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val sectionTitlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 18f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val normalPaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        isAntiAlias = true
    }

    private val smallPaint = Paint().apply {
        color = Color.GRAY
        textSize = 12f
        isAntiAlias = true
    }

    private val boldPaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        isFakeBoldText = true
        isAntiAlias = true
    }

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        pageHeight = pageInfo.pageHeight
        pageWidth = pageInfo.pageWidth

        val info = PrintDocumentInfo.Builder("doctor_report.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            pdfDocument.close()
            return
        }

        // Draw content
        drawReport(page.canvas)
        pdfDocument.finishPage(page)

        // Write to file
        try {
            pdfDocument.writeTo(FileOutputStream(destination?.fileDescriptor))
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawReport(canvas: Canvas) {
        var yPosition = marginTop

        // Title
        yPosition = drawText(canvas, "健康日记 - 医生报表", marginLeft, yPosition, titlePaint)
        yPosition += lineHeight

        // Patient Info Section
        yPosition = drawPatientInfo(canvas, yPosition)
        yPosition += lineHeight * 2

        // Data Completeness
        yPosition = drawDataCompleteness(canvas, yPosition)
        yPosition += lineHeight * 2

        // Medication Summary
        yPosition = drawMedicationSummary(canvas, yPosition)
        yPosition += lineHeight * 2

        // Symptom Summary
        yPosition = drawSymptomSummary(canvas, yPosition)
        yPosition += lineHeight * 2

        // Lifestyle Summary
        yPosition = drawLifestyleSummary(canvas, yPosition)
        yPosition += lineHeight * 2

        // AI Insights (if available)
        if (reportData.aiInsightsSummary != null) {
            yPosition = drawAiInsights(canvas, yPosition)
            yPosition += lineHeight * 2
        }

        // Disclaimer at bottom
        drawDisclaimer(canvas)
    }

    private fun drawPatientInfo(canvas: Canvas, startY: Float): Float {
        var y = startY
        y = drawText(canvas, "报表信息", marginLeft, y, sectionTitlePaint)
        y += lineHeight * 0.5f

        val info = reportData.patientInfo
        y = drawText(canvas, "生成时间：${info.reportGeneratedAt}", marginLeft + 20f, y, normalPaint)
        y += lineHeight * 0.8f
        y = drawText(canvas, "数据范围：${info.dataRangeStart} 至 ${info.dataRangeEnd} (${reportData.timeWindow}天)", 
            marginLeft + 20f, y, normalPaint)
        
        return y
    }

    private fun drawDataCompleteness(canvas: Canvas, startY: Float): Float {
        var y = startY
        y = drawText(canvas, "数据完整度", marginLeft, y, sectionTitlePaint)
        y += lineHeight * 0.5f

        val completeness = reportData.dataCompleteness
        val percentage = (completeness.completionRate * 100).toInt()
        y = drawText(canvas, "已填写 ${completeness.filledDays} / ${completeness.totalDays} 天 ($percentage%)", 
            marginLeft + 20f, y, normalPaint)
        
        return y
    }

    private fun drawMedicationSummary(canvas: Canvas, startY: Float): Float {
        var y = startY
        y = drawText(canvas, "用药情况", marginLeft, y, sectionTitlePaint)
        y += lineHeight * 0.5f

        val medSummary = reportData.medicationSummary

        // Active medications
        if (medSummary.activeMedications.isEmpty()) {
            y = drawText(canvas, "当前无活跃用药", marginLeft + 20f, y, normalPaint)
            y += lineHeight * 0.8f
        } else {
            y = drawText(canvas, "活跃用药：", marginLeft + 20f, y, boldPaint)
            y += lineHeight * 0.8f
            medSummary.activeMedications.forEach { med ->
                val medInfo = buildString {
                    append("• ${med.name}")
                    if (med.dosage != null) append(" (${med.dosage})")
                    append(" - ${med.frequency}")
                    if (med.timeHints != null) append(" [${med.timeHints}]")
                }
                y = drawText(canvas, medInfo, marginLeft + 40f, y, normalPaint)
                y += lineHeight * 0.8f
            }
        }

        // Adherence
        val adherence = medSummary.adherence
        val total = adherence.onTime + adherence.missed + adherence.na
        if (total > 0) {
            y += lineHeight * 0.3f
            y = drawText(canvas, "用药依从性：", marginLeft + 20f, y, boldPaint)
            y += lineHeight * 0.8f
            y = drawText(canvas, "按时服用：${adherence.onTime}天，有遗漏：${adherence.missed}天，无需用药：${adherence.na}天", 
                marginLeft + 40f, y, normalPaint)
        }

        return y
    }

    private fun drawSymptomSummary(canvas: Canvas, startY: Float): Float {
        var y = startY
        y = drawText(canvas, "症状趋势", marginLeft, y, sectionTitlePaint)
        y += lineHeight * 0.5f

        val symptoms = reportData.symptomSummary.metrics
        if (symptoms.isEmpty()) {
            y = drawText(canvas, "无症状数据", marginLeft + 20f, y, normalPaint)
        } else {
            // Table header
            y = drawText(canvas, "症状名称", marginLeft + 20f, y, boldPaint)
            drawText(canvas, "平均值", marginLeft + 150f, y, boldPaint)
            drawText(canvas, "最近值", marginLeft + 250f, y, boldPaint)
            drawText(canvas, "趋势", marginLeft + 350f, y, boldPaint)
            y += lineHeight * 0.8f

            // Draw line
            canvas.drawLine(marginLeft + 20f, y - 5f, pageWidth - marginRight, y - 5f, normalPaint)
            y += lineHeight * 0.3f

            symptoms.forEach { symptom ->
                y = drawText(canvas, symptom.symptomName, marginLeft + 20f, y, normalPaint)
                drawText(canvas, String.format("%.1f", symptom.average), marginLeft + 150f, y, normalPaint)
                drawText(canvas, symptom.latestValue?.let { String.format("%.1f", it) } ?: "-", 
                    marginLeft + 250f, y, normalPaint)
                drawText(canvas, symptom.trendDescription, marginLeft + 350f, y, normalPaint)
                y += lineHeight * 0.8f
            }
        }

        return y
    }

    private fun drawLifestyleSummary(canvas: Canvas, startY: Float): Float {
        var y = startY
        y = drawText(canvas, "生活方式", marginLeft, y, sectionTitlePaint)
        y += lineHeight * 0.5f

        val lifestyle = reportData.lifestyleSummary
        y = drawText(canvas, "睡眠时长：${lifestyle.sleepSummary}", marginLeft + 20f, y, normalPaint)
        y += lineHeight * 0.8f
        y = drawText(canvas, "午睡时长：${lifestyle.napSummary}", marginLeft + 20f, y, normalPaint)
        y += lineHeight * 0.8f
        y = drawText(canvas, "日均步数：${lifestyle.stepsSummary}", marginLeft + 20f, y, normalPaint)
        y += lineHeight * 0.8f
        y = drawText(canvas, "受凉天数：${lifestyle.chillExposureDays}天", marginLeft + 20f, y, normalPaint)
        y += lineHeight * 0.8f

        if (lifestyle.menstrualSummary != null) {
            y = drawText(canvas, "经期情况：${lifestyle.menstrualSummary}", marginLeft + 20f, y, normalPaint)
            y += lineHeight * 0.8f
        }

        return y
    }

    private fun drawAiInsights(canvas: Canvas, startY: Float): Float {
        var y = startY
        val insights = reportData.aiInsightsSummary ?: return y

        y = drawText(canvas, "AI健康洞察（仅供参考）", marginLeft, y, sectionTitlePaint)
        y += lineHeight * 0.5f

        if (insights.weeklyInsights.isNotEmpty()) {
            y = drawText(canvas, "关键观察：", marginLeft + 20f, y, boldPaint)
            y += lineHeight * 0.8f
            insights.weeklyInsights.forEach { insight ->
                y = drawWrappedText(canvas, "• $insight", marginLeft + 40f, y, normalPaint, pageWidth - marginRight - 40f)
                y += lineHeight * 0.5f
            }
        }

        if (insights.topSuggestions.isNotEmpty()) {
            y += lineHeight * 0.3f
            y = drawText(canvas, "生活建议：", marginLeft + 20f, y, boldPaint)
            y += lineHeight * 0.8f
            insights.topSuggestions.forEach { suggestion ->
                y = drawWrappedText(canvas, "• $suggestion", marginLeft + 40f, y, normalPaint, pageWidth - marginRight - 40f)
                y += lineHeight * 0.5f
            }
        }

        return y
    }

    private fun drawDisclaimer(canvas: Canvas) {
        val disclaimerY = pageHeight - marginTop - lineHeight * 2
        canvas.drawLine(marginLeft, disclaimerY - 10f, pageWidth - marginRight, disclaimerY - 10f, smallPaint)
        drawWrappedText(canvas, reportData.patientInfo.disclaimer, marginLeft, disclaimerY, smallPaint, 
            pageWidth - marginLeft - marginRight)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint): Float {
        canvas.drawText(text, x, y, paint)
        return y + lineHeight
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, startY: Float, paint: Paint, maxWidth: Float): Float {
        var y = startY
        val words = text.split(" ")
        var line = ""

        words.forEach { word ->
            val testLine = if (line.isEmpty()) word else "$line $word"
            val width = paint.measureText(testLine)

            if (width > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, x, y, paint)
                y += lineHeight * 0.8f
                line = word
            } else {
                line = testLine
            }
        }

        if (line.isNotEmpty()) {
            canvas.drawText(line, x, y, paint)
            y += lineHeight * 0.8f
        }

        return y
    }
}
