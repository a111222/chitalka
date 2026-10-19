package ru.chitalka.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.chitalka.core.domain.AnnotationRepository
import ru.chitalka.core.domain.BookContentRepository
import ru.chitalka.core.domain.BookImporter
import ru.chitalka.core.domain.BookRepository
import ru.chitalka.core.domain.ReadingStatsRepository
import ru.chitalka.core.domain.SettingsRepository

/** Говорит Hilt, какую реализацию отдавать, когда фича просит интерфейс из domain. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindBookRepository(impl: OfflineBookRepository): BookRepository

    @Binds
    abstract fun bindBookContentRepository(impl: LocalBookContentRepository): BookContentRepository

    @Binds
    abstract fun bindBookImporter(impl: LocalBookImporter): BookImporter

    @Binds
    abstract fun bindAnnotationRepository(impl: OfflineAnnotationRepository): AnnotationRepository

    @Binds
    abstract fun bindReadingStatsRepository(impl: OfflineReadingStatsRepository): ReadingStatsRepository

    @Binds
    abstract fun bindSettingsRepository(impl: OfflineSettingsRepository): SettingsRepository
}
