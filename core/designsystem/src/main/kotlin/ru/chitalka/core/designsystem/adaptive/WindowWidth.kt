package ru.chitalka.core.designsystem.adaptive

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ширина окна по правилам WindowSizeClass: Compact < 600 dp — телефон,
 * Medium и Expanded — планшетная раскладка. Считается один раз в app и передаётся вниз.
 */
enum class WindowWidth {
    Compact,
    Medium,
    Expanded,
    ;

    val isPhone: Boolean get() = this == Compact

    companion object {
        fun fromWidth(width: Dp): WindowWidth = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }
    }
}

val LocalWindowWidth = staticCompositionLocalOf { WindowWidth.Compact }

/** Максимальная ширина колонки текста: на планшете строка не растягивается. */
val ReadingColumnMaxWidth: Dp = 640.dp
