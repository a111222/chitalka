package ru.chitalka.feature.settings

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode
import ru.chitalka.core.testing.FakeSettingsRepository
import ru.chitalka.core.testing.MainDispatcherRule

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun changingThemeIsSaved() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        viewModel.uiState.test {
            skipItems(1) // начальное состояние «загрузка»
            viewModel.onEvent(SettingsEvent.ChangeTheme(ReaderThemeMode.Midnight))
            assertEquals(ReaderThemeMode.Midnight, expectMostRecentItem().settings.theme)
        }
    }

    @Test
    fun fontSizeIsClampedToDesignRange() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        viewModel.uiState.test {
            skipItems(1)
            viewModel.onEvent(SettingsEvent.ChangeFontSize(99))
            assertEquals(ReaderSettings.MAX_FONT_SIZE, expectMostRecentItem().settings.fontSizeSp)
        }
    }
}
