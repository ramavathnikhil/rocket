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
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDetailScreen(
    releaseId: String,
    authRepository: FirebaseAuthRepository,
    onBack: () -> Unit,
    onEditRelease: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<Release?>(null) }
    var workflowSteps by remember { mutableStateOf<List<WorkflowStep>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loadingSteps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isRefreshing by remember { mutableStateOf(false) }

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
    
    // Observe workflow steps for real-time updates
    LaunchedEffect(releaseId) {
        try {
            workflowRepository.observeWorkflowSteps(releaseId).collect { stepsList ->
                workflowSteps = stepsList
            }
        } catch (e: Exception) {
            println("Error observing workflow steps: ${e.message}")
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
                            onClick = { errorMessage = null }
                        ) {
                            Text(
                                text = "Dismiss",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
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
                    text = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Workflow (${workflowSteps.size})")
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.dp
                                )
                            }
                        }
                    }
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
                    loadingSteps = loadingSteps,
                    isRefreshing = isRefreshing,
                    onStepAction = { stepId, action ->
                        scope.launch {
                            try {
                                // Add step to loading set
                                loadingSteps = loadingSteps + stepId
                                isRefreshing = true
                                
                                val currentUser = authRepository.getCurrentUser()
                                val completedBy = currentUser?.email ?: ""
                                
                                // Determine the new status based on action
                                val newStatus = when (action) {
                                    "start" -> StepStatus.IN_PROGRESS
                                    "complete" -> StepStatus.COMPLETED
                                    "fail" -> StepStatus.FAILED
                                    "retry" -> StepStatus.PENDING
                                    else -> null
                                }
                                
                                // Perform optimistic update first
                                if (newStatus != null) {
                                    workflowSteps = workflowSteps.map { step ->
                                        if (step.id == stepId) {
                                            step.copy(
                                                status = newStatus,
                                                completedBy = if (newStatus == StepStatus.COMPLETED) completedBy else step.completedBy,
                                                completedAt = if (newStatus == StepStatus.COMPLETED) currentTimeMillis() else step.completedAt,
                                                updatedAt = currentTimeMillis()
                                            )
                                        } else step
                                    }
                                }
                                
                                when (action) {
                                    "start" -> {
                                        delay(300) // Small delay to show loading state
                                        val result = RepositoryProvider.getWorkflowRepository()
                                            .updateStepStatus(stepId, StepStatus.IN_PROGRESS, completedBy, null)
                                        if (result.isFailure) {
                                            // Revert optimistic update on failure
                                            val originalStep = result.getOrNull()
                                            if (originalStep != null) {
                                                workflowSteps = workflowSteps.map { step ->
                                                    if (step.id == stepId) originalStep else step
                                                }
                                            }
                                            errorMessage = "Failed to start step: ${result.exceptionOrNull()?.message}"
                                        }
                                    }
                                    "complete" -> {
                                        delay(300) // Small delay to show loading state
                                        val result = RepositoryProvider.getWorkflowRepository()
                                            .updateStepStatus(stepId, StepStatus.COMPLETED, completedBy, null)
                                        if (result.isFailure) {
                                            // Revert optimistic update on failure
                                            val originalStep = result.getOrNull()
                                            if (originalStep != null) {
                                                workflowSteps = workflowSteps.map { step ->
                                                    if (step.id == stepId) originalStep else step
                                                }
                                            }
                                            errorMessage = "Failed to complete step: ${result.exceptionOrNull()?.message}"
                                        }
                                    }
                                    "fail" -> {
                                        delay(300) // Small delay to show loading state
                                        val result = RepositoryProvider.getWorkflowRepository()
                                            .updateStepStatus(stepId, StepStatus.FAILED, completedBy, "Marked as failed by user")
                                        if (result.isFailure) {
                                            // Revert optimistic update on failure
                                            val originalStep = result.getOrNull()
                                            if (originalStep != null) {
                                                workflowSteps = workflowSteps.map { step ->
                                                    if (step.id == stepId) originalStep else step
                                                }
                                            }
                                            errorMessage = "Failed to mark step as failed: ${result.exceptionOrNull()?.message}"
                                        }
                                    }
                                    "retry" -> {
                                        delay(300) // Small delay to show loading state
                                        val result = RepositoryProvider.getWorkflowRepository()
                                            .updateStepStatus(stepId, StepStatus.PENDING, null, "Retrying step")
                                        if (result.isFailure) {
                                            // Revert optimistic update on failure
                                            val originalStep = result.getOrNull()
                                            if (originalStep != null) {
                                                workflowSteps = workflowSteps.map { step ->
                                                    if (step.id == stepId) originalStep else step
                                                }
                                            }
                                            errorMessage = "Failed to retry step: ${result.exceptionOrNull()?.message}"
                                        }
                                    }
                                    "open_external" -> {
                                        val step = workflowSteps.find { it.id == stepId }
                                        step?.actionUrl?.let { url ->
                                            // TODO: Open external URL in browser
                                            println("Opening external URL: $url")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error updating step: ${e.message}"
                            } finally {
                                // Remove step from loading set
                                loadingSteps = loadingSteps - stepId
                                isRefreshing = false
                            }
                        }
                    },
                    onRefresh = {
                        scope.launch {
                            try {
                                isRefreshing = true
                                val stepsResult = workflowRepository.getWorkflowStepsByRelease(releaseId)
                                stepsResult.fold(
                                    onSuccess = { stepsList ->
                                        workflowSteps = stepsList
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Failed to refresh workflow steps: ${error.message}"
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = "Error refreshing workflow steps: ${e.message}"
                            } finally {
                                isRefreshing = false
                            }
                        }
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
    loadingSteps: Set<String>,
    isRefreshing: Boolean,
    onStepAction: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    val sortedSteps = workflowSteps.sortedBy { it.stepNumber }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress overview
        item {
            WorkflowOverviewCard(
                steps = sortedSteps,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
        
        // Individual workflow steps
        items(sortedSteps) { step ->
            WorkflowStepCard(
                step = step,
                allSteps = sortedSteps,
                loadingSteps = loadingSteps,
                onAction = { action -> onStepAction(step.id, action) }
            )
        }
    }
}

@Composable
private fun WorkflowOverviewCard(
    steps: List<WorkflowStep>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Workflow Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Steps must be completed in order (1 → 2 → 3 → ...)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Refresh button
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.height(32.dp),
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Refresh", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
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
    allSteps: List<WorkflowStep>,
    loadingSteps: Set<String>,
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
                            StepStatus.PENDING -> {
                                if (canStartStep(step, allSteps)) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) // Ready to start
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant // Waiting
                                }
                            }
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
                                color = when (step.status) {
                                    StepStatus.PENDING -> {
                                        if (canStartStep(step, allSteps)) {
                                            Color.White // Ready to start
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant // Waiting
                                        }
                                    }
                                    else -> Color.White
                                },
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
                
                val isStepLoading = step.id in loadingSteps
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (step.status) {
                        StepStatus.PENDING -> {
                            if (canStartStep(step, allSteps)) {
                                Button(
                                    onClick = { onAction("start") },
                                    modifier = Modifier.height(32.dp),
                                    enabled = !isStepLoading
                                ) {
                                    if (isStepLoading) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Text("Starting...", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        Text("Start", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else {
                                // Show why the step can't be started
                                val previousIncompleteSteps = allSteps.filter { 
                                    it.stepNumber < step.stepNumber && it.status != StepStatus.COMPLETED 
                                }
                                val message = if (previousIncompleteSteps.isNotEmpty()) {
                                    "Complete Step ${previousIncompleteSteps.minBy { it.stepNumber }.stepNumber} first"
                                } else {
                                    "Waiting for dependencies"
                                }
                                
                                OutlinedButton(
                                    onClick = { }, // No action for disabled button
                                    modifier = Modifier.height(32.dp),
                                    enabled = false
                                ) {
                                    Text(message, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        StepStatus.IN_PROGRESS -> {
                            Button(
                                onClick = { onAction("complete") },
                                modifier = Modifier.height(32.dp),
                                enabled = !isStepLoading
                            ) {
                                if (isStepLoading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text("Completing...", style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    Text("Complete", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            OutlinedButton(
                                onClick = { onAction("fail") },
                                modifier = Modifier.height(32.dp),
                                enabled = !isStepLoading
                            ) {
                                if (isStepLoading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text("Updating...", style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    Text("Mark Failed", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        StepStatus.FAILED -> {
                            Button(
                                onClick = { onAction("retry") },
                                modifier = Modifier.height(32.dp),
                                enabled = !isStepLoading
                            ) {
                                if (isStepLoading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text("Retrying...", style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    Text("Retry", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        else -> {}
                    }
                    
                    if (step.actionUrl.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onAction("open_external") },
                            modifier = Modifier.height(32.dp),
                            enabled = !isStepLoading
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

// Helper function to check if step dependencies are met
private fun areDepencenciesMet(step: WorkflowStep, allSteps: List<WorkflowStep> = emptyList()): Boolean {
    if (step.dependsOn.isEmpty()) return true
    
    return step.dependsOn.all { dependencyId ->
        allSteps.find { it.id == dependencyId }?.status == StepStatus.COMPLETED
    }
}

// Helper function to check if this is the next step that can be started
private fun canStartStep(step: WorkflowStep, allSteps: List<WorkflowStep>): Boolean {
    // Step must be pending to be startable
    if (step.status != StepStatus.PENDING) return false
    
    // Check if all previous steps (by step number) are completed
    val previousSteps = allSteps.filter { it.stepNumber < step.stepNumber }
    val allPreviousCompleted = previousSteps.all { it.status == StepStatus.COMPLETED }
    
    // Also check explicit dependencies if they exist
    val dependenciesMet = areDepencenciesMet(step, allSteps)
    
    return allPreviousCompleted && dependenciesMet
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