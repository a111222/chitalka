package ru.chitalka.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.chitalka.core.domain.AnnotationRepository
import ru.chitalka.core.model.Bookmark
import ru.chitalka.core.model.Highlight
import ru.chitalka.core.model.HighlightColor
import ru.chitalka.core.model.WithBook

/** Закладки и цитаты в памяти. Книга для общего списка всегда «Книга N». */
class FakeAnnotationRepository : AnnotationRepository {
    val bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val highlights = MutableStateFlow<List<Highlight>>(emptyList())
    private var nextId = 1L

    override fun observeBookmarks(bookId: Long): Flow<List<Bookmark>> = bookmarks.map { list -> list.filter { it.bookId == bookId } }

    override fun observeHighlights(bookId: Long): Flow<List<Highlight>> = highlights.map { list -> list.filter { it.bookId == bookId } }

    override fun observeAllBookmarks(): Flow<List<WithBook<Bookmark>>> =
        bookmarks.map { list -> list.sortedByDescending { it.createdAt }.map { WithBook(it, "Книга ${it.bookId}", null, null) } }

    override fun observeAllHighlights(): Flow<List<WithBook<Highlight>>> =
        highlights.map { list -> list.sortedByDescending { it.createdAt }.map { WithBook(it, "Книга ${it.bookId}", null, null) } }

    override suspend fun addBookmark(bookmark: Bookmark): Long {
        val id = nextId++
        bookmarks.update { it + bookmark.copy(id = id) }
        return id
    }

    override suspend fun deleteBookmark(id: Long) = bookmarks.update { list -> list.filterNot { it.id == id } }

    override suspend fun addHighlight(highlight: Highlight): Long {
        val id = nextId++
        highlights.update { it + highlight.copy(id = id) }
        return id
    }

    override suspend fun changeHighlightColor(id: Long, color: HighlightColor) =
        highlights.update { list -> list.map { if (it.id == id) it.copy(color = color) else it } }

    override suspend fun deleteHighlight(id: Long) = highlights.update { list -> list.filterNot { it.id == id } }
}
