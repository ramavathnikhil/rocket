package com.rapido.rocket.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.ui.theme.ThemeManager
import kotlinx.coroutines.launch

@Composable
fun LoginPage(
    authRepository: FirebaseAuthRepository,
    themeManager: ThemeManager,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Check if Firebase is initialized
    LaunchedEffect(Unit) {
        // Firebase initialization is handled by platform-specific code
        isLoading = false
    }

    // Check auth state
    LaunchedEffect(Unit) {
        authRepository.observeAuthState().collect { user ->
            if (user != null) {
                onLoginSuccess()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Theme toggle button in top-right corner
        IconButton(
            onClick = { themeManager.toggleTheme() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = if (themeManager.isDarkTheme) "Light" else "Dark",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Rapido Rocket",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        error = "Please fill in all fields"
                        return@Button
                    }
                    
                    error = null
                    isLoading = true
                    println("Starting login process for email: $email")
                    scope.launch {
                        try {
                            println("Calling authRepository.signIn...")
                            val result = authRepository.signIn(email, password)
                            println("SignIn result received: $result")
                            result.onSuccess { user ->
                                println("Login successful, user: $user")
                                if (user != null) {
                                    println("User is not null, calling onLoginSuccess")
                                    onLoginSuccess()
                                } else {
                                    println("User is null after successful login")
                                    error = "Login failed: User is null"
                                }
                                isLoading = false
                            }.onFailure { exception ->
                                println("Login failed with exception: $exception")
                                error = exception.message ?: "Login failed"
                                isLoading = false
                            }
                        } catch (e: Exception) {
                            println("Login error caught: $e")
                            error = e.message ?: "Login failed"
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToRegister,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Don't have an account? Sign up")
            }
        }

        // Firebase initialization is handled by the main App composable
    }
} 