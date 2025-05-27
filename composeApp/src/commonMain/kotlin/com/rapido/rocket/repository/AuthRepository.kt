package com.rapido.rocket.repository

import com.rapido.rocket.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signUp(email: String, password: String, username: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut()
    suspend fun getCurrentUser(): User?
    fun observeAuthState(): Flow<User?>
    suspend fun updateUserStatus(userId: String, status: String): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
} 