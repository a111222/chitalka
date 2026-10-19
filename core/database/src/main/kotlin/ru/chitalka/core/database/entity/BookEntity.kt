package ru.chitalka.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.chitalka.core.model.BookFormat
import java.time.Instant

@Entity(
    tableName = "books",
    // fileHash уникален — дубли книг не импортируем; lastOpenedAt — для полки «Читаю сейчас»
    indices = [Index(value = ["fileHash"], unique = true), Index("lastOpenedAt")],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    val series: String?,
    val seriesIndex: Int?,
    val language: String?,
    val format: BookFormat,
    val filePath: String,
    val coverPath: String?,
    val fileHash: String,
    val sizeBytes: Long,
    val addedAt: Instant,
    val lastOpenedAt: Instant?,
    val isFinished: Boolean = false,
)
