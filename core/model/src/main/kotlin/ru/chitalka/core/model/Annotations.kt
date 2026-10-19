package ru.chitalka.core.model

import java.time.Instant

/**
 * Закладка на странице. Место хранится как [ReadingPosition] (глава + смещение), а не номер страницы:
 * после смены шрифта закладка всё равно откроется там, где её поставили.
 */
data class Bookmark(
    val id: Long = 0,
    val bookId: Long,
    val position: ReadingPosition,
    /** Название главы: заголовок карточки во вкладке «Закладки». */
    val label: String,
    /** Начало текста страницы, чтобы закладку можно было узнать. */
    val snippet: String,
    /** Сколько книги было прочитано к этой странице, 0..1. */
    val percent: Float,
    val createdAt: Instant,
)

/** Фраза, выделенная маркером. [start]..[end] — смещения в тексте главы [chapterIndex], как у [ReadingPosition]. */
data class Highlight(
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val start: Int,
    val end: Int,
    val text: String,
    val color: HighlightColor = HighlightColor.Teal,
    val percent: Float,
    val createdAt: Instant,
) {
    val position: ReadingPosition get() = ReadingPosition(chapterIndex, start)

    fun contains(chapter: Int, offset: Int): Boolean = chapter == chapterIndex && offset in start until end
}

/** Цвета маркера из макета v3: бирюзовый по умолчанию, ещё розовый, янтарный и фиолетовый. */
enum class HighlightColor(val argb: Long) {
    Teal(0xFF1FB5A0),
    Pink(0xFFFF4F8B),
    Amber(0xFFFFC24B),
    Violet(0xFF9B5CFF),
}

/** Закладка или цитата вместе с книгой, из которой она: для общего списка во вкладке «Закладки». */
data class WithBook<T>(
    val item: T,
    val bookTitle: String,
    val bookAuthor: String?,
    val coverPath: String?,
)

/** Демонстрационная книга «Сад за рекой»: её нет в базе, но в ней тоже можно ставить закладки и выделять. */
object DemoBook {
    const val ID = 0L
    const val TITLE = "Сад за рекой"
    const val AUTHOR = "Демонстрационный текст"
}
