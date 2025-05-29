package com.rapido.rocket.repository

import com.rapido.rocket.model.User
import com.rapido.rocket.model.UserRole
import com.rapido.rocket.model.UserStatus
import com.rapido.rocket.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.js.Promise
import kotlin.js.JsAny
import kotlin.js.JsName

@JsName("localStorage")
external object localStorage {
    fun setItem(key: String, value: String)
    fun getItem(key: String): String?
    fun removeItem(key: String)
}

class WasmFirebaseAuthRepository : FirebaseAuthRepository {
    private val auth: FirebaseAuth = Firebase.auth()
    private val firestore: FirebaseFirestore = Firebase.firestore()
    private val usersCollection = firestore.collection("users")
    private val _authStateFlow = MutableStateFlow<User?>(null)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val TOKEN_KEY = "auth_token"

    init {
        println("WasmFirebaseAuthRepository: Initializing auth state listener")
        
        // Test Firestore connection
        try {
            println("WasmFirebaseAuthRepository: Testing Firestore connection...")
            println("WasmFirebaseAuthRepository: Firestore instance: $firestore")
            println("WasmFirebaseAuthRepository: Users collection: $usersCollection")
            
            // Skip Firestore test for now to avoid blocking initialization
            println("WasmFirebaseAuthRepository: Skipping Firestore test to avoid blocking initialization")
        } catch (e: Exception) {
            println("WasmFirebaseAuthRepository: Error testing Firestore connection: $e")
        }
        
        // Check if there's already a current user (for page refresh scenarios)
        val currentUser = auth.currentUser
        println("WasmFirebaseAuthRepository: Initial current user: ${currentUser?.uid}")
        
        auth.onAuthStateChanged { user ->
            println("WasmFirebaseAuthRepository: Auth state changed, user: ${user?.uid}")
            if (user != null) {
                scope.launch {
                    try {
                        val doc = usersCollection.doc(user.uid).get().await()
                        if (doc.exists) {
                            println("WasmFirebaseAuthRepository: Document exists, parsing data...")
                            val data = try {
                                doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                            } catch (parseError: Exception) {
                                println("WasmFirebaseAuthRepository: Error parsing document data: $parseError")
                                emptyMap<String, Any>()
                            }
                            
                            val userData = User(
                                id = data["id"] as? String ?: user.uid,
                                email = data["email"] as? String ?: user.email ?: "",
                                username = data["username"] as? String ?: "",
                                role = UserRole.valueOf(data["role"] as? String ?: UserRole.USER.name),
                                status = UserStatus.valueOf(data["status"] as? String ?: UserStatus.PENDING_APPROVAL.name),
                                createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis()
                            )
                            println("WasmFirebaseAuthRepository: Setting user in auth state: $userData")
                            _authStateFlow.update { userData }
                            // Store token on successful auth
                            user.getIdToken(true).then { token ->
                                localStorage.setItem(TOKEN_KEY, token.toString())
                                println("WasmFirebaseAuthRepository: Token stored in localStorage")
                                null
                            }
                        } else {
                            println("WasmFirebaseAuthRepository: User document does not exist, creating minimal user")
                            // Create a minimal user object even if document doesn't exist
                            val userData = User(
                                id = user.uid,
                                email = user.email ?: "",
                                username = "",
                                role = UserRole.USER,
                                status = UserStatus.PENDING_APPROVAL,
                                createdAt = currentTimeMillis(),
                                updatedAt = currentTimeMillis()
                            )
                            
                            // Try to create the user document in Firestore
                            try {
                                println("WasmFirebaseAuthRepository: Attempting to create user document for ${user.uid}")
                                val userDataMap = mapOf(
                                    "id" to userData.id,
                                    "email" to userData.email,
                                    "username" to userData.username,
                                    "role" to userData.role.name,
                                    "status" to userData.status.name,
                                    "createdAt" to userData.createdAt,
                                    "updatedAt" to userData.updatedAt
                                )
                                println("WasmFirebaseAuthRepository: User data map: $userDataMap")
                                println("WasmFirebaseAuthRepository: Converting to JS object...")
                                
                                val jsUserData = try {
                                    println("WasmFirebaseAuthRepository: Starting JS object conversion...")
                                    val result = userDataMap.toJsObject()
                                    println("WasmFirebaseAuthRepository: JS object conversion successful")
                                    result
                                } catch (conversionError: Exception) {
                                    println("WasmFirebaseAuthRepository: Error converting user data to JS object: $conversionError")
                                    println("WasmFirebaseAuthRepository: Conversion error details: ${conversionError.message}")
                                    println("WasmFirebaseAuthRepository: Conversion error stack: ${conversionError.stackTraceToString()}")
                                    return@launch
                                }
                                
                                println("WasmFirebaseAuthRepository: JS object created, attempting Firestore write...")
                                usersCollection.doc(user.uid).set(jsUserData).then { result ->
                                    println("WasmFirebaseAuthRepository: User document created successfully in auth state listener!")
                                    null
                                }.catch { error ->
                                    println("WasmFirebaseAuthRepository: Error creating user document in auth state listener: $error")
                                    println("WasmFirebaseAuthRepository: Error type: ${error::class.simpleName}")
                                    println("WasmFirebaseAuthRepository: Error message: ${error.toString()}")
                                    console.error("User document creation error", error)
                                    null as JsAny?
                                }
                            } catch (e: Exception) {
                                println("WasmFirebaseAuthRepository: Exception creating user document in auth state listener: $e")
                            }
                            
                            _authStateFlow.update { userData }
                            // Store token anyway
                            user.getIdToken(true).then { token ->
                                localStorage.setItem(TOKEN_KEY, token.toString())
                                println("WasmFirebaseAuthRepository: Token stored in localStorage")
                                null
                            }
                        }
                    } catch (e: Exception) {
                        println("WasmFirebaseAuthRepository: Error in auth state listener: $e")
                    }
                }
            } else {
                println("WasmFirebaseAuthRepository: User is null, clearing auth state")
                _authStateFlow.update { null }
                localStorage.removeItem(TOKEN_KEY)
            }
        }
    }

