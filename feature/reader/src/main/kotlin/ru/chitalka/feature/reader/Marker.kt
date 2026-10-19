package ru.chitalka.feature.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.math.max
import kotlin.math.min

/** Конец страницы [index] в тексте главы (не включая): начало следующей страницы или конец главы. */
internal fun PagedChapter.pageEnd(index: Int): Int =
    pages.getOrNull(index + 1)?.startOffset ?: layout.layoutInput.text.length

/** Куда попал палец: страница главы и символ на ней. */
internal data class TextHit(val paged: PagedChapter, val pageIndex: Int, val offset: Int)

/** Выделение маркером на странице: смещения в главе и цвет. */
internal data class Mark(val start: Int, val end: Int, val color: Color)

/**
 * Где на экране лежит текст страниц: нужно, чтобы по точке касания найти символ.
 * Координаты — в области страниц (между верхней и нижней панелями читалки).
 * [singleX] — левый край текста на телефоне, [leftX]/[rightX] — левой и правой страниц разворота.
 */
internal class PageGeometry(
    val isSpread: Boolean,
    val areaWidth: Float,
    val singleX: Float,
    val leftX: Float,
    val rightX: Float,
    val topY: Float,
    val textWidth: Float,
    /** Насколько можно промахнуться мимо текста, чтобы касание всё равно засчиталось. */
    val slop: Float,
) {
    /**
     * Символ под точкой [at] на видимых страницах [spread]. Пока ведут маркером ([within] не null),
     * палец «привязан» к странице, где начали, и выход за край текста просто упирается в него.
     */
    fun hit(at: Offset, spread: Spread, within: TextHit? = null): TextHit? {
        val pageIndex = when {
            within != null -> within.pageIndex
            !isSpread -> spread.first
            at.x < areaWidth / 2 -> spread.first
            else -> spread.first + 1
        }
        val originX = when {
            !isSpread -> singleX
            pageIndex == spread.first -> leftX
            else -> rightX
        }
        val paged = within?.paged ?: spread.paged
        val page = paged.pages.getOrNull(pageIndex) ?: return null
        val height = page.bottom - page.top
        val x = at.x - originX
        val y = at.y - topY
        if (within == null && (x < -slop || x > textWidth + slop || y < -slop || y > height + slop)) return null
        val inLayout = Offset(x.coerceIn(0f, textWidth), y.coerceIn(0f, (height - 1f).coerceAtLeast(0f)) + page.top)
        val offset = paged.layout.getOffsetForPosition(inLayout).coerceIn(page.startOffset, paged.pageEnd(pageIndex))
        return TextHit(paged, pageIndex, offset)
    }
}

/**
 * Что выделит маркер, если его довели от слова [anchor] до символа [offset]: всегда целые слова,
 * в пределах страницы, без пробелов по краям. Пусто — выделять нечего.
 */
internal fun markerRange(layout: TextLayoutResult, anchor: TextRange, offset: Int, pageStart: Int, pageEnd: Int): TextRange {
    val word = wordAt(layout, offset)
    val text = layout.layoutInput.text.text
    var start = min(anchor.min, word.min).coerceIn(pageStart, pageEnd)
    var end = max(anchor.max, word.max).coerceIn(pageStart, pageEnd)
    while (start < end && text[start].isWhitespace()) start++
    while (end > start && text[end - 1].isWhitespace()) end--
    return TextRange(start, end)
}

/** Слово под символом; если под пальцем пробел или знак — только этот символ. */
internal fun wordAt(layout: TextLayoutResult, offset: Int): TextRange {
    val length = layout.layoutInput.text.length
    if (length == 0) return TextRange.Zero
    val at = offset.coerceIn(0, length - 1)
    val word = layout.getWordBoundary(at)
    return if (word.collapsed) TextRange(at, at + 1) else word
}

/**
 * Полосы маркера для [start]..[end]: по одной на строку. Полоса закрывает буквы от верха строчных
 * до хвостов «у» и «р», а не всю высоту строки с интерлиньяжем: так выглядит настоящий маркер.
 */
internal fun markRects(layout: TextLayoutResult, start: Int, end: Int, fontPx: Float): List<Rect> {
    val length = layout.layoutInput.text.length
    val from = start.coerceIn(0, length)
    val to = end.coerceIn(0, length)
    if (from >= to) return emptyList()
    val firstLine = layout.getLineForOffset(from)
    val lastLine = layout.getLineForOffset(to - 1)
    return (firstLine..lastLine).mapNotNull { line ->
        val lineStart = layout.getLineStart(line)
        val lineEnd = layout.getLineEnd(line, visibleEnd = true)
        val s = max(from, lineStart)
        val e = min(to, lineEnd)
        if (s >= e) return@mapNotNull null
        val x1 = layout.getHorizontalPosition(s, usePrimaryDirection = true)
        val x2 = if (e >= lineEnd) layout.getLineRight(line) else layout.getHorizontalPosition(e, usePrimaryDirection = true)
        val baseline = layout.getLineBaseline(line)
        Rect(min(x1, x2) - fontPx * 0.06f, baseline - fontPx * 0.9f, max(x1, x2) + fontPx * 0.06f, baseline + fontPx * 0.28f)
    }
}
