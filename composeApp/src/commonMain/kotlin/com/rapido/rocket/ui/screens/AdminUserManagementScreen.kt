package com.rapido.rocket.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rapido.rocket.model.User
import com.rapido.rocket.model.UserRole
import com.rapido.rocket.model.UserStatus
import com.rapido.rocket.repository.FirebaseAuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    authRepository: FirebaseAuthRepository,
    onBack: () -> Unit
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var showApproveDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Load users on screen load
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                error = null
                val result = authRepository.getAllUsers()
                result.onSuccess { userList ->
                    users = userList
                }.onFailure { exception ->
                    error = exception.message ?: "Failed to load users"
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load users"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) {
                Text("← Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "User Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading users...")
                    }
                }
            }
            error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        isLoading = true
                                        error = null
                                        val result = authRepository.getAllUsers()
                                        result.onSuccess { userList ->
                                            users = userList
                                        }.onFailure { exception ->
                                            error = exception.message ?: "Failed to load users"
                                        }
                                    } catch (e: Exception) {
                                        error = e.message ?: "Failed to load users"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            users.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👥",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No users found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Users will appear here once they register",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                // User list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { user ->
                        UserCard(
                            user = user,
                            onApprove = {
                                selectedUser = user
                                showApproveDialog = true
                            },
                            onReject = {
                                selectedUser = user
                                showRejectDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Approve confirmation dialog
    if (showApproveDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Approve User") },
            text = {
                Text("Are you sure you want to approve ${selectedUser!!.email}? This will grant them access to the application.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val result = authRepository.updateUserStatus(
                                    selectedUser!!.id,
                                    UserStatus.APPROVED.name
                                )
                                result.onSuccess {
                                    // Refresh user list
                                    val refreshResult = authRepository.getAllUsers()
                                    refreshResult.onSuccess { userList ->
                                        users = userList
                                    }
                                }.onFailure { exception ->
                                    error = exception.message ?: "Failed to approve user"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Failed to approve user"
                            }
                        }
                        showApproveDialog = false
                        selectedUser = null
                    }
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showApproveDialog = false
                        selectedUser = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject dialog with reason
    if (showRejectDialog && selectedUser != null) {
        Dialog(
            onDismissRequest = {
                showRejectDialog = false
                rejectionReason = ""
                selectedUser = null
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Reject User",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Are you sure you want to reject ${selectedUser!!.email}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason *") },
                        placeholder = { Text("Please provide a reason for rejection...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        isError = rejectionReason.isBlank()
                    )
                    
                    if (rejectionReason.isBlank()) {
                        Text(
                            text = "Rejection reason is required",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                showRejectDialog = false
                                rejectionReason = ""
                                selectedUser = null
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (rejectionReason.isNotBlank()) {
                                    scope.launch {
                                        try {
                                            val result = authRepository.updateUserStatus(
                                                selectedUser!!.id,
                                                UserStatus.REJECTED.name
                                            )
                                            result.onSuccess {
                                                // Refresh user list
                                                val refreshResult = authRepository.getAllUsers()
                                                refreshResult.onSuccess { userList ->
                                                    users = userList
                                                }
                                            }.onFailure { exception ->
                                                error = exception.message ?: "Failed to reject user"
                                            }
                                        } catch (e: Exception) {
                                            error = e.message ?: "Failed to reject user"
                                        }
                                    }
                                    showRejectDialog = false
                                    rejectionReason = ""
                                    selectedUser = null
                                }
                            },
                            enabled = rejectionReason.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Reject User")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (user.username.isNotBlank()) {
                        Text(
                            text = "Username: ${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(status = user.status)
                        Spacer(modifier = Modifier.width(8.dp))
                        RoleChip(role = user.role)
                    }
                }
                
                // Action buttons (only show for pending users)
                if (user.status == UserStatus.PENDING_APPROVAL) {
                    Column {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.width(80.dp)
                        ) {
                            Text("✓", color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.width(80.dp)
                        ) {
                            Text("✗", color = Color.White)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "ID: ${user.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun StatusChip(status: UserStatus) {
    val (backgroundColor, contentColor, text) = when (status) {
        UserStatus.PENDING_APPROVAL -> Triple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            "Pending"
        )
        UserStatus.APPROVED -> Triple(
            Color(0xFFE8F5E8),
            Color(0xFF2E7D32),
            "Approved"
        )
        UserStatus.REJECTED -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            "Rejected"
        )
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
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
private fun RoleChip(role: UserRole) {
    val (backgroundColor, contentColor, text) = when (role) {
        UserRole.ADMIN -> Triple(
            Color(0xFFE3F2FD),
            Color(0xFF1565C0),
            "Admin"
        )
        UserRole.USER -> Triple(
            Color(0xFFF3E5F5),
            Color(0xFF7B1FA2),
            "User"
        )
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
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