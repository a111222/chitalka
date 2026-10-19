package ru.chitalka.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ru.chitalka.core.database.dao.BookDao
import ru.chitalka.core.database.dao.ProgressDao
import ru.chitalka.core.database.entity.BookEntity
import ru.chitalka.core.database.entity.ReadingProgressEntity
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.model.Book
import ru.chitalka.core.model.ReadingPosition
import java.time.Instant
import javax.inject.Inject

/** Книги целиком из локальной базы: сети в приложении нет вовсе. */
internal class OfflineBookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val progressDao: ProgressDao,
) : BookRepository {

    override fun observeBooks(): Flow<List<Book>> =
        combine(bookDao.observeAll(), progressDao.observeAll()) { books, progress ->
            val percentById = progress.associate { it.bookId to it.percent }
            books.map { it.toBook(percentById[it.id] ?: 0f) }
        }

    override fun observeBook(id: Long): Flow<Book?> =
        combine(bookDao.observeById(id), progressDao.observe(id)) { book, progress ->
            book?.toBook(progress?.percent ?: 0f)
        }

    override fun search(query: String): Flow<List<Book>> {
        val ftsQuery = query.toFtsQuery() ?: return flowOf(emptyList())
        return bookDao.search(ftsQuery).map { list -> list.map { it.toBook(0f) } }
    }

    override fun observePosition(bookId: Long): Flow<ReadingPosition?> =
        progressDao.observe(bookId).map { it?.let { p -> ReadingPosition(p.chapterIndex, p.charOffset) } }

    override suspend fun savePosition(bookId: Long, position: ReadingPosition, percent: Float) {
        progressDao.upsert(
            ReadingProgressEntity(
                bookId = bookId,
                chapterIndex = position.chapterIndex,
                charOffset = position.charOffset,
                percent = percent.coerceIn(0f, 1f),
                updatedAt = Instant.now(),
            ),
        )
        bookDao.markOpened(bookId, Instant.now())
    }
}

internal fun BookEntity.toBook(progress: Float) = Book(
    id = id,
    title = title,
    author = author,
    series = series,
    format = format,
    coverPath = coverPath,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt,
    progressPercent = progress,
)

/** «толстой война» → «толстой* война*»: ищем по началу каждого слова. */
internal fun String.toFtsQuery(): String? {
    val words = split(Regex("\\s+"))
        .map { word -> word.filter { it.isLetterOrDigit() } }
        .filter { it.isNotEmpty() }
    return if (words.isEmpty()) null else words.joinToString(" ") { "$it*" }
}
