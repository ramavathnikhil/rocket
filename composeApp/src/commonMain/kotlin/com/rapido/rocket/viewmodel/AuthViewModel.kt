package com.rapido.rocket.viewmodel

import com.rapido.rocket.model.User
import com.rapido.rocket.model.UserStatus
import com.rapido.rocket.repository.FirebaseAuthRepository
import com.rapido.rocket.repository.FirebaseAuthRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Initial : AuthState()
    data object Loading : AuthState()
    data class Error(val message: String) : AuthState()
    data class Success(val user: User?) : AuthState()
}

class AuthViewModel(
    private val repository: FirebaseAuthRepository = FirebaseAuthRepositoryFactory.create()
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        scope.launch {
            repository.observeAuthState().collect { user ->
                _authState.value = AuthState.Success(user)
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        scope.launch {
            _authState.value = AuthState.Loading
            repository.signUp(email, password, username)
                .onSuccess { user ->
                    _authState.value = AuthState.Success(user)
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign up failed")
                }
        }
    }

    fun signIn(email: String, password: String) {
        scope.launch {
            _authState.value = AuthState.Loading
            repository.signIn(email, password)
                .onSuccess { user ->
                    if (user.status == UserStatus.APPROVED) {
                        _authState.value = AuthState.Success(user)
                    } else {
                        _authState.value = AuthState.Error("Your account is pending approval")
                        repository.signOut()
                    }
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                }
        }
    }

    fun signOut() {
        scope.launch {
            repository.signOut()
            _authState.value = AuthState.Success(null)
        }
    }

    fun updateUserStatus(userId: String, status: String) {
        scope.launch {
            repository.updateUserStatus(userId, status)
        }
    }
} 