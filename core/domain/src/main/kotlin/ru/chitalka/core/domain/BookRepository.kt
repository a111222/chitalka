package ru.chitalka.core.domain

import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.model.Book
import ru.chitalka.core.model.ReadingPosition

/**
 * Книги и прогресс чтения. Интерфейс живёт в domain, реализация — в core:data.
 * Так фичи не знают, что внутри Room.
 */
interface BookRepository {
    /** Все книги, сначала недавно открытые. Flow сам присылает новое значение при изменении базы. */
    fun observeBooks(): Flow<List<Book>>

    fun observeBook(id: Long): Flow<Book?>

    fun search(query: String): Flow<List<Book>>

    fun observePosition(bookId: Long): Flow<ReadingPosition?>

    suspend fun savePosition(bookId: Long, position: ReadingPosition, percent: Float)
}
