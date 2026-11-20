package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorOutlineLight = androidx.compose.ui.graphics.Color(0xFFCBD5E1)

private val VaultDarkColorScheme = darkColorScheme(
    primary = MetallicGold,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepAmber,
    onPrimaryContainer = WarmGoldSoft,
    secondary = ElectricCyan,
    onSecondary = ObsidianBlack,
    secondaryContainer = DeepRoyalBlue,
    onSecondaryContainer = SnowText,
    tertiary = EmeraldSuccess,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = SnowText,
    surface = DeepSapphire,
    onSurface = SnowText,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = MutedSlateText,
    outline = CardBorderDark,
    error = RubyError,
    onError = SnowText
)

private val VaultLightColorScheme = lightColorScheme(
    primary = DeepRoyalBlue,
    onPrimary = CrispWhite,
    primaryContainer = SoftBlueSurface,
    onPrimaryContainer = InkDark,
    secondary = DeepAmber,
    onSecondary = CrispWhite,
    secondaryContainer = WarmGoldSoft,
    onSecondaryContainer = InkDark,
    tertiary = EmeraldSuccess,
    onTertiary = CrispWhite,
    background = PorcelainBg,
    onBackground = InkDark,
    surface = CrispWhite,
    onSurface = InkDark,
    surfaceVariant = SoftBlueSurface,
    onSurfaceVariant = InkMuted,
    outline = ColorOutlineLight,
    error = RubyError,
    onError = CrispWhite
)

val VaultShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VaultPlayTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VaultDarkColorScheme else VaultLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VaultTypography,
        shapes = VaultShapes,
        content = content
    )
}