    override fun observeAuthState(): Flow<User?> = _authStateFlow.asStateFlow()

    override suspend fun getCurrentUser(): User? {
        println("WasmFirebaseAuthRepository: Getting current user: ${_authStateFlow.value}")
        return _authStateFlow.value
    }

    override suspend fun signUp(email: String, password: String, username: String): Result<User> = try {
        println("WasmFirebaseAuthRepository: Starting signup process for email: $email")
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user
        
        println("WasmFirebaseAuthRepository: Firebase user created: ${firebaseUser.uid}")
        
        val userData = User(
            id = firebaseUser.uid,
            email = email,
            username = username,
            role = UserRole.USER,
            status = UserStatus.PENDING_APPROVAL,
            createdAt = currentTimeMillis(),
            updatedAt = currentTimeMillis()
        )
        
        // Create user document in Firestore
        val userDataMap = mapOf(
            "id" to userData.id,
            "email" to userData.email,
            "username" to userData.username,
            "role" to userData.role.name,
            "status" to userData.status.name,
            "createdAt" to userData.createdAt,
            "updatedAt" to userData.updatedAt
        )
        
        val jsUserData = userDataMap.toJsObject()
        usersCollection.doc(firebaseUser.uid).set(jsUserData).await()
        
        println("WasmFirebaseAuthRepository: User document created in Firestore")
        _authStateFlow.update { userData }
        
        // Store token
        val token = firebaseUser.getIdToken(true).await()
        localStorage.setItem(TOKEN_KEY, token.toString())
        
        println("WasmFirebaseAuthRepository: Signup completed successfully")
        Result.success(userData)
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: Error during signup: $e")
        Result.failure(e)
    }

