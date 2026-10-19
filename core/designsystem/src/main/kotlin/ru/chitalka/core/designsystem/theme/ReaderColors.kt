package ru.chitalka.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Цвета читалки из макета v2. Экраны никогда не пишут цвет руками,
 * только ReaderTheme.colors.paper, ReaderTheme.colors.shelfGlow и т. п.
 */
@Immutable
data class ReaderColors(
    /** Фон приложения (стена комнаты). */
    val background: Color,
    /** Бумага страницы. */
    val paper: Color,
    /** Основной текст. */
    val ink: Color,
    /** Вторичный текст: подписи, номер страницы. */
    val inkSecondary: Color,
    /** Акцент: тёмная охра днём, латунь ночью. */
    val accent: Color,
    /** Дерево шкафа: дуб или орех. */
    val wood: Color,
    /** Тень и торцы дерева. */
    val woodShadow: Color,
    /** Задняя стенка шкафа. */
    val wall: Color,
    /** Тёплый свет светодиода под полкой. В E-Ink прозрачный. */
    val shelfGlow: Color,
    /** Линии резьбы (в E-Ink шкаф рисуется только линиями, как гравюра). */
    val line: Color,
    val isDark: Boolean,
    /** E-Ink: без анимаций, теней и градиентов. */
    val isEInk: Boolean = false,
)

/** Светлая: светлый дуб, бумага цвета слоновой кости. Для дня и яркого света. */
val LightOakColors = ReaderColors(
    background = Color(0xFFF3EEE6),
    paper = Color(0xFFF7F1E5),
    ink = Color(0xFF2B241D),
    inkSecondary = Color(0xFF6E6254),
    accent = Color(0xFF8A5A1F),
    wood = Color(0xFFB98A5A),
    woodShadow = Color(0xFF7A5236),
    wall = Color(0xFFE2D2B8),
    shelfGlow = Color(0xFFFFF1D6),
    line = Color(0xFF7A5236),
    isDark = false,
)

/** Тёмная: орех, латунь, тёплые светодиоды. Бумага тёмно-тёплая, а не чёрная. */
val DarkWalnutColors = ReaderColors(
    background = Color(0xFF0F0C0A),
    paper = Color(0xFF1E1B17),
    ink = Color(0xFFD8CFBF),
    inkSecondary = Color(0xFF9A8F7F),
    accent = Color(0xFFE0B46A),
    wood = Color(0xFF4A2E1C),
    woodShadow = Color(0xFF25160D),
    wall = Color(0xFF160E09),
    shelfGlow = Color(0xFFFFD6A0),
    line = Color(0xFFE0B46A),
    isDark = true,
)

/** E-Ink: 16 оттенков серого, шкаф как гравюра, без свечения. */
val EInkColors = ReaderColors(
    background = Color(0xFFECECE8),
    paper = Color(0xFFF2F2EE),
    ink = Color(0xFF111111),
    inkSecondary = Color(0xFF444444),
    accent = Color(0xFF111111),
    wood = Color(0xFFF2F2EE),
    woodShadow = Color(0xFF5A5A5A),
    wall = Color(0xFFECECE8),
    shelfGlow = Color.Transparent,
    line = Color(0xFF111111),
    isDark = false,
    isEInk = true,
)

val LocalReaderColors = staticCompositionLocalOf { LightOakColors }
