package ru.chitalka.feature.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import ru.chitalka.core.designsystem.component.glass
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.designsystem.theme.colorsFor
import ru.chitalka.core.designsystem.typography.ChitalkaFonts
import ru.chitalka.core.designsystem.typography.ReaderFonts
import ru.chitalka.core.model.LineSpacing
import ru.chitalka.core.model.ReaderFont
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReaderThemeMode
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private val Tabs = listOf("Шрифт", "Макет", "Темы", "Ещё")

/**
 * Шторка «Аа» из видео: матовое стекло поверх страницы, вкладки «Шрифт», «Макет», «Темы», «Ещё».
 * Каждое изменение сразу видно на странице под стеклом и сохраняется.
 */
@Composable
internal fun AppearanceSheet(
    settings: ReaderSettings,
    glassState: HazeState,
    onEvent: (ReaderEvent) -> Unit,
    onContents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReaderTheme.colors
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .navigationBarsPadding()
            .padding(start = 10.dp, end = 10.dp, bottom = 14.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .shadow(30.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .glass(glassState, tint = colors.glass, background = colors.surface, blurRadius = 22.dp)
            // Тапы по шторке не должны закрывать её через затемнение под ней
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 22.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Tabs.forEachIndexed { index, label -> SheetTab(label, selected = index == tab) { tab = index } }
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = false) },
            label = "sheetTab",
        ) { shown ->
            when (shown) {
                0 -> FontTab(settings, onEvent)
                1 -> LayoutTab(settings, onEvent)
                2 -> ThemesTab(settings, onEvent)
                else -> MoreTab(settings, onEvent, onContents)
            }
        }
    }
}

@Composable
private fun SheetTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = ReaderTheme.colors
    val color by animateColorAsState(if (selected) colors.ink else colors.inkSecondary, tween(250), label = "tab")
    val dot by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "tabDot")
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .size(4.dp)
                .graphicsLayer { alpha = dot }
                .clip(CircleShape)
                .background(colors.accent),
        )
    }
}

/** Карусель шрифтов: выбранный по центру крупно, соседние меньше и бледнее. Ниже — ползунок размера. */
@Composable
private fun FontTab(settings: ReaderSettings, onEvent: (ReaderEvent) -> Unit) {
    val colors = ReaderTheme.colors
    val fonts = ReaderFont.entries
    val selected = fonts.indexOf(settings.font)
    val position by animateFloatAsState(selected.toFloat(), tween(450, easing = MotionTokens.Emphasized), label = "fontCarousel")
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            fonts.forEachIndexed { index, font ->
                val offset = index - position
                val distance = min(abs(offset), 2f)
                Column(
                    Modifier
                        .padding(top = 8.dp)
                        .width(96.dp)
                        .graphicsLayer {
                            translationX = offset * 92.dp.toPx()
                            val scale = 1f - 0.18f * min(distance, 1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - 0.38f * distance
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.RadioButton) { onEvent(ReaderEvent.ChangeFont(font)) }
                        .semantics { this.selected = index == selected },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Аа", style = TextStyle(fontFamily = ReaderFonts.familyFor(font), fontSize = 30.sp, lineHeight = 36.sp), color = colors.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(font.displayName, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = colors.inkSecondary, maxLines = 1)
                }
            }
        }
        // Пока палец на ползунке, значение живёт здесь: страница перестраивается на каждом шаге
        var size by remember(settings.fontSizeSp) { mutableFloatStateOf(settings.fontSizeSp.toFloat()) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text("Аа", style = TextStyle(fontSize = 13.sp), color = colors.ink)
            Slider(
                value = size,
                onValueChange = { value ->
                    size = value
                    val rounded = value.roundToInt()
                    if (rounded != settings.fontSizeSp) onEvent(ReaderEvent.ChangeFontSize(rounded))
                },
                valueRange = ReaderSettings.MIN_FONT_SIZE.toFloat()..ReaderSettings.MAX_FONT_SIZE.toFloat(),
                steps = ReaderSettings.MAX_FONT_SIZE - ReaderSettings.MIN_FONT_SIZE - 1,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.track,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .semantics {
                        contentDescription = "Размер текста"
                        stateDescription = "${size.roundToInt()} пунктов"
                    },
            )
            Text("Аа", style = TextStyle(fontSize = 22.sp), color = colors.ink)
        }
    }
}

