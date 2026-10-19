package ru.chitalka.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.database.entity.BookEntity
import java.time.Instant

@Dao
interface BookDao {
    /** Сначала недавно открытые, потом недавно добавленные. */
    @Query(
        """
        SELECT * FROM books
        ORDER BY lastOpenedAt IS NULL, lastOpenedAt DESC, addedAt DESC
        """,
    )
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: Long): Flow<BookEntity?>

    /** Поиск через FTS4. query — в синтаксисе MATCH, например «толст*». */
    @Query(
        """
        SELECT books.* FROM books
        JOIN books_fts ON books.id = books_fts.rowid
        WHERE books_fts MATCH :query
        """,
    )
    fun search(query: String): Flow<List<BookEntity>>

    /** Возвращает -1, если книга с таким fileHash уже есть. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookEntity): Long

    @Query("UPDATE books SET lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun markOpened(id: Long, openedAt: Instant)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: Long)
}
