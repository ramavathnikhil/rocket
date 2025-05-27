package com.rapido.rocket.viewmodel

import com.rapido.rocket.model.User
import com.rapido.rocket.model.UserRole
import com.rapido.rocket.model.UserStatus
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.repository.FirebaseAuthRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UserManagementState {
    data object Initial : UserManagementState()
    data object Loading : UserManagementState()
    data class Success(val message: String) : UserManagementState()
    data class Error(val message: String) : UserManagementState()
    data class UsersLoaded(val users: List<User>) : UserManagementState()
}

class UserManagementViewModel(
    private val repository: FirebaseAuthRepository = FirebaseAuthRepositoryFactory.create()
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow<UserManagementState>(UserManagementState.Initial)
    val state: StateFlow<UserManagementState> = _state.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        scope.launch {
            repository.observeAuthState().collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun loadAllUsers() {
        scope.launch {
            _state.value = UserManagementState.Loading
            repository.getAllUsers()
                .onSuccess { users ->
                    _state.value = UserManagementState.UsersLoaded(users)
                }
                .onFailure { exception ->
                    _state.value = UserManagementState.Error(exception.message ?: "Failed to load users")
                }
        }
    }

    fun updateUserStatus(userId: String, status: UserStatus) {
        scope.launch {
            _state.value = UserManagementState.Loading
            repository.updateUserStatus(userId, status.name)
                .onSuccess {
                    _state.value = UserManagementState.Success("User status updated successfully")
                    // Reload users to reflect changes
                    loadAllUsers()
                }
                .onFailure { exception ->
                    _state.value = UserManagementState.Error(exception.message ?: "Failed to update user status")
                }
        }
    }

    fun updateUserRole(userId: String, role: UserRole) {
        scope.launch {
            _state.value = UserManagementState.Loading
            repository.updateUserRole(userId, role.name)
                .onSuccess {
                    _state.value = UserManagementState.Success("User role updated successfully")
                    // Reload users to reflect changes
                    loadAllUsers()
                }
                .onFailure { exception ->
                    _state.value = UserManagementState.Error(exception.message ?: "Failed to update user role")
                }
        }
    }

    fun updateUserProfile(userId: String, username: String, email: String) {
        scope.launch {
            _state.value = UserManagementState.Loading
            repository.updateUserProfile(userId, username, email)
                .onSuccess {
                    _state.value = UserManagementState.Success("User profile updated successfully")
                    // Reload users to reflect changes
                    loadAllUsers()
                }
                .onFailure { exception ->
                    _state.value = UserManagementState.Error(exception.message ?: "Failed to update user profile")
                }
        }
    }

    fun deleteUser(userId: String) {
        scope.launch {
            _state.value = UserManagementState.Loading
            repository.deleteUser(userId)
                .onSuccess {
                    _state.value = UserManagementState.Success("User deleted successfully")
                    // Reload users to reflect changes
                    loadAllUsers()
                }
                .onFailure { exception ->
                    _state.value = UserManagementState.Error(exception.message ?: "Failed to delete user")
                }
        }
    }

    fun approveUser(userId: String) {
        updateUserStatus(userId, UserStatus.APPROVED)
    }

    fun rejectUser(userId: String) {
        updateUserStatus(userId, UserStatus.REJECTED)
    }

    fun makeAdmin(userId: String) {
        updateUserRole(userId, UserRole.ADMIN)
    }

    fun makeUser(userId: String) {
        updateUserRole(userId, UserRole.USER)
    }

    fun clearState() {
        _state.value = UserManagementState.Initial
    }
} 