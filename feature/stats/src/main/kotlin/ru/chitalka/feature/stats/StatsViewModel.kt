package ru.chitalka.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.chitalka.core.domain.ReadingStatsRepository
import ru.chitalka.core.domain.SettingsRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class StatsUiState(
    val stats: ReadingStats = ReadingStats(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    statsRepository: ReadingStatsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val zone: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<StatsUiState> = combine(
        // Год с запасом: хватит и на лучшую серию, и на «Книг за год»
        statsRepository.observeDays(today.minusDays(400)),
        statsRepository.observeFinishedAt(),
        settingsRepository.settings,
    ) { days, finished, settings ->
        StatsUiState(stats = buildStats(days, finished, settings.dailyGoalMinutes, today, zone), isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun setGoal(minutes: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(minutes) }
    }
}
