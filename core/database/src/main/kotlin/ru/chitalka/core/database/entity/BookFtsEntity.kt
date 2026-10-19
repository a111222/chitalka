package ru.chitalka.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

/** Полнотекстовый индекс FTS4 над books: мгновенный поиск по библиотеке. */
@Fts4(contentEntity = BookEntity::class)
@Entity(tableName = "books_fts")
data class BookFtsEntity(
    val title: String,
    val author: String?,
    val series: String?,
)
