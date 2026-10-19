package ru.chitalka.core.domain

import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.model.Bookmark
import ru.chitalka.core.model.Highlight
import ru.chitalka.core.model.HighlightColor
import ru.chitalka.core.model.WithBook

/** Закладки и выделения маркером. Читалка видит их по одной книге, вкладка «Закладки» — все сразу. */
interface AnnotationRepository {
    fun observeBookmarks(bookId: Long): Flow<List<Bookmark>>

    fun observeHighlights(bookId: Long): Flow<List<Highlight>>

    /** Все закладки, сначала новые. Закладки удалённых книг сюда не попадают. */
    fun observeAllBookmarks(): Flow<List<WithBook<Bookmark>>>

    /** Все цитаты, сначала новые. */
    fun observeAllHighlights(): Flow<List<WithBook<Highlight>>>

    suspend fun addBookmark(bookmark: Bookmark): Long

    suspend fun deleteBookmark(id: Long)

    suspend fun addHighlight(highlight: Highlight): Long

    suspend fun changeHighlightColor(id: Long, color: HighlightColor)

    suspend fun deleteHighlight(id: Long)
}
