package ru.chitalka.core.bookparser

import ru.chitalka.core.model.BookFormat
import java.io.File

/** Парсер одного формата. Реализации EPUB и FB2 появятся на этапе «Библиотека». */
interface BookParser {
    val format: BookFormat

    fun parse(file: File): ParsedBook

    fun readChapter(file: File, index: Int): Chapter
}
