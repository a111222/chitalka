package ru.chitalka.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.chitalka.core.domain.AnnotationRepository
import ru.chitalka.core.domain.BookContentRepository
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.domain.ReadingStatsRepository
import ru.chitalka.core.domain.SettingsRepository
import ru.chitalka.core.model.Block
import ru.chitalka.core.model.BookContent
import ru.chitalka.core.model.Bookmark
import ru.chitalka.core.model.Chapter
import ru.chitalka.core.model.Highlight
import ru.chitalka.core.model.HighlightColor
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode
import ru.chitalka.core.model.ReadingPosition
import ru.chitalka.core.model.TocEntry
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class ReaderUiState(
    val title: String = "",
    val author: String? = null,
    val language: String? = null,
    val toc: List<TocEntry> = emptyList(),
    val chapterCount: Int = 0,
    /** Где читатель сейчас: глава + смещение. Номер страницы считает экран — он зависит от шрифта. */
    val position: ReadingPosition = ReadingPosition.Start,
    /** Загруженная глава. Пока грузится следующая, здесь остаётся предыдущая — экран не мигает. */
    val chapter: LoadedChapter? = null,
    /**
     * Текущая глава и её соседи, загруженные заранее: лист можно согнуть и через границу главы,
     * следующая страница уже готова под ним.
     */
    val nearby: List<LoadedChapter> = emptyList(),
    val settings: ReaderSettings = ReaderSettings(),
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Закладки и выделения этой книги, во всех главах. */
    val bookmarks: List<Bookmark> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
)

/** Глава и, если в ней нет текста, её картинка (обычно обложка). Сравнивается по ссылке. */
class LoadedChapter(val chapter: Chapter, val image: ByteArray? = null)

sealed interface ReaderEvent {
    /** Перейти к месту в книге: листание, переход по оглавлению. */
    data class GoTo(val position: ReadingPosition) : ReaderEvent

    /** Экран показал страницу: запоминаем место и процент прочитанного. */
    data class PageShown(val position: ReadingPosition, val percent: Float) : ReaderEvent

    /** Шторка «Аа»: шрифт, размер, интервал, оттенок страницы, вибрация. Сохраняются сразу. */
    data class ChangeFont(val font: ReaderFont) : ReaderEvent

    data class ChangeFontSize(val sizeSp: Int) : ReaderEvent

    data class ChangeSpacing(val spacing: LineSpacing) : ReaderEvent

    data class ChangeTheme(val theme: ReaderThemeMode) : ReaderEvent

    data class ChangeHaptics(val enabled: Boolean) : ReaderEvent

    /** Провели маркером по фразе: [start]..[end] — смещения в тексте главы. */
    data class AddHighlight(val chapterIndex: Int, val start: Int, val end: Int, val text: String, val percent: Float) : ReaderEvent

    data class ChangeHighlightColor(val id: Long, val color: HighlightColor) : ReaderEvent

    data class DeleteHighlight(val id: Long) : ReaderEvent

    /**
     * Закладка на видимой странице (в развороте — на двух): если на ней уже есть закладка, она снимается.
     * Страница — это смещения с [ReadingPosition.charOffset] до [endOffset] в главе.
     */
    data class ToggleBookmark(
        val position: ReadingPosition,
        val endOffset: Int,
        val label: String,
        val snippet: String,
        val percent: Float,
    ) : ReaderEvent

    /** Экран на виду / ушёл с экрана: между ними идут минуты чтения для статистики. */
    data object Resumed : ReaderEvent

    data object Paused : ReaderEvent
}

