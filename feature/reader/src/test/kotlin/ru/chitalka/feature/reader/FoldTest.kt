package ru.chitalka.feature.reader

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FoldTest {
    private val width = 400f
    private val height = 800f
    private val corner = Offset(width, height)

    @Test
    fun cornerLandsUnderFinger() {
        val fold = Fold(0f, width, height, corner, Offset(250f, 700f))
        val landed = fold.reflect(corner)
        assertEquals(250f, landed.x, 0.01f)
        assertEquals(700f, landed.y, 0.01f)
    }

    @Test
    fun liftedAndRemainingPartsMakeTheWholeSheet() {
        val fold = Fold(0f, width, height, corner, Offset(120f, 520f))
        assertEquals(width * height, area(fold.remaining) + area(fold.lifted), 1f)
        // Оборот — та же отогнутая часть, только перевёрнутая: площадь не меняется
        assertEquals(area(fold.lifted), area(fold.flap), 1f)
    }

    @Test
    fun untouchedSheetHasNoFlap() {
        val fold = Fold(0f, width, height, corner, corner)
        assertEquals(width * height, area(fold.remaining), 1f)
        assertTrue(fold.flap.isEmpty())
    }

    @Test
    fun fingerCannotTearSheetFromSpine() {
        // Палец утащили далеко влево за корешок: уголок остаётся не дальше ширины листа от корешка
        val fold = Fold(0f, width, height, corner, Offset(-900f, height))
        assertTrue(fold.finger.getDistance(Offset(0f, height)) <= width + 0.01f)
    }

    @Test
    fun spreadSheetTurnsAroundSpine() {
        // Разворот 800 px: правый лист от корешка 400 до 800, взят за нижний правый угол
        val fold = Fold(400f, 400f, height, Offset(800f, height), Offset(600f, 760f))
        assertEquals(400f * height, area(fold.remaining) + area(fold.lifted), 1f)
        assertTrue(fold.remaining.all { it.x >= 400f - 0.01f })
    }

    private fun Offset.getDistance(other: Offset) = (this - other).getDistance()

    /** Площадь многоугольника по формуле шнурования. */
    private fun area(poly: List<Offset>): Float {
        var sum = 0f
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            sum += a.x * b.y - b.x * a.y
        }
        return abs(sum) / 2f
    }
}
