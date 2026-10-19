package ru.chitalka.core.data

import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.datastore.ReaderSettingsDataSource
import ru.chitalka.core.domain.SettingsRepository
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode
import javax.inject.Inject

internal class OfflineSettingsRepository @Inject constructor(
    private val dataSource: ReaderSettingsDataSource,
) : SettingsRepository {
    override val settings: Flow<ReaderSettings> = dataSource.settings

    override suspend fun setTheme(theme: ReaderThemeMode) {
        dataSource.setTheme(theme)
    }

    override suspend fun setFont(font: ReaderFont) {
        dataSource.setFont(font)
    }

    override suspend fun setFontSize(sizeSp: Int) {
        dataSource.setFontSize(sizeSp)
    }

    override suspend fun setLineSpacing(spacing: LineSpacing) {
        dataSource.setLineSpacing(spacing)
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataSource.setHapticsEnabled(enabled)
    }

    override suspend fun setShelfLightsOn(on: Boolean) {
        dataSource.setShelfLightsOn(on)
    }
}
