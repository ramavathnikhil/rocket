package com.rapido.rocket.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rapido.rocket.model.Project
import com.rapido.rocket.model.Release
import com.rapido.rocket.model.ReleaseStatus
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.repository.RepositoryProvider
import com.rapido.rocket.ui.theme.ThemeManager

@Composable
fun DashboardScreen(
    authRepository: FirebaseAuthRepository,
    themeManager: ThemeManager,
    onNavigateToProjects: () -> Unit,
    onNavigateToProject: (String) -> Unit,
    onNavigateToRelease: (String) -> Unit,
    onCreateProject: () -> Unit,
    onLogout: () -> Unit
) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var recentReleases by remember { mutableStateOf<List<Release>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf<com.rapido.rocket.model.User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val projectRepository = remember { RepositoryProvider.getProjectRepository() }
    val releaseRepository = remember { RepositoryProvider.getReleaseRepository() }

    // Observe current user
    LaunchedEffect(Unit) {
        authRepository.observeAuthState().collect { user ->
            currentUser = user
        }
    }

    // Load projects and releases from repositories
    LaunchedEffect(Unit) {
        try {
            // Load projects
            val projectsResult = projectRepository.getAllProjects()
            projectsResult.fold(
                onSuccess = { projectsList ->
                    projects = projectsList.take(5) // Show recent 5 projects
                },
                onFailure = { error ->
                    errorMessage = "Failed to load projects: ${error.message}"
                }
            )

            // Observe releases for real-time updates
            releaseRepository.observeReleases().collect { allReleases ->
                recentReleases = allReleases
                    .filter { it.status == ReleaseStatus.IN_PROGRESS || it.status == ReleaseStatus.STAGING }
                    .sortedByDescending { it.updatedAt }
                    .take(5)
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = "Error loading dashboard data: ${e.message}"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Error message display
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { 
                            errorMessage = null
                            isLoading = true
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

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

        // Logout button in top-left corner
        IconButton(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Logout",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 48.dp) // Account for top buttons
        ) {
            // Header
            Text(
                text = "Release Management Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            currentUser?.let { user ->
                Text(
                    text = "Welcome back, ${user.getDisplayName()}!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Actions
                    item {
                        QuickActionsCard(
                            onCreateProject = onCreateProject,
                            onViewAllProjects = onNavigateToProjects
                        )
                    }

                    // Projects Overview
                    item {
                        ProjectsOverviewCard(
                            projects = projects.take(3),
                            onViewProject = onNavigateToProject,
                            onViewAll = onNavigateToProjects
                        )
                    }

                    // Recent Releases
                    item {
                        RecentReleasesCard(
                            releases = recentReleases,
                            projects = projects,
                            onViewRelease = onNavigateToRelease
                        )
                    }

                    // Release Statistics
                    item {
                        ReleaseStatsCard(releases = recentReleases)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onCreateProject: () -> Unit,
    onViewAllProjects: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCreateProject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create Project")
                }

                OutlinedButton(
                    onClick = onViewAllProjects,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Projects")
                }
            }
        }
    }
}

@Composable
private fun ProjectsOverviewCard(
    projects: List<Project>,
    onViewProject: (String) -> Unit,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Projects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onViewAll) {
                    Text("View All")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            projects.forEach { project ->
                ProjectItem(
                    project = project,
                    onClick = { onViewProject(project.id) }
                )
                if (project != projects.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ProjectItem(
    project: Project,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onClick) {
                Text("View")
            }
        }
    }
}

@Composable
private fun RecentReleasesCard(
    releases: List<Release>,
    projects: List<Project>,
    onViewRelease: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Releases",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            releases.forEach { release ->
                val project = projects.find { it.id == release.projectId }
                ReleaseItem(
                    release = release,
                    projectName = project?.name ?: "Unknown Project",
                    onClick = { onViewRelease(release.id) }
                )
                if (release != releases.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ReleaseItem(
    release: Release,
    projectName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${release.title} (${release.version})",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = projectName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            ReleaseStatusChip(status = release.status)
        }

        TextButton(onClick = onClick) {
            Text("View")
        }
    }
}

@Composable
private fun ReleaseStatusChip(status: ReleaseStatus) {
    val (backgroundColor, contentColor, text) = when (status) {
        ReleaseStatus.DRAFT -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Draft"
        )
        ReleaseStatus.IN_PROGRESS -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "In Progress"
        )
        ReleaseStatus.STAGING -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Staging"
        )
        ReleaseStatus.PRODUCTION_PENDING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Production Pending"
        )
        ReleaseStatus.PRODUCTION -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Production"
        )
        ReleaseStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.primary,
            "Completed"
        )
        ReleaseStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Cancelled"
        )
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ReleaseStatsCard(releases: List<Release>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Release Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total",
                    value = releases.size.toString()
                )
                StatItem(
                    label = "In Progress",
                    value = releases.count { it.status == ReleaseStatus.IN_PROGRESS }.toString()
                )
                StatItem(
                    label = "Completed",
                    value = releases.count { it.status == ReleaseStatus.COMPLETED }.toString()
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 