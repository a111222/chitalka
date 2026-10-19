package ru.chitalka

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Точка входа Hilt: отсюда он начинает собирать граф зависимостей. */
@HiltAndroidApp
class ChitalkaApplication : Application()
