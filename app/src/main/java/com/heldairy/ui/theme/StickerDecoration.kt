package com.heldairy.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 通用贴纸装饰组件
 *
 * 用于在卡片角落、标题旁等位置放置 PNG 贴纸，
 * 自动适配深色模式（alpha × 0.65）。
 *
 * 调用方通过 [modifier] 控制对齐 / 偏移，例如：
 * ```
 * StickerDecoration(
 *     drawableRes = R.drawable.bow,
 *     size = 32.dp,
 *     rotation = 20f,
 *     modifier = Modifier
 *         .align(Alignment.TopEnd)
 *         .offset(x = 12.dp, y = (-12).dp)
 * )
 * ```
 */
@Composable
fun StickerDecoration(
    @DrawableRes drawableRes: Int,
    size: Dp = 32.dp,
    rotation: Float = 0f,
    alpha: Float = 0.6f,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val effectiveAlpha = if (isDark) (alpha * 0.65f).coerceIn(0f, 1f) else alpha

    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = null, // 纯装饰
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .rotate(rotation)
            .alpha(effectiveAlpha)
    )
}
