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
import com.rapido.rocket.model.Project
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.repository.RepositoryProvider
import com.rapido.rocket.util.currentTimeMillis
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    authRepository: FirebaseAuthRepository,
    onBack: () -> Unit,
    onProjectCreated: (Project) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var repositoryUrl by remember { mutableStateOf("") }
    var playStoreUrl by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<com.rapido.rocket.model.User?>(null) }
    
    val scope = rememberCoroutineScope()
    val projectRepository = remember { RepositoryProvider.getProjectRepository() }
    val isFormValid = projectName.isNotBlank() && description.isNotBlank()

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
                text = "Create Project",
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
                    text = "Creating project as: ${user.email} (${user.username})",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Form fields
        OutlinedTextField(
            value = projectName,
            onValueChange = { 
                projectName = it
                errorMessage = null
            },
            label = { Text("Project Name *") },
            modifier = Modifier.fillMaxWidth(),
            isError = projectName.isBlank(),
            supportingText = if (projectName.isBlank()) {
                { Text("Project name is required") }
            } else null,
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
            minLines = 3,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = repositoryUrl,
            onValueChange = { repositoryUrl = it },
            label = { Text("Repository URL (optional)") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://github.com/company/project") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = playStoreUrl,
            onValueChange = { playStoreUrl = it },
            label = { Text("Play Store URL (optional)") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://play.google.com/store/apps/details?id=...") },
            singleLine = true
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
                        
                        val newProject = Project(
                            id = generateProjectId(),
                            name = projectName.trim(),
                            description = description.trim(),
                            repositoryUrl = repositoryUrl.trim(),
                            playStoreUrl = playStoreUrl.trim(),
                            createdBy = currentUser?.email ?: "",
                            createdAt = currentTimeMillis(),
                            updatedAt = currentTimeMillis(),
                            isActive = true
                        )
                        
                        // Create project using repository
                        scope.launch {
                            try {
                                val result = projectRepository.createProject(newProject)
                                result.fold(
                                    onSuccess = { createdProject ->
                                        isCreating = false
                                        onProjectCreated(createdProject)
                                    },
                                    onFailure = { error ->
                                        isCreating = false
                                        errorMessage = "Failed to create project: ${error.message}"
                                    }
                                )
                            } catch (e: Exception) {
                                isCreating = false
                                errorMessage = "Error creating project: ${e.message}"
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
                    Text("Create Project")
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

// Helper functions - these would typically be in utility classes
private fun generateProjectId(): String {
    return "proj_${currentTimeMillis()}_${(100..999).random()}"
} 