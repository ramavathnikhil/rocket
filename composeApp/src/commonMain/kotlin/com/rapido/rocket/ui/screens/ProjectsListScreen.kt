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
import com.rapido.rocket.repository.FirebaseAuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsListScreen(
    authRepository: FirebaseAuthRepository,
    onBack: () -> Unit,
    onProjectClick: (String) -> Unit,
    onCreateProject: () -> Unit
) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // TODO: Load projects from repository
    LaunchedEffect(Unit) {
        // Simulate loading data
        kotlinx.coroutines.delay(1000)
        
        // Mock data for now
        projects = listOf(
            Project(
                id = "1",
                name = "Rapido Customer App",
                description = "Main customer facing application for ride booking",
                repositoryUrl = "https://github.com/company/rapido-customer",
                playStoreUrl = "https://play.google.com/store/apps/details?id=com.rapido.customer",
                createdBy = "john@company.com",
                isActive = true
            ),
            Project(
                id = "2",
                name = "Rapido Driver App",
                description = "Driver application for ride management and earnings",
                repositoryUrl = "https://github.com/company/rapido-driver",
                playStoreUrl = "https://play.google.com/store/apps/details?id=com.rapido.driver",
                createdBy = "jane@company.com",
                isActive = true
            ),
            Project(
                id = "3",
                name = "Rapido Admin Portal",
                description = "Administrative web portal for operations management",
                repositoryUrl = "https://github.com/company/rapido-admin",
                createdBy = "admin@company.com",
                isActive = true
            ),
            Project(
                id = "4",
                name = "Legacy Customer App",
                description = "Previous version of customer app (deprecated)",
                repositoryUrl = "https://github.com/company/rapido-customer-legacy",
                createdBy = "legacy@company.com",
                isActive = false
            )
        )
        
        isLoading = false
    }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) {
            projects
        } else {
            projects.filter { project ->
                project.name.contains(searchQuery, ignoreCase = true) ||
                project.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }
            
            Text(
                text = "Projects",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(onClick = onCreateProject) {
                Text("Create")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search projects...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProjects.size) { index ->
                    val project = filteredProjects[index]
                    ProjectCard(
                        project = project,
                        onClick = { onProjectClick(project.id) }
                    )
                }
                
                if (filteredProjects.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No projects found matching \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Created by: ${project.createdBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status indicator
                Surface(
                    color = if (project.isActive) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (project.isActive) "Active" else "Inactive",
                        color = if (project.isActive) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            if (project.repositoryUrl.isNotEmpty() || project.playStoreUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (project.repositoryUrl.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { /* TODO: Open repository URL */ },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Repository",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    if (project.playStoreUrl.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { /* TODO: Open Play Store URL */ },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Play Store",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
} 