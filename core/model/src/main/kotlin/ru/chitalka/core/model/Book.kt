package ru.chitalka.core.model

import java.time.Instant

/** Книга в библиотеке. Сам файл лежит в папке приложения, здесь только сведения о нём. */
data class Book(
    val id: Long,
    val title: String,
    val author: String?,
    val series: String? = null,
    val format: BookFormat,
    val coverPath: String? = null,
    val addedAt: Instant,
    val lastOpenedAt: Instant? = null,
    val progressPercent: Float = 0f,
)

enum class BookFormat { EPUB, FB2, PDF }
