package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

fun buildMyAiTypography(isArabic: Boolean): Typography {
    val displayFont = if (isArabic) CairoFontFamily else SpaceGroteskFontFamily
    val bodyFont = if (isArabic) CairoFontFamily else PlusJakartaFontFamily

    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 34.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 21.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    )
}
