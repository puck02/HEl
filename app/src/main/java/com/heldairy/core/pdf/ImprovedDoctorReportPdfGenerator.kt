package com.heldairy.core.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import com.heldairy.core.data.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 专业PDF健康洞察报告生成器 v5.0
 *
 * 全新专业设计，清晰可读，美观大方：
 *   P1 — 封面 + 健康概览仪表盘
 *   P2 — 症状详细分析（雷达图 + 趋势表 + 随访）
 *   P3 — 生活方式 + 用药情况
 *   P4 — AI 综合分析与健康建议
 *
 * 全矢量绘制，PDF 轻量，字体清晰。
 */
class ImprovedDoctorReportPdfGenerator(
    private val context: Context,
    private val reportData: DoctorReportData
) {
    // ==================== 尺寸常量 ====================
    private val pageWidth = 595
    private val pageHeight = 842
    private val marginH = 36f
    private val marginTop = 56f
    private val marginBottom = 50f
    private val contentWidth get() = pageWidth - 2 * marginH
    private val sectionGap = 18f

    // ==================== 专业调色板 ====================
    private val cPrimary    = Color.parseColor("#0D47A1")
    private val cPrimaryMed = Color.parseColor("#1976D2")
    private val cPrimaryLt  = Color.parseColor("#E8EAF6")
    private val cAccent     = Color.parseColor("#00897B")
    private val cSuccess    = Color.parseColor("#2E7D32")
    private val cSuccessLt  = Color.parseColor("#E8F5E9")
    private val cWarning    = Color.parseColor("#E65100")
    private val cWarningLt  = Color.parseColor("#FFF3E0")
    private val cError      = Color.parseColor("#C62828")
    private val cGray900    = Color.parseColor("#1A1A1A")
    private val cGray700    = Color.parseColor("#505050")
    private val cGray500    = Color.parseColor("#9E9E9E")
    private val cGray300    = Color.parseColor("#E0E0E0")
    private val cGray100    = Color.parseColor("#F5F5F5")
    private val cGray50     = Color.parseColor("#FAFAFA")
    private val cWhite      = Color.WHITE
    private val cHeaderBg   = Color.parseColor("#0D47A1")

    private val feelingColors = mapOf(
        "great" to Color.parseColor("#43A047"), "ok" to Color.parseColor("#1976D2"),
        "unwell" to Color.parseColor("#FB8C00"), "awful" to Color.parseColor("#E53935")
    )
    private val feelingLabels = mapOf(
        "great" to "很好", "ok" to "还行", "unwell" to "不太舒服", "awful" to "很难受"
    )
    private val radarDimensions = listOf(
        "headache_intensity" to "头痛", "neck_back_intensity" to "颈肩腰",
        "stomach_intensity" to "胃部", "nasal_intensity" to "鼻咽",
        "knee_intensity" to "膝盖", "mood_irritability" to "情绪"
    )

    // ==================== 画笔 ====================
    private val titlePaint       = tp(cWhite, 24f, true)
    private val subtitlePaint    = tp(cWhite, 12f, false)
    private val sectionPaint     = tp(cPrimary, 15f, true)
    private val bodyPaint        = tp(cGray900, 11f, false)
    private val bodyBold         = tp(cGray900, 11f, true)
    private val captionPaint     = tp(cGray500, 9f, false)
    private val smallBold        = tp(cGray700, 9.5f, true)
    private val valuePaint       = tp(cPrimary, 20f, true)
    private val tableHeaderPaint = tp(cWhite, 10f, true)
    private val tableCellPaint   = tp(cGray900, 10f, false)
    private val tableCellBold    = tp(cGray900, 10f, true)

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 0.6f; isAntiAlias = true; color = cGray300
    }
    private val fillPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }

    private fun tp(color: Int, size: Float, bold: Boolean) = TextPaint().apply {
        this.color = color; textSize = size; isAntiAlias = true; isSubpixelText = true
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    // ==================== 页面状态 ====================
    private var currentY = 0f
    private var currentPage: PdfDocument.Page? = null
    private var currentCanvas: Canvas? = null
    private var pageNumber = 1
    private val totalPages = 4
    private lateinit var pdfDoc: PdfDocument
    private fun canvas(): Canvas = currentCanvas ?: throw IllegalStateException("No canvas")

    // ==================== 入口 ====================
    fun generateToTempFile(): File {
        val tempFile = File(context.cacheDir, "health_report_${System.currentTimeMillis()}.pdf")
        pdfDoc = PdfDocument()
        try {
            pageNumber = 1
            startNewPage(); drawPage1CoverAndDashboard()
            startNewPage(); drawPage2SymptomAnalysis()
            startNewPage(); drawPage3LifestyleAndMedication()
            startNewPage(); drawPage4AiAnalysis()
            finishCurrentPage()
            FileOutputStream(tempFile).use { pdfDoc.writeTo(it) }
            return tempFile
        } finally { pdfDoc.close() }
    }

    // ==================== 页面管理 ====================
    private fun startNewPage() {
        if (currentPage != null) finishCurrentPage()
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        currentPage = pdfDoc.startPage(info)
        currentCanvas = currentPage?.canvas
        currentY = marginTop
        pageNumber++
    }

    private fun finishCurrentPage() {
        currentPage?.let { page ->
            drawPageFooter(page.canvas, pageNumber - 1)
            pdfDoc.finishPage(page)
        }
        currentPage = null; currentCanvas = null
    }

    private fun ensureSpace(h: Float) {
        if (currentY + h > pageHeight - marginBottom) startNewPage()
    }

    private fun drawPageFooter(c: Canvas, pageNum: Int) {
        val y = pageHeight - 20f
        val lp = Paint().apply { color = cGray300; strokeWidth = 0.5f; style = Paint.Style.STROKE; isAntiAlias = true }
        c.drawLine(marginH, y - 12f, pageWidth - marginH, y - 12f, lp)
        captionPaint.color = cGray500
        c.drawText("仅供参考，不构成诊断依据", marginH, y, captionPaint)
        val center = "健康洞察报告"
        c.drawText(center, (pageWidth - captionPaint.measureText(center)) / 2f, y, captionPaint)
        val right = "$pageNum / $totalPages"
        c.drawText(right, pageWidth - marginH - captionPaint.measureText(right), y, captionPaint)
    }

    // ╔═══════════════════════════════════════════════╗
    // ║  PAGE 1 — 封面 + 健康概览仪表盘                 ║
    // ╚═══════════════════════════════════════════════╝
    private fun drawPage1CoverAndDashboard() {
        val c = canvas()
        val info = reportData.patientInfo

        // 封面头部 — 深蓝渐变横幅
        fillPaint.color = cHeaderBg
        c.drawRect(0f, 0f, pageWidth.toFloat(), 120f, fillPaint)
        // 底部强调线
        fillPaint.color = cAccent
        c.drawRect(0f, 116f, pageWidth.toFloat(), 120f, fillPaint)

        titlePaint.color = cWhite
        c.drawText("健康洞察报告", marginH, 52f, titlePaint)
        val subPaint = tp(Color.parseColor("#B3D4FC"), 12f, false)
        c.drawText("${info.dataRangeStart}  至  ${info.dataRangeEnd}  ·  ${reportData.timeWindow}天分析周期", marginH, 74f, subPaint)
        val timePaint = tp(Color.parseColor("#90CAF9"), 9f, false)
        c.drawText("报告生成: ${info.reportGeneratedAt}", marginH, 94f, timePaint)

        currentY = 138f

        // 核心指标卡片
        drawDashboardCards(c)
        currentY += sectionGap

        // 整体感受分布
        drawSectionHeader("整体感受分布")
        drawOverallFeelingPie(c)
        currentY += sectionGap

        // 关键发现
        val imps = reportData.improvements
        val cons = reportData.concernPatterns
        if (imps.isNotEmpty() || cons.isNotEmpty()) {
            drawSectionHeader("关键发现")
            drawKeyFindings(c)
        }
    }

    private fun drawDashboardCards(c: Canvas) {
        val gap = 10f
        val cardW = (contentWidth - gap * 2) / 3f
        val cardH = 72f
        ensureSpace(cardH + 8f)

        val comp = reportData.dataCompleteness
        val adh = reportData.medicationSummary.adherence
        val adhTotal = adh.onTime + adh.missed
        val adhPct = if (adhTotal > 0) (adh.onTime * 100 / adhTotal) else 0
        val topFeeling = reportData.overallFeelingDistribution.maxByOrNull { it.value }?.key ?: "ok"
        val topFeelingLabel = feelingLabels[topFeeling] ?: "还行"

        data class CD(val title: String, val value: String, val sub: String, val color: Int)
        val cards = listOf(
            CD("填报率", "${(comp.completionRate * 100).toInt()}%", "${comp.filledDays}/${comp.totalDays}天",
                when { comp.completionRate >= 0.7 -> cSuccess; comp.completionRate >= 0.5 -> cWarning; else -> cError }),
            CD("用药依从", "${adhPct}%", "按时${adh.onTime} 遗漏${adh.missed}",
                when { adhPct >= 80 -> cSuccess; adhPct >= 50 -> cWarning; else -> cError }),
            CD("主要状态", topFeelingLabel, "最频繁感受", feelingColors[topFeeling] ?: cPrimary)
        )

        cards.forEachIndexed { i, card ->
            val x = marginH + i * (cardW + gap)
            drawMetricCard(c, x, currentY, cardW, cardH, card.title, card.value, card.sub, card.color)
        }
        currentY += cardH + 8f
    }

    private fun drawMetricCard(c: Canvas, x: Float, y: Float, w: Float, h: Float,
                               title: String, value: String, sub: String, accent: Int) {
        // 卡片阴影
        fillPaint.color = cGray100
        c.drawRoundRect(RectF(x + 1f, y + 1f, x + w + 1f, y + h + 1f), 6f, 6f, fillPaint)
        // 卡片背景
        fillPaint.color = cWhite
        c.drawRoundRect(RectF(x, y, x + w, y + h), 6f, 6f, fillPaint)
        // 顶部彩色条
        fillPaint.color = accent
        c.drawRoundRect(RectF(x, y, x + w, y + 4f), 6f, 6f, fillPaint)
        c.drawRect(RectF(x, y + 2f, x + w, y + 4f), fillPaint)

        val tp1 = tp(cGray700, 10f, false)
        c.drawText(title, x + 12f, y + 20f, tp1)
        valuePaint.color = accent
        c.drawText(value, x + 12f, y + 46f, valuePaint)
        valuePaint.color = cPrimary
        captionPaint.color = cGray500
        c.drawText(sub, x + 12f, y + 62f, captionPaint)
    }

    private fun drawOverallFeelingPie(c: Canvas) {
        val dist = reportData.overallFeelingDistribution
        val total = dist.values.sum().toFloat()
        if (total <= 0) { drawBodyText("暂无整体感受数据"); return }
        ensureSpace(110f)

        val cx = marginH + 65f; val cy = currentY + 50f; val radius = 45f
        var startAngle = -90f
        val rectF = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val order = listOf("great", "ok", "unwell", "awful")

        order.forEach { key ->
            val count = dist[key] ?: 0
            if (count > 0) {
                val sweep = count / total * 360f
                fillPaint.color = feelingColors[key] ?: cGray500
                c.drawArc(rectF, startAngle, sweep, true, fillPaint)
                startAngle += sweep
            }
        }
        // 中心空洞形成环形图
        fillPaint.color = cWhite
        c.drawCircle(cx, cy, radius * 0.52f, fillPaint)
        val centerPaint = tp(cGray900, 14f, true)
        val totalInt = total.toInt()
        val tw = centerPaint.measureText("${totalInt}天")
        c.drawText("${totalInt}天", cx - tw / 2, cy + 5f, centerPaint)

        // 图例 + 迷你进度条
        val legendX = marginH + 150f
        var legendY = currentY + 14f
        val barMaxW = 120f

        order.forEach { key ->
            val count = dist[key] ?: 0
            val pct = if (total > 0) count / total else 0f
            fillPaint.color = feelingColors[key] ?: cGray500
            c.drawRoundRect(RectF(legendX, legendY - 7f, legendX + 10f, legendY + 3f), 2f, 2f, fillPaint)

            bodyPaint.color = cGray900
            c.drawText("${feelingLabels[key]}  ${count}天 (${(pct * 100).toInt()}%)", legendX + 16f, legendY, bodyPaint)
            legendY += 6f

            // 迷你进度条
            fillPaint.color = cGray100
            c.drawRoundRect(RectF(legendX + 16f, legendY, legendX + 16f + barMaxW, legendY + 4f), 2f, 2f, fillPaint)
            fillPaint.color = feelingColors[key] ?: cGray500
            c.drawRoundRect(RectF(legendX + 16f, legendY, legendX + 16f + barMaxW * pct, legendY + 4f), 2f, 2f, fillPaint)
            legendY += 16f
        }
        currentY += 110f
    }

    private fun drawKeyFindings(c: Canvas) {
        reportData.improvements.forEach { text ->
            ensureSpace(20f)
            fillPaint.color = cSuccessLt
            c.drawRoundRect(RectF(marginH, currentY - 10f, marginH + contentWidth, currentY + 8f), 4f, 4f, fillPaint)
            fillPaint.color = cSuccess
            c.drawRoundRect(RectF(marginH, currentY - 10f, marginH + 3f, currentY + 8f), 2f, 2f, fillPaint)
            bodyPaint.color = cSuccess
            c.drawText("✓  $text", marginH + 10f, currentY + 2f, bodyPaint)
            bodyPaint.color = cGray900
            currentY += 18f
        }
        reportData.concernPatterns.forEach { text ->
            ensureSpace(20f)
            fillPaint.color = cWarningLt
            c.drawRoundRect(RectF(marginH, currentY - 10f, marginH + contentWidth, currentY + 8f), 4f, 4f, fillPaint)
            fillPaint.color = cWarning
            c.drawRoundRect(RectF(marginH, currentY - 10f, marginH + 3f, currentY + 8f), 2f, 2f, fillPaint)
            bodyPaint.color = cWarning
            c.drawText("⚠  $text", marginH + 10f, currentY + 2f, bodyPaint)
            bodyPaint.color = cGray900
            currentY += 18f
        }
    }

    // ╔═══════════════════════════════════════════════╗
    // ║  PAGE 2 — 症状详细分析                         ║
    // ╚═══════════════════════════════════════════════╝
    private fun drawPage2SymptomAnalysis() {
        drawPageTitle("症状详细分析")

        drawSectionHeader("症状总览 · 雷达图")
        drawRadarChart(canvas())
        currentY += sectionGap

        drawSectionHeader("趋势明细")
        drawTrendDetailTable(canvas())
        currentY += sectionGap

        if (reportData.anomalies.isNotEmpty()) {
            drawSectionHeader("异常记录")
            drawAnomalies(canvas())
            currentY += sectionGap
        }
        if (reportData.followUpSummary.isNotEmpty()) {
            drawSectionHeader("随访细节")
            drawFollowUpSummary(canvas())
        }
    }

    private fun drawRadarChart(c: Canvas) {
        val n = radarDimensions.size
        val radius = 80f
        val cx = pageWidth / 2f
        val cy = currentY + radius + 28f
        ensureSpace(radius * 2 + 65f)

        val currentValues = radarDimensions.map { (qId, _) ->
            reportData.symptomSummary.metrics.firstOrNull { it.questionId == qId }?.average ?: 0.0
        }

        // 如果7天以内且有周环比数据，计算上周近似值
        val lastWeekValues: List<Double>? =
            if (reportData.timeWindow <= 7 && reportData.weekOverWeekChange != null) {
                radarDimensions.mapIndexed { idx, (qId, _) ->
                    val cur = currentValues[idx]
                    val pctChange = reportData.weekOverWeekChange!![qId] ?: 0f
                    if (pctChange != 0f && cur != 0.0) cur / (1 + pctChange / 100f) else cur
                }
            } else null

        // 网格
        val gridPaint = Paint().apply { color = cGray300; style = Paint.Style.STROKE; strokeWidth = 0.5f; isAntiAlias = true }
        for (level in 1..3) {
            val r = radius * level / 3f
            val path = Path()
            for (i in 0 until n) {
                val pt = radarPoint(cx, cy, r, i, n)
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            path.close()
            if (level == 3) {
                val bg = Paint().apply { color = Color.argb(15, 0, 0, 0); style = Paint.Style.FILL; isAntiAlias = true }
                c.drawPath(path, bg)
            }
            c.drawPath(path, gridPaint)
        }
        // 刻度标签
        for (level in 1..3) {
            c.drawText(String.format("%.0f", level * 10.0 / 3), cx + 3f, cy - radius * level / 3f + 10f, captionPaint)
        }

        // 轴线
        val axisPaint = Paint().apply { color = cGray300; style = Paint.Style.STROKE; strokeWidth = 0.4f; isAntiAlias = true }
        for (i in 0 until n) {
            val pt = radarPoint(cx, cy, radius, i, n)
            c.drawLine(cx, cy, pt.x, pt.y, axisPaint)
        }

        // 上周数据 (虚线)
        if (lastWeekValues != null) {
            val dashPaint = Paint().apply {
                color = cGray500; style = Paint.Style.STROKE; strokeWidth = 1.2f
                isAntiAlias = true; pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
            }
            val lastPath = Path()
            lastWeekValues.forEachIndexed { i, v ->
                val r = (v / 10.0).toFloat().coerceIn(0f, 1f) * radius
                val pt = radarPoint(cx, cy, r, i, n)
                if (i == 0) lastPath.moveTo(pt.x, pt.y) else lastPath.lineTo(pt.x, pt.y)
            }
            lastPath.close()
            c.drawPath(lastPath, dashPaint)
        }

        // 本周数据 (填充+描边)
        val dataPath = Path()
        currentValues.forEachIndexed { i, v ->
            val r = (v / 10.0).toFloat().coerceIn(0f, 1f) * radius
            val pt = radarPoint(cx, cy, r, i, n)
            if (i == 0) dataPath.moveTo(pt.x, pt.y) else dataPath.lineTo(pt.x, pt.y)
        }
        dataPath.close()
        c.drawPath(dataPath, Paint().apply { color = Color.argb(40, 25, 118, 210); style = Paint.Style.FILL; isAntiAlias = true })
        c.drawPath(dataPath, Paint().apply { color = cPrimaryMed; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true })

        // 维度标签 + 数据点
        val labelPaint = tp(cGray900, 10.5f, true)
        radarDimensions.forEachIndexed { i, (_, label) ->
            val pt = radarPoint(cx, cy, radius + 22f, i, n)
            val valStr = String.format("%.1f", currentValues[i])
            val fullLabel = "$label $valStr"
            val textW = labelPaint.measureText(fullLabel)
            val textX = when {
                pt.x < cx - 10 -> pt.x - textW
                pt.x > cx + 10 -> pt.x
                else -> pt.x - textW / 2
            }
            c.drawText(fullLabel, textX, pt.y + 4f, labelPaint)

            // 数据点
            val dataR = (currentValues[i] / 10.0).toFloat().coerceIn(0f, 1f) * radius
            val dataPt = radarPoint(cx, cy, dataR, i, n)
            fillPaint.color = severityColor(currentValues[i].toFloat())
            c.drawCircle(dataPt.x, dataPt.y, 3.5f, fillPaint)
        }

        // 图例
        if (lastWeekValues != null) {
            val legendY = cy + radius + 20f
            fillPaint.color = cPrimaryMed
            c.drawRoundRect(RectF(cx - 60f, legendY - 5f, cx - 48f, legendY + 2f), 2f, 2f, fillPaint)
            c.drawText("本周", cx - 44f, legendY, captionPaint)
            val dl = Paint().apply {
                color = cGray500; style = Paint.Style.STROKE; strokeWidth = 1.2f
                pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f); isAntiAlias = true
            }
            c.drawLine(cx + 10f, legendY - 2f, cx + 22f, legendY - 2f, dl)
            c.drawText("上周", cx + 26f, legendY, captionPaint)
            currentY = legendY + 12f
        } else {
            currentY = cy + radius + 16f
        }
    }

    private fun radarPoint(cx: Float, cy: Float, r: Float, index: Int, total: Int): PointF {
        val angle = -PI / 2 + 2 * PI * index / total
        return PointF(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
    }

    private fun drawTrendDetailTable(c: Canvas) {
        val metrics = reportData.symptomSummary.metrics
        if (metrics.isEmpty()) { drawBodyText("暂无症状数据"); return }

        val cols = floatArrayOf(85f, 55f, 55f, 45f, contentWidth - 240f)
        val headers = listOf("症状", "均值", "最近值", "趋势", "分析")
        val startX = marginH

        // 表头
        ensureSpace(24f)
        fillPaint.color = cPrimary
        c.drawRoundRect(RectF(startX, currentY - 12f, startX + contentWidth, currentY + 6f), 4f, 4f, fillPaint)
        var hx = startX
        headers.forEachIndexed { i, h ->
            c.drawText(h, hx + 6f, currentY, tableHeaderPaint)
            hx += cols[i]
        }
        currentY += 12f

        // 数据行
        metrics.forEachIndexed { idx, m ->
            ensureSpace(22f)
            if (idx % 2 == 0) {
                fillPaint.color = cGray50
                c.drawRect(startX, currentY - 4f, startX + contentWidth, currentY + 14f, fillPaint)
            }
            val trend = reportData.detailedTrends[m.questionId]
            var x = startX

            c.drawText(m.symptomName, x + 6f, currentY + 6f, tableCellBold); x += cols[0]
            tableCellBold.color = severityColor(m.average.toFloat())
            c.drawText(String.format("%.1f", m.average), x + 6f, currentY + 6f, tableCellBold)
            tableCellBold.color = cGray900; x += cols[1]

            val lv = m.latestValue ?: m.average
            tableCellPaint.color = severityColor(lv.toFloat())
            c.drawText(String.format("%.1f", lv), x + 6f, currentY + 6f, tableCellPaint)
            tableCellPaint.color = cGray900; x += cols[2]

            val (arrow, trendColor) = trendArrowColor(m.trend)
            tableCellBold.color = trendColor
            c.drawText(arrow, x + 6f, currentY + 6f, tableCellBold)
            tableCellBold.color = cGray900; x += cols[3]

            val desc = trend?.description ?: m.trendDescription
            c.drawText(desc, x + 6f, currentY + 6f, captionPaint)
            currentY += 20f
        }
        linePaint.color = cGray300
        c.drawLine(startX, currentY, startX + contentWidth, currentY, linePaint)
    }

    private fun drawAnomalies(c: Canvas) {
        reportData.anomalies.take(5).forEach { a ->
            ensureSpace(24f)
            val color = when (a.severity) {
                AnomalySeverity.SEVERE -> cError
                AnomalySeverity.MODERATE -> cWarning
                AnomalySeverity.MILD -> Color.parseColor("#FBC02D")
            }
            fillPaint.color = color
            c.drawCircle(marginH + 8f, currentY - 3f, 4f, fillPaint)
            val metricName = radarDimensions.firstOrNull { it.first == a.metric }?.second ?: a.metric
            bodyPaint.color = cGray900
            c.drawText("${a.date}  $metricName ${String.format("%.1f", a.value)}（期望 ${a.expectedRange}）",
                marginH + 20f, currentY, bodyPaint)
            currentY += 16f
        }
    }

    private fun drawFollowUpSummary(c: Canvas) {
        reportData.followUpSummary.forEach { (symptom, labels) ->
            ensureSpace(30f)
            bodyBold.color = cPrimaryMed
            c.drawText("$symptom:", marginH + 4f, currentY, bodyBold)
            bodyBold.color = cGray900
            currentY += 14f
            drawMultilineText(labels.joinToString("、"), marginH + 16f, (contentWidth - 24f).toInt(), bodyPaint)
            currentY += 6f
        }
    }

    // ╔═══════════════════════════════════════════════╗
    // ║  PAGE 3 — 生活方式 + 用药情况                   ║
    // ╚═══════════════════════════════════════════════╝
    private fun drawPage3LifestyleAndMedication() {
        drawPageTitle("生活方式与用药")
        drawLifestyleSection(canvas())
        currentY += sectionGap
        drawMedicationSection(canvas())
    }

    private fun drawLifestyleSection(c: Canvas) {
        drawSectionHeader("生活方式分析")
        val ls = reportData.lifestyleSummary

        drawProStackedBar(c, "睡眠时长", ls.sleepDistribution,
            mapOf("lt6" to cError, "6_7" to cWarning, "7_8" to cSuccess, "gt8" to cPrimaryMed),
            mapOf("lt6" to "<6h", "6_7" to "6-7h", "7_8" to "7-8h", "gt8" to ">8h"))

        drawProStackedBar(c, "午休情况", ls.napDistribution,
            mapOf("none" to cGray500, "lt30" to cPrimaryMed, "30_60" to cSuccess, "gt60" to cWarning),
            mapOf("none" to "无", "lt30" to "<30m", "30_60" to "30-60m", "gt60" to ">60m"))

        drawProStackedBar(c, "运动步数", ls.stepsDistribution,
            mapOf("lt3k" to cError, "3_6k" to cWarning, "6_10k" to cSuccess, "gt10k" to cPrimaryMed),
            mapOf("lt3k" to "<3k", "3_6k" to "3-6k", "6_10k" to "6-10k", "gt10k" to ">10k"))

        currentY += 6f
        if (ls.chillExposureDays > 0) {
            ensureSpace(18f)
            c.drawText("受凉天数: ${ls.chillExposureDays}天", marginH, currentY, bodyPaint)
            currentY += 16f
        }
        if (!ls.menstrualSummary.isNullOrBlank()) {
            ensureSpace(18f)
            c.drawText("经期状态: ${ls.menstrualSummary}", marginH, currentY, bodyPaint)
            currentY += 16f
        }
    }

    private fun drawProStackedBar(c: Canvas, label: String, dist: Map<String, Int>,
                                  colors: Map<String, Int>, labels: Map<String, String>) {
        val total = dist.values.sum().toFloat()
        if (total <= 0) return
        ensureSpace(48f)

        bodyBold.color = cGray900
        c.drawText(label, marginH, currentY, bodyBold)
        currentY += 12f

        val barX = marginH; val barW = contentWidth; val barH = 16f
        var x = barX
        val order = labels.keys.toList()

        // 圆角背景
        fillPaint.color = cGray100
        c.drawRoundRect(RectF(barX, currentY, barX + barW, currentY + barH), 8f, 8f, fillPaint)

        // 使用clipPath实现圆角堆叠
        c.save()
        val clipPath = Path().apply {
            addRoundRect(RectF(barX, currentY, barX + barW, currentY + barH), 8f, 8f, Path.Direction.CW)
        }
        c.clipPath(clipPath)

        order.forEach { key ->
            val count = dist[key] ?: 0
            if (count <= 0) return@forEach
            val segW = count / total * barW
            fillPaint.color = colors[key] ?: cGray500
            c.drawRect(x, currentY, x + segW, currentY + barH, fillPaint)
            // 在足够宽的段内显示百分比
            if (count / total > 0.12f) {
                val pctText = "${(count / total * 100).toInt()}%"
                val ptw = captionPaint.measureText(pctText)
                if (ptw + 6 < segW) {
                    val wp = tp(cWhite, 9f, true)
                    c.drawText(pctText, x + (segW - ptw) / 2, currentY + barH - 4f, wp)
                }
            }
            x += segW
        }
        c.restore()
        currentY += barH + 4f

        // 图例
        var lx = marginH
        order.forEach { key ->
            val count = dist[key] ?: 0
            if (count <= 0) return@forEach
            fillPaint.color = colors[key] ?: cGray500
            c.drawCircle(lx + 4f, currentY - 1f, 3.5f, fillPaint)
            val lbl = "${labels[key]} $count"
            captionPaint.color = cGray700
            c.drawText(lbl, lx + 11f, currentY + 2f, captionPaint)
            lx += captionPaint.measureText(lbl) + 20f
        }
        captionPaint.color = cGray500
        currentY += 14f
    }

    private fun drawMedicationSection(c: Canvas) {
        drawSectionHeader("用药情况")
        val summary = reportData.medicationSummary

        if (summary.activeMedications.isEmpty() && summary.events.isEmpty()) {
            drawBodyText("当前无活跃用药记录")
            return
        }

        if (summary.activeMedications.isNotEmpty()) {
            ensureSpace(22f)
            bodyBold.color = cPrimaryMed
            c.drawText("当前用药", marginH, currentY, bodyBold)
            bodyBold.color = cGray900
            currentY += 14f

            val cols = floatArrayOf(90f, 70f, 80f, 80f, contentWidth - 320f)
            ensureSpace(22f)
            fillPaint.color = cPrimary
            c.drawRoundRect(RectF(marginH, currentY - 11f, marginH + contentWidth, currentY + 5f), 4f, 4f, fillPaint)
            var hx = marginH
            listOf("药名", "剂量", "频次", "开始日期", "服药时段").forEachIndexed { i, h ->
                c.drawText(h, hx + 6f, currentY, tableHeaderPaint)
                hx += cols[i]
            }
            currentY += 12f

            summary.activeMedications.forEachIndexed { idx, med ->
                ensureSpace(18f)
                if (idx % 2 == 0) {
                    fillPaint.color = cGray50
                    c.drawRect(marginH, currentY - 8f, marginH + contentWidth, currentY + 6f, fillPaint)
                }
                var x = marginH
                c.drawText(med.name, x + 6f, currentY, tableCellPaint); x += cols[0]
                c.drawText(med.dosage ?: "-", x + 6f, currentY, tableCellPaint); x += cols[1]
                c.drawText(med.frequency, x + 6f, currentY, tableCellPaint); x += cols[2]
                c.drawText(med.startDate, x + 6f, currentY, tableCellPaint); x += cols[3]
                c.drawText(med.timeHints ?: "-", x + 6f, currentY, tableCellPaint)
                currentY += 16f
            }
            currentY += 8f
        }

        // 依从性摘要
        ensureSpace(24f)
        val adh = summary.adherence
        bodyBold.color = cGray900
        c.drawText("用药依从: ", marginH, currentY, bodyBold)
        c.drawText("按时 ${adh.onTime}天  |  遗漏 ${adh.missed}天  |  无需 ${adh.na}天",
            marginH + 68f, currentY, bodyPaint)
        currentY += 16f

        // 用药变更
        if (summary.events.isNotEmpty()) {
            ensureSpace(24f)
            bodyBold.color = cPrimaryMed
            c.drawText("用药变更记录", marginH, currentY, bodyBold)
            bodyBold.color = cGray900
            currentY += 14f

            summary.events.take(6).forEach { evt ->
                ensureSpace(16f)
                captionPaint.color = cGray500
                c.drawText("${evt.date} ${evt.time}", marginH + 8f, currentY, captionPaint)
                c.drawText(evt.description, marginH + 100f, currentY, bodyPaint)
                currentY += 14f
            }
            if (summary.events.size > 6) {
                c.drawText("... 共 ${summary.events.size} 条", marginH + 8f, currentY, captionPaint)
                currentY += 14f
            }
        }
    }

    // ╔═══════════════════════════════════════════════╗
    // ║  PAGE 4 — AI 综合健康分析                      ║
    // ╚═══════════════════════════════════════════════╝
    private fun drawPage4AiAnalysis() {
        val c = canvas()
        drawPageTitle("AI 综合健康分析")

        val ai = reportData.aiInsightsSummary
        val hasAi = ai != null && ai.summary.isNotBlank()

        // 1. 总评 — 蓝色引用块
        drawSectionHeader("分析总评")
        val summaryText = if (hasAi) ai!!.summary else buildContextAnalysis()
        ensureSpace(60f)
        val summaryLayout = StaticLayout.Builder.obtain(summaryText, 0, summaryText.length, bodyPaint, (contentWidth - 28f).toInt()).build()
        val blockH = summaryLayout.height + 16f
        ensureSpace(blockH + 4f)
        fillPaint.color = cPrimaryLt
        c.drawRoundRect(RectF(marginH, currentY - 4f, marginH + contentWidth, currentY + blockH), 6f, 6f, fillPaint)
        fillPaint.color = cPrimary
        c.drawRoundRect(RectF(marginH, currentY - 4f, marginH + 4f, currentY + blockH), 4f, 4f, fillPaint)
        c.save(); c.translate(marginH + 14f, currentY + 4f); summaryLayout.draw(c); c.restore()
        currentY += blockH + 10f

        // 2. 要点发现 — 编号圆圈
        val highlights = if (hasAi) ai!!.weeklyInsights else emptyList()
        if (highlights.isNotEmpty()) {
            drawSectionHeader("要点发现")
            highlights.forEachIndexed { i, text ->
                ensureSpace(22f)
                fillPaint.color = cPrimaryMed
                c.drawCircle(marginH + 8f, currentY - 3f, 8f, fillPaint)
                val numPaint = tp(cWhite, 9f, true)
                val numStr = "${i + 1}"
                c.drawText(numStr, marginH + 8f - numPaint.measureText(numStr) / 2, currentY + 1f, numPaint)
                drawMultilineText(text, marginH + 22f, (contentWidth - 28f).toInt(), bodyPaint)
                currentY += 4f
            }
            currentY += 8f
        }

        // 3. 生活建议 — 绿色块
        val suggestions = if (hasAi) ai!!.topSuggestions else buildDefaultSuggestions()
        if (suggestions.isNotEmpty()) {
            drawSectionHeader("生活建议")
            suggestions.forEach { text ->
                ensureSpace(24f)
                fillPaint.color = cSuccessLt
                c.drawRoundRect(RectF(marginH, currentY - 10f, marginH + contentWidth, currentY + 10f), 4f, 4f, fillPaint)
                fillPaint.color = cSuccess
                c.drawRoundRect(RectF(marginH, currentY - 10f, marginH + 3f, currentY + 10f), 2f, 2f, fillPaint)
                bodyPaint.color = cSuccess
                c.drawText("✦", marginH + 10f, currentY + 2f, bodyPaint)
                bodyPaint.color = cGray900
                drawMultilineText(text, marginH + 24f, (contentWidth - 30f).toInt(), bodyPaint)
                currentY += 6f
            }
            currentY += 8f
        }

        // 4. 健康警示 — 橙色块
        val cautions = if (hasAi) ai!!.cautions else buildDefaultCautions()
        if (cautions.isNotEmpty()) {
            drawSectionHeader("健康警示")
            cautions.forEach { text ->
                ensureSpace(24f)
                fillPaint.color = cWarningLt
                c.drawRoundRect(RectF(marginH, currentY - 10f, marginH + contentWidth, currentY + 10f), 4f, 4f, fillPaint)
                fillPaint.color = cWarning
                c.drawRoundRect(RectF(marginH, currentY - 10f, marginH + 3f, currentY + 10f), 2f, 2f, fillPaint)
                bodyPaint.color = cWarning
                c.drawText("⚠", marginH + 10f, currentY + 2f, bodyPaint)
                bodyPaint.color = cGray900
                drawMultilineText(text, marginH + 24f, (contentWidth - 30f).toInt(), bodyPaint)
                currentY += 6f
            }
            currentY += 8f
        }

        // 5. 置信度
        if (hasAi) {
            ensureSpace(24f)
            captionPaint.color = cGray700
            c.drawText("分析置信度:", marginH, currentY, captionPaint)
            drawConfidenceBadge(c, marginH + 72f, currentY - 10f, ai!!.confidence)
            currentY += sectionGap
        }

        // 6. 免责声明
        drawDisclaimer(c)
    }

    /** 无AI时，基于数据自动生成分析总结 */
    private fun buildContextAnalysis(): String {
        val sb = StringBuilder()
        val highSev = reportData.symptomSummary.metrics.filter { it.average > 6.0 }
        val rising = reportData.symptomSummary.metrics.filter { it.trend == TrendFlag.rising }
        val falling = reportData.symptomSummary.metrics.filter { it.trend == TrendFlag.falling }

        if (highSev.isNotEmpty())
            sb.append("${highSev.joinToString("、") { it.symptomName }}症状较为明显（均值>6），建议重点关注。")
        if (rising.isNotEmpty())
            sb.append("${rising.joinToString("、") { it.symptomName }}呈上升趋势，需持续观察。")
        if (falling.isNotEmpty())
            sb.append("${falling.joinToString("、") { it.symptomName }}有所好转，可继续保持当前方案。")
        val adh = reportData.medicationSummary.adherence
        if (adh.missed > adh.onTime && adh.onTime + adh.missed > 0)
            sb.append("用药依从性有待提高，建议设置用药提醒。")
        val comp = reportData.dataCompleteness
        if (comp.completionRate < 0.5)
            sb.append("数据记录完整度较低（${(comp.completionRate * 100).toInt()}%），坚持每日记录有助于更准确的分析。")
        if (sb.isEmpty())
            sb.append("本周期健康状况总体稳定，各项指标均在正常范围内，请继续保持良好的生活习惯。")
        return sb.toString()
    }

    /** 无AI时，基于生活数据生成建议 */
    private fun buildDefaultSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        val ls = reportData.lifestyleSummary
        val shortSleep = (ls.sleepDistribution["lt6"] ?: 0) + (ls.sleepDistribution["6_7"] ?: 0)
        val totalSleep = ls.sleepDistribution.values.sum()
        if (totalSleep > 0 && shortSleep.toFloat() / totalSleep > 0.3f)
            suggestions.add("建议保持7-8小时的充足睡眠，睡前避免使用电子设备")
        val lowSteps = ls.stepsDistribution["lt3k"] ?: 0
        val totalSteps = ls.stepsDistribution.values.sum()
        if (totalSteps > 0 && lowSteps.toFloat() / totalSteps > 0.3f)
            suggestions.add("日均步数偏少，建议每天至少进行30分钟的中等强度运动")
        reportData.symptomSummary.metrics.filter { it.average > 5.0 }.firstOrNull()?.let {
            suggestions.add("${it.symptomName}持续偏高，建议关注诱发因素并考虑就医咨询")
        }
        if (suggestions.isEmpty())
            suggestions.add("继续保持当前良好的生活习惯，定期记录健康数据")
        return suggestions
    }

    /** 无AI时，基于症状数据生成警示 */
    private fun buildDefaultCautions(): List<String> {
        val cautions = mutableListOf<String>()
        reportData.symptomSummary.metrics.filter { it.average > 7.0 }.forEach {
            cautions.add("${it.symptomName}均值达${String.format("%.1f", it.average)}，需要密切关注")
        }
        reportData.symptomSummary.metrics.filter { it.trend == TrendFlag.rising && it.average > 4.0 }.forEach {
            cautions.add("${it.symptomName}呈上升趋势，建议排查可能的诱因")
        }
        return cautions
    }

    private fun drawConfidenceBadge(c: Canvas, x: Float, y: Float, level: String) {
        val (color, label) = when (level.lowercase()) {
            "high" -> cSuccess to "高"
            "medium" -> cWarning to "中"
            else -> cGray500 to "低"
        }
        fillPaint.color = color
        val tw = smallBold.measureText(label)
        c.drawRoundRect(RectF(x, y, x + tw + 18f, y + 18f), 9f, 9f, fillPaint)
        smallBold.color = cWhite
        c.drawText(label, x + 9f, y + 13f, smallBold)
        smallBold.color = cGray700
    }

    private fun drawDisclaimer(c: Canvas) {
        ensureSpace(50f)
        linePaint.color = cGray300
        linePaint.strokeWidth = 0.5f
        c.drawLine(marginH, currentY, marginH + contentWidth, currentY, linePaint)
        currentY += 12f

        fillPaint.color = Color.parseColor("#FFF8E1")
        c.drawRoundRect(RectF(marginH, currentY - 4f, marginH + contentWidth, currentY + 32f), 4f, 4f, fillPaint)
        fillPaint.color = cWarning
        c.drawRoundRect(RectF(marginH, currentY - 4f, marginH + 3f, currentY + 32f), 2f, 2f, fillPaint)

        val dp = tp(cGray700, 8.5f, false)
        c.drawText("⚠ 本报告由健康日记APP自动生成，仅供医生参考，不构成医疗诊断依据。", marginH + 10f, currentY + 10f, dp)
        c.drawText("请以医生的专业判断为准，所有AI分析内容均为辅助参考信息。", marginH + 10f, currentY + 24f, dp)
        currentY += 40f
    }

    // ==================== 通用绘制工具 ====================
    private fun drawPageTitle(title: String) {
        val c = canvas()
        fillPaint.color = cPrimaryLt
        c.drawRect(marginH, currentY - 10f, marginH + contentWidth, currentY + 12f, fillPaint)
        fillPaint.color = cPrimary
        c.drawRect(marginH, currentY - 10f, marginH + 4f, currentY + 12f, fillPaint)
        c.drawText(title, marginH + 12f, currentY + 5f, tp(cPrimary, 14f, true))
        currentY += 24f
    }

    private fun drawSectionHeader(text: String) {
        ensureSpace(24f)
        val c = canvas()
        fillPaint.color = cPrimary
        c.drawRoundRect(RectF(marginH, currentY - 10f, marginH + 4f, currentY + 4f), 2f, 2f, fillPaint)
        sectionPaint.color = cPrimary
        c.drawText(text, marginH + 10f, currentY, sectionPaint)
        currentY += 16f
    }

    private fun drawBodyText(text: String) {
        ensureSpace(16f)
        bodyPaint.color = cGray900
        canvas().drawText(text, marginH + 4f, currentY, bodyPaint)
        currentY += 14f
    }

    private fun drawMultilineText(text: String, x: Float, maxWidth: Int, paint: TextPaint) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth).build()
        if (currentY + layout.height > pageHeight - marginBottom) startNewPage()
        val c = canvas()
        c.save()
        c.translate(x, currentY)
        layout.draw(c)
        c.restore()
        currentY += layout.height
    }

    private fun severityColor(v: Float) = when {
        v < 3f -> cSuccess
        v < 7f -> cWarning
        else -> cError
    }

    private fun trendArrowColor(trend: TrendFlag) = when (trend) {
        TrendFlag.rising -> "↑" to cError
        TrendFlag.falling -> "↓" to cSuccess
        TrendFlag.stable -> "→" to cGray700
    }
}
