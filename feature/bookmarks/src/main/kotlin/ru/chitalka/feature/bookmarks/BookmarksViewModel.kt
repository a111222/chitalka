package ru.chitalka.feature.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.chitalka.core.domain.AnnotationRepository
import ru.chitalka.core.model.Bookmark
import ru.chitalka.core.model.Highlight
import ru.chitalka.core.model.WithBook
import javax.inject.Inject

/** Переключатель «Цитаты · Закладки» из макета. */
enum class MarksTab(val label: String) {
    Quotes("Цитаты"),
    Bookmarks("Закладки"),
}

data class BookmarksUiState(
    val tab: MarksTab = MarksTab.Quotes,
    val quotes: List<WithBook<Highlight>> = emptyList(),
    val bookmarks: List<WithBook<Bookmark>> = emptyList(),
    val isLoading: Boolean = true,
    /** Только что удалили: снизу предложение «Вернуть». */
    val undo: Removed? = null,
)

/** Удалённая цитата или закладка, которую ещё можно вернуть. */
sealed interface Removed {
    val message: String

    data class Quote(val highlight: Highlight) : Removed {
        override val message = "Цитата удалена"
    }

    data class Mark(val bookmark: Bookmark) : Removed {
        override val message = "Закладка удалена"
    }
}

sealed interface BookmarksEvent {
    data class SelectTab(val tab: MarksTab) : BookmarksEvent

    data class DeleteQuote(val highlight: Highlight) : BookmarksEvent

    data class DeleteBookmark(val bookmark: Bookmark) : BookmarksEvent

    data object Undo : BookmarksEvent

    /** Предложение вернуть исчезло само: удаление окончательное. */
    data object UndoDismissed : BookmarksEvent
}

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: AnnotationRepository,
) : ViewModel() {

    private val tab = MutableStateFlow(MarksTab.Quotes)
    private val undo = MutableStateFlow<Removed?>(null)

    val uiState: StateFlow<BookmarksUiState> = combine(
        tab,
        repository.observeAllHighlights(),
        repository.observeAllBookmarks(),
        undo,
    ) { tab, quotes, bookmarks, undo ->
        BookmarksUiState(tab = tab, quotes = quotes, bookmarks = bookmarks, isLoading = false, undo = undo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookmarksUiState())

    fun onEvent(event: BookmarksEvent) {
        when (event) {
            is BookmarksEvent.SelectTab -> tab.value = event.tab
            is BookmarksEvent.DeleteQuote -> {
                undo.value = Removed.Quote(event.highlight)
                viewModelScope.launch { repository.deleteHighlight(event.highlight.id) }
            }
            is BookmarksEvent.DeleteBookmark -> {
                undo.value = Removed.Mark(event.bookmark)
                viewModelScope.launch { repository.deleteBookmark(event.bookmark.id) }
            }
            BookmarksEvent.Undo -> {
                val removed = undo.value ?: return
                undo.value = null
                viewModelScope.launch {
                    when (removed) {
                        is Removed.Quote -> repository.addHighlight(removed.highlight.copy(id = 0))
                        is Removed.Mark -> repository.addBookmark(removed.bookmark.copy(id = 0))
                    }
                }
            }
            BookmarksEvent.UndoDismissed -> undo.value = null
        }
    }
}
