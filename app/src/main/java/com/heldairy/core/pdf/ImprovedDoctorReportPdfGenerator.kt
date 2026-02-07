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
import kotlin.math.min
import kotlin.math.sin

/**
 * 专业PDF健康洞察报告生成器 v4.0
 *
 * 4 页专业布局：
 *   P1 — 封面与概览仪表盘（状态卡片 + 饼图 + 关键发现）
 *   P2 — 症状雷达图与深度分析（雷达图 + 趋势 + 异常 + 随访）
 *   P3 — 用药与生活方式（药物表 + 堆叠色条）
 *   P4 — AI 健康分析（总评 + 要点 + 建议 + 警示 + 免责声明）
 *
 * 全矢量绘制（Path/Arc），不含位图，PDF 轻量。
 */
class ImprovedDoctorReportPdfGenerator(
    private val context: Context,
    private val reportData: DoctorReportData
) {
    // ==================== 尺寸常量 ====================
    private val pageWidth = 595
    private val pageHeight = 842
    private val marginH = 40f
    private val marginTop = 48f
    private val marginBottom = 48f
    private val contentWidth get() = pageWidth - 2 * marginH
    private val sectionGap = 22f

    // ==================== 调色板 ====================
    private val cPrimary   = Color.parseColor("#1565C0")
    private val cPrimaryLt = Color.parseColor("#E3F2FD")
    private val cSuccess   = Color.parseColor("#2E7D32")
    private val cSuccessLt = Color.parseColor("#E8F5E9")
    private val cWarning   = Color.parseColor("#E65100")
    private val cWarningLt = Color.parseColor("#FFF3E0")
    private val cError     = Color.parseColor("#C62828")
    private val cErrorLt   = Color.parseColor("#FFEBEE")
    private val cGray900   = Color.parseColor("#212121")
    private val cGray700   = Color.parseColor("#616161")
    private val cGray500   = Color.parseColor("#9E9E9E")
    private val cGray300   = Color.parseColor("#E0E0E0")
    private val cGray100   = Color.parseColor("#F5F5F5")
    private val cWhite     = Color.WHITE

    // 饼图颜色 (overall_feeling)
    private val feelingColors = mapOf(
        "great"  to Color.parseColor("#43A047"),
        "ok"     to Color.parseColor("#1E88E5"),
        "unwell" to Color.parseColor("#FB8C00"),
        "awful"  to Color.parseColor("#E53935")
    )
    private val feelingLabels = mapOf(
        "great" to "很好", "ok" to "还行", "unwell" to "不太舒服", "awful" to "很难受"
    )

    // 雷达图维度（6 个症状，值越高越不好）
    private val radarDimensions = listOf(
        "headache_intensity"  to "头痛",
        "neck_back_intensity" to "颈肩腰",
        "stomach_intensity"   to "胃部",
        "nasal_intensity"     to "鼻咽",
        "knee_intensity"      to "膝盖",
        "mood_irritability"   to "情绪"
    )

    // ==================== 画笔 ====================
    private val titlePaint = tp(cPrimary, 22f, true)
    private val subtitlePaint = tp(cGray700, 11f, false)
    private val sectionPaint = tp(cGray900, 14f, true)
    private val bodyPaint = tp(cGray900, 10f, false)
    private val bodyBold = tp(cGray900, 10f, true)
    private val captionPaint = tp(cGray500, 9f, false)
    private val smallBold = tp(cGray900, 9f, true)
    private val valuePaint = tp(cPrimary, 18f, true)

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true; color = cGray300
    }
    private val fillPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }

    private fun tp(color: Int, size: Float, bold: Boolean) = TextPaint().apply {
        this.color = color; textSize = size; isAntiAlias = true
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    // ==================== 页面状态 ====================
    private var currentY = 0f
    private var currentPage: PdfDocument.Page? = null
    private var currentCanvas: Canvas? = null
    private var pageNumber = 1
    private lateinit var pdfDoc: PdfDocument

    private fun canvas(): Canvas = currentCanvas
        ?: throw IllegalStateException("Canvas not available")

    // ==================== 入口 ====================
    fun generateToTempFile(): File {
        val tempFile = File(context.cacheDir, "health_report_${System.currentTimeMillis()}.pdf")
        pdfDoc = PdfDocument()
        try {
            pageNumber = 1
            // ---- Page 1: 封面与概览 ----
            startNewPage()
            drawPage1CoverAndDashboard()
            // ---- Page 2: 症状雷达图与分析 ----
            startNewPage()
            drawPage2SymptomAnalysis()
            // ---- Page 3: 用药与生活方式 ----
            startNewPage()
            drawPage3MedicationAndLifestyle()
            // ---- Page 4: AI 健康分析 ----
            startNewPage()
            drawPage4AiInsights()

            finishCurrentPage()
            FileOutputStream(tempFile).use { pdfDoc.writeTo(it) }
            return tempFile
        } finally {
            pdfDoc.close()
        }
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
            drawPageHeader(page.canvas)
            drawPageFooter(page.canvas, pageNumber - 1)
            pdfDoc.finishPage(page)
        }
        currentPage = null; currentCanvas = null
    }

    private fun ensureSpace(h: Float) {
        if (currentY + h > pageHeight - marginBottom) startNewPage()
    }

    // ==================== 页眉 / 页脚 ====================
    private fun drawPageHeader(c: Canvas) {
        val info = reportData.patientInfo
        val headerText = "健康洞察报告 · ${info.dataRangeStart} ~ ${info.dataRangeEnd}"
        c.drawText(headerText, marginH, 28f, captionPaint)
        val lp = Paint().apply {
            color = cGray300; strokeWidth = 0.5f; style = Paint.Style.STROKE; isAntiAlias = true
        }
        c.drawLine(marginH, 34f, pageWidth - marginH, 34f, lp)
    }

    private fun drawPageFooter(c: Canvas, pageNum: Int) {
        val y = pageHeight - 24f
        val lp = Paint().apply {
            color = cGray300; strokeWidth = 0.5f; style = Paint.Style.STROKE; isAntiAlias = true
        }
        c.drawLine(marginH, y - 8f, pageWidth - marginH, y - 8f, lp)
        val left = "仅供参考，不构成诊断依据"
        c.drawText(left, marginH, y, captionPaint)
        val right = "第 $pageNum 页"
        val rw = captionPaint.measureText(right)
        c.drawText(right, pageWidth - marginH - rw, y, captionPaint)
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  PAGE 1 — 封面与概览仪表盘                                ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun drawPage1CoverAndDashboard() {
        val c = canvas()
        val info = reportData.patientInfo

        // ---- 标题区 ----
        c.drawText("健康洞察报告", marginH, currentY, titlePaint)
        currentY += 28f
        c.drawText(
            "${info.dataRangeStart}  至  ${info.dataRangeEnd}  ·  ${reportData.timeWindow}天",
            marginH, currentY, subtitlePaint
        )
        currentY += 14f
        c.drawText("生成时间: ${info.reportGeneratedAt}", marginH, currentY, captionPaint)
        currentY += 20f

        // 粗装饰线
        fillPaint.color = cPrimary
        c.drawRect(marginH, currentY, marginH + contentWidth, currentY + 3f, fillPaint)
        currentY += 16f

        // ---- 三卡片仪表盘 ----
        drawDashboardCards(c)
        currentY += sectionGap

        // ---- 整体感受饼图 ----
        drawSectionTitle("整体感受分布")
        drawOverallFeelingPie(c)
        currentY += sectionGap

        // ---- 心情 & 能量条 ----
        drawMoodEnergyBars(c)
        currentY += sectionGap

        // ---- 关键发现 ----
        drawKeyFindings(c)
    }

    /** 3 张状态卡片横排 */
    private fun drawDashboardCards(c: Canvas) {
        val cardW = (contentWidth - 16f) / 3f
        val cardH = 70f
        ensureSpace(cardH + 8f)

        val comp = reportData.dataCompleteness
        val adh = reportData.medicationSummary.adherence
        val adhTotal = adh.onTime + adh.missed
        val adhPct = if (adhTotal > 0) (adh.onTime * 100 / adhTotal) else 0

        // 整体感受众数
        val topFeeling = reportData.overallFeelingDistribution
            .maxByOrNull { it.value }?.key ?: "ok"
        val topFeelingLabel = feelingLabels[topFeeling] ?: "还行"

        val cards = listOf(
            Triple(
                "填报率",
                "${(comp.completionRate * 100).toInt()}%",
                "${comp.filledDays}/${comp.totalDays}天"
            ),
            Triple("用药依从", "${adhPct}%", "按时${adh.onTime} 遗漏${adh.missed}"),
            Triple("主要状态", topFeelingLabel, "最频繁感受")
        )
        val accentColors = listOf(
            when {
                comp.completionRate >= 0.7 -> cSuccess
                comp.completionRate >= 0.5 -> cWarning
                else -> cError
            },
            when {
                adhPct >= 80 -> cSuccess
                adhPct >= 50 -> cWarning
                else -> cError
            },
            feelingColors[topFeeling] ?: cPrimary
        )

        cards.forEachIndexed { i, (title, value, sub) ->
            val x = marginH + i * (cardW + 8f)
            drawStatusCard(c, x, currentY, cardW, cardH, title, value, sub, accentColors[i])
        }
        currentY += cardH + 8f
    }

    private fun drawStatusCard(
        c: Canvas, x: Float, y: Float, w: Float, h: Float,
        title: String, value: String, sub: String, accent: Int
    ) {
        // 卡片背景
        fillPaint.color = cWhite
        val rect = RectF(x, y, x + w, y + h)
        c.drawRoundRect(rect, 6f, 6f, fillPaint)
        // 边框
        val borderPaint = Paint().apply {
            color = cGray300; style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true
        }
        c.drawRoundRect(rect, 6f, 6f, borderPaint)
        // 左侧色条
        fillPaint.color = accent
        c.drawRoundRect(RectF(x, y, x + 4f, y + h), 2f, 2f, fillPaint)
        // 标题
        c.drawText(title, x + 12f, y + 16f, smallBold)
        // 大字数值
        valuePaint.color = accent
        c.drawText(value, x + 12f, y + 40f, valuePaint)
        valuePaint.color = cPrimary
        // 副标题
        c.drawText(sub, x + 12f, y + 56f, captionPaint)
    }

    /** overall_feeling 迷你饼图 + 图例 */
    private fun drawOverallFeelingPie(c: Canvas) {
        val dist = reportData.overallFeelingDistribution
        val total = dist.values.sum().toFloat()
        if (total <= 0) {
            drawBodyText("暂无整体感受数据")
            return
        }
        ensureSpace(110f)

        val cx = marginH + 60f
        val cy = currentY + 50f
        val radius = 42f

        // 绘制饼图
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
        // 中心白圆（环形饼图效果）
        fillPaint.color = cWhite
        c.drawCircle(cx, cy, radius * 0.5f, fillPaint)
        // 中心文字
        val totalInt = total.toInt()
        val tw = smallBold.measureText("${totalInt}天")
        c.drawText("${totalInt}天", cx - tw / 2, cy + 4f, smallBold)

        // 图例
        val legendX = marginH + 140f
        var legendY = currentY + 16f
        order.forEach { key ->
            val count = dist[key] ?: 0
            val pct = if (total > 0) (count / total * 100).toInt() else 0
            fillPaint.color = feelingColors[key] ?: cGray500
            c.drawRoundRect(
                RectF(legendX, legendY - 7f, legendX + 10f, legendY + 3f), 2f, 2f, fillPaint
            )
            c.drawText(
                "${feelingLabels[key]}  ${count}天 (${pct}%)", legendX + 16f, legendY, bodyPaint
            )
            legendY += 18f
        }
        currentY += 110f
    }

    /** 心情 & 能量水平条 */
    private fun drawMoodEnergyBars(c: Canvas) {
        val mood = reportData.moodScaleAverage
        val energy = reportData.energyLevelAverage
        if (mood == null && energy == null) return
        ensureSpace(60f)
        drawSectionTitle("心情与能量水平")
        if (mood != null) drawLinearGauge(c, "心情评分", mood, 10.0, cPrimary)
        if (energy != null) drawLinearGauge(c, "能量水平", energy, 10.0, cSuccess)
    }

    private fun drawLinearGauge(
        c: Canvas, label: String, value: Double, max: Double, color: Int
    ) {
        ensureSpace(28f)
        val barX = marginH + 80f
        val barW = contentWidth - 120f
        val barH = 10f
        c.drawText(label, marginH, currentY + 8f, bodyBold)
        // 背景条
        fillPaint.color = cGray100
        c.drawRoundRect(
            RectF(barX, currentY, barX + barW, currentY + barH), 5f, 5f, fillPaint
        )
        // 前景
        val frac = (value / max).toFloat().coerceIn(0f, 1f)
        fillPaint.color = color
        c.drawRoundRect(
            RectF(barX, currentY, barX + barW * frac, currentY + barH), 5f, 5f, fillPaint
        )
        // 数值
        val valStr = String.format("%.1f", value)
        c.drawText(valStr, barX + barW + 6f, currentY + 9f, bodyBold)
        currentY += 24f
    }

    /** 关键发现（improvements + concernPatterns） */
    private fun drawKeyFindings(c: Canvas) {
        val imps = reportData.improvements
        val cons = reportData.concernPatterns
        if (imps.isEmpty() && cons.isEmpty()) return
        ensureSpace(30f)
        drawSectionTitle("关键发现")
        imps.forEach { text ->
            ensureSpace(18f)
            drawKeyFindingItem(c, "✓", text, cSuccess)
        }
        cons.forEach { text ->
            ensureSpace(18f)
            drawKeyFindingItem(c, "⚠", text, cWarning)
        }
    }

    private fun drawKeyFindingItem(c: Canvas, icon: String, text: String, color: Int) {
        // 圆形小图标
        fillPaint.color = color
        c.drawCircle(marginH + 8f, currentY - 3f, 5f, fillPaint)
        // 图标文字
        val iconPaint = tp(cWhite, 7f, true)
        val iw = iconPaint.measureText(icon)
        c.drawText(icon, marginH + 8f - iw / 2, currentY, iconPaint)
        // 文字
        drawMultilineText(text, marginH + 22f, (contentWidth - 30f).toInt(), bodyPaint)
        currentY += 4f
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  PAGE 2 — 症状雷达图与深度分析                             ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun drawPage2SymptomAnalysis() {
        drawSectionTitle("症状总览 · 雷达图")
        drawRadarChart(canvas())
        currentY += sectionGap

        // 趋势明细表
        drawSectionTitle("趋势明细")
        drawTrendDetailTable(canvas())
        currentY += sectionGap

        // 异常事件
        if (reportData.anomalies.isNotEmpty()) {
            drawSectionTitle("异常事件")
            drawAnomalies(canvas())
            currentY += sectionGap
        }

        // 随访细节
        if (reportData.followUpSummary.isNotEmpty()) {
            drawSectionTitle("随访细节")
            drawFollowUpSummary(canvas())
        }
    }

    /** 雷达图：6 维症状 + 可选上周对比 */
    private fun drawRadarChart(c: Canvas) {
        val n = radarDimensions.size
        val radius = 85f
        val cx = pageWidth / 2f
        val cy = currentY + radius + 24f
        ensureSpace(radius * 2 + 60f)

        // 当周值
        val currentValues = radarDimensions.map { (qId, _) ->
            reportData.symptomSummary.metrics
                .firstOrNull { it.questionId == qId }?.average ?: 0.0
        }

        // 上周对比值 (仅7天报告)
        val lastWeekValues: List<Double>? =
            if (reportData.timeWindow <= 7 && reportData.weekOverWeekChange != null) {
                radarDimensions.mapIndexed { idx, (qId, _) ->
                    val cur = currentValues[idx]
                    val pctChange = reportData.weekOverWeekChange!![qId] ?: 0f
                    if (pctChange != 0f && cur != 0.0) {
                        cur / (1 + pctChange / 100f)
                    } else cur
                }
            } else null

        // --- 同心网格（3 层） ---
        val gridPaint = Paint().apply {
            color = cGray300; style = Paint.Style.STROKE; strokeWidth = 0.6f; isAntiAlias = true
        }
        for (level in 1..3) {
            val r = radius * level / 3f
            val path = Path()
            for (i in 0 until n) {
                val pt = radarPoint(cx, cy, r, i, n)
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            path.close()
            c.drawPath(path, gridPaint)
        }
        // 网格数值标注
        for (level in 1..3) {
            val v = String.format("%.0f", level * 10.0 / 3)
            c.drawText(v, cx + 3f, cy - radius * level / 3f + 10f, captionPaint)
        }

        // --- 轴线 ---
        for (i in 0 until n) {
            val pt = radarPoint(cx, cy, radius, i, n)
            c.drawLine(cx, cy, pt.x, pt.y, gridPaint)
        }

        // --- 上周数据多边形（虚线） ---
        if (lastWeekValues != null) {
            val dashPaint = Paint().apply {
                color = cGray500; style = Paint.Style.STROKE; strokeWidth = 1.2f
                isAntiAlias = true
                pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
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

        // --- 当周数据多边形 ---
        val dataPath = Path()
        currentValues.forEachIndexed { i, v ->
            val r = (v / 10.0).toFloat().coerceIn(0f, 1f) * radius
            val pt = radarPoint(cx, cy, r, i, n)
            if (i == 0) dataPath.moveTo(pt.x, pt.y) else dataPath.lineTo(pt.x, pt.y)
        }
        dataPath.close()
        // 半透明填充
        val fillP = Paint().apply {
            color = Color.argb(50, 21, 101, 192); style = Paint.Style.FILL; isAntiAlias = true
        }
        c.drawPath(dataPath, fillP)
        // 描边
        val strokeP = Paint().apply {
            color = cPrimary; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true
        }
        c.drawPath(dataPath, strokeP)

        // --- 顶点标签 + 数值 ---
        radarDimensions.forEachIndexed { i, (_, label) ->
            val pt = radarPoint(cx, cy, radius + 20f, i, n)
            val valStr = String.format("%.1f", currentValues[i])
            val fullLabel = "$label $valStr"
            val textW = bodyBold.measureText(fullLabel)
            val textX = when {
                pt.x < cx - 10 -> pt.x - textW
                pt.x > cx + 10 -> pt.x
                else -> pt.x - textW / 2
            }
            c.drawText(fullLabel, textX, pt.y + 4f, bodyBold)

            // 顶点小圆点
            val dataR = (currentValues[i] / 10.0).toFloat().coerceIn(0f, 1f) * radius
            val dataPt = radarPoint(cx, cy, dataR, i, n)
            fillPaint.color = severityColor(currentValues[i].toFloat())
            c.drawCircle(dataPt.x, dataPt.y, 3.5f, fillPaint)
        }

        // --- 图例（如果有上周对比） ---
        if (lastWeekValues != null) {
            val legendY = cy + radius + 24f
            // 当周
            fillPaint.color = cPrimary
            c.drawRoundRect(
                RectF(cx - 70f, legendY - 6f, cx - 58f, legendY + 2f), 2f, 2f, fillPaint
            )
            c.drawText("本周", cx - 54f, legendY, captionPaint)
            // 上周
            val dashLegend = Paint().apply {
                color = cGray500; style = Paint.Style.STROKE; strokeWidth = 1.2f
                pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f); isAntiAlias = true
            }
            c.drawLine(cx + 10f, legendY - 2f, cx + 22f, legendY - 2f, dashLegend)
            c.drawText("上周", cx + 26f, legendY, captionPaint)
            currentY = legendY + 12f
        } else {
            currentY = cy + radius + 16f
        }
    }

    /** 正多边形顶点计算（从正上方开始，顺时针） */
    private fun radarPoint(cx: Float, cy: Float, r: Float, index: Int, total: Int): PointF {
        val angle = -PI / 2 + 2 * PI * index / total
        return PointF(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
    }

    /** 趋势明细表 */
    private fun drawTrendDetailTable(c: Canvas) {
        val metrics = reportData.symptomSummary.metrics
        if (metrics.isEmpty()) {
            drawBodyText("暂无症状数据")
            return
        }

        val cols = floatArrayOf(80f, 55f, 55f, 50f, contentWidth - 240f)
        val headers = listOf("症状", "均值", "最近值", "趋势", "分析")
        val startX = marginH

        // 表头背景
        ensureSpace(24f)
        fillPaint.color = cPrimaryLt
        c.drawRect(startX, currentY - 10f, startX + contentWidth, currentY + 4f, fillPaint)
        var hx = startX
        headers.forEachIndexed { i, h ->
            c.drawText(h, hx + 4f, currentY, smallBold)
            hx += cols[i]
        }
        currentY += 10f

        // 数据行
        metrics.forEach { m ->
            ensureSpace(20f)
            val trend = reportData.detailedTrends[m.questionId]
            var x = startX
            // 症状名
            c.drawText(m.symptomName, x + 4f, currentY, bodyPaint)
            x += cols[0]
            // 均值（带颜色）
            bodyBold.color = severityColor(m.average.toFloat())
            c.drawText(String.format("%.1f", m.average), x + 4f, currentY, bodyBold)
            bodyBold.color = cGray900
            x += cols[1]
            // 最近值
            val lv = m.latestValue ?: m.average
            bodyPaint.color = severityColor(lv.toFloat())
            c.drawText(String.format("%.1f", lv), x + 4f, currentY, bodyPaint)
            bodyPaint.color = cGray900
            x += cols[2]
            // 趋势箭头
            val (arrow, trendColor) = trendArrowColor(m.trend)
            bodyBold.color = trendColor
            c.drawText(arrow, x + 4f, currentY, bodyBold)
            bodyBold.color = cGray900
            x += cols[3]
            // 详细描述
            val desc = trend?.description ?: m.trendDescription
            c.drawText(desc, x + 4f, currentY, captionPaint)

            currentY += 16f
            // 分隔线
            linePaint.color = cGray100
            c.drawLine(startX, currentY - 4f, startX + contentWidth, currentY - 4f, linePaint)
            linePaint.color = cGray300
        }
    }

    /** 异常事件列表 */
    private fun drawAnomalies(c: Canvas) {
        reportData.anomalies.take(6).forEach { a ->
            ensureSpace(22f)
            val color = when (a.severity) {
                AnomalySeverity.SEVERE -> cError
                AnomalySeverity.MODERATE -> cWarning
                AnomalySeverity.MILD -> Color.parseColor("#FBC02D")
            }
            fillPaint.color = color
            c.drawCircle(marginH + 6f, currentY - 3f, 4f, fillPaint)
            val metricName = radarDimensions
                .firstOrNull { it.first == a.metric }?.second ?: a.metric
            val text = "${a.date}  $metricName ${String.format("%.1f", a.value)}" +
                "（期望 ${a.expectedRange}）"
            c.drawText(text, marginH + 18f, currentY, bodyPaint)
            currentY += 16f
        }
    }

    /** 随访细节 */
    private fun drawFollowUpSummary(c: Canvas) {
        reportData.followUpSummary.forEach { (symptom, labels) ->
            ensureSpace(30f)
            c.drawText("$symptom:", marginH + 4f, currentY, bodyBold)
            currentY += 14f
            val text = labels.joinToString("、")
            drawMultilineText(text, marginH + 16f, (contentWidth - 24f).toInt(), bodyPaint)
            currentY += 6f
        }
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  PAGE 3 — 用药与生活方式                                  ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun drawPage3MedicationAndLifestyle() {
        drawMedicationSection(canvas())
        currentY += sectionGap
        drawLifestyleSection(canvas())
    }

    private fun drawMedicationSection(c: Canvas) {
        drawSectionTitle("用药情况")
        val summary = reportData.medicationSummary
        if (summary.activeMedications.isEmpty() && summary.events.isEmpty()) {
            drawBodyText("当前无活跃用药记录")
            return
        }

        // 活跃药物表
        if (summary.activeMedications.isNotEmpty()) {
            ensureSpace(20f)
            c.drawText("当前用药", marginH, currentY, bodyBold)
            currentY += 14f

            // 表头
            val cols = floatArrayOf(90f, 70f, 80f, 80f, contentWidth - 320f)
            ensureSpace(20f)
            fillPaint.color = cGray100
            c.drawRect(
                marginH, currentY - 10f, marginH + contentWidth, currentY + 4f, fillPaint
            )
            var hx = marginH
            listOf("药名", "剂量", "频次", "开始日期", "服药时段").forEachIndexed { i, h ->
                c.drawText(h, hx + 3f, currentY, smallBold)
                hx += cols[i]
            }
            currentY += 10f

            summary.activeMedications.forEachIndexed { idx, med ->
                ensureSpace(18f)
                if (idx % 2 == 1) {
                    fillPaint.color = cGray100
                    c.drawRect(
                        marginH, currentY - 10f,
                        marginH + contentWidth, currentY + 4f, fillPaint
                    )
                }
                var x = marginH
                c.drawText(med.name, x + 3f, currentY, bodyPaint)
                x += cols[0]
                c.drawText(med.dosage ?: "-", x + 3f, currentY, bodyPaint)
                x += cols[1]
                c.drawText(med.frequency, x + 3f, currentY, bodyPaint)
                x += cols[2]
                c.drawText(med.startDate, x + 3f, currentY, bodyPaint)
                x += cols[3]
                c.drawText(med.timeHints ?: "-", x + 3f, currentY, bodyPaint)
                currentY += 16f
            }
            currentY += 8f
        }

        // 用药依从性
        ensureSpace(24f)
        val adh = summary.adherence
        c.drawText("用药依从:", marginH, currentY, bodyBold)
        c.drawText(
            "按时 ${adh.onTime} 天  |  遗漏 ${adh.missed} 天  |  无需 ${adh.na} 天",
            marginH + 65f, currentY, bodyPaint
        )
        currentY += 16f

        // 用药事件
        if (summary.events.isNotEmpty()) {
            ensureSpace(24f)
            c.drawText("用药变更记录", marginH, currentY, bodyBold)
            currentY += 14f

            summary.events.take(8).forEach { evt ->
                ensureSpace(16f)
                c.drawText(
                    "${evt.date} ${evt.time}", marginH + 8f, currentY, captionPaint
                )
                c.drawText(evt.description, marginH + 100f, currentY, bodyPaint)
                currentY += 14f
            }
            if (summary.events.size > 8) {
                c.drawText(
                    "... 共 ${summary.events.size} 条变更记录",
                    marginH + 8f, currentY, captionPaint
                )
                currentY += 14f
            }
        }
    }

    private fun drawLifestyleSection(c: Canvas) {
        drawSectionTitle("生活方式")
        val ls = reportData.lifestyleSummary

        // 睡眠堆叠色条
        val sleepColors = mapOf(
            "lt6" to cError, "6_7" to cWarning, "7_8" to cSuccess, "gt8" to cPrimary
        )
        val sleepLabels = mapOf(
            "lt6" to "<6h", "6_7" to "6-7h", "7_8" to "7-8h", "gt8" to ">8h"
        )
        drawStackedBar(c, "睡眠时长", ls.sleepDistribution, sleepColors, sleepLabels)

        // 午休堆叠色条
        val napColors = mapOf(
            "none" to cGray500, "lt30" to cPrimary, "30_60" to cSuccess, "gt60" to cWarning
        )
        val napLabels = mapOf(
            "none" to "无", "lt30" to "<30m", "30_60" to "30-60m", "gt60" to ">60m"
        )
        drawStackedBar(c, "午休情况", ls.napDistribution, napColors, napLabels)

        // 步数堆叠色条
        val stepColors = mapOf(
            "lt3k" to cError, "3_6k" to cWarning, "6_10k" to cSuccess, "gt10k" to cPrimary
        )
        val stepLabels = mapOf(
            "lt3k" to "<3k", "3_6k" to "3-6k", "6_10k" to "6-10k", "gt10k" to ">10k"
        )
        drawStackedBar(c, "运动步数", ls.stepsDistribution, stepColors, stepLabels)

        currentY += 8f

        // 受寒 + 月经
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

    /** 水平堆叠色条 */
    private fun drawStackedBar(
        c: Canvas, label: String, dist: Map<String, Int>,
        colors: Map<String, Int>, labels: Map<String, String>
    ) {
        val total = dist.values.sum().toFloat()
        if (total <= 0) return
        ensureSpace(42f)

        c.drawText(label, marginH, currentY, bodyBold)
        currentY += 14f
        val barX = marginH
        val barW = contentWidth
        val barH = 14f
        var x = barX

        // 绘制堆叠段
        val order = labels.keys.toList()
        order.forEach { key ->
            val count = dist[key] ?: 0
            if (count <= 0) return@forEach
            val segW = count / total * barW
            fillPaint.color = colors[key] ?: cGray500
            c.drawRect(x, currentY, x + segW, currentY + barH, fillPaint)
            // 段内标签（仅占比>15%时显示）
            if (count / total > 0.15f) {
                val pctText = "${(count / total * 100).toInt()}%"
                val tw = captionPaint.measureText(pctText)
                if (tw + 4 < segW) {
                    captionPaint.color = cWhite
                    c.drawText(
                        pctText, x + (segW - tw) / 2, currentY + barH - 3f, captionPaint
                    )
                    captionPaint.color = cGray500
                }
            }
            x += segW
        }
        currentY += barH + 4f

        // 图例行
        var lx = marginH
        order.forEach { key ->
            val count = dist[key] ?: 0
            if (count <= 0) return@forEach
            fillPaint.color = colors[key] ?: cGray500
            c.drawRect(lx, currentY - 6f, lx + 8f, currentY + 2f, fillPaint)
            val lbl = "${labels[key]} $count"
            c.drawText(lbl, lx + 11f, currentY, captionPaint)
            lx += captionPaint.measureText(lbl) + 22f
        }
        currentY += 14f
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  PAGE 4 — AI 健康分析                                    ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun drawPage4AiInsights() {
        val c = canvas()
        val ai = reportData.aiInsightsSummary

        drawSectionTitle("AI 健康分析")

        if (ai == null) {
            drawBodyText("AI 分析暂不可用——本周期内未生成 AI 洞察报告。")
            drawBodyText("请确保已配置API密钥并在洞察页面生成周度洞察。")
            currentY += sectionGap
            drawSectionTitle("综合分析（确定性）")
            drawMultilineText(
                buildContextAnalysis(), marginH, contentWidth.toInt(), bodyPaint
            )
            currentY += sectionGap
            drawDisclaimer(c)
            return
        }

        // AI 总评（蓝色左边框引用块）
        if (ai.summary.isNotBlank()) {
            ensureSpace(60f)
            c.drawText("总评", marginH, currentY, bodyBold)
            currentY += 12f
            val blockX = marginH + 8f
            // 测量文本高度
            val summaryLayout = StaticLayout.Builder.obtain(
                ai.summary, 0, ai.summary.length, bodyPaint, (contentWidth - 24f).toInt()
            ).build()
            val blockH = summaryLayout.height + 12f
            ensureSpace(blockH + 4f)
            // 左边框
            fillPaint.color = cPrimary
            c.drawRect(
                marginH, currentY - 4f, marginH + 3f, currentY + blockH, fillPaint
            )
            // 浅蓝背景
            fillPaint.color = cPrimaryLt
            c.drawRect(
                marginH + 3f, currentY - 4f,
                marginH + contentWidth, currentY + blockH, fillPaint
            )
            // 文本
            c.save()
            c.translate(blockX + 8f, currentY)
            summaryLayout.draw(c)
            c.restore()
            currentY += blockH + 8f
        }

        // 要点发现
        if (ai.weeklyInsights.isNotEmpty()) {
            ensureSpace(30f)
            c.drawText("要点发现", marginH, currentY, bodyBold)
            currentY += 12f
            ai.weeklyInsights.forEachIndexed { i, text ->
                ensureSpace(20f)
                drawMultilineText(
                    "${i + 1}. $text", marginH + 8f, (contentWidth - 16f).toInt(), bodyPaint
                )
                currentY += 4f
            }
            currentY += 8f
        }

        // 生活建议
        if (ai.topSuggestions.isNotEmpty()) {
            ensureSpace(30f)
            c.drawText("生活建议", marginH, currentY, bodyBold)
            currentY += 12f
            ai.topSuggestions.forEachIndexed { i, text ->
                ensureSpace(20f)
                bodyPaint.color = cSuccess
                c.drawText("✦", marginH + 8f, currentY, bodyPaint)
                bodyPaint.color = cGray900
                drawMultilineText(
                    text, marginH + 22f, (contentWidth - 30f).toInt(), bodyPaint
                )
                currentY += 4f
            }
            currentY += 8f
        }

        // 健康警示（橙色背景块）
        if (ai.cautions.isNotEmpty()) {
            ensureSpace(40f)
            c.drawText("⚠ 健康警示", marginH, currentY, bodyBold)
            currentY += 10f
            val cautionText = ai.cautions.joinToString("\n") { "• $it" }
            val cautionLayout = StaticLayout.Builder.obtain(
                cautionText, 0, cautionText.length, bodyPaint, (contentWidth - 20f).toInt()
            ).build()
            val blockH = cautionLayout.height + 12f
            ensureSpace(blockH + 4f)
            fillPaint.color = cWarningLt
            c.drawRoundRect(
                RectF(marginH, currentY - 4f, marginH + contentWidth, currentY + blockH),
                4f, 4f, fillPaint
            )
            fillPaint.color = cWarning
            c.drawRect(
                marginH, currentY - 4f, marginH + 3f, currentY + blockH, fillPaint
            )
            c.save()
            c.translate(marginH + 12f, currentY + 2f)
            cautionLayout.draw(c)
            c.restore()
            currentY += blockH + 8f
        }

        // 置信度标签
        ensureSpace(24f)
        c.drawText("分析置信度:", marginH, currentY, captionPaint)
        drawConfidenceBadge(c, marginH + 70f, currentY - 8f, ai.confidence)
        currentY += sectionGap

        // 免责声明
        drawDisclaimer(c)
    }

    /** 置信度标签 */
    private fun drawConfidenceBadge(c: Canvas, x: Float, y: Float, level: String) {
        val (color, label) = when (level.lowercase()) {
            "high" -> cSuccess to "高"
            "medium" -> cWarning to "中"
            else -> cGray500 to "低"
        }
        fillPaint.color = color
        val tw = smallBold.measureText(label)
        c.drawRoundRect(
            RectF(x, y, x + tw + 16f, y + 16f), 8f, 8f, fillPaint
        )
        smallBold.color = cWhite
        c.drawText(label, x + 8f, y + 12f, smallBold)
        smallBold.color = cGray900
    }

    /** 确定性综合分析 */
    private fun buildContextAnalysis(): String {
        val sb = StringBuilder()
        sb.append("【综合分析】\n")
        val highSev = reportData.symptomSummary.metrics.filter { it.average > 6.0 }
        val rising = reportData.symptomSummary.metrics.filter { it.trend == TrendFlag.rising }
        val falling = reportData.symptomSummary.metrics.filter { it.trend == TrendFlag.falling }

        if (highSev.isNotEmpty()) {
            sb.append(
                "${highSev.joinToString("、") { it.symptomName }}" +
                    "症状较为严重（均值>6），建议重点关注。\n"
            )
        }
        if (rising.isNotEmpty()) {
            sb.append(
                "${rising.joinToString("、") { it.symptomName }}呈上升趋势，需注意观察。\n"
            )
        }
        if (falling.isNotEmpty()) {
            sb.append(
                "${falling.joinToString("、") { it.symptomName }}有所改善，可继续保持。\n"
            )
        }

        val adh = reportData.medicationSummary.adherence
        if (adh.missed > adh.onTime && adh.onTime + adh.missed > 0) {
            sb.append("用药依从性有待改善，建议设置提醒。\n")
        }
        if (reportData.dataCompleteness.completionRate < 0.5) {
            sb.append(
                "数据记录完整度较低（" +
                    "${(reportData.dataCompleteness.completionRate * 100).toInt()}%），" +
                    "建议坚持记录。\n"
            )
        }
        if (sb.toString() == "【综合分析】\n") {
            sb.append("本周期健康状况总体稳定，请继续保持良好习惯。")
        }
        return sb.toString()
    }

    /** 免责声明 */
    private fun drawDisclaimer(c: Canvas) {
        ensureSpace(40f)
        linePaint.color = cGray300; linePaint.strokeWidth = 0.5f
        c.drawLine(marginH, currentY, marginH + contentWidth, currentY, linePaint)
        currentY += 10f
        val text = "⚠️ 本报告由健康日记APP自动生成，仅供医生参考，不构成医疗诊断依据。\n" +
            "请以医生的专业判断为准。所有AI分析内容均为辅助信息。"
        drawMultilineText(text, marginH, contentWidth.toInt(), captionPaint)
    }

    // ==================== 通用绘制工具 ====================
    private fun drawSectionTitle(text: String) {
        ensureSpace(22f)
        val c = canvas()
        // 小色块装饰
        fillPaint.color = cPrimary
        c.drawRoundRect(
            RectF(marginH, currentY - 10f, marginH + 4f, currentY + 4f), 2f, 2f, fillPaint
        )
        c.drawText(text, marginH + 10f, currentY, sectionPaint)
        currentY += 16f
    }

    private fun drawBodyText(text: String) {
        ensureSpace(16f)
        canvas().drawText(text, marginH + 4f, currentY, bodyPaint)
        currentY += 14f
    }

    private fun drawMultilineText(
        text: String, x: Float, maxWidth: Int, paint: TextPaint
    ) {
        val layout = StaticLayout.Builder.obtain(
            text, 0, text.length, paint, maxWidth
        ).build()
        if (currentY + layout.height > pageHeight - marginBottom) startNewPage()
        val c = canvas()
        c.save(); c.translate(x, currentY); layout.draw(c); c.restore()
        currentY += layout.height
    }

    private fun severityColor(v: Float) = when {
        v < 3f -> cSuccess; v < 7f -> cWarning; else -> cError
    }

    private fun trendArrowColor(trend: TrendFlag) = when (trend) {
        TrendFlag.rising  -> "↑" to cError
        TrendFlag.falling -> "↓" to cSuccess
        TrendFlag.stable  -> "→" to cGray700
    }
}