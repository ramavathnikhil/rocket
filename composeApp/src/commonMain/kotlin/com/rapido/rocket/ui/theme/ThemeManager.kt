package com.rapido.rocket.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class ThemeManager {
    var isDarkTheme by mutableStateOf(false)
        private set

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }

    fun setDarkTheme(dark: Boolean) {
        isDarkTheme = dark
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    return remember { ThemeManager() }
} 