package ru.chitalka.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.chitalka.core.domain.BookImporter
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.domain.ImportResult
import ru.chitalka.core.model.Book
import java.time.Instant
import javax.inject.Inject

/** Всё, что нужно экрану шкафа, одним неизменяемым объектом. */
data class ShelfUiState(
    val books: List<Book> = emptyList(),
    val sort: ShelfSort = ShelfSort.RecentlyRead,
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    /** Короткое сообщение для снекбара: итог импорта или удаления. */
    val message: String? = null,
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

    /** Пользователь выбрал файлы в системном окне. */
    data class ImportBooks(val uris: List<String>) : ShelfEvent

    data class DeleteBook(val book: Book) : ShelfEvent

    data object MessageShown : ShelfEvent
}

@HiltViewModel
class ShelfViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val bookImporter: BookImporter,
) : ViewModel() {

    private val sort = MutableStateFlow(ShelfSort.RecentlyRead)
    private val transient = MutableStateFlow(Transient())

    val uiState: StateFlow<ShelfUiState> =
        combine(bookRepository.observeBooks(), sort, transient) { books, sort, transient ->
            ShelfUiState(
                books = books.sortedWith(sort.comparator),
                sort = sort,
                isLoading = false,
                isImporting = transient.isImporting,
                message = transient.message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ShelfUiState(),
        )

    fun onEvent(event: ShelfEvent) {
        when (event) {
            is ShelfEvent.ChangeSort -> sort.value = event.sort
            is ShelfEvent.ImportBooks -> importBooks(event.uris)
            is ShelfEvent.DeleteBook -> viewModelScope.launch {
                bookRepository.deleteBook(event.book.id)
                transient.update { it.copy(message = "«${event.book.title}» убрана из шкафа") }
            }
            ShelfEvent.MessageShown -> transient.update { it.copy(message = null) }
        }
    }

    private fun importBooks(uris: List<String>) {
        if (uris.isEmpty()) return
        transient.update { it.copy(isImporting = true) }
        viewModelScope.launch {
            val results = bookImporter.import(uris)
            transient.update { Transient(isImporting = false, message = importMessage(results)) }
        }
    }

    /** Состояние, которого нет в базе: идёт ли импорт и что сказать пользователю. */
    private data class Transient(val isImporting: Boolean = false, val message: String? = null)
}

/** «Книга «Анна Каренина» в шкафу», «Добавлено книг: 3. Уже были в шкафу: 1» и т. п. */
internal fun importMessage(results: List<ImportResult>): String {
    val imported = results.filterIsInstance<ImportResult.Imported>()
    val duplicates = results.count { it is ImportResult.Duplicate }
    val failed = results.filterIsInstance<ImportResult.Failed>()
    return buildList {
        when (imported.size) {
            0 -> Unit
            1 -> add("«${imported.single().title}» теперь в шкафу")
            else -> add("Добавлено книг: ${imported.size}")
        }
        if (duplicates > 0) add(if (results.size == 1) "Эта книга уже в шкафу" else "Уже были в шкафу: $duplicates")
        when (failed.size) {
            0 -> Unit
            1 -> add("Не получилось открыть «${failed.single().fileName}»: ${failed.single().reason}")
            else -> add("Не получилось открыть файлов: ${failed.size}")
        }
    }.joinToString(". ")
}

private val ShelfSort.comparator: Comparator<Book>
    get() = when (this) {
        ShelfSort.RecentlyRead -> compareByDescending<Book> { it.lastOpenedAt ?: Instant.EPOCH }.thenByDescending { it.addedAt }
        ShelfSort.Title -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        ShelfSort.Author -> compareBy<Book, String>(String.CASE_INSENSITIVE_ORDER) { it.author.orEmpty() }.thenBy { it.title }
    }
