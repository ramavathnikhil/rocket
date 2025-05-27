package com.rapido.rocket.repository

import com.rapido.rocket.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

actual class FirebaseAuthRepository {
    private val _authStateFlow = MutableStateFlow<User?>(null)

    actual suspend fun signUp(email: String, password: String, username: String): Result<User> {
        TODO("Not yet implemented")
    }

    actual suspend fun signIn(email: String, password: String): Result<User> {
        TODO("Not yet implemented")
    }

    actual suspend fun signOut() {
        TODO("Not yet implemented")
    }

    actual suspend fun getCurrentUser(): User? {
        TODO("Not yet implemented")
    }

    actual fun observeAuthState(): Flow<User?> = _authStateFlow

    actual suspend fun updateUserStatus(userId: String, status: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun deleteUser(userId: String): Result<Unit> {
        TODO("Not yet implemented")
    }
} 