/** Смещение «конец главы»: листая назад, попадаем на последнюю страницу предыдущей главы. */
const val END_OF_CHAPTER = Int.MAX_VALUE

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val contentRepository: BookContentRepository,
    private val settingsRepository: SettingsRepository,
    private val annotationRepository: AnnotationRepository,
    private val statsRepository: ReadingStatsRepository,
) : ViewModel() {

    /** Аргумент маршрута ReaderRoute(bookId): Navigation кладёт его под именем свойства. */
    private val bookId: Long = savedStateHandle["bookId"] ?: SAMPLE_BOOK_ID
    private val isSample = bookId == SAMPLE_BOOK_ID

    /**
     * Место, куда открыть книгу вместо сохранённого: из вкладки «Закладки». Используется один раз,
     * после пересоздания экрана книга откроется там, где читатель остановился.
     */
    private val requestedPosition: ReadingPosition? = savedStateHandle.get<Int>("chapterIndex")
        ?.takeIf { it >= 0 }
        ?.let { ReadingPosition(it, (savedStateHandle.get<Int>("charOffset") ?: 0).coerceAtLeast(0)) }
        .also { savedStateHandle["chapterIndex"] = -1 }

    /** Часы для статистики; тесты подменяют их. */
    internal var now: () -> Long = System::currentTimeMillis
    internal var today: () -> LocalDate = LocalDate::now
    private val timer = ReadingTimer { now() }
    private var resumed = false
    private var lastShown: ReadingPosition? = null

    private val content = MutableStateFlow<BookContent?>(null)
    private val position = MutableStateFlow<ReadingPosition?>(null)
    private val error = MutableStateFlow<String?>(null)

    /** Загруженные главы по номеру: текущая и соседние. Читается только из потока [nearby]. */
    private val chapterCache = mutableMapOf<Int, LoadedChapter>()

    /**
     * Глава грузится заново, только когда меняется её номер, а не смещение внутри.
     * Сначала грузится текущая (и сразу показывается), потом соседние — про запас для листания.
     */
    private val nearby: StateFlow<List<LoadedChapter>> = position.filterNotNull()
        .map { it.chapterIndex }
        .distinctUntilChanged()
        .transformLatest { index ->
            val count = content.value?.chapterCount ?: 0
            val wanted = listOf(index, index + 1, index - 1).filter { it in 0 until count }
            chapterCache.keys.retainAll(wanted.toSet())
            for (i in wanted) {
                if (i !in chapterCache) loadChapter(i, reportError = i == index)?.let { chapterCache[i] = it }
                emit(wanted.mapNotNull { chapterCache[it] })
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Последняя показанная глава: пока грузится новая, экран держит её и не мигает. */
    private var lastChapter: LoadedChapter? = null

    private val readingState = combine(
        content,
        position,
        nearby,
        settingsRepository.settings,
        error,
    ) { content, position, nearby, settings, error ->
        val current = position ?: ReadingPosition.Start
        val chapter = nearby.firstOrNull { it.chapter.index == current.chapterIndex } ?: lastChapter
        lastChapter = chapter
        ReaderUiState(
            title = content?.title.orEmpty(),
            author = content?.author,
            language = content?.language,
            toc = content?.toc.orEmpty(),
            chapterCount = content?.chapterCount ?: 0,
            position = current,
            chapter = chapter,
            nearby = nearby,
            settings = settings,
            isLoading = error == null && (content == null || chapter == null),
            error = error,
        )
    }

    val uiState: StateFlow<ReaderUiState> = combine(
        readingState,
        annotationRepository.observeBookmarks(bookId),
        annotationRepository.observeHighlights(bookId),
    ) { state, bookmarks, highlights ->
        state.copy(bookmarks = bookmarks, highlights = highlights)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init {
        viewModelScope.launch {
            try {
                val opened = if (isSample) SampleBook.content else contentRepository.open(bookId)
                val saved = requestedPosition ?: if (isSample) null else bookRepository.observePosition(bookId).first()
                content.value = opened
                position.value = saved?.coerceIn(opened) ?: ReadingPosition.Start
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = "Не получилось открыть книгу. Возможно, файл повреждён."
            }
        }
    }

    fun onEvent(event: ReaderEvent) {
        when (event) {
            is ReaderEvent.ChangeFont -> viewModelScope.launch { settingsRepository.setFont(event.font) }
            is ReaderEvent.ChangeFontSize -> viewModelScope.launch {
                settingsRepository.setFontSize(event.sizeSp.coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE))
            }
            is ReaderEvent.ChangeSpacing -> viewModelScope.launch { settingsRepository.setLineSpacing(event.spacing) }
            is ReaderEvent.ChangeTheme -> viewModelScope.launch { settingsRepository.setTheme(event.theme) }
            is ReaderEvent.ChangeHaptics -> viewModelScope.launch { settingsRepository.setHapticsEnabled(event.enabled) }
            is ReaderEvent.GoTo -> {
                val count = content.value?.chapterCount ?: return
                if (event.position.chapterIndex in 0 until count) position.value = event.position
            }
            is ReaderEvent.PageShown -> {
                if (content.value == null) return
                position.value = event.position
                // Страница засчитывается, только если листали вперёд: открытие книги и шаг назад не в счёт
                val turnedForward = lastShown?.let { event.position.isAfter(it) } ?: false
                lastShown = event.position
                recordReading(if (resumed) timer.tick() else 0L, if (turnedForward) 1 else 0)
                // Пример не хранится в базе: сохранять его прогресс некуда
                if (!isSample) {
                    viewModelScope.launch { bookRepository.savePosition(bookId, event.position, event.percent) }
                }
            }
            is ReaderEvent.AddHighlight -> {
                if (event.end <= event.start || event.text.isBlank()) return
                val highlight = Highlight(
                    bookId = bookId,
                    chapterIndex = event.chapterIndex,
                    start = event.start,
                    end = event.end,
                    text = event.text.trim(),
                    percent = event.percent,
                    createdAt = Instant.ofEpochMilli(now()),
                )
                save { annotationRepository.addHighlight(highlight) }
            }
            is ReaderEvent.ChangeHighlightColor -> save { annotationRepository.changeHighlightColor(event.id, event.color) }
            is ReaderEvent.DeleteHighlight -> save { annotationRepository.deleteHighlight(event.id) }
            is ReaderEvent.ToggleBookmark -> {
                val start = event.position
                val onPage = uiState.value.bookmarks.filter {
                    it.position.chapterIndex == start.chapterIndex && it.position.charOffset in start.charOffset until event.endOffset
                }
                if (onPage.isNotEmpty()) {
                    save { onPage.forEach { annotationRepository.deleteBookmark(it.id) } }
                } else {
                    val bookmark = Bookmark(
                        bookId = bookId,
                        position = start,
                        label = event.label,
                        snippet = event.snippet,
                        percent = event.percent,
                        createdAt = Instant.ofEpochMilli(now()),
                    )
                    save { annotationRepository.addBookmark(bookmark) }
                }
            }
            ReaderEvent.Resumed -> {
                resumed = true
                timer.start()
            }
            ReaderEvent.Paused -> {
                resumed = false
                recordReading(timer.stop(), 0)
            }
        }
    }

    /**
     * Запись не отменяется, даже если экран уже закрывают: иначе потерялись бы последние минуты
     * чтения и только что поставленная закладка.
     */
    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch(NonCancellable) { block() }
    }

    private fun recordReading(durationMs: Long, pages: Int) {
        if (durationMs <= 0 && pages <= 0) return
        val date = today()
        save { statsRepository.addReading(bookId, date, durationMs, pages) }
    }

    private suspend fun loadChapter(index: Int, reportError: Boolean): LoadedChapter? = try {
        val chapter = if (isSample) SampleBook.chapter else contentRepository.chapter(bookId, index)
        val image = if (chapter.hasText || isSample) {
            null
        } else {
            chapter.blocks.firstNotNullOfOrNull { it as? Block.Image }?.let { contentRepository.image(bookId, it.path) }
        }
        LoadedChapter(chapter, image)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Соседняя глава про запас может и не загрузиться: ошибку показываем только для той, что читают
        if (reportError) error.value = "Не получилось открыть главу ${index + 1}."
        null
    }
}

/** Сохранённое место могло устареть, если книгу переимпортировали с другим числом глав. */
private fun ReadingPosition.coerceIn(content: BookContent): ReadingPosition =
    if (chapterIndex < content.chapterCount) this else ReadingPosition.Start

private fun ReadingPosition.isAfter(other: ReadingPosition): Boolean =
    chapterIndex > other.chapterIndex || (chapterIndex == other.chapterIndex && charOffset > other.charOffset)
