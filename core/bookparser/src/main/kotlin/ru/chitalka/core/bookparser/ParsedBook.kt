package ru.chitalka.core.bookparser

import ru.chitalka.core.model.BookFormat

/**
 * Единая внутренняя модель книги: любой формат превращается в неё.
 * Новый формат = новый [BookParser], а не новая читалка.
 */
data class ParsedBook(
    val metadata: BookMetadata,
    val toc: List<TocItem>,
    val chapterCount: Int,
)

data class BookMetadata(
    val title: String,
    val author: String?,
    val series: String? = null,
    val seriesIndex: Int? = null,
    val language: String? = null,
    val format: BookFormat,
)

data class TocItem(
    val title: String,
    val level: Int,
    val chapterIndex: Int,
    val anchor: String? = null,
)

/** Глава — список блоков. Главы читаются по требованию, вся книга в память не грузится. */
data class Chapter(
    val index: Int,
    val title: String?,
    val blocks: List<Block>,
)

sealed interface Block {
    data class Paragraph(val text: String) : Block
    data class Heading(val text: String, val level: Int) : Block
    data class Quote(val text: String) : Block
    data class Image(val path: String, val alt: String?) : Block
    data class Footnote(val id: String, val text: String) : Block
}
