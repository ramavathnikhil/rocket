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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.rapido.rocket.FirebaseApp
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.repository.FirebaseAuthRepositoryFactory
import com.rapido.rocket.ui.HomePage
import com.rapido.rocket.ui.LoginPage
import com.rapido.rocket.ui.screens.RegisterScreen
import com.rapido.rocket.ui.screens.AdminUserManagementScreen
import com.rapido.rocket.ui.screens.TestUserCreationScreen
import com.rapido.rocket.ui.theme.RapidoRocketTheme
import com.rapido.rocket.viewmodel.AuthViewModel

@Composable
fun App() {
    var initializationError by remember { mutableStateOf<String?>(null) }
    var authRepository by remember { mutableStateOf<FirebaseAuthRepository?>(null) }
    
    // Initialize Firebase and auth repository
    LaunchedEffect(Unit) {
        try {
            println("App: Initializing Firebase...")
            FirebaseApp.initialize()
            println("App: Firebase initialized successfully")
            
            println("App: Creating auth repository...")
            authRepository = FirebaseAuthRepositoryFactory.create()
            println("App: Auth repository created successfully")
        } catch (e: Exception) {
            println("App: Error during initialization: $e")
            initializationError = "Failed to initialize: ${e.message}"
        }
    }
    
    RapidoRocketTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                initializationError != null -> {
                    // Show error UI
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Initialization Error",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = initializationError!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                authRepository == null -> {
                    // Show loading
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
                            Text("Initializing app...")
                        }
                    }
                }
                else -> {
                    // Show main app
                    MainAppContent(authRepository!!)
                }
            }
        }
    }
}

@Composable
private fun MainAppContent(authRepository: FirebaseAuthRepository) {
    val authViewModel = remember { AuthViewModel(authRepository) }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) } // null = unknown, true = logged in, false = not logged in
    var showRegister by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var showTestUsers by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    // Wait for Firebase to restore auth state, with timeout
    LaunchedEffect(Unit) {
        println("Waiting for Firebase auth state restoration...")
        var attempts = 0
        val maxAttempts = 50 // 5 seconds total (50 * 100ms)
        
        while (attempts < maxAttempts && isLoggedIn == null) {
            println("Attempt $attempts: Checking auth state...")
            
            // Check if we have a stored token
            val hasToken = authRepository.isUserLoggedIn()
            val token = authRepository.getAuthToken()
            println("Has token: $hasToken, token: $token")
            
            if (hasToken) {
                // Try to get current user
                try {
                    println("Attempting to get current user...")
                    val currentUser = authRepository.getCurrentUser()
                    println("Current user: $currentUser")
                    if (currentUser != null) {
                        println("Setting isLoggedIn to true")
                        isLoggedIn = true
                        break
                    }
                } catch (e: Exception) {
                    println("Error getting current user: $e")
                }
            }
            
            // Wait a bit for Firebase to restore auth state
            delay(100)
            attempts++
        }
        
        // If we still don't have a login state after waiting, assume not logged in
        if (isLoggedIn == null) {
            println("Timeout waiting for auth state, setting to false")
            isLoggedIn = false
        }
    }

    // Observe auth state changes
    LaunchedEffect(Unit) {
        authRepository.observeAuthState().collect { user ->
            println("Auth state observer triggered, user: $user")
            val newLoginState = user != null
            println("Setting isLoggedIn to: $newLoginState")
            isLoggedIn = newLoginState
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

    Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !isInitialized || isLoggedIn == null -> {
                        // Show loading while Firebase initializes or checking auth state
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
                                Text(
                                    if (!isInitialized) "Initializing Rocket..."
                                    else "Checking authentication..."
                                )
                            }
                        }
                    }
                    isLoggedIn == true && showAdminPanel -> {
                        println("Rendering AdminUserManagementScreen")
                        com.rapido.rocket.ui.screens.AdminUserManagementScreen(
                            authRepository = authRepository,
                            onBack = {
                                showAdminPanel = false
                            }
                        )
                    }
                    isLoggedIn == true && showTestUsers -> {
                        println("Rendering TestUserCreationScreen")
                        com.rapido.rocket.ui.screens.TestUserCreationScreen(
                            authRepository = authRepository,
                            onBack = {
                                showTestUsers = false
                            }
                        )
                    }
                    isLoggedIn == true -> {
                        println("Rendering HomePage")
                        HomePage(
                            authRepository = authRepository,
                            onLogout = {
                                isLoggedIn = false
                            },
                            onNavigateToAdminPanel = {
                                showAdminPanel = true
                            },
                            onNavigateToTestUsers = {
                                showTestUsers = true
                            }
                        )
                    }
                    showRegister -> {
                        println("Rendering RegisterScreen")
                        RegisterScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = {
                                showRegister = false
                            }
                        )
                    }
                    else -> {
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