    override suspend fun signIn(email: String, password: String): Result<User> = try {
        println("WasmFirebaseAuthRepository: Starting signin process for email: $email")
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user
        
        println("WasmFirebaseAuthRepository: Firebase signin successful: ${firebaseUser.uid}")
        
        // Get user document from Firestore
        val userDoc = usersCollection.doc(firebaseUser.uid).get().await()
        if (userDoc.exists) {
            val data = userDoc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
            val userData = User(
                id = data["id"] as? String ?: firebaseUser.uid,
                email = data["email"] as? String ?: firebaseUser.email ?: "",
                username = data["username"] as? String ?: "",
                role = UserRole.valueOf(data["role"] as? String ?: UserRole.USER.name),
                status = UserStatus.valueOf(data["status"] as? String ?: UserStatus.PENDING_APPROVAL.name),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis()
            )
            _authStateFlow.update { userData }
            
            // Store token
            val token = firebaseUser.getIdToken(true).await()
            localStorage.setItem(TOKEN_KEY, token.toString())
            
            println("WasmFirebaseAuthRepository: Signin completed successfully")
            Result.success(userData)
        } else {
            println("WasmFirebaseAuthRepository: User document not found")
            Result.failure(Exception("User document not found"))
        }
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: Error during signin: $e")
        Result.failure(e)
    }

    override suspend fun signOut() {
        try {
            println("WasmFirebaseAuthRepository: Starting signout process")
            auth.signOut().await()
            _authStateFlow.update { null }
            localStorage.removeItem(TOKEN_KEY)
            println("WasmFirebaseAuthRepository: Signout completed successfully")
        } catch (e: Throwable) {
            println("WasmFirebaseAuthRepository: Error during signout: $e")
        }
    }

    override suspend fun updateUserStatus(userId: String, status: String): Result<Unit> = try {
        println("WasmFirebaseAuthRepository: Updating user status for $userId to $status")
        val updateData = mapOf(
            "status" to status,
            "updatedAt" to currentTimeMillis()
        )
        usersCollection.doc(userId).update(updateData.toJsObject()).await()
        println("WasmFirebaseAuthRepository: User status updated successfully")
        Result.success(Unit)
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: Error updating user status: $e")
        Result.failure(e)
    }

    override suspend fun updateUserRole(userId: String, role: String): Result<Unit> = try {
        println("WasmFirebaseAuthRepository: Updating user role for $userId to $role")
        val updateData = mapOf(
            "role" to role,
            "updatedAt" to currentTimeMillis()
        )
        usersCollection.doc(userId).update(updateData.toJsObject()).await()
        println("WasmFirebaseAuthRepository: User role updated successfully")
        Result.success(Unit)
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: Error updating user role: $e")
        Result.failure(e)
    }

    override suspend fun updateUserProfile(userId: String, username: String, email: String): Result<Unit> = try {
        println("WasmFirebaseAuthRepository: Updating user profile for $userId")
        val updateData = mapOf(
            "username" to username,
            "email" to email,
            "updatedAt" to currentTimeMillis()
        )
        usersCollection.doc(userId).update(updateData.toJsObject()).await()
        println("WasmFirebaseAuthRepository: User profile updated successfully")
        Result.success(Unit)
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: Error updating user profile: $e")
        Result.failure(e)
    }

