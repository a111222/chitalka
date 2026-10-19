package ru.chitalka.core.model

/**
 * Содержимое открытой книги: всё, что нужно экрану чтения, кроме самих глав.
 * Главы грузятся по одной ([Chapter]), вся книга в память не попадает.
 */
data class BookContent(
    val title: String,
    val author: String?,
    val language: String?,
    val chapterCount: Int,
    val toc: List<TocEntry>,
)

/** Пункт оглавления. level = 0 для верхнего уровня, 1 для вложенного и т. д. */
data class TocEntry(
    val title: String,
    val level: Int,
    val chapterIndex: Int,
)

/** Глава — список блоков. Любой формат (EPUB, FB2) превращается в эту модель. */
data class Chapter(
    val index: Int,
    val title: String?,
    val blocks: List<Block>,
) {
    /** Глава без текста, например обложка из одной картинки. */
    val hasText: Boolean get() = blocks.any { it !is Block.Image }
}

sealed interface Block {
    data class Paragraph(val text: String) : Block
    data class Heading(val text: String, val level: Int) : Block
    data class Quote(val text: String) : Block

    /** path — путь картинки внутри файла книги (для EPUB — путь в ZIP, для FB2 — id в binary). */
    data class Image(val path: String, val alt: String? = null) : Block
}
