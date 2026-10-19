package ru.chitalka.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 → 2: закладки и выделения без внешнего ключа на books (чтобы работали и в демо-книге),
 * у них появились текст, название главы и процент; новая таблица минут чтения по дням.
 * В версии 1 закладки и выделения ещё ничего не сохраняли, поэтому таблицы можно пересоздать.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `bookmarks`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bookmarks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`bookId` INTEGER NOT NULL, `chapterIndex` INTEGER NOT NULL, `charOffset` INTEGER NOT NULL, " +
                "`label` TEXT NOT NULL, `snippet` TEXT NOT NULL, `percent` REAL NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_bookId` ON `bookmarks` (`bookId`)")

        db.execSQL("DROP TABLE IF EXISTS `highlights`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `highlights` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`bookId` INTEGER NOT NULL, `chapterIndex` INTEGER NOT NULL, `startOffset` INTEGER NOT NULL, " +
                "`endOffset` INTEGER NOT NULL, `text` TEXT NOT NULL, `color` INTEGER NOT NULL, `note` TEXT, " +
                "`percent` REAL NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_highlights_bookId` ON `highlights` (`bookId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reading_days` (`day` INTEGER NOT NULL, `bookId` INTEGER NOT NULL, " +
                "`durationMs` INTEGER NOT NULL, `pages` INTEGER NOT NULL, PRIMARY KEY(`day`, `bookId`))",
        )
    }
}
