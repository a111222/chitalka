package ru.chitalka.core.designsystem.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme

/**
 * Общие элементы между экранами, как в видео: обложка из карусели перелетает в карточку книги.
 * Приложение кладёт сюда SharedTransitionLayout вокруг навигации, а каждый экран — свою анимацию появления.
 * Без них (превью, тесты) модификаторы ничего не делают.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope?> = staticCompositionLocalOf { null }

val LocalNavAnimatedScope: ProvidableCompositionLocal<AnimatedVisibilityScope?> = staticCompositionLocalOf { null }

@OptIn(ExperimentalSharedTransitionApi::class)
private val BookFlight = BoundsTransform { _, _ -> tween(MotionTokens.BOOK_FLIGHT_MS, easing = MotionTokens.Emphasized) }

/** Книга [bookId] — один и тот же предмет на разных экранах: при переходе она перелетает, а не исчезает. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBook(bookId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalNavAnimatedScope.current ?: return this
    if (!ReaderTheme.animationsEnabled) return this
    return with(shared) {
        this@sharedBook.sharedElement(
            rememberSharedContentState("book-$bookId"),
            visibility,
            boundsTransform = BookFlight,
        )
    }
}

/**
 * Своя анимация блока при входе на экран и уходе с него поверх общего проявления экранов:
 * в видео соседние обложки разъезжаются в стороны, а разделы главной уезжают вниз.
 */
@Composable
fun Modifier.navEnterExit(enter: EnterTransition, exit: ExitTransition): Modifier {
    val visibility = LocalNavAnimatedScope.current ?: return this
    if (!ReaderTheme.animationsEnabled) return this
    return with(visibility) { this@navEnterExit.animateEnterExit(enter = enter, exit = exit) }
}

/** Каждый экран навигации отдаёт свою анимацию появления вниз по дереву: её используют [sharedBook] и [navEnterExit]. */
@Composable
fun AnimatedVisibilityScope.ProvideNavAnimatedScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedScope provides this, content = content)
}
