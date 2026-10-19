package ru.chitalka.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.model.Book
import java.time.Instant
import javax.inject.Inject

/** Всё, что нужно экрану шкафа, одним неизменяемым объектом. */
data class ShelfUiState(
    val books: List<Book> = emptyList(),
    val sort: ShelfSort = ShelfSort.RecentlyRead,
    val isLoading: Boolean = true,
) {
    /** Книга для карточки «Продолжить»: последняя открытая. */
    val continueReading: Book? get() = books.filter { it.lastOpenedAt != null }.maxByOrNull { it.lastOpenedAt!! }
}

enum class ShelfSort(val label: String) {
    RecentlyRead("Недавние"),
    Title("По названию"),
    Author("По автору"),
}

/** События от экрана к ViewModel. */
sealed interface ShelfEvent {
    data class ChangeSort(val sort: ShelfSort) : ShelfEvent
}

@HiltViewModel
class ShelfViewModel @Inject constructor(
    bookRepository: BookRepository,
) : ViewModel() {

    private val sort = MutableStateFlow(ShelfSort.RecentlyRead)

    val uiState: StateFlow<ShelfUiState> =
        combine(bookRepository.observeBooks(), sort) { books, sort ->
            ShelfUiState(books = books.sortedWith(sort.comparator), sort = sort, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ShelfUiState(),
        )

    fun onEvent(event: ShelfEvent) {
        when (event) {
            is ShelfEvent.ChangeSort -> sort.value = event.sort
        }
    }
}

private val ShelfSort.comparator: Comparator<Book>
    get() = when (this) {
        ShelfSort.RecentlyRead -> compareByDescending<Book> { it.lastOpenedAt ?: Instant.EPOCH }.thenByDescending { it.addedAt }
        ShelfSort.Title -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        ShelfSort.Author -> compareBy<Book, String>(String.CASE_INSENSITIVE_ORDER) { it.author.orEmpty() }.thenBy { it.title }
    }
