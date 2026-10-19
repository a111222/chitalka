package ru.chitalka.core.testing

import ru.chitalka.core.domain.BookImporter
import ru.chitalka.core.domain.ImportResult

/** Импорт, который возвращает заранее заданные результаты. */
class FakeBookImporter(var results: List<ImportResult> = emptyList()) : BookImporter {
    val imported = mutableListOf<String>()

    override suspend fun import(uris: List<String>): List<ImportResult> {
        imported += uris
        return results
    }
}
