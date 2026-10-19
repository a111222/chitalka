package ru.chitalka.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.chitalka.core.domain.ReadingStatsRepository
import ru.chitalka.core.model.ReadingDay
import java.time.Instant
import java.time.LocalDate

class FakeReadingStatsRepository : ReadingStatsRepository {
    val days = MutableStateFlow<List<ReadingDay>>(emptyList())
    val finishedAt = MutableStateFlow<List<Instant>>(emptyList())

    override fun observeDays(since: LocalDate): Flow<List<ReadingDay>> = days.map { list -> list.filter { !it.date.isBefore(since) } }

    override suspend fun addReading(bookId: Long, date: LocalDate, durationMs: Long, pages: Int) = days.update { list ->
        val old = list.firstOrNull { it.date == date && it.bookId == bookId }
        val new = ReadingDay(date, bookId, (old?.durationMs ?: 0) + durationMs, (old?.pages ?: 0) + pages)
        list.filterNot { it === old } + new
    }

    override fun observeFinishedAt(): Flow<List<Instant>> = finishedAt
}
