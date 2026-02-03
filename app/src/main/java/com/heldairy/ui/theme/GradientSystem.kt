package com.heldairy.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 优雅动画常量 - 流畅自然的动画参数
 * Apple Health 风格：精致的弹簧和缓动曲线
 */
object ElegantAnimations {
    // 优雅弹簧 - 适合按钮、卡片等交互元素
    val elegantSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 300f
    )
    
    // 柔和入场 - 适合卡片、内容显示
    val smoothEntry = tween<Float>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )
    
    // 快速响应 - 适合开关、选择等即时反馈
    val quickResponse = tween<Float>(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )
    
    // 满意进度 - 适合进度条、加载等渐进动画
    val satisfyingProgress = tween<Float>(
        durationMillis = 800,
        easing = FastOutSlowInEasing
    )
}

/**
 * Canvas: 脉冲圆环动画
 * 用于空状态、等待状态，替代时间渐变
 */
@Composable
fun PulseRingCanvas(
    modifier: Modifier = Modifier,
    progress: Float,  // 0f -> 1f
    color: Color
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        
        // 三个同心环，依次扩散
        for (i in 0..2) {
            val delay = i * 0.33f
            val ringProgress = ((progress - delay).coerceIn(0f, 1f))
            val currentRadius = radius * (0.4f + 0.6f * ringProgress)
            val alpha = (1f - ringProgress) * 0.5f
            
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = currentRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Canvas: 五彩纸屑庆祝动画
 * 用于问卷完成等场景
 */
@Composable
fun ConfettiCanvas(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    colors: List<Color> = listOf(
        Color(0xFFFF6B8A),  // Hello Kitty 粉
        Color(0xFFFFD1DC),  // 浅粉
        Color(0xFFFFFFFF),  // 白色
        Color(0xFFE4002B),  // 蝴蝶结红
        Color(0xFFFFE4D1)   // 蜜桃奶油
    )
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // 30 个纸屑粒子
        repeat(30) { index ->
            val colorIndex = index % colors.size
            val particleColor = colors[colorIndex]
            
            // 初始位置（顶部随机）
            val startX = width * (index / 30f)
            val startY = -50f
            
            // 下落运动
            val currentY = startY + progress * (height + 100f)
            
            // 左右摆动（sin 曲线）
            val swingAmplitude = 50f
            val swingFrequency = 2f + (index % 3)
            val currentX = startX + swingAmplitude * sin(progress * swingFrequency * 2 * Math.PI).toFloat()
            
            // 淡出（接近底部时）
            val fadeOut = if (progress > 0.8f) (1f - (progress - 0.8f) / 0.2f) else 1f
            
            // 绘制纸屑
            drawCircle(
                color = particleColor.copy(alpha = 0.8f * fadeOut),
                radius = 4.dp.toPx(),
                center = Offset(currentX, currentY)
            )
        }
    }
}

/**
 * Canvas: 成功勾选动画
 */
@Composable
fun CheckmarkCanvas(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    color: Color
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val checkSize = size.minDimension * 0.6f
        val startX = center.x - checkSize * 0.3f
        val startY = center.y
        val midX = center.x - checkSize * 0.05f
        val midY = center.y + checkSize * 0.25f
        val endX = center.x + checkSize * 0.35f
        val endY = center.y - checkSize * 0.3f
        
        // 绘制勾选路径
        if (progress > 0.5f) {
            val p = (progress - 0.5f) * 2f
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(midX, midY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(midX, midY),
                end = Offset(
                    midX + (endX - midX) * p,
                    midY + (endY - midY) * p
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        } else {
            val p = progress * 2f
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(
                    startX + (midX - startX) * p,
                    startY + (midY - startY) * p
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
