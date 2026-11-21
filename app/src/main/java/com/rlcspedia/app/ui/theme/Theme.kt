@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.rlcspedia.app.R
import com.rlcspedia.app.data.AppSettings
import com.rlcspedia.app.data.ContrastLevel
import com.rlcspedia.app.data.ThemeMode

/* ---------- Color: semilla azul arena; terciario = naranja ---------- */
private val Light = lightColorScheme(
    primary = Color(0xFF1E5AC8), onPrimary = Color.White,
    primaryContainer = Color(0xFFDAE2FF), onPrimaryContainer = Color(0xFF001946),
    secondary = Color(0xFF565E71), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2F9), onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF8F4E00), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC2), onTertiaryContainer = Color(0xFF2E1500),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF9FF), onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFAF9FF), onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE1E2EC), onSurfaceVariant = Color(0xFF44464F),
    outline = Color(0xFF757780), outlineVariant = Color(0xFFC5C6D0),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF4F3FA),
    surfaceContainer = Color(0xFFEEEDF4), surfaceContainerHigh = Color(0xFFE8E7EF),
    surfaceContainerHighest = Color(0xFFE2E2E9),
    inverseSurface = Color(0xFF2F3036), inverseOnSurface = Color(0xFFF1F0F7), inversePrimary = Color(0xFFB1C5FF),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFB1C5FF), onPrimary = Color(0xFF002C71),
    primaryContainer = Color(0xFF00419E), onPrimaryContainer = Color(0xFFDAE2FF),
    secondary = Color(0xFFBEC6DC), onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759), onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFFFB77B), onTertiary = Color(0xFF4C2700),
    tertiaryContainer = Color(0xFF6D3A00), onTertiaryContainer = Color(0xFFFFDCC2),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318), onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF121318), onSurface = Color(0xFFE3E2E9),
    surfaceVariant = Color(0xFF44464F), onSurfaceVariant = Color(0xFFC5C6D0),
    outline = Color(0xFF8F9099), outlineVariant = Color(0xFF44464F),
    surfaceContainerLowest = Color(0xFF0D0E13), surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25), surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    inverseSurface = Color(0xFFE3E2E9), inverseOnSurface = Color(0xFF2F3036), inversePrimary = Color(0xFF1E5AC8),
)
private val LightHigh = Light.copy(
    primary = Color(0xFF002F80), onSurfaceVariant = Color(0xFF1A1B21),
    outline = Color(0xFF2D2F36), outlineVariant = Color(0xFF44464F), tertiary = Color(0xFF4F2800),
    secondaryContainer = Color(0xFF3E4759), onSecondaryContainer = Color.White,
)
private val DarkHigh = Dark.copy(
    primary = Color(0xFFEBEFFF), onPrimary = Color.Black, onSurface = Color.White,
    onSurfaceVariant = Color(0xFFF1F0F7), outline = Color(0xFFC9CAD4), outlineVariant = Color(0xFFC9CAD4),
    tertiary = Color(0xFFFFEDE3), secondaryContainer = Color(0xFFDAE2F9), onSecondaryContainer = Color.Black,
)

/* ---------- Colores de equipo (semánticos, no dependen del color dinámico) ---------- */
@Immutable
data class TeamColors(val blue: Color, val orange: Color, val live: Color, val onLive: Color)

private val TeamLight = TeamColors(Color(0xFF1E5AC8), Color(0xFFB35A00), Color(0xFFC4161C), Color.White)
private val TeamDark = TeamColors(Color(0xFF9DB8FF), Color(0xFFFFB77B), Color(0xFFFF8A80), Color(0xFF3A0003))
private val TeamLightHigh = TeamColors(Color(0xFF002F80), Color(0xFF6B3500), Color(0xFF8C0009), Color.White)
private val TeamDarkHigh = TeamColors(Color(0xFFDDE5FF), Color(0xFFFFD2B0), Color(0xFFFFD0CB), Color.Black)

val LocalTeamColors = staticCompositionLocalOf { TeamLight }
val LocalReduceMotion = staticCompositionLocalOf { false }

/* ---------- Tipografía: Chakra Petch (marcadores) + Atkinson Hyperlegible (texto) ---------- */
private val Chakra = FontFamily(Font(R.font.chakra_petch_semibold, FontWeight.SemiBold), Font(R.font.chakra_petch_bold, FontWeight.Bold))
private val Atkinson = FontFamily(Font(R.font.atkinson_regular, FontWeight.Normal), Font(R.font.atkinson_bold, FontWeight.Bold))

private fun typography(legible: Boolean): Typography {
    val display = if (legible) Atkinson else Chakra
    val base = Typography()
    fun TextStyle.d() = copy(fontFamily = display, fontWeight = FontWeight.Bold)
    fun TextStyle.b() = copy(fontFamily = Atkinson)
    return base.copy(
        displayLarge = base.displayLarge.d(), displayMedium = base.displayMedium.d(), displaySmall = base.displaySmall.d(),
        headlineLarge = base.headlineLarge.d(), headlineMedium = base.headlineMedium.d(), headlineSmall = base.headlineSmall.d(),
        titleLarge = base.titleLarge.b().copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.b().copy(fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.b().copy(fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.b(), bodyMedium = base.bodyMedium.b(), bodySmall = base.bodySmall.b(),
        labelLarge = base.labelLarge.b().copy(fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.b().copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.b().copy(fontWeight = FontWeight.Bold),
        // Escala enfatizada de M3 Expressive: marcadores, títulos y acciones destacadas
        displayLargeEmphasized = base.displayLargeEmphasized.d(),
        displayMediumEmphasized = base.displayMediumEmphasized.d(),
        displaySmallEmphasized = base.displaySmallEmphasized.d(),
        headlineLargeEmphasized = base.headlineLargeEmphasized.d(),
        headlineMediumEmphasized = base.headlineMediumEmphasized.d(),
        headlineSmallEmphasized = base.headlineSmallEmphasized.d(),
        titleLargeEmphasized = base.titleLargeEmphasized.b().copy(fontWeight = FontWeight.Bold),
        titleMediumEmphasized = base.titleMediumEmphasized.b().copy(fontWeight = FontWeight.Bold),
        titleSmallEmphasized = base.titleSmallEmphasized.b().copy(fontWeight = FontWeight.Bold),
        bodyLargeEmphasized = base.bodyLargeEmphasized.b().copy(fontWeight = FontWeight.Bold),
        bodyMediumEmphasized = base.bodyMediumEmphasized.b().copy(fontWeight = FontWeight.Bold),
        bodySmallEmphasized = base.bodySmallEmphasized.b().copy(fontWeight = FontWeight.Bold),
        labelLargeEmphasized = base.labelLargeEmphasized.b().copy(fontWeight = FontWeight.Bold),
        labelMediumEmphasized = base.labelMediumEmphasized.b().copy(fontWeight = FontWeight.Bold),
        labelSmallEmphasized = base.labelSmallEmphasized.b().copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun RLCSpediaTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val high = settings.contrast == ContrastLevel.HIGH
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        high -> if (dark) DarkHigh else LightHigh
        settings.dynamicColor && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        else -> if (dark) Dark else Light
    }
    val team = when {
        high && dark -> TeamDarkHigh
        high -> TeamLightHigh
        dark -> TeamDark
        else -> TeamLight
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * settings.fontScale),
        LocalTeamColors provides team,
        LocalReduceMotion provides settings.reduceMotion,
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = if (settings.reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
            typography = typography(settings.legibleFont),
            content = content,
        )
    }
}
