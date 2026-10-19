package ru.chitalka.core.designsystem.cover

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Нарисованная обложка для книги без картинки: двенадцать стилей из макета v3
 * (градиенты, полосы, круги). Стиль выбирается по названию и автору, поэтому у книги
 * он всегда один и тот же.
 */
@Immutable
class CoverArt internal constructor(
    /** Цвет текста на обложке. */
    val ink: Color,
    /** Название набирается Playfair (иначе Rubik). */
    val serifTitle: Boolean,
    /** Основной цвет обложки: им красится размытый фон за каруселью. */
    val glow: Color,
    private val paint: DrawScope.() -> Unit,
) {
    fun draw(scope: DrawScope) = scope.paint()

    companion object {
        val styles: List<CoverArt> = listOf(
            // 0. Розово-фиолетовый закат с солнцем
            CoverArt(Color(0xFF2A1438), true, Color(0xFFC793DA)) {
                linear(160f, Color(0xFFF7A9C4), Color(0xFF8E7CF0))
                disc(0.70f, 0.30f, 0.18f, Color(0xFFFFD6A8))
            },
            // 1. Красный верх, графитовый низ
            CoverArt(Color(0xFFFFF1E6), true, Color(0xFFE8423F)) {
                drawRect(Color(0xFF1E1E26))
                drawRect(Color(0xFFE8423F), size = Size(size.width, size.height * 0.62f))
            },
            // 2. Бирюзовые диагональные полосы
            CoverArt(Color(0xFF0E2B27), false, Color(0xFF1FB5A0)) {
                drawRect(Color(0xFF19A190))
                stripes(Color(0xFF1FB5A0), period = 20.dp.toPx(), angle = 45f)
            },
            // 3. Синяя ночь и луна
            CoverArt(Color(0xFFF6E7C1), true, Color(0xFF2A3A7A)) {
                drawRect(Color(0xFF2A3A7A))
                disc(0.5f, 0.38f, 0.26f, Color(0xFFF6E7C1))
            },
            // 4. Золотисто-оранжевый градиент
            CoverArt(Color(0xFF3A1A0A), true, Color(0xFFEE9D44)) {
                linear(135f, Color(0xFFF2C14E), Color(0xFFE8783A))
            },
            // 5. Две зелёные половины
            CoverArt(Color(0xFFE9F5EC), false, Color(0xFF3F6B52)) {
                drawRect(Color(0xFF3F6B52))
                drawRect(Color(0xFF2E4A3A), size = Size(size.width / 2, size.height))
            },
            // 6. Фиолетовый круг в темноте
            CoverArt(Color(0xFFE4DBFF), true, Color(0xFF6A4BC4)) {
                drawRect(Color(0xFF1C1B33))
                disc(0.3f, 0.7f, 0.3f, Color(0xFF9B5CFF))
            },
            // 7. Вишни на светлом
            CoverArt(Color(0xFF5A1730), true, Color(0xFFFF8FB0)) {
                drawRect(Color(0xFFF4ECE4))
                disc(0.75f, 0.25f, 0.14f, Color(0xFFFF8FB0))
                disc(0.30f, 0.45f, 0.10f, Color(0xFFFF8FB0))
            },
            // 8. Сумеречный синий
            CoverArt(Color(0xFFD7E0FF), false, Color(0xFF3B4D8F)) {
                linear(200f, Color(0xFF3B4D8F), Color(0xFF0F1430))
            },
            // 9. Серый с красной полосой внизу
            CoverArt(Color(0xFFF3F5F6), false, Color(0xFF5B6B70)) {
                drawRect(Color(0xFFE04A4A))
                drawRect(Color(0xFF5B6B70), size = Size(size.width, size.height * 0.7f))
            },
            // 10. Красная карта на чёрном
            CoverArt(Color(0xFFF4E9DA), true, Color(0xFFE04A4A)) {
                drawRect(Color(0xFF111116))
                disc(0.5f, 0.5f, 0.22f, Color(0xFFE04A4A))
            },
            // 11. Морская волна
            CoverArt(Color(0xFFEFFFFC), true, Color(0xFF22C3B0)) {
                linear(135f, Color(0xFF22C3B0), Color(0xFF1F4E5F))
            },
        )

        /** Стабильный стиль для книги: одинаковый при каждом запуске. */
        fun forBook(title: String, author: String?): CoverArt {
            val key = (title.trim().lowercase() + "|" + author.orEmpty().trim().lowercase()).hashCode()
            return styles[abs(key % styles.size)]
        }
    }
}

/** Линейный градиент под углом как в CSS: 180° — сверху вниз, 90° — слева направо. */
private fun DrawScope.linear(cssAngle: Float, from: Color, to: Color) {
    val rad = (cssAngle - 90f) * PI.toFloat() / 180f
    val dx = cos(rad)
    val dy = sin(rad)
    // Длина градиентной линии CSS: проекция прямоугольника на направление
    val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
    val c = center
    drawRect(Brush.linearGradient(listOf(from, to), start = Offset(c.x - dx * half, c.y - dy * half), end = Offset(c.x + dx * half, c.y + dy * half)))
}

/** Круг как radial-gradient(circle at x y, color 0 r%): радиус в долях расстояния до дальнего угла. */
private fun DrawScope.disc(cx: Float, cy: Float, radius: Float, color: Color) {
    val center = Offset(size.width * cx, size.height * cy)
    val farthest = max(
        max(hypot(center.x, center.y), hypot(size.width - center.x, center.y)),
        max(hypot(center.x, size.height - center.y), hypot(size.width - center.x, size.height - center.y)),
    )
    drawCircle(color, radius = farthest * radius, center = center)
}

/** Полосы шириной в полпериода под углом [angle]. */
private fun DrawScope.stripes(color: Color, period: Float, angle: Float) {
    val diagonal = hypot(size.width, size.height)
    clipRect {
        rotate(angle) {
            var x = center.x - diagonal
            while (x < center.x + diagonal) {
                drawRect(color, topLeft = Offset(x, center.y - diagonal), size = Size(period / 2, diagonal * 2))
                x += period
            }
        }
    }
}
