package ru.chitalka.feature.stats

import ru.chitalka.core.model.ReadingDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Столбик графика «Эта неделя». */
data class DayBar(
    val date: LocalDate,
    /** «Пн», «Вт»… */
    val label: String,
    val minutes: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/** Всё, что показывает вкладка «Статистика», уже посчитанное. */
data class ReadingStats(
    val todayMinutes: Int = 0,
    val goalMinutes: Int = 30,
    /** Понедельник — воскресенье текущей недели. */
    val week: List<DayBar> = emptyList(),
    val weekMinutes: Int = 0,
    /** Дней подряд с выполненной целью; сегодняшний день не обрывает серию, пока он не закончился. */
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val booksThisYear: Int = 0,
    val booksThisMonth: Int = 0,
    val month: Int = 1,
    /** Перелистнуто с понедельника и за те же дни прошлой недели. */
    val pagesThisWeek: Int = 0,
    val pagesLastWeek: Int = 0,
    /** Среднее за последние 7 дней, в минутах. */
    val averageMinutes: Int = 0,
) {
    val goalReached: Boolean get() = todayMinutes >= goalMinutes
    val goalProgress: Float get() = if (goalMinutes <= 0) 0f else (todayMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
}

private val DayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private const val MINUTE_MS = 60_000L

/**
 * Собирает статистику из строк «день + книга». Чистая функция без часов и базы:
 * [today] и [zone] передаются снаружи, поэтому её легко проверить тестом.
 */
fun buildStats(
    days: List<ReadingDay>,
    finishedAt: List<Instant>,
    goalMinutes: Int,
    today: LocalDate,
    zone: ZoneId,
): ReadingStats {
    val msByDate = days.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.durationMs } }
    val pagesByDate = days.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.pages } }
    fun minutes(date: LocalDate) = ((msByDate[date] ?: 0L) / MINUTE_MS).toInt()
    val goalMs = goalMinutes * MINUTE_MS
    fun met(date: LocalDate) = (msByDate[date] ?: 0L) >= goalMs

    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val week = (0 until 7).map { i ->
        val date = monday.plusDays(i.toLong())
        DayBar(date, DayLabels[i], minutes(date), isToday = date == today, isFuture = date.isAfter(today))
    }

    var streak = 0
    var day = if (met(today)) today else today.minusDays(1)
    while (met(day)) {
        streak++
        day = day.minusDays(1)
    }
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (date in msByDate.keys.filter(::met).sorted()) {
        run = if (previous != null && previous.plusDays(1) == date) run + 1 else 1
        best = maxOf(best, run)
        previous = date
    }

    val finishedDates = finishedAt.map { it.atZone(zone).toLocalDate() }
    val elapsed = (today.toEpochDay() - monday.toEpochDay()).toInt()
    fun pagesBetween(from: LocalDate, count: Int) = (0..count).sumOf { pagesByDate[from.plusDays(it.toLong())] ?: 0 }

    return ReadingStats(
        todayMinutes = minutes(today),
        goalMinutes = goalMinutes,
        week = week,
        weekMinutes = week.sumOf { it.minutes },
        streak = streak,
        bestStreak = maxOf(best, streak),
        booksThisYear = finishedDates.count { it.year == today.year },
        booksThisMonth = finishedDates.count { it.year == today.year && it.month == today.month },
        month = today.monthValue,
        pagesThisWeek = pagesBetween(monday, elapsed),
        pagesLastWeek = pagesBetween(monday.minusWeeks(1), elapsed),
        averageMinutes = ((0L..6L).sumOf { msByDate[today.minusDays(it)] ?: 0L } / MINUTE_MS / 7).toInt(),
    )
}

/** «45 мин», «3 ч 12 мин», «2 ч». */
fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m мин"
        m == 0 -> "$h ч"
        else -> "$h ч $m мин"
    }
}

/** Русское число: plural(5, "день", "дня", "дней") → «дней». */
fun plural(n: Int, one: String, few: String, many: String): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

/** Месяц для «+2 за октябрь». */
fun monthName(month: Int): String = listOf(
    "январь", "февраль", "март", "апрель", "май", "июнь",
    "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
)[(month - 1).coerceIn(0, 11)]

/** Подпись под кольцом цели. */
fun goalCaption(stats: ReadingStats): String {
    val left = stats.goalMinutes - stats.todayMinutes
    return when {
        stats.goalReached && stats.streak > 1 -> "Цель выполнена, серия — ${stats.streak} ${plural(stats.streak, "день", "дня", "дней")}"
        stats.goalReached -> "Цель на сегодня выполнена"
        stats.streak > 0 -> "Ещё $left ${plural(left, "минута", "минуты", "минут")}, и серия продолжится"
        stats.todayMinutes > 0 -> "Ещё $left ${plural(left, "минута", "минуты", "минут")} до цели"
        else -> "Почитайте ${stats.goalMinutes} ${plural(stats.goalMinutes, "минуту", "минуты", "минут")}, чтобы начать серию"
    }
}

/** «на 12% больше», «на 5% меньше», «как на прошлой неделе». */
fun pagesTrend(thisWeek: Int, lastWeek: Int): String = when {
    lastWeek == 0 && thisWeek == 0 -> "листайте, и здесь появится счёт"
    lastWeek == 0 -> "с понедельника"
    else -> {
        val change = ((thisWeek - lastWeek) * 100f / lastWeek).toInt()
        when {
            change > 0 -> "на $change% больше"
            change < 0 -> "на ${-change}% меньше"
            else -> "как на прошлой неделе"
        }
    }
}
