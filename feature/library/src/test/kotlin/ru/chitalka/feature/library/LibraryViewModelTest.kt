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

class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository(
        listOf(
            book(1, "Белая гвардия", "Булгаков", opened = 100, progress = 1f),
            book(2, "Анна Каренина", "Толстой", opened = 300, progress = 0.4f),
            book(3, "Вишнёвый сад", "Чехов", opened = null),
        ),
    )

    private val importer = FakeBookImporter()

    @Test
    fun recentlyReadComesFirst() = runTest {
        val viewModel = LibraryViewModel(repository, importer)
        viewModel.uiState.test {
            val state = awaitLoaded()
            assertEquals(listOf(2L, 1L, 3L), state.books.map { it.id })
            assertEquals(state.books, state.shown)
        }
    }

    @Test
    fun filterKeepsOnlyMatchingBooks() = runTest {
        val viewModel = LibraryViewModel(repository, importer)
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(LibraryEvent.ChangeFilter(LibraryFilter.Reading))
            assertEquals(listOf(2L), expectMostRecentItem().shown.map { it.id })
            viewModel.onEvent(LibraryEvent.ChangeFilter(LibraryFilter.Finished))
            assertEquals(listOf(1L), expectMostRecentItem().shown.map { it.id })
            viewModel.onEvent(LibraryEvent.ChangeFilter(LibraryFilter.WantToRead))
            assertEquals(listOf(3L), expectMostRecentItem().shown.map { it.id })
        }
    }

    @Test
    fun searchNarrowsShownBooks() = runTest {
        val viewModel = LibraryViewModel(repository, importer)
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(LibraryEvent.ChangeQuery("чехов"))
            assertEquals(listOf(3L), expectMostRecentItem().shown.map { it.id })
            viewModel.onEvent(LibraryEvent.ChangeQuery(""))
            assertEquals(3, expectMostRecentItem().shown.size)
        }
    }

    @Test
    fun initialFilterFromShelfIsAppliedOnce() = runTest {
        val viewModel = LibraryViewModel(repository, importer)
        viewModel.applyInitialFilter(LibraryFilter.Finished)
        viewModel.onEvent(LibraryEvent.ChangeFilter(LibraryFilter.All))
        // Поворот экрана: фильтр с полки не должен затереть выбор пользователя
        viewModel.applyInitialFilter(LibraryFilter.Finished)
        viewModel.uiState.test {
            assertEquals(LibraryFilter.All, awaitLoaded().filter)
        }
    }

    @Test
    fun importReportsResultAndDeleteRemovesBook() = runTest {
        importer.results = listOf(ImportResult.Imported(4, "Мёртвые души"))
        val viewModel = LibraryViewModel(repository, importer)
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(LibraryEvent.ImportBooks(listOf("content://книга.fb2")))
            assertEquals("«Мёртвые души» теперь в библиотеке", expectMostRecentItem().message)
            viewModel.onEvent(LibraryEvent.MessageShown)
            viewModel.onEvent(LibraryEvent.DeleteBook(repository.observeBooksNow().first()))
            val state = expectMostRecentItem()
            assertEquals(2, state.books.size)
            assertEquals("«Белая гвардия» убрана из библиотеки", state.message)
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
            "Добавлено книг: 2. Уже были в библиотеке: 1. Не получилось открыть «фото.jpg»: Это не EPUB и не FB2",
            message,
        )
    }

    private suspend fun FakeBookRepository.observeBooksNow() = observeBooks().first()

    private suspend fun app.cash.turbine.ReceiveTurbine<LibraryUiState>.awaitLoaded(): LibraryUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    private fun book(id: Long, title: String, author: String, opened: Long?, progress: Float = 0f) = Book(
        id = id,
        title = title,
        author = author,
        format = BookFormat.EPUB,
        addedAt = Instant.ofEpochSecond(id),
        lastOpenedAt = opened?.let(Instant::ofEpochSecond),
        progressPercent = progress,
    )
}
