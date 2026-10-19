package ru.chitalka.core.designsystem.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Хаптика с названными моментами. Экран говорит «перелистнули страницу»,
 * а какая именно вибрация — решается здесь, в одном месте.
 */
interface Haptics {
    fun pageTurn()

    fun bookPicked()

    fun shelfSnap()

    fun confirm()

    fun toggle(on: Boolean)
}

/** Хаптика выключена в настройках. */
object NoHaptics : Haptics {
    override fun pageTurn() = Unit
    override fun bookPicked() = Unit
    override fun shelfSnap() = Unit
    override fun confirm() = Unit
    override fun toggle(on: Boolean) = Unit
}

/** Системная хаптика через View: не нужно разрешение VIBRATE, уважает настройки телефона. */
private class ViewHaptics(private val view: View) : Haptics {
    override fun pageTurn() = perform(HapticFeedbackConstants.CLOCK_TICK)

    override fun bookPicked() = perform(HapticFeedbackConstants.LONG_PRESS)

    override fun shelfSnap() = perform(HapticFeedbackConstants.CLOCK_TICK)

    override fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY,
    )

    override fun toggle(on: Boolean) = perform(
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.VIRTUAL_KEY
            on -> HapticFeedbackConstants.TOGGLE_ON
            else -> HapticFeedbackConstants.TOGGLE_OFF
        },
    )

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { NoHaptics }

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val view = LocalView.current
    return remember(view, enabled) { if (enabled) ViewHaptics(view) else NoHaptics }
}
