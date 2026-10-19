package ru.chitalka.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Линейные иконки макета v3 (сетка 24×24, линия 1.8). В стандартном наборе Material
 * нет книжных иконок и нужной толщины линии, поэтому рисуем свои из тех же контуров, что в макете.
 * Цвет задаёт Icon(tint = ...), здесь он не важен.
 */
object ChitalkaIcons {
    val Home: ImageVector by lazy { lineIcon("Home", "M4 11l8-7 8 7v9h-5v-6H9v6H4z") }
    val Library: ImageVector by lazy { lineIcon("Library", "M5 4h4v16H5zM10 4h4v16h-4zM15.5 5.2l3.8-1 3.7 15.5-3.8 1z") }
    val Bookmark: ImageVector by lazy { lineIcon("Bookmark", "M7 4h10v16l-5-4-5 4z") }
    val Stats: ImageVector by lazy { lineIcon("Stats", "M4 20V10M10 20V4M16 20v-7M22 20H2") }
    val Search: ImageVector by lazy { lineIcon("Search", "M18 11a7 7 0 1 1-14 0a7 7 0 1 1 14 0zM20 20l-3.5-3.5") }
    val Settings: ImageVector by lazy {
        lineIcon(
            "Settings",
            "M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0z" +
                "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1",
        )
    }
    val Plus: ImageVector by lazy { lineIcon("Plus", "M12 5v14M5 12h14", width = 2f) }
    val ArrowForward: ImageVector by lazy { lineIcon("ArrowForward", "M5 12h14M13 6l6 6-6 6", width = 2f) }
    val Back: ImageVector by lazy { lineIcon("Back", "M15 5l-7 7 7 7", width = 1.9f) }
    val Close: ImageVector by lazy { lineIcon("Close", "M6 6l12 12M18 6L6 18") }

    private fun lineIcon(name: String, pathData: String, width: Float = 1.8f): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
}
