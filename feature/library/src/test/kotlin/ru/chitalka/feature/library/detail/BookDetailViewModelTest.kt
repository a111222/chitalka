package ru.chitalka.feature.library.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.chitalka.core.model.Block
import ru.chitalka.core.model.Book
import ru.chitalka.core.model.BookFormat
import ru.chitalka.core.model.Chapter
import ru.chitalka.core.testing.FakeBookContentRepository
import ru.chitalka.core.testing.FakeBookRepository
import ru.chitalka.core.testing.MainDispatcherRule
import ru.chitalka.feature.library.SAMPLE_BOOK_ID
import java.time.Instant

class BookDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val book = Book(
        id = 7,
        title = "Анна Каренина",
        author = "Лев Толстой",
        format = BookFormat.FB2,
        addedAt = Instant.EPOCH,
        progressPercent = 0.42f,
    )

    private fun viewModel(
        bookId: Long = book.id,
        books: FakeBookRepository = FakeBookRepository(listOf(book)),
        content: FakeBookContentRepository = FakeBookContentRepository(),
    ) = BookDetailViewModel(SavedStateHandle(mapOf("bookId" to bookId)), books, content)

    @Test
    fun showsAnnotationAndGenreFromFile() = runTest {
        val content = FakeBookContentRepository(chapterCount = 12, description = "Роман о семье.", genres = listOf("Русская классика"))
        viewModel(content = content).uiState.test {
            val detail = awaitFileDetails()
            assertEquals("Анна Каренина", detail.title)
            assertEquals("FB2", detail.format)
            assertEquals(0.42f, detail.progress)
            assertEquals(12, detail.chapterCount)
            assertEquals("Русская классика", detail.genre)
            assertEquals("Роман о семье.", detail.description)
            assertFalse(detail.descriptionIsExcerpt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun withoutAnnotationShowsBeginningOfBook() = runTest {
        val content = FakeBookContentRepository(chapterCount = 3)
        viewModel(content = content).uiState.test {
            val detail = awaitFileDetails()
            assertEquals("Глава 1\n\nГлава 2\n\nГлава 3", detail.description)
            assertTrue(detail.descriptionIsExcerpt)
            assertNull(detail.genre)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletedBookClosesCard() = runTest {
        val books = FakeBookRepository(listOf(book))
        viewModel(books = books).uiState.test {
            awaitFileDetails()
            books.setBooks(emptyList())
            // Карточка закрывается, когда книги больше нет
            while (!awaitItem().isGone) Unit
        }
    }

    @Test
    fun sampleBookNeedsNoFile() = runTest {
        viewModel(bookId = SAMPLE_BOOK_ID).uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals(SampleDetail, state.book)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun excerptStopsAtWordBoundary() {
        val long = "слово ".repeat(200).trim()
        val excerpt = BookDetailViewModel.excerptOf(
            listOf(Chapter(0, "Глава", listOf(Block.Heading("Глава", 1), Block.Paragraph("Начало."), Block.Paragraph(long)))),
        )!!
        assertTrue(excerpt.startsWith("Начало.\n\nслово"))
        assertTrue(excerpt.endsWith("слово…"))
        assertTrue(excerpt.length <= BookDetailViewModel.EXCERPT_CHARS + 3)
    }

    private suspend fun ReceiveTurbine<BookDetailUiState>.awaitFileDetails(): BookDetail {
        while (true) {
            val detail = awaitItem().book
            if (detail?.chapterCount != null) return detail
        }
    }
}
