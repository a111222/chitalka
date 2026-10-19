package ru.chitalka.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.chitalka.core.domain.SettingsRepository
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode
import javax.inject.Inject

data class SettingsUiState(
    val settings: ReaderSettings = ReaderSettings(),
    val isLoading: Boolean = true,
)

sealed interface SettingsEvent {
    data class ChangeTheme(val theme: ReaderThemeMode) : SettingsEvent
    data class ChangeFont(val font: ReaderFont) : SettingsEvent
    data class ChangeFontSize(val sizeSp: Int) : SettingsEvent
    data class ChangeLineSpacing(val spacing: LineSpacing) : SettingsEvent
    data class ToggleHaptics(val enabled: Boolean) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { SettingsUiState(settings = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.ChangeTheme -> settingsRepository.setTheme(event.theme)
                is SettingsEvent.ChangeFont -> settingsRepository.setFont(event.font)
                is SettingsEvent.ChangeFontSize -> settingsRepository.setFontSize(
                    event.sizeSp.coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE),
                )
                is SettingsEvent.ChangeLineSpacing -> settingsRepository.setLineSpacing(event.spacing)
                is SettingsEvent.ToggleHaptics -> settingsRepository.setHapticsEnabled(event.enabled)
            }
        }
    }
}
