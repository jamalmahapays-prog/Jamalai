package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MyAiDarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = IceWhite,
    primaryContainer = VioletDeep,
    onPrimaryContainer = VioletBright,
    secondary = CyanSecondary,
    onSecondary = ObsidianBlack,
    secondaryContainer = CyanDeep,
    onSecondaryContainer = IceWhite,
    tertiary = IndigoTertiary,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = IceWhite,
    surface = DeepCharcoal,
    onSurface = IceWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = DeepCharcoal,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = SurfaceBright,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle.copy(alpha = 0.6f),
    error = RoseError,
    onError = IceWhite
)

private val MyAiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyAiProTheme(
    isArabic: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MyAiDarkColorScheme,
        typography = buildMyAiTypography(isArabic = isArabic),
        shapes = MyAiShapes,
        content = content
    )
}
