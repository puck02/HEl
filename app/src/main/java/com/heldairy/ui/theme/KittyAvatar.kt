package com.heldairy.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hello Kitty 风格默认头像
 * 使用 Canvas 绘制简化的 Kitty 脸部特征：
 * - 粉色圆脸
 * - 两个黑点眼睛
 * - 小小的鼻子和胡须
 * - 红色蝴蝶结装饰
 */
@Composable
fun KittyAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 粉色圆脸背景
        Canvas(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        ) {
            // 粉白渐变脸部
            drawCircle(
                color = Color(0xFFFFD1DC),
                radius = this.size.width / 2
            )
            
            val centerX = this.size.width / 2
            val centerY = this.size.height / 2
            val eyeY = centerY - this.size.height * 0.12f
            val eyeRadius = this.size.width * 0.04f
            
            // 左眼
            drawCircle(
                color = Color(0xFF2A2520),
                radius = eyeRadius,
                center = Offset(centerX - this.size.width * 0.18f, eyeY)
            )
            
            // 右眼
            drawCircle(
                color = Color(0xFF2A2520),
                radius = eyeRadius,
                center = Offset(centerX + this.size.width * 0.18f, eyeY)
            )
            
            // 小鼻子（黄色椭圆）
            drawCircle(
                color = Color(0xFFFFE57F),
                radius = this.size.width * 0.05f,
                center = Offset(centerX, centerY + this.size.height * 0.05f)
            )
            
            // 左胡须
            drawLine(
                color = Color(0xFF2A2520),
                start = Offset(centerX - this.size.width * 0.1f, centerY + this.size.height * 0.05f),
                end = Offset(centerX - this.size.width * 0.35f, centerY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color(0xFF2A2520),
                start = Offset(centerX - this.size.width * 0.1f, centerY + this.size.height * 0.1f),
                end = Offset(centerX - this.size.width * 0.35f, centerY + this.size.height * 0.1f),
                strokeWidth = 1.5f
            )
            
            // 右胡须
            drawLine(
                color = Color(0xFF2A2520),
                start = Offset(centerX + this.size.width * 0.1f, centerY + this.size.height * 0.05f),
                end = Offset(centerX + this.size.width * 0.35f, centerY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color(0xFF2A2520),
                start = Offset(centerX + this.size.width * 0.1f, centerY + this.size.height * 0.1f),
                end = Offset(centerX + this.size.width * 0.35f, centerY + this.size.height * 0.1f),
                strokeWidth = 1.5f
            )
        }
        
        // 蝴蝶结装饰（左上角）
        BowDecoration(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = size * 0.1f, y = size * 0.05f),
            size = size * 0.35f,
            color = Color(0xFFE4002B)  // 红色蝴蝶结
        )
    }
}