    override suspend fun getAllUsers(): Result<List<User>> = try {
        println("WasmFirebaseAuthRepository: Getting all users from Firestore")
        val snapshot = usersCollection.get().await()
        val users = mutableListOf<User>()
        
        snapshot.forEach { doc ->
            if (doc.exists) {
                val data = doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                val user = User(
                    id = data["id"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    username = data["username"] as? String ?: "",
                    role = UserRole.valueOf(data["role"] as? String ?: UserRole.USER.name),
                    status = UserStatus.valueOf(data["status"] as? String ?: UserStatus.PENDING_APPROVAL.name),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis()
                )
                users.add(user)
            }
        }
        
        println("WasmFirebaseAuthRepository: Loaded ${users.size} users from Firestore")
        Result.success(users)
    } catch (e: Throwable) {
        try {
            println("WasmFirebaseAuthRepository: Error fetching users from Firestore: $e")
            // Fallback: return current user if available
            val currentUser = getCurrentUser()
            if (currentUser != null) {
                Result.success(listOf(currentUser))
            } else {
                Result.success(emptyList())
            }
        } catch (e2: Throwable) {
            println("WasmFirebaseAuthRepository: Error getting all users: $e2")
            Result.failure(e2)
        }
    }

    override suspend fun getUserById(userId: String): Result<User?> = try {
        println("WasmFirebaseAuthRepository: Getting user by ID: $userId")
        val doc = usersCollection.doc(userId).get().await()
        if (doc.exists) {
            val data = try {
                doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
            } catch (parseError: Exception) {
                println("WasmFirebaseAuthRepository: Error parsing document data in getUserById: $parseError")
                emptyMap<String, Any>()
            }
            val user = User(
                id = data["id"] as? String ?: userId,
                email = data["email"] as? String ?: "",
                username = data["username"] as? String ?: "",
                role = UserRole.valueOf(data["role"] as? String ?: UserRole.USER.name),
                status = UserStatus.valueOf(data["status"] as? String ?: UserStatus.PENDING_APPROVAL.name),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis()
            )
            println("WasmFirebaseAuthRepository: User found: $user")
            Result.success(user)
        } else {
            println("WasmFirebaseAuthRepository: User not found")
            Result.success(null)
        }
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: Error getting user by ID: $e")
        Result.failure(e)
    }

    override suspend fun deleteUser(userId: String): Result<Unit> = try {
        println("WasmFirebaseAuthRepository: Deleting user: $userId")
        usersCollection.doc(userId).delete().await()
        println("WasmFirebaseAuthRepository: User deleted successfully")
        Result.success(Unit)
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: Error deleting user: $e")
        Result.failure(e)
    }

    override fun isUserLoggedIn(): Boolean {
        val token = getAuthToken()
        val hasToken = token != null
        println("WasmFirebaseAuthRepository: isUserLoggedIn - token: $token, hasToken: $hasToken")
        return hasToken
    }

    override fun getAuthToken(): String? {
        try {
            // Test localStorage access
            localStorage.setItem("test", "test_value")
            val testValue = localStorage.getItem("test")
            println("WasmFirebaseAuthRepository: localStorage test - set: test_value, got: $testValue")
            localStorage.removeItem("test")
            
            val token = localStorage.getItem(TOKEN_KEY)
            println("WasmFirebaseAuthRepository: getAuthToken - token: $token")
            return token
        } catch (e: Throwable) {
            println("WasmFirebaseAuthRepository: Error accessing localStorage: $e")
            return null
        }
    }

    override fun signInWithEmailAndPassword(email: String, password: String, onResult: (Boolean) -> Unit) {
        try {
            auth.signInWithEmailAndPassword(email, password)
                .then { userCredential ->
                    userCredential.user.getIdToken(true).then { token ->
                        localStorage.setItem(TOKEN_KEY, token.toString())
                        onResult(true)
                        null
                    }
                    null
                }
                .catch { error ->
                    console.error("Login failed", error)
                    onResult(false)
                    null as JsAny?
                }
        } catch (e: Throwable) {
            console.error("Login error", e as JsAny)
            onResult(false)
        }
    }

    override fun signOut(onComplete: () -> Unit) {
        try {
            auth.signOut()
                .then {
                    localStorage.removeItem(TOKEN_KEY)
                    onComplete()
                    null
                }
                .catch { error ->
                    console.error("Logout error", error)
                    onComplete()
                    null as JsAny?
                }
        } catch (e: Throwable) {
            console.error("Logout error", e as JsAny)
            onComplete()
        }
    }
} 