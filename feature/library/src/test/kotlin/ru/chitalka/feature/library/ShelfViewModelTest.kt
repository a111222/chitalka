package ru.chitalka.feature.library

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.chitalka.core.model.Book
import ru.chitalka.core.model.BookFormat
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

    @Test
    fun recentlyReadComesFirstByDefault() = runTest {
        val viewModel = ShelfViewModel(repository)
        viewModel.uiState.test {
            val state = awaitLoaded()
            assertEquals(listOf(2L, 1L, 3L), state.books.map { it.id })
            assertEquals(2L, state.continueReading?.id)
        }
    }

    @Test
    fun changingSortReordersBooks() = runTest {
        val viewModel = ShelfViewModel(repository)
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(ShelfEvent.ChangeSort(ShelfSort.Title))
            val state = awaitItem()
            assertEquals(ShelfSort.Title, state.sort)
            assertEquals(listOf("Анна Каренина", "Белая гвардия", "Вишнёвый сад"), state.books.map { it.title })
        }
    }

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
