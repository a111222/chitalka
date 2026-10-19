package ru.chitalka

import android.os.Bundle
import androidx.activity.ComponentActivity
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val current = settings ?: ReaderSettings()
            ReaderTheme(mode = current.theme, hapticsEnabled = current.hapticsEnabled) {
                // Ширина окна считается один раз здесь и передаётся вниз всем экранам
                BoxWithConstraints {
                    CompositionLocalProvider(LocalWindowWidth provides WindowWidth.fromWidth(maxWidth)) {
                        ChitalkaApp()
                    }
                }
            }
        }
    }
}
