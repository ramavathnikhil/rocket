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
import com.rapido.rocket.model.WorkflowStepType
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
    var workflowIds by remember { mutableStateOf(mutableMapOf<String, String>()) }
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
                        workflowIds = it.workflowIds.toMutableMap()
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
                
                // Workflow Configuration Section
                Text(
                    text = "GitHub Actions Workflow Configuration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Configure workflow IDs for build automation steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Workflow ID fields for each supported step type
                GitHubConfig.getSupportedWorkflowStepTypes().forEach { stepType ->
                    OutlinedTextField(
                        value = workflowIds[stepType.key] ?: "",
                        onValueChange = { newValue ->
                            workflowIds = workflowIds.toMutableMap().apply {
                                if (newValue.isEmpty()) {
                                    remove(stepType.key)
                                } else {
                                    put(stepType.key, newValue)
                                }
                            }
                        },
                        label = { Text(stepType.displayName) },
                        placeholder = { Text("e.g., 123456789") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(stepType.description)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
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
                
                // Test Firebase Functions Button
                Button(
                    onClick = {
                        println("[DEBUG] Test Firebase Functions button clicked")
                        scope.launch {
                            try {
                                isValidatingToken = true
                                errorMessage = null
                                successMessage = null
                                
                                println("[DEBUG] Starting token validation with Firebase Functions SDK...")
                                val result = githubRepository.validateToken(githubToken)
                                
                                println("[DEBUG] Validation result received")
                                result.fold(
                                    onSuccess = { isValid ->
                                        println("[DEBUG] Validation successful: $isValid")
                                        if (isValid) {
                                            successMessage = "✅ Token is valid! Firebase Functions working correctly."
                                        } else {
                                            errorMessage = "❌ Token is invalid"
                                        }
                                    },
                                    onFailure = { error ->
                                        println("[DEBUG] Validation failed: ${error.message}")
                                        errorMessage = "❌ Token validation failed: ${error.message}"
                                    }
                                )
                            } catch (e: Exception) {
                                println("[DEBUG] Exception during validation: ${e.message}")
                                errorMessage = "❌ Exception: ${e.message}"
                            } finally {
                                isValidatingToken = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = githubToken.isNotEmpty() && !isValidatingToken
                ) {
                    if (isValidatingToken) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Testing Firebase Functions...")
                        }
                    } else {
                        Text("🔥 Test Firebase Functions")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Repository Validation Button
                Button(
                    onClick = {
                        println("[DEBUG] Repository validation button clicked")
                        scope.launch {
                            try {
                                if (appRepositoryUrl.isNotEmpty()) {
                                    isValidatingAppRepo = true
                                    errorMessage = null
                                    successMessage = null
                                    
                                    println("[DEBUG] Validating app repository: $appRepositoryUrl")
                                    val result = githubRepository.validateRepository(appRepositoryUrl, githubToken)
                                    
                                    result.fold(
                                        onSuccess = { isValid ->
                                            println("[DEBUG] App repo validation successful: $isValid")
                                            if (isValid) {
                                                successMessage = "✅ App repository is accessible"
                                            } else {
                                                errorMessage = "❌ Cannot access app repository"
                                            }
                                        },
                                        onFailure = { error ->
                                            println("[DEBUG] App repo validation failed: ${error.message}")
                                            errorMessage = "❌ App repository validation failed: ${error.message}"
                                        }
                                    )
                                }
                                
                                if (bffRepositoryUrl.isNotEmpty()) {
                                    isValidatingBffRepo = true
                                    
                                    println("[DEBUG] Validating BFF repository: $bffRepositoryUrl")
                                    val result = githubRepository.validateRepository(bffRepositoryUrl, githubToken)
                                    
                                    result.fold(
                                        onSuccess = { isValid ->
                                            println("[DEBUG] BFF repo validation successful: $isValid")
                                            if (isValid) {
                                                if (successMessage.isNullOrEmpty()) {
                                                    successMessage = "✅ BFF repository is accessible"
                                                } else {
                                                    successMessage += " • BFF repository is accessible"
                                                }
                                            } else {
                                                errorMessage = "❌ Cannot access BFF repository"
                                            }
                                        },
                                        onFailure = { error ->
                                            println("[DEBUG] BFF repo validation failed: ${error.message}")
                                            errorMessage = "❌ BFF repository validation failed: ${error.message}"
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                println("[DEBUG] Exception during repository validation: ${e.message}")
                                errorMessage = "❌ Repository validation exception: ${e.message}"
                            } finally {
                                isValidatingAppRepo = false
                                isValidatingBffRepo = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (appRepositoryUrl.isNotEmpty() || bffRepositoryUrl.isNotEmpty()) && 
                            githubToken.isNotEmpty() && !isValidatingAppRepo && !isValidatingBffRepo
                ) {
                    if (isValidatingAppRepo || isValidatingBffRepo) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Validating Repositories...")
                        }
                    } else {
                        Text("📂 Validate Repositories")
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
                                
                                println("🔍 Preparing to save GitHub config:")
                                println("   - Form appRepositoryUrl: '$appRepositoryUrl'")
                                println("   - Form bffRepositoryUrl: '$bffRepositoryUrl'")
                                println("   - Form githubToken length: ${githubToken.length}")
                                println("   - Form workflowIds: $workflowIds")
                                println("   - Form defaultBaseBranch: '$defaultBaseBranch'")
                                println("   - Form defaultTargetBranch: '$defaultTargetBranch'")
                                
                                val configToSave = githubConfig?.copy(
                                    appRepositoryUrl = appRepositoryUrl,
                                    bffRepositoryUrl = bffRepositoryUrl,
                                    githubToken = githubToken,
                                    workflowIds = workflowIds.toMap(),
                                    defaultBaseBranch = defaultBaseBranch,
                                    defaultTargetBranch = defaultTargetBranch,
                                    updatedAt = currentTimeMillis()
                                ) ?: GitHubConfig(
                                    projectId = projectId,
                                    appRepositoryUrl = appRepositoryUrl,
                                    bffRepositoryUrl = bffRepositoryUrl,
                                    githubToken = githubToken,
                                    workflowIds = workflowIds.toMap(),
                                    defaultBaseBranch = defaultBaseBranch,
                                    defaultTargetBranch = defaultTargetBranch,
                                    createdAt = currentTimeMillis(),
                                    updatedAt = currentTimeMillis()
                                )
                                
                                println("🔍 Final config to save:")
                                println("   - projectId: '${configToSave.projectId}'")
                                println("   - appRepositoryUrl: '${configToSave.appRepositoryUrl}'")
                                println("   - bffRepositoryUrl: '${configToSave.bffRepositoryUrl}'")
                                println("   - githubToken length: ${configToSave.githubToken.length}")
                                println("   - workflowIds: ${configToSave.workflowIds}")
                                
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
                    text = "• Personal Access Token needs 'repo' and 'actions' permissions\n" +
                          "• Repository URLs should be in 'owner/repository' format\n" +
                          "• Base branch is typically 'develop' or 'main'\n" +
                          "• Target branch is typically 'release' or 'staging'\n" +
                          "• Workflow IDs are numeric IDs from GitHub Actions URLs\n" +
                          "• Find workflow ID in: github.com/owner/repo/actions/workflows/ID\n" +
                          "• Configure different workflows for different build steps\n" +
                          "• This enables automatic PR creation and build triggers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 