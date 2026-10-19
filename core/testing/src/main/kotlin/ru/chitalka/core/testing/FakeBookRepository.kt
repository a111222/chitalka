package ru.chitalka.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.model.Book
import ru.chitalka.core.model.ReadingPosition

/** Репозиторий книг в памяти для тестов ViewModel. */
class FakeBookRepository(initial: List<Book> = emptyList()) : BookRepository {
    private val books = MutableStateFlow(initial)
    private val positions = MutableStateFlow<Map<Long, ReadingPosition>>(emptyMap())

    fun setBooks(value: List<Book>) {
        books.value = value
    }

    override fun observeBooks(): Flow<List<Book>> = books

    override fun observeBook(id: Long): Flow<Book?> = books.map { list -> list.firstOrNull { it.id == id } }

    override fun search(query: String): Flow<List<Book>> = books.map { list ->
        list.filter { it.title.contains(query, ignoreCase = true) || it.author.orEmpty().contains(query, ignoreCase = true) }
    }

    override fun observePosition(bookId: Long): Flow<ReadingPosition?> = positions.map { it[bookId] }

    override suspend fun savePosition(bookId: Long, position: ReadingPosition, percent: Float) {
        positions.update { it + (bookId to position) }
    }

    override suspend fun deleteBook(id: Long) {
        books.update { list -> list.filterNot { it.id == id } }
    }

    fun positionOf(bookId: Long): ReadingPosition? = positions.value[bookId]
}
