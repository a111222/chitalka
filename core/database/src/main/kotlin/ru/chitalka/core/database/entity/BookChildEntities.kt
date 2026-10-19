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

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(BookEntity::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId")],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val charOffset: Int,
    val snippet: String,
    val createdAt: Instant,
)

@Entity(
    tableName = "highlights",
    foreignKeys = [ForeignKey(BookEntity::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId")],
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val color: Int,
    val note: String?,
    val createdAt: Instant,
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
