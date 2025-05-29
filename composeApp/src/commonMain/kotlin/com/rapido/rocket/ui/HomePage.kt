package com.rapido.rocket.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rapido.rocket.repository.FirebaseAuthRepository
import kotlinx.coroutines.launch

@Composable
fun HomePage(
    authRepository: FirebaseAuthRepository,
    onLogout: () -> Unit,
    onNavigateToAdminPanel: () -> Unit = {},
    onNavigateToTestUsers: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<com.rapido.rocket.model.User?>(null) }
    val scope = rememberCoroutineScope()

    // Observe current user
    LaunchedEffect(Unit) {
        authRepository.observeAuthState().collect { user ->
            currentUser = user
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Rapido Rocket!",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // User information card
            currentUser?.let { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "User Profile",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text("Name: ${user.getDisplayName()}")
                        Text("Email: ${user.email}")
                        Text("Role: ${user.role.name}")
                        Text("Status: ${user.status.name}")
                        
                        // Show role-specific information
                        if (user.isAdmin()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🔧 Admin Access",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (!user.isApproved()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⏳ Account pending approval",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                
                // Admin features
                if (user.canManageUsers()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Admin Panel",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Button(
                                onClick = onNavigateToAdminPanel,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Manage Users")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedButton(
                                onClick = onNavigateToTestUsers,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Create Test Users")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        authRepository.signOut()
                        isLoading = false
                        onLogout()
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Logout")
                }
            }
        }
    }
} 