package com.rapido.rocket.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rapido.rocket.model.*
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.repository.RepositoryProvider
import com.rapido.rocket.util.currentTimeMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    authRepository: FirebaseAuthRepository,
    onBack: () -> Unit,
    onCreateRelease: () -> Unit,
    onReleaseClick: (String) -> Unit,
    onGitHubConfig: () -> Unit = {}
) {
    var project by remember { mutableStateOf<Project?>(null) }
    var releases by remember { mutableStateOf<List<Release>>(emptyList()) }
    var workflowSteps by remember { mutableStateOf<Map<String, List<WorkflowStep>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val projectRepository = remember { RepositoryProvider.getProjectRepository() }
    val releaseRepository = remember { RepositoryProvider.getReleaseRepository() }
    val workflowRepository = remember { RepositoryProvider.getWorkflowRepository() }

    // Load data from repositories
    LaunchedEffect(projectId) {
        try {
            // Load project
            val projectResult = projectRepository.getProject(projectId)
            projectResult.fold(
                onSuccess = { loadedProject ->
                    project = loadedProject
                },
                onFailure = { error ->
                    errorMessage = "Failed to load project: ${error.message}"
                }
            )

            // Load releases for this project
            val releasesResult = releaseRepository.getReleasesByProject(projectId)
            releasesResult.fold(
                onSuccess = { releasesList ->
                    releases = releasesList
                    
                    // Load workflow steps for each release
                    val stepsMap = mutableMapOf<String, List<WorkflowStep>>()
                    releasesList.forEach { release ->
                        val stepsResult = workflowRepository.getWorkflowStepsByRelease(release.id)
                        stepsResult.fold(
                            onSuccess = { steps ->
                                stepsMap[release.id] = steps
                            },
                            onFailure = { /* Ignore individual step loading failures */ }
                        )
                    }
                    workflowSteps = stepsMap
                },
                onFailure = { error ->
                    errorMessage = "Failed to load releases: ${error.message}"
                }
            )
            
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading project data: ${e.message}"
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    project?.let { proj ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onBack) {
                            Text("← Back")
                        }
                        
                        Text(
                            text = proj.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Button(onClick = onCreateRelease) {
                            Text("New Release")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = proj.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Releases (${releases.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Project Info") }
                )
            }

            // Content
            when (selectedTab) {
                0 -> ReleasesContent(
                    releases = releases,
                    workflowSteps = workflowSteps,
                    onReleaseClick = onReleaseClick
                )
                1 -> ProjectInfoContent(project = proj, onGitHubConfig = onGitHubConfig)
            }
        }
    }
}

@Composable
private fun ReleasesContent(
    releases: List<Release>,
    workflowSteps: Map<String, List<WorkflowStep>>,
    onReleaseClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(releases) { release ->
            ReleaseCard(
                release = release,
                workflowSteps = workflowSteps[release.id] ?: emptyList(),
                onClick = { onReleaseClick(release.id) }
            )
        }
        
        if (releases.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No releases found. Create your first release!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseCard(
    release: Release,
    workflowSteps: List<WorkflowStep>,
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
            // Release header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${release.version} - ${release.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = release.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status badge
                ReleaseStatusBadge(status = release.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Release info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Assigned to: ${release.assignedTo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                release.targetReleaseDate?.let { date ->
                    Text(
                        text = "Target: ${formatDate(date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (workflowSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Workflow progress
                Text(
                    text = "Workflow Progress",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                WorkflowProgressBar(steps = workflowSteps)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Current step info
                val currentStep = workflowSteps.find { it.status == StepStatus.IN_PROGRESS }
                    ?: workflowSteps.firstOrNull { it.status == StepStatus.PENDING }
                
                currentStep?.let { step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (step.status) {
                                StepStatus.IN_PROGRESS -> "▶"
                                StepStatus.PENDING -> "⏱"
                                else -> "✓"
                            },
                            color = when (step.status) {
                                StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                                StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> Color.Green
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "Current: ${step.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseStatusBadge(status: ReleaseStatus) {
    val (color, text) = when (status) {
        ReleaseStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant to "Draft"
        ReleaseStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer to "In Progress"
        ReleaseStatus.STAGING -> MaterialTheme.colorScheme.secondaryContainer to "Staging"
        ReleaseStatus.PRODUCTION_PENDING -> MaterialTheme.colorScheme.tertiaryContainer to "Prod Pending"
        ReleaseStatus.PRODUCTION -> Color.Green.copy(alpha = 0.2f) to "Production"
        ReleaseStatus.COMPLETED -> Color.Green.copy(alpha = 0.3f) to "Completed"
        ReleaseStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to "Cancelled"
    }
    
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun WorkflowProgressBar(steps: List<WorkflowStep>) {
    val sortedSteps = steps.sortedBy { it.stepNumber }
    val totalSteps = sortedSteps.size
    val completedSteps = sortedSteps.count { it.status == StepStatus.COMPLETED }
    val progress = if (totalSteps > 0) completedSteps.toFloat() / totalSteps else 0f
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$completedSteps/$totalSteps completed",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun ProjectInfoContent(project: Project, onGitHubConfig: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Project details
        InfoSection(title = "Project Details") {
            InfoRow(label = "Name", value = project.name)
            InfoRow(label = "Description", value = project.description)
            InfoRow(label = "Created by", value = project.createdBy)
            InfoRow(label = "Status", value = if (project.isActive) "Active" else "Inactive")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // GitHub Integration
        InfoSection(title = "GitHub Integration") {
            Text(
                text = "Configure GitHub repositories for automatic PR creation during releases",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Button(
                onClick = onGitHubConfig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Configure GitHub")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Links
        InfoSection(title = "Links") {
            if (project.repositoryUrl.isNotEmpty()) {
                InfoRow(label = "Repository", value = project.repositoryUrl, isLink = true)
            }
            if (project.playStoreUrl.isNotEmpty()) {
                InfoRow(label = "Play Store", value = project.playStoreUrl, isLink = true)
            }
            if (project.repositoryUrl.isEmpty() && project.playStoreUrl.isEmpty()) {
                Text(
                    text = "No external links configured",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isLink: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
    }
}

// Helper function to format dates
private fun formatDate(timestamp: Long): String {
    // TODO: Implement proper date formatting
    return "In 7 days"
} 