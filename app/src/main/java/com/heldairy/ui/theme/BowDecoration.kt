package com.heldairy.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hello Kitty 蝴蝶结装饰组件
 * 使用 Material Icons 组合模拟蝴蝶结效果：
 * - 两个爱心图标旋转 ±45° 作为蝴蝶结的两翼
 * - 中心一个小圆点作为蝴蝶结的系结
 */
@Composable
fun BowDecoration(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.tertiary,  // 默认使用蝴蝶结红
    tint: Color = color
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 左侧蝴蝶结翼（爱心旋转 -45°）
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            modifier = Modifier
                .size(size * 0.6f)
                .offset(x = -size * 0.2f, y = 0.dp)
                .rotate(-45f),
            tint = tint
        )
        
        // 右侧蝴蝶结翼（爱心旋转 +45°）
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            modifier = Modifier
                .size(size * 0.6f)
                .offset(x = size * 0.2f, y = 0.dp)
                .rotate(45f),
            tint = tint
        )
        
        // 中心系结（小圆圈用 Favorite 表示）
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Hello Kitty 蝴蝶结",
            modifier = Modifier.size(size * 0.25f),
            tint = tint
        )
    }
}

/**
 * 装饰性蝴蝶结（更小，用于卡片角落等位置）
 */
@Composable
fun SmallBowDecoration(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary
) {
    BowDecoration(
        modifier = modifier,
        size = 20.dp,
        color = color
    )
}

/**
 * 大号英雄蝴蝶结（用于空状态、完成页面等）
 */
@Composable
fun HeroBowDecoration(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary
) {
    BowDecoration(
        modifier = modifier,
        size = 64.dp,
        color = color
    )
}
