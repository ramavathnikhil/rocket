package com.rapido.rocket.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.ui.theme.ThemeManager
import com.rapido.rocket.ui.screens.DashboardScreen
import kotlinx.coroutines.launch

@Composable
fun HomePage(
    authRepository: FirebaseAuthRepository,
    themeManager: ThemeManager,
    onLogout: () -> Unit,
    onNavigateToAdminPanel: () -> Unit = {},
    onNavigateToTestUsers: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    onNavigateToCreateProject: () -> Unit = {},
    onNavigateToProject: (String) -> Unit = {},
    onNavigateToRelease: (String) -> Unit = {}
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

    // Show Release Management Dashboard
    DashboardScreen(
        authRepository = authRepository,
        themeManager = themeManager,
        onNavigateToProjects = onNavigateToProjects,
        onNavigateToProject = onNavigateToProject,
        onNavigateToRelease = onNavigateToRelease,
        onCreateProject = onNavigateToCreateProject,
        onLogout = {
            isLoading = true
            scope.launch {
                authRepository.signOut()
                isLoading = false
                onLogout()
            }
        }
    )
} 