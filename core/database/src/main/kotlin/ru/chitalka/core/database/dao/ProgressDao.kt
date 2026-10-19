package ru.chitalka.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.database.entity.ReadingProgressEntity
import java.time.Instant

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun observe(bookId: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress")
    fun observeAll(): Flow<List<ReadingProgressEntity>>

    /** Когда в последний раз открывали дочитанные книги: считаем это днём, когда книгу закончили. */
    @Query("SELECT updatedAt FROM reading_progress WHERE percent >= 0.99")
    fun observeFinishedAt(): Flow<List<Instant>>

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)
}
