package ru.chitalka.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.chitalka.core.designsystem.adaptive.ReadingColumnMaxWidth
import ru.chitalka.core.designsystem.component.HeaderIconButton
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.designsystem.theme.colorsFor
import ru.chitalka.core.designsystem.typography.ReaderFonts
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode

@Composable
internal fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit = {},
) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    val settings = state.settings

    Surface(color = colors.background, contentColor = colors.ink, modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .safeDrawingPadding()
                    .widthIn(max = ReadingColumnMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeaderIconButton(ChitalkaIcons.Back, contentDescription = "Назад", onClick = onBack)
                    Text("Оформление", style = MaterialTheme.typography.headlineLarge)
                }

                Section("Оттенок страницы") {
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderThemeMode.entries.forEach { mode ->
                            ThemeSwatch(
                                mode = mode,
                                selected = settings.theme == mode,
                                onClick = {
                                    haptics.confirm()
                                    onEvent(SettingsEvent.ChangeTheme(mode))
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Section("Шрифт") {
                    Column(Modifier.selectableGroup()) {
                        ReaderFont.entries.forEach { font ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = settings.font == font,
                                        role = Role.RadioButton,
                                        onClick = { onEvent(SettingsEvent.ChangeFont(font)) },
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = settings.font == font, onClick = null)
                                Text(
                                    font.displayName,
                                    fontFamily = ReaderFonts.familyFor(font),
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }

                Section("Кегль: ${settings.fontSizeSp} pt") {
                    Slider(
                        value = settings.fontSizeSp.toFloat(),
                        onValueChange = {
                            val size = it.toInt()
                            if (size != settings.fontSizeSp) {
                                haptics.shelfSnap()
                                onEvent(SettingsEvent.ChangeFontSize(size))
                            }
                        },
                        valueRange = ReaderSettings.MIN_FONT_SIZE.toFloat()..ReaderSettings.MAX_FONT_SIZE.toFloat(),
                        steps = ReaderSettings.MAX_FONT_SIZE - ReaderSettings.MIN_FONT_SIZE - 1,
                    )
                    Text(
                        "Утро начиналось с запаха бумаги.",
                        style = TextStyle(
                            fontFamily = ReaderFonts.familyFor(settings.font),
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = settings.lineSpacing.multiplier.em,
                        ),
                    )
                }

                Section("Интервал") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LineSpacing.entries.forEach { spacing ->
                            FilterChip(
                                selected = settings.lineSpacing == spacing,
                                onClick = { onEvent(SettingsEvent.ChangeLineSpacing(spacing)) },
                                label = { Text(spacing.multiplier.toString()) },
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Вибрация", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Лёгкий отклик при листании и выборе книги",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary,
                        )
                    }
                    Switch(
                        checked = settings.hapticsEnabled,
                        onCheckedChange = {
                            haptics.toggle(it)
                            onEvent(SettingsEvent.ToggleHaptics(it))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

/** Образец темы: кусочек бумаги этой темы с текстом её цветом. */
@Composable
private fun ThemeSwatch(
    mode: ReaderThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swatch = colorsFor(mode)
    val label = when (mode) {
        ReaderThemeMode.Graphite -> "Графит"
        ReaderThemeMode.Midnight -> "Полночь"
        ReaderThemeMode.Warm -> "Тёплая"
    }
    Surface(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        color = swatch.paper,
        contentColor = swatch.ink,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) ReaderTheme.colors.accent else ReaderTheme.colors.track,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Аа", style = MaterialTheme.typography.headlineSmall, color = swatch.ink)
            Text(label, style = MaterialTheme.typography.labelMedium, color = swatch.inkSecondary)
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    ReaderTheme {
        SettingsScreen(state = SettingsUiState(isLoading = false), onEvent = {})
    }
}
