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
import ru.chitalka.core.testing.FakeAnnotationRepository
import ru.chitalka.core.testing.FakeBookContentRepository
import ru.chitalka.core.testing.FakeBookRepository
import ru.chitalka.core.testing.FakeSettingsRepository
import ru.chitalka.core.testing.FakeReadingStatsRepository
import ru.chitalka.core.testing.MainDispatcherRule
import ru.chitalka.core.model.HighlightColor
import java.time.Instant
import java.time.LocalDate

class ReaderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(listOf(Book(7, "Книга", "Автор", format = BookFormat.EPUB, addedAt = Instant.EPOCH)))
    private val content = FakeBookContentRepository(chapterCount = 3)
    private val annotations = FakeAnnotationRepository()
    private val stats = FakeReadingStatsRepository()
    private var clock = 1_000_000L
    private val day = LocalDate.of(2026, 10, 19)

    private fun viewModel(bookId: Long, args: Map<String, Any> = emptyMap()) = ReaderViewModel(
        savedStateHandle = SavedStateHandle(mapOf<String, Any>("bookId" to bookId) + args),
        bookRepository = books,
        contentRepository = content,
        settingsRepository = FakeSettingsRepository(ReaderSettings(font = ReaderFont.Lora)),
        annotationRepository = annotations,
        statsRepository = stats,
    ).apply {
        now = { clock }
        today = { day }
    }

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
        // Каждая глава грузится один раз: соседние берутся про запас и потом не перечитываются
        assertEquals(listOf(0, 1, 2), content.requestedChapters)
    }

    @Test
    fun neighbourChaptersArePreloadedForPageCurl() = runTest {
        val vm = viewModel(7)
        vm.uiState.test {
            awaitLoaded()
            assertEquals(listOf(0, 1), vm.uiState.value.nearby.map { it.chapter.index })
            vm.onEvent(ReaderEvent.GoTo(ReadingPosition(1, 0)))
            assertEquals(listOf(1, 2, 0), vm.uiState.value.nearby.map { it.chapter.index })
            cancelAndIgnoreRemainingEvents()
        }
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

    @Test
    fun bookmarkFromListOpensItsPlaceInsteadOfSaved() = runTest {
        books.savePosition(7, ReadingPosition(chapterIndex = 2, charOffset = 40), percent = 0.8f)
        viewModel(7, mapOf("chapterIndex" to 1, "charOffset" to 5)).uiState.test {
            assertEquals(ReadingPosition(1, 5), awaitLoaded().position)
        }
    }

    @Test
    fun highlightIsSavedRecoloredAndDeleted() = runTest {
        val vm = viewModel(7)
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(ReaderEvent.AddHighlight(0, 2, 9, "  фраза ", percent = 0.1f))
            val saved = expectMostRecentItem().highlights.single()
            assertEquals("фраза", saved.text)
            assertEquals(HighlightColor.Teal, saved.color)

            vm.onEvent(ReaderEvent.ChangeHighlightColor(saved.id, HighlightColor.Amber))
            assertEquals(HighlightColor.Amber, expectMostRecentItem().highlights.single().color)

            vm.onEvent(ReaderEvent.DeleteHighlight(saved.id))
            assertEquals(emptyList<Any>(), expectMostRecentItem().highlights)
        }
    }

    @Test
    fun bookmarkTogglesOnTheVisiblePage() = runTest {
        val vm = viewModel(7)
        vm.uiState.test {
            awaitLoaded()
            val toggle = ReaderEvent.ToggleBookmark(ReadingPosition(1, 100), endOffset = 400, label = "Глава 2", snippet = "Текст", percent = 0.4f)
            vm.onEvent(toggle)
            assertEquals(ReadingPosition(1, 100), expectMostRecentItem().bookmarks.single().position)
            // Та же страница, но открытая с другого символа: закладка снимается, а не добавляется вторая
            vm.onEvent(toggle.copy(position = ReadingPosition(1, 90)))
            assertEquals(emptyList<Any>(), expectMostRecentItem().bookmarks)
        }
    }

    @Test
    fun readingTimeAndForwardPagesGoToStats() = runTest {
        val vm = viewModel(7)
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(ReaderEvent.Resumed)
            vm.onEvent(ReaderEvent.PageShown(ReadingPosition(0, 0), percent = 0f)) // открыли книгу: страница не в счёт
            clock += 60_000
            vm.onEvent(ReaderEvent.PageShown(ReadingPosition(0, 500), percent = 0.1f))
            clock += 30_000
            vm.onEvent(ReaderEvent.PageShown(ReadingPosition(0, 0), percent = 0f)) // назад: время идёт, страница нет
            clock += 20 * 60_000 // отложили телефон
            vm.onEvent(ReaderEvent.Paused)
            clock += 60_000 // свёрнуто: не считается
            vm.onEvent(ReaderEvent.PageShown(ReadingPosition(0, 500), percent = 0.1f))
            cancelAndIgnoreRemainingEvents()
        }
        val today = stats.days.value.single()
        assertEquals(day, today.date)
        assertEquals(60_000L + 30_000L + ReadingTimer.IDLE_LIMIT_MS, today.durationMs)
        assertEquals(2, today.pages)
    }

    private suspend fun ReceiveTurbine<ReaderUiState>.awaitLoaded(): ReaderUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }
}
