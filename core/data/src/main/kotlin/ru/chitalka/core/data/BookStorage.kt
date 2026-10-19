package ru.chitalka.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Где лежат файлы книг и обложек: во внутренней папке приложения, без разрешений на память. */
@Singleton
internal class BookStorage @Inject constructor(@ApplicationContext context: Context) {
    val booksDir: File = File(context.filesDir, "books").apply { mkdirs() }
    val coversDir: File = File(context.filesDir, "covers").apply { mkdirs() }
}
