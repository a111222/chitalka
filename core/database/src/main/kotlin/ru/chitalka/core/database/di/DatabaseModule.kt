package ru.chitalka.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.chitalka.core.database.ChitalkaDatabase
import ru.chitalka.core.database.MIGRATION_1_2
import ru.chitalka.core.database.dao.AnnotationDao
import ru.chitalka.core.database.dao.BookDao
import ru.chitalka.core.database.dao.ProgressDao
import ru.chitalka.core.database.dao.StatsDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChitalkaDatabase =
        Room.databaseBuilder(context, ChitalkaDatabase::class.java, "chitalka.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideBookDao(db: ChitalkaDatabase): BookDao = db.bookDao()

    @Provides
    fun provideProgressDao(db: ChitalkaDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideAnnotationDao(db: ChitalkaDatabase): AnnotationDao = db.annotationDao()

    @Provides
    fun provideStatsDao(db: ChitalkaDatabase): StatsDao = db.statsDao()
}
