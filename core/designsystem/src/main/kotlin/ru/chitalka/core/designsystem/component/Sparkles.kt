package ru.chitalka.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Вспышка искр, как в видео при нажатии на книгу и на «Продолжить чтение»: кольцо мелких
 * звёздочек разлетается от центра и гаснет. [progress] от 0 до 1 задаёт сам вызывающий
 * (обычно Animatable), 0 — искр нет. [radius] — доля меньшей стороны, до которой долетают искры.
 */
fun Modifier.sparkleBurst(progress: () -> Float, color: Color = Color.White, accent: Color = Color(0xFFFF8FB6), radius: Float = 0.5f): Modifier =
    drawWithContent {
        drawContent()
        val p = progress()
        if (p <= 0f || p >= 1f) return@drawWithContent
        drawSparkles(p, color, accent, radius)
    }

private const val SPARKS = 28

private fun DrawScope.drawSparkles(p: Float, color: Color, accent: Color, radius: Float) {
    val c = center
    val maxR = size.minDimension * radius
    // Быстрый вылет и мягкое торможение, гаснут во второй половине
    val travel = 1f - (1f - p) * (1f - p) * (1f - p)
    val fade = if (p < 0.45f) 1f else 1f - (p - 0.45f) / 0.55f
    for (i in 0 until SPARKS) {
        // Неравномерные углы и дальность: кольцо выглядит живым, а не циркульным
        val jitter = ((i * 7919) % 97) / 97f
        val angle = (i / SPARKS.toFloat()) * 2f * PI.toFloat() + jitter * 0.35f
        val reach = maxR * (0.55f + 0.45f * jitter) * travel + maxR * 0.35f
        val at = Offset(c.x + cos(angle) * reach, c.y + sin(angle) * reach)
        val tint = if (i % 3 == 0) accent else color
        val s = (1.2f + 2.4f * (1f - jitter)) * density * (1f - 0.5f * p)
        if (i % 2 == 0) {
            // Звёздочка-крестик
            drawLine(tint.copy(alpha = fade), Offset(at.x - s * 1.8f, at.y), Offset(at.x + s * 1.8f, at.y), strokeWidth = s * 0.55f, cap = StrokeCap.Round)
            drawLine(tint.copy(alpha = fade), Offset(at.x, at.y - s * 1.8f), Offset(at.x, at.y + s * 1.8f), strokeWidth = s * 0.55f, cap = StrokeCap.Round)
        } else {
            drawCircle(tint.copy(alpha = fade * 0.9f), radius = s * 0.6f, center = at)
        }
    }
    // Мягкое свечение в центре в самом начале
    if (p < 0.35f) drawCircle(color.copy(alpha = 0.35f * (1f - p / 0.35f)), radius = maxR * 0.5f * (0.4f + p), center = c)
}
