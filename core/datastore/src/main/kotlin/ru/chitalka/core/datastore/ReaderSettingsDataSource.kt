package ru.chitalka.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode
import javax.inject.Inject

/** Настройки чтения как простой «ключ — значение» в Preferences DataStore. */
class ReaderSettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<ReaderSettings> = dataStore.data.map { prefs ->
        val defaults = ReaderSettings()
        ReaderSettings(
            theme = prefs[Keys.THEME].toEnum(defaults.theme),
            font = prefs[Keys.FONT].toEnum(defaults.font),
            fontSizeSp = (prefs[Keys.FONT_SIZE] ?: defaults.fontSizeSp)
                .coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE),
            lineSpacing = prefs[Keys.LINE_SPACING].toEnum(defaults.lineSpacing),
            hapticsEnabled = prefs[Keys.HAPTICS] ?: defaults.hapticsEnabled,
            dailyGoalMinutes = (prefs[Keys.DAILY_GOAL] ?: defaults.dailyGoalMinutes).coerceIn(1, 24 * 60),
        )
    }

    suspend fun setTheme(theme: ReaderThemeMode) = dataStore.edit { it[Keys.THEME] = theme.name }

    suspend fun setFont(font: ReaderFont) = dataStore.edit { it[Keys.FONT] = font.name }

    suspend fun setFontSize(sizeSp: Int) = dataStore.edit {
        it[Keys.FONT_SIZE] = sizeSp.coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE)
    }

    suspend fun setLineSpacing(spacing: LineSpacing) = dataStore.edit { it[Keys.LINE_SPACING] = spacing.name }

    suspend fun setHapticsEnabled(enabled: Boolean) = dataStore.edit { it[Keys.HAPTICS] = enabled }

    suspend fun setDailyGoal(minutes: Int) = dataStore.edit { it[Keys.DAILY_GOAL] = minutes.coerceIn(1, 24 * 60) }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT = stringPreferencesKey("font")
        val FONT_SIZE = intPreferencesKey("font_size_sp")
        val LINE_SPACING = stringPreferencesKey("line_spacing")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val DAILY_GOAL = intPreferencesKey("daily_goal_minutes")
    }
}

/** Если в файле осталось значение, которого больше нет в enum, берём значение по умолчанию. */
private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
