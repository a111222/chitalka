package ru.chitalka.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.database.entity.ReadingDayEntity

@Dao
abstract class StatsDao {
    @Query("SELECT * FROM reading_days WHERE day >= :fromDay")
    abstract fun observeSince(fromDay: Long): Flow<List<ReadingDayEntity>>

    @Query("SELECT * FROM reading_days WHERE day = :day AND bookId = :bookId")
    abstract suspend fun get(day: Long, bookId: Long): ReadingDayEntity?

    @Upsert
    abstract suspend fun upsert(entity: ReadingDayEntity)

    /** Прибавляет минуты и страницы к уже записанным за этот день (на Android 8 нет UPSERT в самом SQLite). */
    @Transaction
    open suspend fun add(day: Long, bookId: Long, durationMs: Long, pages: Int) {
        val old = get(day, bookId)
        upsert(
            ReadingDayEntity(
                day = day,
                bookId = bookId,
                durationMs = (old?.durationMs ?: 0L) + durationMs,
                pages = (old?.pages ?: 0) + pages,
            ),
        )
    }
}
