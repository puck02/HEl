package com.heldairy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ========== 设计系统：Spacing间距标尺 ==========
object Spacing {
    val XXS: Dp = 4.dp
    val XS: Dp = 8.dp
    val S: Dp = 12.dp
    val M: Dp = 20.dp
    val ML: Dp = 24.dp      // 新增：填补 M 与 L 的空档
    val L: Dp = 32.dp
    val XL: Dp = 48.dp
    val XXL: Dp = 64.dp
}

// ========== 设计系统：IconSize图标尺寸 ==========
// 统一图标尺寸标准，避免各处硬编码
object IconSize {
    val XSmall: Dp = 16.dp    // 按钮内图标
    val Small: Dp = 24.dp     // NavigationBar 图标
    val Medium: Dp = 32.dp    // 卡片装饰图标
    val Large: Dp = 48.dp     // 主要内容图标
    val Hero: Dp = 72.dp      // Header 头像
}

// ========== 设计系统：CornerRadius圆角系统 ==========
// Apple风格：更大的圆角营造柔和质感
object CornerRadius {
    val Small: Dp = 16.dp
    val Medium: Dp = 24.dp
    val Large: Dp = 32.dp
    val XLarge: Dp = 44.dp
}

// ========== 设计系统：Elevation阴影策略 ==========
// iOS 17 风格：极微妙阴影（1dp） + 半透明背景
object Elevation {
    val None: Dp = 0.dp
    val Low: Dp = 1.dp         // 从 2dp 改为 1dp（更接近 iOS）
    val High: Dp = 8.dp
}

// ========== ColorScheme扩展：语义色 ==========
val ColorScheme.success: Color
    @Composable
    @ReadOnlyComposable
    get() = SemanticSuccess

val ColorScheme.warning: Color
    @Composable
    @ReadOnlyComposable
    get() = SemanticWarning

val ColorScheme.info: Color
    @Composable
    @ReadOnlyComposable
    get() = SemanticInfo

val ColorScheme.semanticError: Color
    @Composable
    @ReadOnlyComposable
    get() = SemanticError

// ========== ColorScheme扩展：Accent Colors ==========
val ColorScheme.accentPurple: Color
    @Composable
    @ReadOnlyComposable
    get() = AccentPurple

val ColorScheme.accentIndigo: Color
    @Composable
    @ReadOnlyComposable
    get() = AccentIndigo

val ColorScheme.accentTeal: Color
    @Composable
    @ReadOnlyComposable
    get() = AccentTeal

val ColorScheme.accentPeach: Color
    @Composable
    @ReadOnlyComposable
    get() = AccentPeach

val ColorScheme.accentLavender: Color
    @Composable
    @ReadOnlyComposable
    get() = AccentLavender

val ColorScheme.accentMint: Color
    @Composable
    @ReadOnlyComposable
    get() = AccentMint

// ========== Light/Dark ColorScheme ==========
private val LightColors = lightColorScheme(
    primary = DayPrimary,
    onPrimary = Color.White,
    secondary = DaySecondary,
    onSecondary = Color.White,
    tertiary = DayTertiary,
    onTertiary = Color.White,
    background = DayBackground,
    surface = DaySurface,
    surfaceVariant = DaySurfaceVariant,
    onSurface = DayOnSurface,
    onSurfaceVariant = DayOnSurfaceVariant,
    outline = DayOutline,
    outlineVariant = DayOutline,
    inverseOnSurface = Color.White,
    inverseSurface = DayOnSurface,
    tertiaryContainer = DayTrack,
    primaryContainer = DaySurfaceVariant,
    secondaryContainer = DaySurfaceVariant,
    error = SemanticError,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = NightPrimary,
    onPrimary = Color.White,
    secondary = NightSecondary,
    onSecondary = Color.White,
    tertiary = NightTertiary,
    onTertiary = Color.White,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurface = NightOnSurface,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline,
    outlineVariant = NightOutline,
    inverseOnSurface = NightSurface,
    inverseSurface = NightOnSurface,
    tertiaryContainer = NightTrack,
    primaryContainer = NightSurfaceVariant,
    secondaryContainer = NightSurfaceVariant,
    error = SemanticError,
    onError = Color.White
)

@Composable
fun HElDairyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // 新增：支持 Material You 动态颜色
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when {
        // Android 12+ 支持动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
