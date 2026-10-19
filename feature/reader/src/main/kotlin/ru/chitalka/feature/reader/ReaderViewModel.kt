package ru.chitalka.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.domain.SettingsRepository
import ru.chitalka.core.model.ReaderSettings
import javax.inject.Inject

data class ReaderUiState(
    val title: String = "",
    val author: String? = null,
    val pages: List<String> = emptyList(),
    val currentPage: Int = 0,
    val settings: ReaderSettings = ReaderSettings(),
    val isLoading: Boolean = true,
) {
    val pageCount: Int get() = pages.size
    val canGoForward: Boolean get() = currentPage < pages.lastIndex
    val canGoBack: Boolean get() = currentPage > 0
}

sealed interface ReaderEvent {
    data object NextPage : ReaderEvent
    data object PreviousPage : ReaderEvent
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    bookRepository: BookRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Аргумент маршрута ReaderRoute(bookId): Navigation кладёт его под именем свойства. */
    private val bookId: Long = savedStateHandle["bookId"] ?: 0L

    private val currentPage = MutableStateFlow(0)

    val uiState: StateFlow<ReaderUiState> = combine(
        bookRepository.observeBook(bookId),
        settingsRepository.settings,
        currentPage,
    ) { book, settings, page ->
        // Пока парсеров нет, любая книга показывает демонстрационный текст
        ReaderUiState(
            title = book?.title ?: SampleBook.TITLE,
            author = book?.author ?: SampleBook.AUTHOR,
            pages = SampleBook.pages,
            currentPage = page,
            settings = settings,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    fun onEvent(event: ReaderEvent) {
        val lastIndex = SampleBook.pages.lastIndex
        when (event) {
            ReaderEvent.NextPage -> currentPage.update { (it + 1).coerceAtMost(lastIndex) }
            ReaderEvent.PreviousPage -> currentPage.update { (it - 1).coerceAtLeast(0) }
        }
    }
}
