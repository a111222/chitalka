package ru.chitalka.core.bookparser

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import ru.chitalka.core.model.Block

/**
 * Превращает HTML главы EPUB в плоский список блоков.
 * Вёрстку книги не повторяем: берём смысл (абзац, заголовок, цитата, картинка),
 * а внешний вид задаёт читалка — так все книги выглядят одинаково аккуратно.
 */
internal object HtmlBlocks {
    private val HEADINGS = mapOf("h1" to 1, "h2" to 2, "h3" to 3, "h4" to 4, "h5" to 5, "h6" to 6)
    private val TEXT_BLOCKS = setOf("p", "li", "dt", "dd", "pre", "figcaption", "caption", "td", "th", "address")
    private val INLINE = setOf(
        "a", "abbr", "b", "big", "cite", "code", "del", "dfn", "em", "font", "i", "ins", "kbd", "mark",
        "q", "s", "samp", "small", "span", "strike", "strong", "sub", "sup", "tt", "u", "var", "wbr",
    )
    private val SKIP = setOf("head", "script", "style", "title", "noscript")
    private val IMAGES = setOf("img", "image")
    private const val NOT_INLINE = "img, image, p, div, h1, h2, h3, h4, h5, h6, blockquote, li, table, br"

    /** @param resolve превращает src картинки в путь внутри книги */
    fun parse(root: Element, resolve: (String) -> String): List<Block> =
        Walker(resolve).apply { walk(root); flush() }.blocks

    private class Walker(private val resolve: (String) -> String) {
        val blocks = mutableListOf<Block>()
        private val pending = StringBuilder()

        fun flush() {
            pending.toString().cleanText()?.let { blocks += Block.Paragraph(it) }
            pending.clear()
        }

        fun walk(element: Element) {
            for (node in element.childNodes()) {
                when (node) {
                    is TextNode -> pending.append(node.text())
                    is Element -> handle(node)
                }
            }
        }

        private fun handle(e: Element) {
            val tag = e.normalName()
            when {
                tag in SKIP -> Unit
                tag in HEADINGS -> {
                    flush()
                    e.text().cleanText()?.let { blocks += Block.Heading(it, HEADINGS.getValue(tag)) }
                }
                tag in TEXT_BLOCKS -> {
                    flush()
                    images(e)
                    e.text().cleanText()?.let { blocks += Block.Paragraph(it) }
                }
                tag == "blockquote" -> {
                    flush()
                    val parts = e.children().filter { it.normalName() in TEXT_BLOCKS || it.normalName() == "div" }
                    val texts = if (parts.isEmpty()) listOf(e.text()) else parts.map { it.text() }
                    texts.mapNotNull { it.cleanText() }.forEach { blocks += Block.Quote(it) }
                }
                tag in IMAGES -> {
                    flush()
                    image(e)
                }
                tag == "br" || tag == "hr" -> flush()
                tag in INLINE && e.selectFirst(NOT_INLINE) == null -> pending.append(e.text())
                else -> {
                    // div, section, table, svg и всё незнакомое: заходим внутрь
                    val isBlock = tag !in INLINE
                    if (isBlock) flush()
                    walk(e)
                    if (isBlock) flush()
                }
            }
        }

        private fun images(e: Element) {
            e.select("img, image").forEach(::image)
        }

        private fun image(e: Element) {
            val src = e.attr("src").ifEmpty { e.attr("xlink:href") }.ifEmpty { e.attr("href") }
            if (src.isNotBlank() && !src.startsWith("data:")) {
                blocks += Block.Image(resolve(src), e.attr("alt").ifBlank { null })
            }
        }
    }
}

/** Схлопывает пробелы (включая неразрывные по краям) и возвращает null для пустой строки. */
internal fun String.cleanText(): String? =
    replace(WHITESPACE, " ").trim { it.isWhitespace() || it == '\u00A0' }.ifEmpty { null }

// Неразрывные пробелы внутри строки сохраняем: «— Да» не должно разрываться после тире
private val WHITESPACE = Regex("\\s+")
