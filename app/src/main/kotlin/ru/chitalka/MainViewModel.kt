package ru.chitalka

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ru.chitalka.core.domain.SettingsRepository
import ru.chitalka.core.model.ReaderSettings
import javax.inject.Inject

/** Следит за настройками, чтобы смена темы сразу перекрашивала всё приложение. */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<ReaderSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)
}
