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

    @Test
    fun onlyEInkDisablesEffects() {
        assertTrue(colorsFor(ReaderThemeMode.EInk).isEInk)
        assertTrue(!colorsFor(ReaderThemeMode.LightOak).isEInk)
        assertTrue(colorsFor(ReaderThemeMode.DarkWalnut).isDark)
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
