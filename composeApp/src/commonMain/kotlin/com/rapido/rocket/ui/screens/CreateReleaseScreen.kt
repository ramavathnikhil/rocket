package com.rapido.rocket.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rapido.rocket.model.Release
import com.rapido.rocket.model.ReleaseStatus
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.repository.RepositoryProvider
import com.rapido.rocket.util.currentTimeMillis
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReleaseScreen(
    projectId: String,
    authRepository: FirebaseAuthRepository,
    onBack: () -> Unit,
    onReleaseCreated: (Release) -> Unit
) {
    var version by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf("") }
    var targetReleaseDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<com.rapido.rocket.model.User?>(null) }
    
    val scope = rememberCoroutineScope()
    val releaseRepository = remember { RepositoryProvider.getReleaseRepository() }
    val workflowRepository = remember { RepositoryProvider.getWorkflowRepository() }
    val isFormValid = version.isNotBlank() && title.isNotBlank() && description.isNotBlank()

    // Get current user
    LaunchedEffect(Unit) {
        try {
            currentUser = authRepository.getCurrentUser()
        } catch (e: Exception) {
            errorMessage = "Failed to get current user: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Cancel")
            }
            
            Text(
                text = "Create Release",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error message display
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Text("Dismiss")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Current user info (for debugging)
        currentUser?.let { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Creating release as: ${user.email} (${user.username})",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Form fields
        OutlinedTextField(
            value = version,
            onValueChange = { 
                version = it
                errorMessage = null
            },
            label = { Text("Version *") },
            modifier = Modifier.fillMaxWidth(),
            isError = version.isBlank(),
            supportingText = if (version.isBlank()) {
                { Text("Version is required (e.g., v1.2.0)") }
            } else null,
            placeholder = { Text("v1.2.0") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { 
                title = it
                errorMessage = null
            },
            label = { Text("Release Title *") },
            modifier = Modifier.fillMaxWidth(),
            isError = title.isBlank(),
            supportingText = if (title.isBlank()) {
                { Text("Release title is required") }
            } else null,
            placeholder = { Text("Q4 Feature Release") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { 
                description = it
                errorMessage = null
            },
            label = { Text("Description *") },
            modifier = Modifier.fillMaxWidth(),
            isError = description.isBlank(),
            supportingText = if (description.isBlank()) {
                { Text("Description is required") }
            } else null,
            placeholder = { Text("Bug fixes, new features, and improvements") },
            minLines = 3,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = assignedTo,
            onValueChange = { assignedTo = it },
            label = { Text("Assigned To (optional)") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("developer@company.com") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = targetReleaseDate,
            onValueChange = { targetReleaseDate = it },
            label = { Text("Target Release Date (optional)") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("YYYY-MM-DD") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Release Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Additional notes about this release...") },
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                enabled = !isCreating
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (isFormValid) {
                        isCreating = true
                        errorMessage = null
                        
                        val newRelease = Release(
                            id = generateReleaseId(),
                            projectId = projectId,
                            version = version.trim(),
                            title = title.trim(),
                            description = description.trim(),
                            status = ReleaseStatus.DRAFT,
                            createdBy = currentUser?.email ?: "",
                            assignedTo = assignedTo.trim().ifEmpty { currentUser?.email ?: "" },
                            createdAt = currentTimeMillis(),
                            updatedAt = currentTimeMillis(),
                            targetReleaseDate = parseTargetDate(targetReleaseDate),
                            notes = notes.trim()
                        )
                        
                        // Create release using repository
                        scope.launch {
                            try {
                                val result = releaseRepository.createRelease(newRelease)
                                result.fold(
                                    onSuccess = { createdRelease ->
                                        // Create default workflow steps for the release
                                        workflowRepository.createDefaultWorkflowSteps(createdRelease.id).fold(
                                            onSuccess = { steps ->
                                                println("Created ${steps.size} default workflow steps")
                                            },
                                            onFailure = { error ->
                                                println("Warning: Failed to create default workflow steps: ${error.message}")
                                            }
                                        )
                                        
                                        isCreating = false
                                        onReleaseCreated(createdRelease)
                                    },
                                    onFailure = { error ->
                                        isCreating = false
                                        errorMessage = "Failed to create release: ${error.message}"
                                    }
                                )
                            } catch (e: Exception) {
                                isCreating = false
                                errorMessage = "Error creating release: ${e.message}"
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = isFormValid && !isCreating
            ) {
                if (isCreating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Creating...")
                    }
                } else {
                    Text("Create Release")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Helper text
        Text(
            text = "* Required fields",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper functions
private fun generateReleaseId(): String {
    return "rel_${currentTimeMillis()}_${(100..999).random()}"
}

private fun parseTargetDate(dateString: String): Long? {
    if (dateString.isBlank()) return null
    
    // Simple date parsing - in a real app you'd use a proper date library
    return try {
        // For now, just add 7 days from now if they entered anything
        currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
    } catch (e: Exception) {
        null
    }
} 