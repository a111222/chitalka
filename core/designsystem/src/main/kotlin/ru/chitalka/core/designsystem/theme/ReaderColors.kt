package ru.chitalka.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Цвета из макета v3: приложение всегда тёмное. Экраны никогда не пишут цвет руками,
 * только ReaderTheme.colors.surface, ReaderTheme.colors.accent и т. п.
 */
@Immutable
data class ReaderColors(
    /** Фон приложения. */
    val background: Color,
    /** Карточки: полки, поле поиска, неактивные чипы. */
    val surface: Color,
    /** Приподнятые элементы поверх карточек. */
    val surfaceHigh: Color,
    /** Дорожка прогресса и разделители. */
    val track: Color,
    /** Бумага страницы в читалке: зависит от выбранного оттенка. */
    val paper: Color,
    /** Основной текст. */
    val ink: Color,
    /** Вторичный текст: подписи, счётчики, неактивные иконки. */
    val inkSecondary: Color,
    /** Текст на фоне обложки: «Добрый вечер», автор под каруселью. */
    val inkSoft: Color,
    /** Акцент: активная вкладка, выбранный пункт. */
    val accent: Color,
    /** Начало и конец фирменного градиента (кнопки, прогресс, активные чипы). */
    val gradientStart: Color,
    val gradientEnd: Color,
    /** Имя автора на карточке книги, подписи в статистике. */
    val author: Color,
    /** Звезда оценки. */
    val star: Color,
    /** Выделение маркером в тексте. */
    val highlight: Color,
    /** Матовое стекло: нижняя панель и шторка «Аа». */
    val glass: Color,
) {
    /** Фирменный розово-фиолетовый градиент слева направо. */
    val accentBrush: Brush get() = Brush.horizontalGradient(listOf(gradientStart, gradientEnd))

    /** Тот же градиент по диагонали: для круглых и квадратных кнопок. */
    val accentDiagonal: Brush get() = Brush.linearGradient(listOf(gradientStart, gradientEnd))
}

private val Base = ReaderColors(
    background = Color(0xFF191A23),
    surface = Color(0xFF23242F),
    surfaceHigh = Color(0xFF2B2D3A),
    track = Color(0xFF33354A),
    paper = Color(0xFF191A23),
    ink = Color(0xFFF2F2F7),
    inkSecondary = Color(0xFF9A9CB0),
    inkSoft = Color(0xFFC9CADB),
    accent = Color(0xFFFF6F9F),
    gradientStart = Color(0xFFFF4F8B),
    gradientEnd = Color(0xFF9B5CFF),
    author = Color(0xFFB48CFF),
    star = Color(0xFFFFC24B),
    highlight = Color(0x6B1FB5A0),
    glass = Color(0xC72B2D3A),
)

/** Графит: страница в цвет приложения. Оттенок по умолчанию. */
val GraphiteColors = Base

/** Полночь: почти чёрная страница для чтения в темноте. */
val MidnightColors = Base.copy(paper = Color(0xFF0B0C12))

/** Тёплая: коричневатая бумага и чуть желтоватый текст, мягче для глаз. */
val WarmColors = Base.copy(
    paper = Color(0xFF1E1A17),
    ink = Color(0xFFEDE6DA),
    inkSecondary = Color(0xFFA39A8C),
)

val LocalReaderColors = staticCompositionLocalOf { GraphiteColors }
