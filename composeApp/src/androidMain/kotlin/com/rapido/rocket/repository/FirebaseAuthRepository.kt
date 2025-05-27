package com.rapido.rocket.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rapido.rocket.model.User
import com.rapido.rocket.model.UserRole
import com.rapido.rocket.model.UserStatus
import com.rapido.rocket.util.currentTimeMillis
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class FirebaseAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    actual suspend fun signUp(email: String, password: String, username: String): Result<User> = try {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = User(
            id = authResult.user?.uid ?: "",
            email = email,
            username = username,
            role = UserRole.USER,
            status = UserStatus.PENDING_APPROVAL
        )
        usersCollection.document(user.id).set(user.toMap()).await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    actual suspend fun signIn(email: String, password: String): Result<User> = try {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val doc = usersCollection.document(authResult.user?.uid ?: "").get().await()
        val user = User.fromMap(doc.data ?: emptyMap())
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    actual suspend fun signOut() {
        auth.signOut()
    }

    actual suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        val doc = usersCollection.document(firebaseUser.uid).get().await()
        return User.fromMap(doc.data ?: emptyMap())
    }

    actual fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                usersCollection.document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        trySend(User.fromMap(doc.data ?: emptyMap()))
                    }
                    .addOnFailureListener {
                        trySend(null)
                    }
            } else {
                trySend(null)
            }
        }
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    actual suspend fun updateUserStatus(userId: String, status: String): Result<Unit> = try {
        usersCollection.document(userId).update(
            mapOf(
                "status" to status,
                "updatedAt" to currentTimeMillis()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    actual suspend fun deleteUser(userId: String): Result<Unit> = try {
        usersCollection.document(userId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
} 