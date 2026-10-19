package ru.chitalka.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

// Все таблицы ниже висят на books и удаляются вместе с книгой (CASCADE).

@Entity(
    tableName = "toc_entries",
    foreignKeys = [ForeignKey(BookEntity::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId")],
)
data class TocEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val title: String,
    val level: Int,
    val chapterIndex: Int,
    val anchor: String?,
)

/** Одна строка на книгу: где остановился читатель. */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [ForeignKey(BookEntity::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
)
data class ReadingProgressEntity(
    @PrimaryKey val bookId: Long,
    val chapterIndex: Int,
    val charOffset: Int,
    val percent: Float,
    val updatedAt: Instant,
)

/**
 * Закладки и выделения не привязаны к books внешним ключом: в демонстрационной книге (её нет в базе)
 * тоже можно ставить закладки. При удалении книги их стирает репозиторий.
 */
@Entity(tableName = "bookmarks", indices = [Index("bookId")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val charOffset: Int,
    val label: String,
    val snippet: String,
    val percent: Float,
    val createdAt: Instant,
)

@Entity(tableName = "highlights", indices = [Index("bookId")])
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    /** Цвет маркера: порядковый номер [ru.chitalka.core.model.HighlightColor]. */
    val color: Int,
    val note: String?,
    val percent: Float,
    val createdAt: Instant,
)

/**
 * Сколько читали книгу в один день. [day] — номер дня (LocalDate.toEpochDay) по часам телефона.
 * Тоже без внешнего ключа: удалили книгу — минуты в статистике остаются.
 */
@Entity(tableName = "reading_days", primaryKeys = ["day", "bookId"])
data class ReadingDayEntity(
    val day: Long,
    val bookId: Long,
    val durationMs: Long,
    val pages: Int,
)

/** Полка шкафа. Книга может стоять на нескольких полках (связь через shelf_books). */
@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "shelf_books",
    primaryKeys = ["shelfId", "bookId"],
    foreignKeys = [
        ForeignKey(ShelfEntity::class, ["id"], ["shelfId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(BookEntity::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("bookId")],
)
data class ShelfBookEntity(
    val shelfId: Long,
    val bookId: Long,
    val position: Int,
)
