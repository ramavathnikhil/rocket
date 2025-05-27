package com.rapido.rocket

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rapido.rocket.repository.FirebaseAuthRepositoryFactory
import com.rapido.rocket.ui.HomePage
import com.rapido.rocket.ui.LoginPage

@Composable
fun App() {
    FirebaseApp.initialize()
    
    val authRepository = remember { FirebaseAuthRepositoryFactory.create() }
    var isLoggedIn by remember { mutableStateOf(authRepository.isUserLoggedIn()) }

    // Observe auth state changes
    LaunchedEffect(Unit) {
        authRepository.observeAuthState().collect { user ->
            isLoggedIn = user != null
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoggedIn) {
                    HomePage(
                        authRepository = authRepository,
                        onLogout = {
                            isLoggedIn = false
                        }
                    )
                } else {
                    LoginPage(
                        authRepository = authRepository,
                        onLoginSuccess = {
                            isLoggedIn = true
                        }
                    )
                }
            }
        }
    }
}