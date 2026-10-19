package ru.chitalka.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.chitalka.core.database.ChitalkaDatabase
import ru.chitalka.core.database.dao.BookDao
import ru.chitalka.core.database.dao.ProgressDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChitalkaDatabase =
        Room.databaseBuilder(context, ChitalkaDatabase::class.java, "chitalka.db").build()

    @Provides
    fun provideBookDao(db: ChitalkaDatabase): BookDao = db.bookDao()

    @Provides
    fun provideProgressDao(db: ChitalkaDatabase): ProgressDao = db.progressDao()
}
