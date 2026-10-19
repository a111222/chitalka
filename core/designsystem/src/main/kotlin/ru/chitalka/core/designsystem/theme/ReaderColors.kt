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
    /** Блик на рёбрах дерева: верх карниза, зубчики, бусины. */
    val woodHighlight: Color,
    /** Лицевая кромка полки. */
    val plank: Color,
    /** Верх задней стенки: под карнизом она чуть светлее. */
    val wallTop: Color,
    /** Светлая грань резьбы на гребне шкафа. */
    val carve: Color,
    /** Тень резьбы: даёт глубину вырезанным линиям. */
    val carveShadow: Color,
    /** Конус света, который светодиод бросает на заднюю стенку и книги. */
    val lampGlow: Color,
    /** Тиснение на корешках: золото днём, латунь ночью. */
    val spineFoil: Color,
    /** Сила теней шкафа и книг от 0 до 1. В E-Ink теней нет. */
    val shadowStrength: Float,
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
    woodHighlight = Color(0xFFDDB383),
    plank = Color(0xFFA8754A),
    wallTop = Color(0xFFCDB896),
    carve = Color(0xFFF3D3A5),
    carveShadow = Color(0xFF6B4626),
    lampGlow = Color(0x99FFD696),
    spineFoil = Color(0xFFF4DCA2),
    shadowStrength = 0.22f,
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
    woodHighlight = Color(0xFF7A5236),
    plank = Color(0xFF5C3A22),
    wallTop = Color(0xFF22160F),
    carve = Color(0xFFA57750),
    carveShadow = Color(0xFF140B06),
    lampGlow = Color(0x6BFFBA69),
    spineFoil = Color(0xFFE3C27A),
    shadowStrength = 0.5f,
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
    woodHighlight = Color(0xFFF2F2EE),
    plank = Color(0xFFF2F2EE),
    wallTop = Color(0xFFECECE8),
    carve = Color(0xFF111111),
    carveShadow = Color(0xFFF2F2EE),
    lampGlow = Color.Transparent,
    spineFoil = Color(0xFFF2F2EE),
    shadowStrength = 0f,
    isDark = false,
    isEInk = true,
)

val LocalReaderColors = staticCompositionLocalOf { LightOakColors }
