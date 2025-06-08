package com.rapido.rocket.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rapido.rocket.model.GitHubConfig
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.repository.RepositoryProvider
import com.rapido.rocket.util.currentTimeMillis
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubConfigScreen(
    projectId: String,
    authRepository: FirebaseAuthRepository,
    onBack: () -> Unit,
    onConfigSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var githubConfig by remember { mutableStateOf<GitHubConfig?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Form fields
    var appRepositoryUrl by remember { mutableStateOf("") }
    var bffRepositoryUrl by remember { mutableStateOf("") }
    var githubToken by remember { mutableStateOf("") }
    var defaultBaseBranch by remember { mutableStateOf("develop") }
    var defaultTargetBranch by remember { mutableStateOf("release") }
    var showToken by remember { mutableStateOf(false) }
    
    // Validation states
    var isValidatingToken by remember { mutableStateOf(false) }
    var isValidatingAppRepo by remember { mutableStateOf(false) }
    var isValidatingBffRepo by remember { mutableStateOf(false) }
    
    val githubRepository = remember { RepositoryProvider.getGitHubRepository() }

    // Load existing config
    LaunchedEffect(projectId) {
        try {
            val result = githubRepository.getGitHubConfig(projectId)
            result.fold(
                onSuccess = { config ->
                    githubConfig = config
                    config?.let {
                        appRepositoryUrl = it.appRepositoryUrl
                        bffRepositoryUrl = it.bffRepositoryUrl
                        githubToken = it.githubToken
                        defaultBaseBranch = it.defaultBaseBranch
                        defaultTargetBranch = it.defaultTargetBranch
                    }
                },
                onFailure = { error ->
                    errorMessage = "Failed to load GitHub config: ${error.message}"
                }
            )
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading GitHub config: ${e.message}"
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
                Text("← Back")
            }
            
            Text(
                text = "GitHub Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Error/Success Messages
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
                    TextButton(onClick = { errorMessage = null }) {
                        Text(
                            text = "Dismiss",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        successMessage?.let { success ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        text = success,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { successMessage = null }) {
                        Text(
                            text = "Dismiss",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Form
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Repository Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // App Repository URL
                OutlinedTextField(
                    value = appRepositoryUrl,
                    onValueChange = { appRepositoryUrl = it },
                    label = { Text("App Repository") },
                    placeholder = { Text("e.g., owner/app-repository") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (isValidatingAppRepo) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    },
                    supportingText = {
                        Text("GitHub repository for the mobile app (owner/repository)")
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // BFF Repository URL
                OutlinedTextField(
                    value = bffRepositoryUrl,
                    onValueChange = { bffRepositoryUrl = it },
                    label = { Text("BFF Repository") },
                    placeholder = { Text("e.g., owner/bff-repository") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (isValidatingBffRepo) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    },
                    supportingText = {
                        Text("GitHub repository for the Backend for Frontend service")
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // GitHub Token
                OutlinedTextField(
                    value = githubToken,
                    onValueChange = { githubToken = it },
                    label = { Text("GitHub Personal Access Token") },
                    placeholder = { Text("ghp_...") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isValidatingToken) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            }
                            TextButton(
                                onClick = { showToken = !showToken }
                            ) {
                                Text(if (showToken) "Hide" else "Show")
                            }
                        }
                    },
                    supportingText = {
                        Text("Token needs 'repo' permissions for creating PRs")
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Branch Configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = defaultBaseBranch,
                        onValueChange = { defaultBaseBranch = it },
                        label = { Text("Default Base Branch") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = defaultTargetBranch,
                        onValueChange = { defaultTargetBranch = it },
                        label = { Text("Default Target Branch") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Validation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isValidatingToken = true
                                val result = githubRepository.validateToken(githubToken)
                                result.fold(
                                    onSuccess = { isValid ->
                                        if (isValid) {
                                            successMessage = "Token is valid"
                                        } else {
                                            errorMessage = "Invalid token"
                                        }
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Token validation failed: ${error.message}"
                                    }
                                )
                                isValidatingToken = false
                            }
                        },
                        enabled = githubToken.isNotEmpty() && !isValidatingToken,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Validate Token")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                if (appRepositoryUrl.isNotEmpty()) {
                                    isValidatingAppRepo = true
                                    val result = githubRepository.validateRepository(appRepositoryUrl, githubToken)
                                    result.fold(
                                        onSuccess = { isValid ->
                                            if (isValid) {
                                                successMessage = "App repository is accessible"
                                            } else {
                                                errorMessage = "Cannot access app repository"
                                            }
                                        },
                                        onFailure = { error ->
                                            errorMessage = "App repository validation failed: ${error.message}"
                                        }
                                    )
                                    isValidatingAppRepo = false
                                }
                                
                                if (bffRepositoryUrl.isNotEmpty()) {
                                    isValidatingBffRepo = true
                                    val result = githubRepository.validateRepository(bffRepositoryUrl, githubToken)
                                    result.fold(
                                        onSuccess = { isValid ->
                                            if (isValid) {
                                                successMessage = "BFF repository is accessible"
                                            } else {
                                                errorMessage = "Cannot access BFF repository"
                                            }
                                        },
                                        onFailure = { error ->
                                            errorMessage = "BFF repository validation failed: ${error.message}"
                                        }
                                    )
                                    isValidatingBffRepo = false
                                }
                            }
                        },
                        enabled = (appRepositoryUrl.isNotEmpty() || bffRepositoryUrl.isNotEmpty()) && 
                                githubToken.isNotEmpty() && !isValidatingAppRepo && !isValidatingBffRepo,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Validate Repos")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Save Button
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isSaving = true
                                val currentUser = authRepository.getCurrentUser()
                                
                                val configToSave = githubConfig?.copy(
                                    appRepositoryUrl = appRepositoryUrl,
                                    bffRepositoryUrl = bffRepositoryUrl,
                                    githubToken = githubToken,
                                    defaultBaseBranch = defaultBaseBranch,
                                    defaultTargetBranch = defaultTargetBranch,
                                    updatedAt = currentTimeMillis()
                                ) ?: GitHubConfig(
                                    projectId = projectId,
                                    appRepositoryUrl = appRepositoryUrl,
                                    bffRepositoryUrl = bffRepositoryUrl,
                                    githubToken = githubToken,
                                    defaultBaseBranch = defaultBaseBranch,
                                    defaultTargetBranch = defaultTargetBranch,
                                    createdAt = currentTimeMillis(),
                                    updatedAt = currentTimeMillis()
                                )
                                
                                val result = githubRepository.saveGitHubConfig(configToSave)
                                result.fold(
                                    onSuccess = { savedConfig ->
                                        githubConfig = savedConfig
                                        successMessage = "GitHub configuration saved successfully"
                                        onConfigSaved()
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Failed to save configuration: ${error.message}"
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = "Error saving configuration: ${e.message}"
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = appRepositoryUrl.isNotEmpty() && githubToken.isNotEmpty() && !isSaving
                ) {
                    if (isSaving) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Saving...")
                        }
                    } else {
                        Text("Save Configuration")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "GitHub Integration Info",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• Personal Access Token needs 'repo' permissions\n" +
                          "• Repository URLs should be in 'owner/repository' format\n" +
                          "• Base branch is typically 'develop' or 'main'\n" +
                          "• Target branch is typically 'release' or 'staging'\n" +
                          "• This enables automatic PR creation in workflow steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 