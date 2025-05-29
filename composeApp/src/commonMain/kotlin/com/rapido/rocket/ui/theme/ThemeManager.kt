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
    
    var isDarkTheme by mutableStateOf(true)
        private set

    init {
        // Load saved theme preference
        val savedTheme = localStorage.getItem(THEME_KEY)
        // If user has explicitly saved a preference, use it
        // Otherwise, default to dark theme
        isDarkTheme = when (savedTheme) {
            "false" -> false  // User explicitly chose light theme
            "true" -> true    // User explicitly chose dark theme  
            null -> true      // No preference saved, default to dark
            else -> true      // Fallback to dark theme
        }
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