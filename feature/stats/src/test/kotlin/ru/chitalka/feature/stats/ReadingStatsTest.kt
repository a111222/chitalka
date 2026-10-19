package ru.chitalka.feature.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.chitalka.core.model.ReadingDay
import java.time.LocalDate
import java.time.ZoneOffset

class ReadingStatsTest {
    // Понедельник, 19 октября 2026
    private val today = LocalDate.of(2026, 10, 19).plusDays(3) // четверг

    private fun day(daysAgo: Long, minutes: Int, pages: Int = 0, bookId: Long = 1) =
        ReadingDay(today.minusDays(daysAgo), bookId, minutes * 60_000L, pages)

    private fun stats(vararg days: ReadingDay, goal: Int = 30) =
        buildStats(days.toList(), emptyList(), goal, today, ZoneOffset.UTC)

    @Test
    fun weekStartsOnMondayAndSumsBooks() {
        val s = stats(day(0, 10, bookId = 1), day(0, 14, bookId = 2), day(3, 32), day(4, 50))
        assertEquals(listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"), s.week.map { it.label })
        assertEquals(listOf(32, 0, 0, 24, 0, 0, 0), s.week.map { it.minutes })
        assertTrue(s.week[3].isToday)
        assertTrue(s.week[4].isFuture)
        assertEquals(24, s.todayMinutes)
        assertEquals(56, s.weekMinutes) // прошлое воскресенье в эту неделю не входит
        assertFalse(s.goalReached)
    }

    @Test
    fun streakCountsGoalDaysAndTodayDoesNotBreakIt() {
        // Вчера, позавчера и третьего дня цель выполнена, сегодня пока 5 минут
        val s = stats(day(0, 5), day(1, 30), day(2, 45), day(3, 31), day(5, 40), day(6, 40), day(7, 40), day(8, 40))
        assertEquals(3, s.streak)
        assertEquals(4, s.bestStreak)
        assertEquals("Ещё 25 минут, и серия продолжится", goalCaption(s))

        val done = stats(day(0, 30), day(1, 30))
        assertEquals(2, done.streak)
        assertEquals("Цель выполнена, серия — 2 дня", goalCaption(done))

        val broken = stats(day(2, 30))
        assertEquals(0, broken.streak)
        assertEquals(1, broken.bestStreak)
    }

    @Test
    fun pagesAreComparedWithTheSameDaysOfLastWeek() {
        val s = stats(day(0, 1, pages = 30), day(3, 1, pages = 80), day(7, 1, pages = 50), day(10, 1, pages = 50), day(11, 1, pages = 999))
        assertEquals(110, s.pagesThisWeek)
        assertEquals(100, s.pagesLastWeek) // прошлый понедельник..четверг; воскресенье до него не считается
        assertEquals("на 10% больше", pagesTrend(s.pagesThisWeek, s.pagesLastWeek))
        assertEquals("на 50% меньше", pagesTrend(50, 100))
        assertEquals("с понедельника", pagesTrend(5, 0))
    }

    @Test
    fun booksFinishedThisYearAndMonth() {
        val finished = listOf(
            LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 3, 1), LocalDate.of(2025, 12, 31),
        ).map { it.atStartOfDay().toInstant(ZoneOffset.UTC) }
        val s = buildStats(emptyList(), finished, 30, today, ZoneOffset.UTC)
        assertEquals(3, s.booksThisYear)
        assertEquals(2, s.booksThisMonth)
        assertEquals("октябрь", monthName(s.month))
    }

    @Test
    fun averageIsOverTheLastSevenDays() {
        assertEquals(10, stats(day(0, 35), day(6, 35), day(7, 500)).averageMinutes)
    }

    @Test
    fun russianWords() {
        assertEquals("3 ч 12 мин", formatMinutes(192))
        assertEquals("45 мин", formatMinutes(45))
        assertEquals("2 ч", formatMinutes(120))
        assertEquals(listOf("день", "дня", "дней", "дней", "день", "дня"), listOf(1, 3, 5, 12, 21, 22).map { plural(it, "день", "дня", "дней") })
    }
}