/** «Макет»: межстрочный интервал тремя кнопками. */
@Composable
private fun LayoutTab(settings: ReaderSettings, onEvent: (ReaderEvent) -> Unit) {
    val colors = ReaderTheme.colors
    Column(Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Межстрочный интервал", style = MaterialTheme.typography.labelMedium, color = colors.inkSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(LineSpacing.Compact to "Плотно", LineSpacing.Normal to "Обычно", LineSpacing.Relaxed to "Свободно").forEach { (spacing, label) ->
                OptionButton(label, selected = settings.lineSpacing == spacing, modifier = Modifier.weight(1f)) {
                    onEvent(ReaderEvent.ChangeSpacing(spacing))
                }
            }
        }
        Text(
            "Строк на странице станет меньше или больше, место в книге не потеряется.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkSecondary,
        )
    }
}

@Composable
private fun OptionButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = ReaderTheme.colors
    val selection by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "option")
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceHigh.copy(alpha = 0.7f))
            .drawBehind { drawRect(colors.accentBrush, alpha = selection) }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.ink)
    }
}

/** «Темы»: три тёмных оттенка страницы, каждый показан своей бумагой и своим цветом текста. */
@Composable
private fun ThemesTab(settings: ReaderSettings, onEvent: (ReaderEvent) -> Unit) {
    val colors = ReaderTheme.colors
    Row(Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(ReaderThemeMode.Graphite to "Графит", ReaderThemeMode.Midnight to "Полночь", ReaderThemeMode.Warm to "Тёплая").forEach { (mode, label) ->
            val swatch = colorsFor(mode)
            val selected = settings.theme == mode
            val ring by animateDpAsState(if (selected) 2.dp else 1.dp, tween(250), label = "themeRing")
            Column(
                Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(swatch.paper)
                    .border(ring, if (selected) colors.accent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable(role = Role.RadioButton) { onEvent(ReaderEvent.ChangeTheme(mode)) }
                    .semantics { this.selected = selected },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Аа", style = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontWeight = FontWeight.SemiBold, fontSize = 24.sp), color = swatch.ink)
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = swatch.inkSecondary)
            }
        }
    }
}

/** «Ещё»: вибрация при листании и переход к оглавлению. */
@Composable
private fun MoreTab(settings: ReaderSettings, onEvent: (ReaderEvent) -> Unit, onContents: () -> Unit) {
    val colors = ReaderTheme.colors
    Column(Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(role = Role.Switch) { onEvent(ReaderEvent.ChangeHaptics(!settings.hapticsEnabled)) }
                .semantics { stateDescription = if (settings.hapticsEnabled) "Включено" else "Выключено" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Вибрация при листании", style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
            GlassSwitch(settings.hapticsEnabled)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(role = Role.Button, onClick = onContents),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Оглавление", style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
            Icon(ChitalkaIcons.ArrowForward, contentDescription = null, tint = colors.inkSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

/** Переключатель из макета: дорожка 46×28, при включении заливается градиентом, кружок переезжает вправо. */
@Composable
private fun GlassSwitch(on: Boolean) {
    val colors = ReaderTheme.colors
    val fill by animateFloatAsState(if (on) 1f else 0f, tween(250), label = "switch")
    Box(
        Modifier
            .size(46.dp, 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.track)
            .drawBehind { drawRect(colors.accentBrush, alpha = fill) },
    ) {
        Box(
            Modifier
                .offset(x = 3.dp + 18.dp * fill, y = 3.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
