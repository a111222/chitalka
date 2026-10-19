package ru.chitalka.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.chitalka.core.domain.BookImporter
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.domain.ImportResult
import ru.chitalka.core.model.Book
import java.time.Instant
import javax.inject.Inject

/** Всё, что нужно главной и вкладке «Библиотека», одним неизменяемым объектом. */
data class LibraryUiState(
    /** Все книги: сначала недавно открытые, потом недавно добавленные. */
    val books: List<Book> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.All,
    val query: String = "",
    /** Книги вкладки «Библиотека» после фильтра и поиска. */
    val shown: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    /** Короткое сообщение для снекбара: итог импорта или удаления. */
    val message: String? = null,
)

/** Чипы над сеткой библиотеки. Они же — полки на главной. */
enum class LibraryFilter(val label: String) {
    All("Все"),
    Reading("Читаю"),
    WantToRead("Хочу прочитать"),
    Finished("Прочитано"),
    ;

    fun matches(book: Book): Boolean = when (this) {
        All -> true
        Reading -> book.readingState == ReadingState.Reading
        WantToRead -> book.readingState == ReadingState.NotStarted
        Finished -> book.readingState == ReadingState.Finished
    }
}

enum class ReadingState { NotStarted, Reading, Finished }

/** Дочитанной считаем книгу с прогрессом от 99%: последняя страница часто пустая или с выходными данными. */
val Book.readingState: ReadingState
    get() = when {
        progressPercent >= 0.99f -> ReadingState.Finished
        progressPercent > 0f -> ReadingState.Reading
        else -> ReadingState.NotStarted
    }

/** События от экранов к ViewModel. */
sealed interface LibraryEvent {
    data class ChangeFilter(val filter: LibraryFilter) : LibraryEvent

    data class ChangeQuery(val query: String) : LibraryEvent

    /** Пользователь выбрал файлы в системном окне. */
    data class ImportBooks(val uris: List<String>) : LibraryEvent

    data class DeleteBook(val book: Book) : LibraryEvent

    data object MessageShown : LibraryEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val bookImporter: BookImporter,
) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter.All)
    private val query = MutableStateFlow("")
    private val transient = MutableStateFlow(Transient())
    private var initialFilterApplied = false

    /** id книг, найденных полнотекстовым поиском; null — поиск не идёт. */
    private val foundIds: Flow<Set<Long>?> = query.flatMapLatest { text ->
        if (text.isBlank()) flowOf(null) else bookRepository.search(text).map { found -> found.mapTo(HashSet()) { it.id } }
    }

    private val books: Flow<List<Book>> = bookRepository.observeBooks().map { it.sortedWith(RecentFirst) }

    val uiState: StateFlow<LibraryUiState> =
        combine(books, filter, query, foundIds, transient) { books, filter, query, found, transient ->
            LibraryUiState(
                books = books,
                filter = filter,
                query = query,
                shown = books.filter { filter.matches(it) && (found == null || it.id in found) },
                isLoading = false,
                isImporting = transient.isImporting,
                message = transient.message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState(),
        )

    /** Полка с главной открывает библиотеку с нужным фильтром. Срабатывает один раз, поворот экрана его не сбросит. */
    fun applyInitialFilter(initial: LibraryFilter?) {
        if (initialFilterApplied) return
        initialFilterApplied = true
        if (initial != null) filter.value = initial
    }

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.ChangeFilter -> filter.value = event.filter
            is LibraryEvent.ChangeQuery -> query.value = event.query
            is LibraryEvent.ImportBooks -> importBooks(event.uris)
            is LibraryEvent.DeleteBook -> viewModelScope.launch {
                bookRepository.deleteBook(event.book.id)
                transient.update { it.copy(message = "«${event.book.title}» убрана из библиотеки") }
            }
            LibraryEvent.MessageShown -> transient.update { it.copy(message = null) }
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

private val RecentFirst: Comparator<Book> =
    compareByDescending<Book> { it.lastOpenedAt ?: Instant.EPOCH }.thenByDescending { it.addedAt }

/** «Книга «Анна Каренина» в библиотеке», «Добавлено книг: 3. Уже были в библиотеке: 1» и т. п. */
internal fun importMessage(results: List<ImportResult>): String {
    val imported = results.filterIsInstance<ImportResult.Imported>()
    val duplicates = results.count { it is ImportResult.Duplicate }
    val failed = results.filterIsInstance<ImportResult.Failed>()
    return buildList {
        when (imported.size) {
            0 -> Unit
            1 -> add("«${imported.single().title}» теперь в библиотеке")
            else -> add("Добавлено книг: ${imported.size}")
        }
        if (duplicates > 0) add(if (results.size == 1) "Эта книга уже в библиотеке" else "Уже были в библиотеке: $duplicates")
        when (failed.size) {
            0 -> Unit
            1 -> add("Не получилось открыть «${failed.single().fileName}»: ${failed.single().reason}")
            else -> add("Не получилось открыть файлов: ${failed.size}")
        }
    }.joinToString(". ")
}
