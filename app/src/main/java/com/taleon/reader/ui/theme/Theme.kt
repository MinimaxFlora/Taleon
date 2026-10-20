package com.taleon.reader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.taleon.reader.domain.model.ReaderTheme
import com.taleon.reader.domain.model.ThemeMode

val TaleonIndigo = Color(0xFF5669D8)
val TaleonLavender = Color(0xFFB4A7FF)
val TaleonPaper = Color(0xFFF5F4EF)
val TaleonInk = Color(0xFF262624)
val TaleonGold = Color(0xFFB58B53)

private val LightColors = lightColorScheme(
    primary = TaleonIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E5FB),
    onPrimaryContainer = Color(0xFF1B2558),
    secondary = Color(0xFF67656E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E5EA),
    onSecondaryContainer = Color(0xFF363439),
    tertiary = TaleonGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4E6D1),
    onTertiaryContainer = Color(0xFF4A3519),
    background = TaleonPaper,
    onBackground = TaleonInk,
    surface = Color(0xFFFCFBF8),
    onSurface = TaleonInk,
    surfaceVariant = Color(0xFFE9E5DD),
    onSurfaceVariant = Color(0xFF625F59),
    surfaceTint = TaleonIndigo,
    outline = Color(0xFF8A857D),
    outlineVariant = Color(0xFFD8D3C9),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
).copy(
    surfaceDim = Color(0xFFE4E1D9),
    surfaceBright = Color(0xFFFFFEFB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F5F0),
    surfaceContainer = Color(0xFFF1EEE7),
    surfaceContainerHigh = Color(0xFFEBE7DF),
    surfaceContainerHighest = Color(0xFFE5E1D8),
    inverseSurface = Color(0xFF303033),
    inverseOnSurface = Color(0xFFF2F0EB),
    inversePrimary = TaleonLavender,
)

private val DarkColors = darkColorScheme(
    primary = TaleonLavender,
    onPrimary = Color(0xFF251B5C),
    primaryContainer = Color(0xFF3D326F),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = Color(0xFFC7C3D8),
    onSecondary = Color(0xFF302E39),
    secondaryContainer = Color(0xFF3F3D47),
    onSecondaryContainer = Color(0xFFEAE6F2),
    tertiary = Color(0xFFE1BE85),
    onTertiary = Color(0xFF422C0B),
    tertiaryContainer = Color(0xFF5E421D),
    onTertiaryContainer = Color(0xFFF8DFB7),
    background = Color(0xFF232326),
    onBackground = Color(0xFFF1EFEA),
    surface = Color(0xFF2A2A2E),
    onSurface = Color(0xFFF1EFEA),
    surfaceVariant = Color(0xFF35353A),
    onSurfaceVariant = Color(0xFFBCB8B0),
    surfaceTint = TaleonLavender,
    outline = Color(0xFF78746D),
    outlineVariant = Color(0xFF48484E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
).copy(
    surfaceDim = Color(0xFF1D1D20),
    surfaceBright = Color(0xFF3B3B40),
    surfaceContainerLowest = Color(0xFF1B1B1E),
    surfaceContainerLow = Color(0xFF26262A),
    surfaceContainer = Color(0xFF2B2B2F),
    surfaceContainerHigh = Color(0xFF333338),
    surfaceContainerHighest = Color(0xFF3C3C42),
    inverseSurface = Color(0xFFF0EEE9),
    inverseOnSurface = Color(0xFF2C2C2F),
    inversePrimary = TaleonIndigo,
)

val TaleonTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 52.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.4).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 33.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun TaleonTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = TaleonTypography,
        content = content,
    )
}

data class ReaderPalette(
    val background: Color,
    val canvas: Color,
    val text: Color,
    val secondaryText: Color,
    val accent: Color,
    val isDark: Boolean,
)

fun readerPalette(theme: ReaderTheme): ReaderPalette = when (theme) {
    ReaderTheme.PAPER -> ReaderPalette(
        background = Color(0xFFF7EFE1),
        canvas = Color(0xFFE4D9C7),
        text = Color(0xFF342F28),
        secondaryText = Color(0xFF7A6F60),
        accent = Color(0xFF8B5E34),
        isDark = false,
    )
    ReaderTheme.LIGHT -> ReaderPalette(
        background = Color(0xFFFCFBF7),
        canvas = Color(0xFFE9E6DF),
        text = Color(0xFF26252A),
        secondaryText = Color(0xFF757078),
        accent = TaleonIndigo,
        isDark = false,
    )
    ReaderTheme.SEPIA -> ReaderPalette(
        background = Color(0xFFEFE3C9),
        canvas = Color(0xFFDCCEAC),
        text = Color(0xFF443725),
        secondaryText = Color(0xFF7E7059),
        accent = Color(0xFF87602F),
        isDark = false,
    )
    ReaderTheme.DARK -> ReaderPalette(
        background = Color(0xFF17171B),
        canvas = Color(0xFF0A0A0C),
        text = Color(0xFFCFCBD4),
        secondaryText = Color(0xFF8A8690),
        accent = TaleonLavender,
        isDark = true,
    )
}
