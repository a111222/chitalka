package ru.chitalka.core.designsystem.theme

import android.provider.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import ru.chitalka.core.designsystem.haptics.Haptics
import ru.chitalka.core.designsystem.haptics.LocalHaptics
import ru.chitalka.core.designsystem.haptics.rememberHaptics
import ru.chitalka.core.designsystem.typography.ChitalkaTypography
import ru.chitalka.core.model.ReaderThemeMode

/** false, если в настройках телефона выключены анимации (спецвозможности). */
private val LocalAnimationsEnabled = staticCompositionLocalOf { true }

/** Корень оформления: тёмная тема макета v3, оттенок страницы зависит от [mode]. */
@Composable
fun ReaderTheme(
    mode: ReaderThemeMode = ReaderThemeMode.Graphite,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = colorsFor(mode)
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    CompositionLocalProvider(
        LocalReaderColors provides colors,
        LocalHaptics provides rememberHaptics(hapticsEnabled),
        LocalAnimationsEnabled provides animationsEnabled,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = ChitalkaTypography,
            content = content,
        )
    }
}

/** Доступ к токенам: ReaderTheme.colors.surface, ReaderTheme.haptics.pageTurn() */
object ReaderTheme {
    val colors: ReaderColors
        @Composable @ReadOnlyComposable
        get() = LocalReaderColors.current

    val haptics: Haptics
        @Composable @ReadOnlyComposable
        get() = LocalHaptics.current

    /** Если человек выключил анимации в системе, переходы становятся мгновенными. */
    val animationsEnabled: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalAnimationsEnabled.current
}

fun colorsFor(mode: ReaderThemeMode): ReaderColors = when (mode) {
    ReaderThemeMode.Graphite -> GraphiteColors
    ReaderThemeMode.Midnight -> MidnightColors
    ReaderThemeMode.Warm -> WarmColors
}

/** Наши токены поверх Material 3, чтобы стандартные компоненты тоже выглядели «по-нашему». */
private fun ReaderColors.toMaterialColorScheme(): ColorScheme = darkColorScheme(
    primary = accent,
    onPrimary = ink,
    primaryContainer = surfaceHigh,
    onPrimaryContainer = ink,
    secondary = author,
    onSecondary = background,
    secondaryContainer = surfaceHigh,
    onSecondaryContainer = ink,
    background = background,
    onBackground = ink,
    surface = background,
    onSurface = ink,
    surfaceVariant = surface,
    onSurfaceVariant = inkSecondary,
    surfaceContainerLowest = background,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceHigh,
    surfaceContainerHighest = surfaceHigh,
    inverseSurface = ink,
    inverseOnSurface = background,
    outline = inkSecondary,
    outlineVariant = track,
)
