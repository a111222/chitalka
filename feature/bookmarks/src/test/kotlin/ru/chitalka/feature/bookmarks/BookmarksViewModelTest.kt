package ru.chitalka.feature.bookmarks

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import ru.chitalka.core.model.Bookmark
import ru.chitalka.core.model.Highlight
import ru.chitalka.core.model.ReadingPosition
import ru.chitalka.core.testing.FakeAnnotationRepository
import ru.chitalka.core.testing.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class BookmarksViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAnnotationRepository()

    @Test
    fun listsNewestFirstAndSwitchesTabs() = runTest {
        repository.addHighlight(highlight("Старая", at = 1))
        repository.addHighlight(highlight("Новая", at = 2))
        repository.addBookmark(Bookmark(bookId = 3, position = ReadingPosition(1, 0), label = "Глава 2", snippet = "", percent = 0.3f, createdAt = Instant.EPOCH))
        val vm = BookmarksViewModel(repository)
        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals(listOf("Новая", "Старая"), state.quotes.map { it.item.text })
            assertEquals(MarksTab.Quotes, state.tab)
            vm.onEvent(BookmarksEvent.SelectTab(MarksTab.Bookmarks))
            state = awaitItem()
            assertEquals(MarksTab.Bookmarks, state.tab)
            assertEquals("Глава 2", state.bookmarks.single().item.label)
        }
    }

    @Test
    fun deletedQuoteComesBackOnUndo() = runTest {
        repository.addHighlight(highlight("Фраза", at = 1))
        val vm = BookmarksViewModel(repository)
        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onEvent(BookmarksEvent.DeleteQuote(state.quotes.single().item))
            state = expectMostRecentItem()
            assertEquals(0, state.quotes.size)
            assertEquals("Цитата удалена", state.undo?.message)

            vm.onEvent(BookmarksEvent.Undo)
            state = expectMostRecentItem()
            assertEquals("Фраза", state.quotes.single().item.text)
            assertNull(state.undo)
        }
    }

    @Test
    fun relativeDays() {
        val today = LocalDate.of(2026, 10, 19)
        fun at(date: LocalDate) = date.atTime(12, 0).toInstant(ZoneOffset.UTC)
        assertEquals("сегодня", relativeDay(at(today), today, ZoneOffset.UTC))
        assertEquals("вчера", relativeDay(at(today.minusDays(1)), today, ZoneOffset.UTC))
        assertEquals("17 окт.", relativeDay(at(today.minusDays(2)), today, ZoneOffset.UTC))
        assertEquals("3 мая 2025", relativeDay(at(LocalDate.of(2025, 5, 3)), today, ZoneOffset.UTC))
    }

    private fun highlight(text: String, at: Long) = Highlight(
        bookId = 1,
        chapterIndex = 0,
        start = 0,
        end = text.length,
        text = text,
        percent = 0.1f,
        createdAt = Instant.ofEpochSecond(at),
    )
}
