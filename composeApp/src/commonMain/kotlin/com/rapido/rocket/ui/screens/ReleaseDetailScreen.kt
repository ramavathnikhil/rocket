package com.rapido.rocket.ui.screens

import androidx.compose.foundation.background
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
fun ReleaseDetailScreen(
    releaseId: String,
    authRepository: FirebaseAuthRepository,
    onBack: () -> Unit,
    onEditRelease: () -> Unit
) {
    var release by remember { mutableStateOf<Release?>(null) }
    var workflowSteps by remember { mutableStateOf<List<WorkflowStep>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val releaseRepository = remember { RepositoryProvider.getReleaseRepository() }
    val workflowRepository = remember { RepositoryProvider.getWorkflowRepository() }

    // Load data from repositories
    LaunchedEffect(releaseId) {
        try {
            // Load release
            val releaseResult = releaseRepository.getRelease(releaseId)
            releaseResult.fold(
                onSuccess = { loadedRelease ->
                    release = loadedRelease
                },
                onFailure = { error ->
                    errorMessage = "Failed to load release: ${error.message}"
                }
            )

            // Load workflow steps for this release
            val stepsResult = workflowRepository.getWorkflowStepsByRelease(releaseId)
            stepsResult.fold(
                onSuccess = { stepsList ->
                    workflowSteps = stepsList
                },
                onFailure = { error ->
                    errorMessage = "Failed to load workflow steps: ${error.message}"
                }
            )
            
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading release data: ${e.message}"
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

    release?.let { rel ->
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
                            text = "${rel.version} - ${rel.title}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Button(onClick = onEditRelease) {
                            Text("Edit")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReleaseStatusBadge(status = rel.status)
                        
                        rel.targetReleaseDate?.let { date ->
                            Text(
                                text = "Target: ${formatDate(date)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Workflow (${workflowSteps.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Release Info") }
                )
            }

            // Content
            when (selectedTab) {
                0 -> WorkflowContent(
                    workflowSteps = workflowSteps,
                    onStepAction = { stepId, action ->
                        // TODO: Handle step actions
                        println("Step $stepId action: $action")
                    }
                )
                1 -> ReleaseInfoContent(release = rel)
            }
        }
    }
}

@Composable
private fun WorkflowContent(
    workflowSteps: List<WorkflowStep>,
    onStepAction: (String, String) -> Unit
) {
    val sortedSteps = workflowSteps.sortedBy { it.stepNumber }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress overview
        item {
            WorkflowOverviewCard(steps = sortedSteps)
        }
        
        // Individual workflow steps
        items(sortedSteps) { step ->
            WorkflowStepCard(
                step = step,
                onAction = { action -> onStepAction(step.id, action) }
            )
        }
    }
}

