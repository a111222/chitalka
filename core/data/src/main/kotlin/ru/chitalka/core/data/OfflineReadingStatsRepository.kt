package ru.chitalka.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.chitalka.core.database.dao.ProgressDao
import ru.chitalka.core.database.dao.StatsDao
import ru.chitalka.core.domain.ReadingStatsRepository
import ru.chitalka.core.model.ReadingDay
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

internal class OfflineReadingStatsRepository @Inject constructor(
    private val statsDao: StatsDao,
    private val progressDao: ProgressDao,
) : ReadingStatsRepository {

    override fun observeDays(since: LocalDate): Flow<List<ReadingDay>> =
        statsDao.observeSince(since.toEpochDay()).map { list ->
            list.map { ReadingDay(LocalDate.ofEpochDay(it.day), it.bookId, it.durationMs, it.pages) }
        }

    override suspend fun addReading(bookId: Long, date: LocalDate, durationMs: Long, pages: Int) {
        if (durationMs <= 0 && pages <= 0) return
        statsDao.add(date.toEpochDay(), bookId, durationMs.coerceAtLeast(0), pages.coerceAtLeast(0))
    }

    override fun observeFinishedAt(): Flow<List<Instant>> = progressDao.observeFinishedAt()
}
