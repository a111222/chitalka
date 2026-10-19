package ru.chitalka.core.domain

import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.model.ReadingDay
import java.time.Instant
import java.time.LocalDate

/** Минуты и страницы по дням для вкладки «Статистика». Всё хранится на телефоне. */
interface ReadingStatsRepository {
    /** Строки «день + книга», начиная с [since] включительно. */
    fun observeDays(since: LocalDate): Flow<List<ReadingDay>>

    /** Прибавляет к дню [date] время и перелистнутые страницы книги [bookId]. */
    suspend fun addReading(bookId: Long, date: LocalDate, durationMs: Long, pages: Int)

    /** Когда дочитали каждую прочитанную до конца книгу: для «Книг за год». */
    fun observeFinishedAt(): Flow<List<Instant>>
}
