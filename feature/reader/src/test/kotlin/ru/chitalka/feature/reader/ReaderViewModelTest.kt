package ru.chitalka.feature.reader

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import ru.chitalka.core.model.Book
import ru.chitalka.core.model.BookFormat
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderThemeMode
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReadingPosition
import ru.chitalka.core.testing.FakeBookContentRepository
import ru.chitalka.core.testing.FakeBookRepository
import ru.chitalka.core.testing.FakeSettingsRepository
import ru.chitalka.core.testing.MainDispatcherRule
import java.time.Instant

class ReaderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(listOf(Book(7, "Книга", "Автор", format = BookFormat.EPUB, addedAt = Instant.EPOCH)))
    private val content = FakeBookContentRepository(chapterCount = 3)

    private fun viewModel(bookId: Long) = ReaderViewModel(
        savedStateHandle = SavedStateHandle(mapOf("bookId" to bookId)),
        bookRepository = books,
        contentRepository = content,
        settingsRepository = FakeSettingsRepository(ReaderSettings(font = ReaderFont.Lora)),
    )

    @Test
    fun sampleBookOpensWithUserSettings() = runTest {
        viewModel(SAMPLE_BOOK_ID).uiState.test {
            val state = awaitLoaded()
            assertEquals(SampleBook.TITLE, state.title)
            assertEquals(1, state.chapterCount)
            assertEquals(ReaderFont.Lora, state.settings.font)
            assertEquals(SampleBook.chapter, state.chapter?.chapter)
        }
    }

    @Test
    fun bookOpensAtSavedPosition() = runTest {
        books.savePosition(7, ReadingPosition(chapterIndex = 2, charOffset = 40), percent = 0.8f)
        viewModel(7).uiState.test {
            val state = awaitLoaded()
            assertEquals("Тестовая книга", state.title)
            assertEquals(ReadingPosition(2, 40), state.position)
            assertEquals(2, state.chapter?.chapter?.index)
        }
    }

    @Test
    fun goToAnotherChapterLoadsIt() = runTest {
        val vm = viewModel(7)
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(ReaderEvent.GoTo(ReadingPosition(1, 0)))
            assertEquals(1, expectMostRecentItem().chapter?.chapter?.index)
            vm.onEvent(ReaderEvent.GoTo(ReadingPosition(5, 0))) // такой главы нет — состояние не меняется
            expectNoEvents()
            assertEquals(ReadingPosition(1, 0), vm.uiState.value.position)
        }
        assertEquals(listOf(0, 1), content.requestedChapters)
    }

    @Test
    fun shownPageIsSavedButSampleIsNot() = runTest {
        val vm = viewModel(7)
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(ReaderEvent.PageShown(ReadingPosition(0, 120), percent = 0.1f))
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(ReadingPosition(0, 120), books.positionOf(7))

        val sample = viewModel(SAMPLE_BOOK_ID)
        sample.uiState.test {
            awaitLoaded()
            sample.onEvent(ReaderEvent.PageShown(ReadingPosition(0, 10), percent = 0.5f))
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(books.positionOf(SAMPLE_BOOK_ID))
    }

    @Test
    fun sheetChangesSettingsAndKeepsSizeInRange() = runTest {
        val vm = viewModel(7)
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(ReaderEvent.ChangeFont(ReaderFont.PtSerif))
            vm.onEvent(ReaderEvent.ChangeFontSize(99))
            vm.onEvent(ReaderEvent.ChangeSpacing(LineSpacing.Relaxed))
            vm.onEvent(ReaderEvent.ChangeTheme(ReaderThemeMode.Warm))
            val settings = expectMostRecentItem().settings
            assertEquals(ReaderFont.PtSerif, settings.font)
            assertEquals(ReaderSettings.MAX_FONT_SIZE, settings.fontSizeSp)
            assertEquals(LineSpacing.Relaxed, settings.lineSpacing)
            assertEquals(ReaderThemeMode.Warm, settings.theme)
        }
    }

    private suspend fun ReceiveTurbine<ReaderUiState>.awaitLoaded(): ReaderUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }
}
