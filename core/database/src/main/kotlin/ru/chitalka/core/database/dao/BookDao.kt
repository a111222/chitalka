package ru.chitalka.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.database.entity.BookEntity
import ru.chitalka.core.database.entity.TocEntryEntity
import java.time.Instant

/** Абстрактный класс, а не интерфейс: так можно написать метод-транзакцию с телом. */
@Dao
abstract class BookDao {
    /** Сначала недавно открытые, потом недавно добавленные. */
    @Query(
        """
        SELECT * FROM books
        ORDER BY lastOpenedAt IS NULL, lastOpenedAt DESC, addedAt DESC
        """,
    )
    abstract fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    abstract fun observeById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    abstract suspend fun getById(id: Long): BookEntity?

    @Query("SELECT id FROM books WHERE fileHash = :hash")
    abstract suspend fun findIdByHash(hash: String): Long?

    /** Поиск через FTS4. query — в синтаксисе MATCH, например «толст*». */
    @Query(
        """
        SELECT books.* FROM books
        JOIN books_fts ON books.id = books_fts.rowid
        WHERE books_fts MATCH :query
        """,
    )
    abstract fun search(query: String): Flow<List<BookEntity>>

    /** Возвращает -1, если книга с таким fileHash уже есть. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(book: BookEntity): Long

    @Insert
    abstract suspend fun insertToc(entries: List<TocEntryEntity>)

    /** Книга и её оглавление попадают в базу вместе или не попадают вовсе. */
    @Transaction
    open suspend fun insertWithToc(book: BookEntity, toc: List<TocEntryEntity>): Long {
        val id = insert(book)
        if (id != -1L && toc.isNotEmpty()) insertToc(toc.map { it.copy(bookId = id) })
        return id
    }

    @Query("UPDATE books SET lastOpenedAt = :openedAt WHERE id = :id")
    abstract suspend fun markOpened(id: Long, openedAt: Instant)

    @Query("DELETE FROM books WHERE id = :id")
    abstract suspend fun delete(id: Long)
}
