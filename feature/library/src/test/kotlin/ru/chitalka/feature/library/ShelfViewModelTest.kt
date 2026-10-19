package ru.chitalka.feature.library

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.chitalka.core.domain.ImportResult
import ru.chitalka.core.model.Book
import ru.chitalka.core.model.BookFormat
import ru.chitalka.core.testing.FakeBookImporter
import ru.chitalka.core.testing.FakeBookRepository
import ru.chitalka.core.testing.MainDispatcherRule
import java.time.Instant

class ShelfViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository(
        listOf(
            book(1, "Белая гвардия", "Булгаков", opened = 100),
            book(2, "Анна Каренина", "Толстой", opened = 300),
            book(3, "Вишнёвый сад", "Чехов", opened = null),
        ),
    )

    private val importer = FakeBookImporter()

    @Test
    fun recentlyReadComesFirstByDefault() = runTest {
        val viewModel = ShelfViewModel(repository, importer)
        viewModel.uiState.test {
            val state = awaitLoaded()
            assertEquals(listOf(2L, 1L, 3L), state.books.map { it.id })
            assertEquals(2L, state.continueReading?.id)
        }
    }

    @Test
    fun changingSortReordersBooks() = runTest {
        val viewModel = ShelfViewModel(repository, importer)
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(ShelfEvent.ChangeSort(ShelfSort.Title))
            val state = awaitItem()
            assertEquals(ShelfSort.Title, state.sort)
            assertEquals(listOf("Анна Каренина", "Белая гвардия", "Вишнёвый сад"), state.books.map { it.title })
        }
    }

    @Test
    fun importReportsResultAndDeleteRemovesBook() = runTest {
        importer.results = listOf(ImportResult.Imported(4, "Мёртвые души"))
        val viewModel = ShelfViewModel(repository, importer)
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(ShelfEvent.ImportBooks(listOf("content://книга.fb2")))
            assertEquals("«Мёртвые души» теперь в шкафу", expectMostRecentItem().message)
            viewModel.onEvent(ShelfEvent.MessageShown)
            viewModel.onEvent(ShelfEvent.DeleteBook(repository.observeBooksNow().first()))
            val state = expectMostRecentItem()
            assertEquals(2, state.books.size)
            assertEquals("«Белая гвардия» убрана из шкафа", state.message)
        }
        assertEquals(listOf("content://книга.fb2"), importer.imported)
    }

    @Test
    fun importMessageSummarizesMixedResults() {
        val message = importMessage(
            listOf(
                ImportResult.Imported(1, "А"),
                ImportResult.Imported(2, "Б"),
                ImportResult.Duplicate(3),
                ImportResult.Failed("фото.jpg", "Это не EPUB и не FB2"),
            ),
        )
        assertEquals(
            "Добавлено книг: 2. Уже были в шкафу: 1. Не получилось открыть «фото.jpg»: Это не EPUB и не FB2",
            message,
        )
    }

    private suspend fun FakeBookRepository.observeBooksNow() = observeBooks().first()

    private suspend fun app.cash.turbine.ReceiveTurbine<ShelfUiState>.awaitLoaded(): ShelfUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    private fun book(id: Long, title: String, author: String, opened: Long?) = Book(
        id = id,
        title = title,
        author = author,
        format = BookFormat.EPUB,
        addedAt = Instant.ofEpochSecond(id),
        lastOpenedAt = opened?.let(Instant::ofEpochSecond),
    )
}
