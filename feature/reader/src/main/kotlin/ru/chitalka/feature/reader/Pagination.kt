package ru.chitalka.feature.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.em
import ru.chitalka.core.model.Block
import ru.chitalka.core.model.Chapter

/**
 * Глава, разрезанная на страницы. Текст главы раскладывается ОДИН раз через TextMeasurer,
 * а страница — это просто окно по высоте в этой раскладке: строки с [Page.top] по [Page.bottom].
 * Поэтому строки на странице всегда те же, что в раскладке, и ничего не «прыгает».
 */
class PagedChapter(
    val chapterIndex: Int,
    val layout: TextLayoutResult,
    val pages: List<Page>,
) {
    /** Страница, на которой стоит символ [charOffset]. Смещение за концом главы — последняя страница. */
    fun pageFor(charOffset: Int): Int = pages.indexOfLast { it.startOffset <= charOffset }.coerceAtLeast(0)
}

/** Одна страница: с какого символа начинается и какую полосу раскладки занимает (в пикселях). */
data class Page(val startOffset: Int, val top: Float, val bottom: Float)

/**
 * Режет текст на страницы высотой [pageHeight]. Строка никогда не разрывается между страницами;
 * если одна строка выше страницы (огромный заголовок), она всё равно занимает свою страницу.
 */
fun paginate(
    measurer: TextMeasurer,
    chapterIndex: Int,
    text: AnnotatedString,
    style: TextStyle,
    pageWidth: Int,
    pageHeight: Int,
): PagedChapter {
    val layout = measurer.measure(
        text = text,
        style = style,
        constraints = Constraints(maxWidth = pageWidth),
        skipCache = true,
    )
    val pages = mutableListOf<Page>()
    var line = 0
    while (line < layout.lineCount) {
        val top = layout.getLineTop(line)
        var last = line
        while (last + 1 < layout.lineCount && layout.getLineBottom(last + 1) - top <= pageHeight) last++
        pages += Page(startOffset = layout.getLineStart(line), top = top, bottom = layout.getLineBottom(last))
        line = last + 1
    }
    if (pages.isEmpty()) pages += Page(0, 0f, 0f)
    return PagedChapter(chapterIndex, layout, pages)
}

/**
 * Блоки главы → размеченный текст. Вид задаёт читалка, а не книга: абзацы по ширине с красной строкой,
 * заголовки по центру, цитаты курсивом с отступом. Смещения символов в этом тексте и есть
 * [ru.chitalka.core.model.ReadingPosition.charOffset], поэтому разметка должна быть детерминированной.
 */
fun chapterText(chapter: Chapter): AnnotatedString = buildAnnotatedString {
    val blocks = chapter.blocks.filterNot { it is Block.Image }
        // Глава без текста (обложка, картинка): показываем хотя бы её название
        .ifEmpty { listOfNotNull(chapter.title?.let { Block.Heading(it, 1) }) }

    blocks.forEachIndexed { i, block ->
        when (block) {
            is Block.Heading -> {
                if (i > 0) spacer()
                withStyle(ParagraphStyle(textAlign = TextAlign.Center, lineHeight = 1.3.em)) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = headingSize(block.level))) {
                        append(block.text)
                    }
                }
                spacer()
            }
            is Block.Paragraph -> withStyle(
                ParagraphStyle(textAlign = TextAlign.Justify, textIndent = TextIndent(firstLine = 1.5.em)),
            ) {
                append(block.text)
            }
            is Block.Quote -> withStyle(
                ParagraphStyle(textAlign = TextAlign.Start, textIndent = TextIndent(firstLine = 2.em, restLine = 2.em)),
            ) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(block.text) }
            }
            is Block.Image -> Unit
        }
    }
}

/** Пустая строка половинной высоты: воздух вокруг заголовка. */
private fun AnnotatedString.Builder.spacer() {
    withStyle(ParagraphStyle(lineHeight = 0.8.em)) { append(' ') }
}

private fun headingSize(level: Int) = when (level) {
    1 -> 1.4.em
    2 -> 1.25.em
    else -> 1.1.em
}
