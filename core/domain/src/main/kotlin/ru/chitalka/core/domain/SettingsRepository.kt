package ru.chitalka.core.domain

import kotlinx.coroutines.flow.Flow
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode

interface SettingsRepository {
    val settings: Flow<ReaderSettings>

    suspend fun setTheme(theme: ReaderThemeMode)

    suspend fun setFont(font: ReaderFont)

    suspend fun setFontSize(sizeSp: Int)

    suspend fun setLineSpacing(spacing: LineSpacing)

    suspend fun setHapticsEnabled(enabled: Boolean)

    suspend fun setDailyGoal(minutes: Int)
}
