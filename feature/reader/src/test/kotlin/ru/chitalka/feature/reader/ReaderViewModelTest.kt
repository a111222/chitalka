package ru.chitalka.feature.reader

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.testing.FakeBookRepository
import ru.chitalka.core.testing.FakeSettingsRepository
import ru.chitalka.core.testing.MainDispatcherRule

class ReaderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = ReaderViewModel(
        savedStateHandle = SavedStateHandle(mapOf("bookId" to 0L)),
        bookRepository = FakeBookRepository(),
        settingsRepository = FakeSettingsRepository(ReaderSettings(font = ReaderFont.Lora)),
    )

    @Test
    fun sampleBookOpensOnFirstPageWithUserSettings() = runTest {
        viewModel().uiState.test {
            val state = awaitLoaded()
            assertEquals(SampleBook.TITLE, state.title)
            assertEquals(0, state.currentPage)
            assertEquals(ReaderFont.Lora, state.settings.font)
        }
    }

    @Test
    fun pagesTurnWithinBounds() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(ReaderEvent.PreviousPage) // на первой странице назад некуда
            vm.onEvent(ReaderEvent.NextPage)
            assertEquals(1, expectMostRecentItem().currentPage)
            repeat(10) { vm.onEvent(ReaderEvent.NextPage) }
            assertEquals(SampleBook.pages.lastIndex, expectMostRecentItem().currentPage)
        }
    }

    private suspend fun ReceiveTurbine<ReaderUiState>.awaitLoaded(): ReaderUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }
}
