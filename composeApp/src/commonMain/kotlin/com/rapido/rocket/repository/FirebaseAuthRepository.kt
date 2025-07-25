package com.rapido.rocket.repository

import com.rapido.rocket.model.User
import kotlinx.coroutines.flow.Flow

interface FirebaseAuthRepository {
    suspend fun signUp(email: String, password: String, username: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut()
    suspend fun getCurrentUser(): User?
    fun observeAuthState(): Flow<User?>
    suspend fun updateUserStatus(userId: String, status: String): Result<Unit>
    suspend fun updateUserRole(userId: String, role: String): Result<Unit>
    suspend fun updateUserProfile(userId: String, username: String, email: String): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun getAllUsers(): Result<List<User>>
    suspend fun getUserById(userId: String): Result<User?>
    fun isUserLoggedIn(): Boolean
    fun getAuthToken(): String?
    
    // Callback-based methods for JS interop
    fun signInWithEmailAndPassword(email: String, password: String, onResult: (Boolean) -> Unit)
    fun signOut(onComplete: () -> Unit)
} 