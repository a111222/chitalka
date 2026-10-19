package ru.chitalka.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.chitalka.core.database.dao.AnnotationDao
import ru.chitalka.core.database.dao.BookDao
import ru.chitalka.core.database.entity.BookEntity
import ru.chitalka.core.database.entity.BookmarkEntity
import ru.chitalka.core.database.entity.HighlightEntity
import ru.chitalka.core.domain.AnnotationRepository
import ru.chitalka.core.model.Bookmark
import ru.chitalka.core.model.DemoBook
import ru.chitalka.core.model.Highlight
import ru.chitalka.core.model.HighlightColor
import ru.chitalka.core.model.ReadingPosition
import ru.chitalka.core.model.WithBook
import javax.inject.Inject

internal class OfflineAnnotationRepository @Inject constructor(
    private val annotationDao: AnnotationDao,
    private val bookDao: BookDao,
) : AnnotationRepository {

    override fun observeBookmarks(bookId: Long): Flow<List<Bookmark>> =
        annotationDao.observeBookmarks(bookId).map { list -> list.map { it.toModel() } }

    override fun observeHighlights(bookId: Long): Flow<List<Highlight>> =
        annotationDao.observeHighlights(bookId).map { list -> list.map { it.toModel() } }

    override fun observeAllBookmarks(): Flow<List<WithBook<Bookmark>>> =
        combine(annotationDao.observeAllBookmarks(), bookDao.observeAll()) { marks, books ->
            val byId = books.associateBy { it.id }
            marks.mapNotNull { mark -> withBook(mark.toModel(), mark.bookId, byId) }
        }

    override fun observeAllHighlights(): Flow<List<WithBook<Highlight>>> =
        combine(annotationDao.observeAllHighlights(), bookDao.observeAll()) { quotes, books ->
            val byId = books.associateBy { it.id }
            quotes.mapNotNull { quote -> withBook(quote.toModel(), quote.bookId, byId) }
        }

    override suspend fun addBookmark(bookmark: Bookmark): Long = annotationDao.insertBookmark(bookmark.toEntity())

    override suspend fun deleteBookmark(id: Long) = annotationDao.deleteBookmark(id)

    override suspend fun addHighlight(highlight: Highlight): Long = annotationDao.insertHighlight(highlight.toEntity())

    override suspend fun changeHighlightColor(id: Long, color: HighlightColor) = annotationDao.updateHighlightColor(id, color.ordinal)

    override suspend fun deleteHighlight(id: Long) = annotationDao.deleteHighlight(id)
}

/** Книга для карточки; демо-книги нет в базе, но её закладки тоже показываем. Книгу удалили — null. */
private fun <T> withBook(item: T, bookId: Long, books: Map<Long, BookEntity>): WithBook<T>? {
    if (bookId == DemoBook.ID) return WithBook(item, DemoBook.TITLE, DemoBook.AUTHOR, null)
    val book = books[bookId] ?: return null
    return WithBook(item, book.title, book.author, book.coverPath)
}

private fun BookmarkEntity.toModel() = Bookmark(
    id = id,
    bookId = bookId,
    position = ReadingPosition(chapterIndex.coerceAtLeast(0), charOffset.coerceAtLeast(0)),
    label = label,
    snippet = snippet,
    percent = percent,
    createdAt = createdAt,
)

private fun Bookmark.toEntity() = BookmarkEntity(
    bookId = bookId,
    chapterIndex = position.chapterIndex,
    charOffset = position.charOffset,
    label = label,
    snippet = snippet,
    percent = percent,
    createdAt = createdAt,
)

private fun HighlightEntity.toModel() = Highlight(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    start = startOffset,
    end = endOffset,
    text = text,
    color = HighlightColor.entries.getOrElse(color) { HighlightColor.Teal },
    percent = percent,
    createdAt = createdAt,
)

private fun Highlight.toEntity() = HighlightEntity(
    bookId = bookId,
    chapterIndex = chapterIndex,
    startOffset = start,
    endOffset = end,
    text = text,
    color = color.ordinal,
    note = null,
    percent = percent,
    createdAt = createdAt,
)
