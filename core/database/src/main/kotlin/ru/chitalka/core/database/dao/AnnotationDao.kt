package ru.chitalka.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.database.entity.BookmarkEntity
import ru.chitalka.core.database.entity.HighlightEntity

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY chapterIndex, charOffset")
    fun observeBookmarks(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY chapterIndex, startOffset")
    fun observeHighlights(bookId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun observeAllHighlights(): Flow<List<HighlightEntity>>

    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Insert
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Long)

    @Query("UPDATE highlights SET color = :color WHERE id = :id")
    suspend fun updateHighlightColor(id: Long, color: Int)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteBookmarksOf(bookId: Long)

    @Query("DELETE FROM highlights WHERE bookId = :bookId")
    suspend fun deleteHighlightsOf(bookId: Long)
}
