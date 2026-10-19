package ru.chitalka.core.model

import java.time.LocalDate

/** Сколько читали одну книгу за один день: из таких строк собирается статистика. */
data class ReadingDay(
    val date: LocalDate,
    val bookId: Long,
    val durationMs: Long,
    /** Сколько раз перелистнули вперёд. */
    val pages: Int,
)
