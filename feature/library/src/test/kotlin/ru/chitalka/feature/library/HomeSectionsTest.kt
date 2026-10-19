package ru.chitalka.feature.library

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.chitalka.core.model.Book
import ru.chitalka.core.model.BookFormat
import java.time.Instant

class HomeSectionsTest {
    private val books = listOf(
        book(1, "Анна Каренина", "Лев Толстой", progress = 0.4f, added = 10),
        book(2, "Война и мир", "Толстой Лев Николаевич", progress = 1f, added = 20),
        book(3, "Вишнёвый сад", "А. Чехов", added = 30),
        book(4, "Мёртвые души", "Николай Гоголь", progress = 0.1f, added = 40),
        book(5, "Без автора", null, added = 50),
    )

    @Test
    fun shelvesCountBooksByReadingState() {
        val shelves = HomeSections.shelves(books).associate { it.name to it.count }
        assertEquals(mapOf("Читаю" to 2, "Хочу прочитать" to 2, "Прочитано" to 1, "Все книги" to 5), shelves)
    }

    @Test
    fun rowsPickNewestAndInProgress() {
        assertEquals(listOf(5L, 4L, 3L, 2L), HomeSections.recentlyAdded(books).map { it.id })
        assertEquals(listOf(1L, 4L), HomeSections.readingNow(books).map { it.id })
    }

    @Test
    fun authorsAreGroupedBySurname() {
        val authors = HomeSections.authors(books)
        assertEquals(listOf("Толстой", "Гоголь", "Чехов"), authors.map { it.name })
        assertEquals(2, authors.first().books)
        assertEquals('Т', authors.first().monogram)
    }

    @Test
    fun surnameHandlesCommonAuthorFormats() {
        assertEquals("Толстой", HomeSections.surnameOf("Лев Толстой"))
        assertEquals("Толстой", HomeSections.surnameOf("Толстой Лев Николаевич"))
        assertEquals("Чехов", HomeSections.surnameOf("А. Чехов"))
        assertEquals("Ахматова", HomeSections.surnameOf("Анна Андреевна Ахматова"))
    }

    @Test
    fun booksCountUsesRussianPlural() {
        assertEquals("1 книга", HomeSections.booksCount(1))
        assertEquals("3 книги", HomeSections.booksCount(3))
        assertEquals("12 книг", HomeSections.booksCount(12))
        assertEquals("21 книга", HomeSections.booksCount(21))
        assertEquals("0 книг", HomeSections.booksCount(0))
    }

    @Test
    fun authorsPyramidPutsTopAuthorInTheMiddle() {
        assertEquals(listOf(3, 1, 0, 2, 4), HomeSections.pyramidOrder(5))
        assertEquals(listOf(1, 0), HomeSections.pyramidOrder(2))
    }

    private fun book(id: Long, title: String, author: String?, progress: Float = 0f, added: Long) = Book(
        id = id,
        title = title,
        author = author,
        format = BookFormat.EPUB,
        addedAt = Instant.ofEpochSecond(added),
        progressPercent = progress,
    )
}
