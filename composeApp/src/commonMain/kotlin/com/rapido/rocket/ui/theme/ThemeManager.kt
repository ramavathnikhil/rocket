package com.rapido.rocket.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

expect class LocalStorageManager {
    fun setItem(key: String, value: String)
    fun getItem(key: String): String?
}

class ThemeManager(private val localStorage: LocalStorageManager) {
    private val THEME_KEY = "app_theme_dark"
    
    var isDarkTheme by mutableStateOf(false)
        private set

    init {
        // Load saved theme preference
        val savedTheme = localStorage.getItem(THEME_KEY)
        isDarkTheme = savedTheme == "true"
    }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        // Save preference to localStorage
        localStorage.setItem(THEME_KEY, isDarkTheme.toString())
    }

    fun setDarkTheme(dark: Boolean) {
        isDarkTheme = dark
        // Save preference to localStorage
        localStorage.setItem(THEME_KEY, isDarkTheme.toString())
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    return remember { 
        val localStorage = createLocalStorageManager()
        ThemeManager(localStorage) 
    }
}

expect fun createLocalStorageManager(): LocalStorageManager 