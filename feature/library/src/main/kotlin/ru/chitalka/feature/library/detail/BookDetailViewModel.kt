package ru.chitalka.feature.library.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import ru.chitalka.core.domain.BookContentRepository
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.model.Block
import ru.chitalka.core.model.Book
import ru.chitalka.core.model.BookFormat
import ru.chitalka.core.model.Chapter
import ru.chitalka.feature.library.SAMPLE_BOOK_ID
import javax.inject.Inject

/** Карточка «О книге»: сведения из библиотеки и то, что удалось прочитать из самого файла. */
data class BookDetailUiState(
    val isLoading: Boolean = true,
    val book: BookDetail? = null,
    /** Книгу удалили, пока карточка была открыта: экран закрывается. */
    val isGone: Boolean = false,
)

data class BookDetail(
    val id: Long,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val format: String,
    val progress: Float,
    /** null, пока файл ещё читается. */
    val chapterCount: Int? = null,
    val genre: String? = null,
    /** Аннотация, а если её нет — начало первой главы. */
    val description: String? = null,
    val descriptionIsExcerpt: Boolean = false,
    val isSample: Boolean = false,
)

/** То, что читается из файла книги: дольше, чем запись в базе, поэтому приходит вторым. */
internal data class FileDetails(val chapterCount: Int, val genre: String?, val description: String?, val isExcerpt: Boolean)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val contentRepository: BookContentRepository,
) : ViewModel() {
    private val bookId: Long = savedStateHandle["bookId"] ?: SAMPLE_BOOK_ID

    val uiState: StateFlow<BookDetailUiState> = if (bookId == SAMPLE_BOOK_ID) {
        flowOf(BookDetailUiState(isLoading = false, book = SampleDetail))
    } else {
        combine(bookRepository.observeBook(bookId), fileDetails()) { book, file ->
            if (book == null) BookDetailUiState(isLoading = false, isGone = true) else BookDetailUiState(isLoading = false, book = book.toDetail(file))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailUiState())

    private fun fileDetails(): Flow<FileDetails?> = flow {
        emit(null)
        val details = runCatching {
            val content = contentRepository.open(bookId)
            val description = content.description
            if (description != null) {
                FileDetails(content.chapterCount, content.genres.firstOrNull(), description, isExcerpt = false)
            } else {
                // Аннотации нет: показываем начало книги, пропуская обложку и пустые главы
                val chapters = mutableListOf<Chapter>()
                var chars = 0
                for (index in 0 until minOf(content.chapterCount, EXCERPT_CHAPTERS)) {
                    val chapter = contentRepository.chapter(bookId, index)
                    chapters += chapter
                    chars += chapter.blocks.sumOf { (it as? Block.Paragraph)?.text?.length ?: 0 }
                    if (chars >= EXCERPT_CHARS) break
                }
                val excerpt = excerptOf(chapters)
                FileDetails(content.chapterCount, content.genres.firstOrNull(), excerpt, isExcerpt = excerpt != null)
            }
        }.getOrNull()
        emit(details)
    }

    private fun Book.toDetail(file: FileDetails?) = BookDetail(
        id = id,
        title = title,
        author = author,
        coverPath = coverPath,
        format = format.label,
        progress = progressPercent,
        chapterCount = file?.chapterCount,
        genre = file?.genre,
        description = file?.description,
        descriptionIsExcerpt = file?.isExcerpt == true,
    )

    companion object {
        private const val EXCERPT_CHAPTERS = 4
        internal const val EXCERPT_CHARS = 600

        /**
         * Первые абзацы книги примерно на [EXCERPT_CHARS] знаков, обрезанные по границе слова.
         * Заголовки пропускаем: они уже есть на карточке.
         */
        internal fun excerptOf(chapters: List<Chapter>): String? {
            val paragraphs = mutableListOf<String>()
            var length = 0
            for (chapter in chapters) {
                for (block in chapter.blocks) {
                    val text = when (block) {
                        is Block.Paragraph -> block.text
                        is Block.Quote -> block.text
                        else -> null
                    } ?: continue
                    if (length + text.length <= EXCERPT_CHARS) {
                        paragraphs += text
                        length += text.length
                    } else {
                        val room = EXCERPT_CHARS - length
                        if (room > 80 || paragraphs.isEmpty()) {
                            val cut = text.take(room).substringBeforeLast(' ').trimEnd(',', ';', ':', '—', ' ')
                            paragraphs += "$cut…"
                        }
                        return paragraphs.joinToString("\n\n")
                    }
                }
            }
            return paragraphs.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
        }
    }
}

private val BookFormat.label: String
    get() = when (this) {
        BookFormat.EPUB -> "EPUB"
        BookFormat.FB2 -> "FB2"
        BookFormat.PDF -> "PDF"
    }

/** Демо-книга «Сад за рекой»: карточку можно посмотреть, пока библиотека пуста. */
internal val SampleDetail = BookDetail(
    id = SAMPLE_BOOK_ID,
    title = "Сад за рекой",
    author = "Демо-книга",
    coverPath = null,
    format = "Демо",
    progress = 0f,
    chapterCount = 1,
    genre = "Проза",
    description = "Летний дом у реки, старый сад тремя уступами и шкаф, полки которого к концу каждого лета " +
        "прогибаются от привезённых гостями книг. Короткая история о памяти, семейных вечерах под зелёной " +
        "лампой и лодке, которую никто не решается спустить на воду.\n\n" +
        "Это демонстрационный текст: на нём можно попробовать листание, маркер и оформление, " +
        "пока в библиотеке нет ваших книг.",
    isSample = true,
)
