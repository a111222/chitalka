package ru.chitalka.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import ru.chitalka.core.domain.BookContentRepository
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.domain.SettingsRepository
import ru.chitalka.core.model.Block
import ru.chitalka.core.model.BookContent
import ru.chitalka.core.model.Chapter
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode
import ru.chitalka.core.model.ReadingPosition
import ru.chitalka.core.model.TocEntry
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
) : ViewModel() {

    /** Аргумент маршрута ReaderRoute(bookId): Navigation кладёт его под именем свойства. */
    private val bookId: Long = savedStateHandle["bookId"] ?: SAMPLE_BOOK_ID
    private val isSample = bookId == SAMPLE_BOOK_ID

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

    val uiState: StateFlow<ReaderUiState> = combine(
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init {
        viewModelScope.launch {
            try {
                val opened = if (isSample) SampleBook.content else contentRepository.open(bookId)
                val saved = if (isSample) null else bookRepository.observePosition(bookId).first()
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
                // Пример не хранится в базе: сохранять его прогресс некуда
                if (!isSample) {
                    viewModelScope.launch { bookRepository.savePosition(bookId, event.position, event.percent) }
                }
            }
        }
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
