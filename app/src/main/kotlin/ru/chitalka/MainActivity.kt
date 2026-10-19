package ru.chitalka

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.chitalka.core.designsystem.adaptive.LocalWindowWidth
import ru.chitalka.core.designsystem.adaptive.WindowWidth
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.model.ReaderSettings

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Приложение всегда тёмное: светлые значки в строке состояния при любой теме телефона
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // При повороте экрана intent тот же — второй раз книгу не импортируем
        if (savedInstanceState == null) handleViewIntent(intent)
        // Заставка — только при обычном запуске. Книга, открытая из другого приложения, открывается сразу
        val showSplash = savedInstanceState == null && intent?.action != Intent.ACTION_VIEW
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val current = settings ?: ReaderSettings()
            ReaderTheme(mode = current.theme, hapticsEnabled = current.hapticsEnabled) {
                // Ширина окна считается один раз здесь и передаётся вниз всем экранам
                BoxWithConstraints {
                    CompositionLocalProvider(LocalWindowWidth provides WindowWidth.fromWidth(maxWidth)) {
                        ChitalkaApp(showSplash = showSplash, openBookRequests = viewModel.openBookRequests)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        val uri = intent?.data
        if (intent?.action == Intent.ACTION_VIEW && uri != null) viewModel.openExternalFile(uri.toString())
    }
}
