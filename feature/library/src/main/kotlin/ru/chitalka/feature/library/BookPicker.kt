package ru.chitalka.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Системное окно выбора файлов (Storage Access Framework): разрешение на память не нужно.
 * Фильтр «любой файл»: у FB2 нет общепринятого MIME-типа, формат всё равно определяется по содержимому.
 */
@Composable
internal fun rememberBookPicker(onPicked: (List<String>) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        onPicked(uris.map { it.toString() })
    }
    return { launcher.launch(arrayOf("*/*")) }
}
