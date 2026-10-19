package ru.chitalka

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.chitalka.core.domain.BookImporter
import ru.chitalka.core.domain.ImportResult
import ru.chitalka.core.domain.SettingsRepository
import ru.chitalka.core.model.ReaderSettings
import javax.inject.Inject

/**
 * Следит за настройками, чтобы смена темы сразу перекрашивала всё приложение,
 * и открывает книги, присланные через «Открыть с помощью».
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val bookImporter: BookImporter,
) : ViewModel() {
    val settings: StateFlow<ReaderSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    private val openBookChannel = Channel<Long>(Channel.BUFFERED)

    /** id книги, которую нужно сразу открыть. Channel: каждое событие обрабатывается ровно один раз. */
    val openBookRequests: Flow<Long> = openBookChannel.receiveAsFlow()

    fun openExternalFile(uri: String) {
        viewModelScope.launch {
            when (val result = bookImporter.import(listOf(uri)).single()) {
                is ImportResult.Imported -> openBookChannel.send(result.bookId)
                is ImportResult.Duplicate -> openBookChannel.send(result.bookId)
                is ImportResult.Failed -> Unit // файл не книга — просто остаёмся в шкафу
            }
        }
    }
}
