package ru.chitalka.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import ru.chitalka.core.designsystem.haptics.Haptics
import ru.chitalka.core.designsystem.haptics.LocalHaptics
import ru.chitalka.core.designsystem.haptics.rememberHaptics
import ru.chitalka.core.designsystem.typography.ChitalkaTypography
import ru.chitalka.core.model.ReaderThemeMode

/** Корень оформления: оборачивает всё приложение в одну из трёх тем макета. */
@Composable
fun ReaderTheme(
    mode: ReaderThemeMode = ReaderThemeMode.LightOak,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = colorsFor(mode)
    CompositionLocalProvider(
        LocalReaderColors provides colors,
        LocalHaptics provides rememberHaptics(hapticsEnabled && !colors.isEInk),
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = ChitalkaTypography,
            content = content,
        )
    }
}

/** Доступ к токенам: ReaderTheme.colors.paper, ReaderTheme.haptics.pageTurn() */
object ReaderTheme {
    val colors: ReaderColors
        @Composable @ReadOnlyComposable
        get() = LocalReaderColors.current

    val haptics: Haptics
        @Composable @ReadOnlyComposable
        get() = LocalHaptics.current

    /** В E-Ink анимации заменяются мгновенной сменой. */
    val animationsEnabled: Boolean
        @Composable @ReadOnlyComposable
        get() = !LocalReaderColors.current.isEInk
}

fun colorsFor(mode: ReaderThemeMode): ReaderColors = when (mode) {
    ReaderThemeMode.LightOak -> LightOakColors
    ReaderThemeMode.DarkWalnut -> DarkWalnutColors
    ReaderThemeMode.EInk -> EInkColors
}

/** Наши токены поверх Material 3, чтобы стандартные компоненты тоже выглядели «по-нашему». */
private fun ReaderColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = paper,
        primaryContainer = wall,
        onPrimaryContainer = ink,
        secondary = woodShadow,
        onSecondary = paper,
        secondaryContainer = wall,
        onSecondaryContainer = ink,
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceVariant = wall,
        onSurfaceVariant = inkSecondary,
        surfaceContainerLowest = paper,
        surfaceContainerLow = paper,
        surfaceContainer = paper,
        surfaceContainerHigh = paper,
        surfaceContainerHighest = wall,
        outline = inkSecondary,
        outlineVariant = wall,
    )
}
