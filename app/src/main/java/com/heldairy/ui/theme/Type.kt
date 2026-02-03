package com.heldairy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.heldairy.R

// Google Fonts配置：Nunito Sans人文温暖字体
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val nunitoSansFont = GoogleFont("Nunito Sans")

// Nunito Sans字体族（4个字重）
val NunitoSansFontFamily = FontFamily(
    Font(googleFont = nunitoSansFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = nunitoSansFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = nunitoSansFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = nunitoSansFont, fontProvider = provider, weight = FontWeight.Bold)
)

// 自定义Typography：5级层级 + 人文行高
val Typography = Typography(
    // Display - 32sp/48sp行高（1.5x）- 用于Hero标题
    displayLarge = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.25).sp
    ),
    
    // Headline - 24sp/36sp行高（1.5x）- 用于页面标题
    headlineLarge = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 33.sp
    ),
    
    // Title - 20sp/30sp行高（1.5x）- 用于卡片标题
    titleLarge = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 27.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    
    // Body - 16sp/25.6sp行高（1.6x中文优化）- 用于正文
    bodyLarge = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.6.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.4.sp
    ),
    
    // Label - 14sp/21sp行高（1.5x）- 用于按钮、标签
    labelLarge = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 19.5.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NunitoSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    )
)
