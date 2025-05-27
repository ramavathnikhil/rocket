package com.rapido.rocket

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.rapido.rocket.FirebaseApp
import com.rapido.rocket.repository.FirebaseAuthRepositoryFactory
import com.rapido.rocket.ui.HomePage
import com.rapido.rocket.ui.LoginPage
import com.rapido.rocket.ui.screens.RegisterScreen
import com.rapido.rocket.viewmodel.AuthViewModel

@Composable
fun App() {
    FirebaseApp.initialize()
    
    val authRepository = remember { FirebaseAuthRepositoryFactory.create() }
    val authViewModel = remember { AuthViewModel(authRepository) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    // Observe auth state changes
    LaunchedEffect(Unit) {
        authRepository.observeAuthState().collect { user ->
            println("Auth state changed, user: $user")
            isLoggedIn = user != null
        }
    }

    // Check Firebase initialization
    LaunchedEffect(Unit) {
        // For now, assume Firebase is initialized after a short delay
        // This will be handled by platform-specific implementations
        kotlinx.coroutines.delay(1000)
        isInitialized = true
    }

    // Debug current state
    LaunchedEffect(isLoggedIn) {
        println("Current login state: $isLoggedIn")
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isInitialized) {
                    // Show loading while Firebase initializes
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Initializing Firebase...")
                        }
                    }
                } else if (isLoggedIn) {
                    println("Rendering HomePage")
                    HomePage(
                        authRepository = authRepository,
                        onLogout = {
                            isLoggedIn = false
                        }
                    )
                } else if (showRegister) {
                    println("Rendering RegisterScreen")
                    RegisterScreen(
                        viewModel = authViewModel,
                        onNavigateToLogin = {
                            showRegister = false
                        }
                    )
                } else {
                    println("Rendering LoginPage")
                    LoginPage(
                        authRepository = authRepository,
                        onLoginSuccess = {
                            println("Login success callback triggered")
                            isLoggedIn = true
                        },
                        onNavigateToRegister = {
                            showRegister = true
                        }
                    )
                }
            }
        }
    }
}