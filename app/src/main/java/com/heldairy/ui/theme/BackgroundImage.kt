package com.heldairy.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.heldairy.R

/**
 * Hello Kitty 风格背景图片组件
 * 为各个页面提供统一的可爱背景
 */
@Composable
fun KittyBackground(
    modifier: Modifier = Modifier,
    backgroundRes: Int = R.drawable.background01,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 背景图片
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.3f  // 设置透明度，让前景内容更清晰
        )
        
        // 前景内容
        content()
    }
}

/**
 * 根据不同页面类型选择不同的背景
 */
object BackgroundTheme {
    val DAILY_REPORT = R.drawable.background01
    val INSIGHTS = R.drawable.background02
    val MEDICATION = R.drawable.background03
    val SETTINGS = R.drawable.background04
    val HOME = R.drawable.background05
}