@Composable
private fun WorkflowOverviewCard(steps: List<WorkflowStep>) {
    val totalSteps = steps.size
    val completedSteps = steps.count { it.status == StepStatus.COMPLETED }
    val inProgressSteps = steps.count { it.status == StepStatus.IN_PROGRESS }
    val failedSteps = steps.count { it.status == StepStatus.FAILED }
    val progress = if (totalSteps > 0) completedSteps.toFloat() / totalSteps else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Workflow Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$completedSteps/$totalSteps completed",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusChip(
                    label = "Completed",
                    count = completedSteps,
                    color = Color.Green
                )
                StatusChip(
                    label = "In Progress",
                    count = inProgressSteps,
                    color = MaterialTheme.colorScheme.primary
                )
                if (failedSteps > 0) {
                    StatusChip(
                        label = "Failed",
                        count = failedSteps,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun WorkflowStepCard(
    step: WorkflowStep,
    onAction: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Step header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step number
                    Surface(
                        color = when (step.status) {
                            StepStatus.COMPLETED -> Color.Green
                            StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                            StepStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (step.status == StepStatus.COMPLETED) "✓" else step.stepNumber.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getStepTypeDisplayName(step.type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status badge
                WorkflowStepStatusBadge(status = step.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Step details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Assigned to",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = step.assignedTo.ifEmpty { "Unassigned" },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (step.estimatedDuration > 0) {
                    Column {
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (step.actualDuration > 0) 
                                "${step.actualDuration}/${step.estimatedDuration} min"
                            else 
                                "${step.estimatedDuration} min est.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Notes
            if (step.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${step.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action buttons
            if (step.status != StepStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (step.status) {
                        StepStatus.PENDING -> {
                            if (step.dependsOn.isEmpty() || areDepencenciesMet(step)) {
                                Button(
                                    onClick = { onAction("start") },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Start", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        StepStatus.IN_PROGRESS -> {
                            Button(
                                onClick = { onAction("complete") },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Complete", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onAction("fail") },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Mark Failed", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        StepStatus.FAILED -> {
                            Button(
                                onClick = { onAction("retry") },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Retry", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        else -> {}
                    }
                    
                    if (step.actionUrl.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onAction("open_external") },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Open Link", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowStepStatusBadge(status: StepStatus) {
    val (color, text) = when (status) {
        StepStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant to "Pending"
        StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer to "In Progress"
        StepStatus.COMPLETED -> Color.Green.copy(alpha = 0.2f) to "Completed"
        StepStatus.FAILED -> MaterialTheme.colorScheme.errorContainer to "Failed"
        StepStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceVariant to "Skipped"
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
private fun ReleaseInfoContent(release: Release) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Release details
        InfoSection(title = "Release Details") {
            InfoRow(label = "Version", value = release.version)
            InfoRow(label = "Title", value = release.title)
            InfoRow(label = "Description", value = release.description)
            InfoRow(label = "Status", value = release.status.name.replace("_", " "))
            InfoRow(label = "Created by", value = release.createdBy)
            InfoRow(label = "Assigned to", value = release.assignedTo)
            release.targetReleaseDate?.let {
                InfoRow(label = "Target Release Date", value = formatDate(it))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Build Links
        InfoSection(title = "Build Links") {
            if (release.stagingBuildUrl.isNotEmpty()) {
                InfoRow(label = "Staging Build", value = release.stagingBuildUrl, isLink = true)
            }
            if (release.productionBuildUrl.isNotEmpty()) {
                InfoRow(label = "Production Build", value = release.productionBuildUrl, isLink = true)
            }
            if (release.githubReleaseUrl.isNotEmpty()) {
                InfoRow(label = "GitHub Release", value = release.githubReleaseUrl, isLink = true)
            }
            if (release.playStoreUrl.isNotEmpty()) {
                InfoRow(label = "Play Store", value = release.playStoreUrl, isLink = true)
            }
            
            if (release.stagingBuildUrl.isEmpty() && 
                release.productionBuildUrl.isEmpty() && 
                release.githubReleaseUrl.isEmpty() && 
                release.playStoreUrl.isEmpty()) {
                Text(
                    text = "No build links available yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Release Notes
        InfoSection(title = "Release Notes") {
            if (release.notes.isNotEmpty()) {
                Text(
                    text = release.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "No release notes added yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper functions
private fun getStepTypeDisplayName(type: StepType): String {
    return when (type) {
        StepType.PR_MERGE -> "Pull Request"
        StepType.BUILD_STAGING -> "Build & Deploy"
        StepType.STAGING_SIGNOFF -> "Approval"
        StepType.PR_TO_MASTER -> "Pull Request"
        StepType.BUILD_PRODUCTION -> "Build & Deploy"
        StepType.QA_SIGNOFF -> "Quality Assurance"
        StepType.DEPLOY_BETA -> "Deployment"
        StepType.CREATE_GITHUB_RELEASE -> "Release Management"
        StepType.PROMOTE_PRODUCTION -> "Deployment"
        StepType.MANUAL_TASK -> "Manual Task"
        StepType.GITHUB_ACTION -> "Automation"
    }
}

private fun areDepencenciesMet(step: WorkflowStep): Boolean {
    // TODO: Implement dependency checking logic
    return true
}

// Reuse components from ProjectDetailScreen
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