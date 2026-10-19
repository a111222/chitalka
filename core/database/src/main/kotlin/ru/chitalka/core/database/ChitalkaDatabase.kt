package ru.chitalka.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.chitalka.core.database.dao.BookDao
import ru.chitalka.core.database.dao.ProgressDao
import ru.chitalka.core.database.entity.BookEntity
import ru.chitalka.core.database.entity.BookFtsEntity
import ru.chitalka.core.database.entity.BookmarkEntity
import ru.chitalka.core.database.entity.HighlightEntity
import ru.chitalka.core.database.entity.ReadingProgressEntity
import ru.chitalka.core.database.entity.ShelfBookEntity
import ru.chitalka.core.database.entity.ShelfEntity
import ru.chitalka.core.database.entity.TocEntryEntity

/**
 * База читалки. exportSchema = true: схема каждой версии лежит в schemas/,
 * чтобы при обновлении приложения писать миграции и никогда не стирать библиотеку.
 */
@Database(
    entities = [
        BookEntity::class,
        BookFtsEntity::class,
        TocEntryEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        ShelfEntity::class,
        ShelfBookEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ChitalkaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun progressDao(): ProgressDao
}
