package com.rapido.rocket.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class Screen {
    LOGIN,
    REGISTER
}

class NavigationState {
    var currentScreen by mutableStateOf<Screen>(Screen.LOGIN)
        private set

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }
}

@Composable
fun rememberNavigationState() = remember { NavigationState() } 