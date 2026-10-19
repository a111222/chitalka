package ru.chitalka.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.chitalka.core.designsystem.adaptive.WindowWidth
import ru.chitalka.core.designsystem.theme.colorsFor
import ru.chitalka.core.model.ReaderThemeMode

class ThemeTokensTest {
    /** Контраст текста к бумаге во всех темах — не ниже 4.5:1 (требование доступности из макета). */
    @Test
    fun inkOnPaperIsReadableInEveryTheme() {
        ReaderThemeMode.entries.forEach { mode ->
            val colors = colorsFor(mode)
            val ratio = contrast(colors.ink, colors.paper)
            assertTrue("$mode: контраст $ratio", ratio >= 4.5f)
            val secondary = contrast(colors.inkSecondary, colors.paper)
            assertTrue("$mode: контраст вторичного текста $secondary", secondary >= 4.5f)
        }
    }

    /** Приложение одинаковое во всех оттенках: меняется только страница читалки. */
    @Test
    fun pageTonesChangeOnlyThePage() {
        val graphite = colorsFor(ReaderThemeMode.Graphite)
        ReaderThemeMode.entries.forEach { mode ->
            val colors = colorsFor(mode)
            assertEquals(graphite.background, colors.background)
            assertEquals(graphite.surface, colors.surface)
            assertEquals(graphite.accent, colors.accent)
        }
        assertTrue(colorsFor(ReaderThemeMode.Midnight).paper.luminance() < graphite.paper.luminance())
    }

    /** Подписи на карточках читаются и на фоне приложения, и на карточке. */
    @Test
    fun secondaryTextIsReadableOnCards() {
        val colors = colorsFor(ReaderThemeMode.Graphite)
        assertTrue(contrast(colors.inkSecondary, colors.surface) >= 4.5f)
        assertTrue(contrast(colors.ink, colors.surfaceHigh) >= 4.5f)
    }

    @Test
    fun windowWidthBreakpointsFollowWindowSizeClass() {
        assertEquals(WindowWidth.Compact, WindowWidth.fromWidth(599.dp))
        assertEquals(WindowWidth.Medium, WindowWidth.fromWidth(600.dp))
        assertEquals(WindowWidth.Expanded, WindowWidth.fromWidth(840.dp))
    }

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return maxOf(la, lb) / minOf(la, lb)
    }
}
