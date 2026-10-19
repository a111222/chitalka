package ru.chitalka.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import ru.chitalka.core.domain.SettingsRepository
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode

class FakeSettingsRepository(initial: ReaderSettings = ReaderSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<ReaderSettings> = state

    override suspend fun setTheme(theme: ReaderThemeMode) = state.update { it.copy(theme = theme) }

    override suspend fun setFont(font: ReaderFont) = state.update { it.copy(font = font) }

    override suspend fun setFontSize(sizeSp: Int) = state.update { it.copy(fontSizeSp = sizeSp) }

    override suspend fun setLineSpacing(spacing: LineSpacing) = state.update { it.copy(lineSpacing = spacing) }

    override suspend fun setHapticsEnabled(enabled: Boolean) = state.update { it.copy(hapticsEnabled = enabled) }
}
