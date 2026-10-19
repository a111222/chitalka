package ru.chitalka.feature.bookmarks

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val Months = listOf("янв.", "февр.", "марта", "апр.", "мая", "июня", "июля", "авг.", "сент.", "окт.", "нояб.", "дек.")

/** «сегодня», «вчера», «17 окт.», а для прошлых лет — «17 окт. 2025». */
internal fun relativeDay(instant: Instant, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String {
    val date = instant.atZone(zone).toLocalDate()
    return when {
        date == today -> "сегодня"
        date == today.minusDays(1) -> "вчера"
        date.year == today.year -> "${date.dayOfMonth} ${Months[date.monthValue - 1]}"
        else -> "${date.dayOfMonth} ${Months[date.monthValue - 1]} ${date.year}"
    }
}

/** Доля книги для подписи: «12%». */
internal fun percentText(percent: Float): String = "${(percent * 100).toInt().coerceIn(0, 100)}%"
