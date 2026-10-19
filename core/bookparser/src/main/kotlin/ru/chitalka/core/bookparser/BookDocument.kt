package ru.chitalka.core.bookparser

import ru.chitalka.core.model.BookFormat
import ru.chitalka.core.model.Chapter
import ru.chitalka.core.model.TocEntry
import java.io.Closeable
import java.io.File

/**
 * Открытая книга. Сведения и оглавление читаются сразу, главы — по требованию.
 * Новый формат = новая реализация [BookDocument], а не новая читалка.
 */
interface BookDocument : Closeable {
    val metadata: BookMetadata
    val toc: List<TocEntry>
    val chapterCount: Int

    fun chapter(index: Int): Chapter

    /** Байты картинки по пути из [ru.chitalka.core.model.Block.Image.path] или null. */
    fun image(path: String): ByteArray?

    /** Обложка или null, если в книге её нет. */
    fun cover(): ByteArray?
}

data class BookMetadata(
    val title: String?,
    val author: String?,
    val series: String? = null,
    val seriesIndex: Int? = null,
    val language: String? = null,
    val format: BookFormat,
    /** Аннотация для карточки «О книге»: абзацы через пустую строку. */
    val description: String? = null,
    /** Жанры по-русски: «Классика», «Фантастика». */
    val genres: List<String> = emptyList(),
)

/** Книгу не удалось прочитать: файл повреждён или это не тот формат. */
class BookParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

object BookParsers {
    /** Открывает файл формата [format]. PDF пока не поддерживается — будет позже. */
    fun open(file: File, format: BookFormat): BookDocument = try {
        when (format) {
            BookFormat.EPUB -> EpubDocument.open(file)
            BookFormat.FB2 -> Fb2Document.open(file)
            BookFormat.PDF -> throw BookParseException("PDF пока не поддерживается")
        }
    } catch (e: BookParseException) {
        throw e
    } catch (e: Exception) {
        throw BookParseException("Не удалось прочитать книгу: ${e.message}", e)
    }
}
