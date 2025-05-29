package com.rapido.rocket.repository

import com.rapido.rocket.model.User
import com.rapido.rocket.model.UserRole
import com.rapido.rocket.model.UserStatus
import com.rapido.rocket.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.js.Promise
import kotlin.js.JsAny
import kotlin.js.JsBoolean
import kotlinx.browser.localStorage
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

external interface FirebaseAuthResult : JsAny {
    val user: FirebaseUser
}

external interface FirebaseUser : JsAny {
    val uid: String
    val email: String?
    fun getIdToken(forceRefresh: Boolean): Promise<JsAny>
}

external interface FirebaseDoc : JsAny {
    val exists: Boolean
    fun data(): JsAny?
}

external interface FirebaseAuth : JsAny {
    fun signInWithEmailAndPassword(email: String, password: String): Promise<FirebaseAuthResult>
    fun createUserWithEmailAndPassword(email: String, password: String): Promise<FirebaseAuthResult>
    fun signOut(): Promise<JsAny>
    val currentUser: FirebaseUser?
    fun onAuthStateChanged(callback: (FirebaseUser?) -> Unit)
}

external interface FirebaseFirestore : JsAny {
    fun collection(path: String): FirebaseCollection
}

external interface FirebaseCollection : JsAny {
    fun doc(path: String): FirebaseDocument
    fun get(): Promise<FirebaseQuerySnapshot>
}

external interface FirebaseQuerySnapshot : JsAny {
    val docs: JsAny
    fun forEach(callback: (FirebaseDoc) -> Unit)
}

external interface FirebaseDocument : JsAny {
    fun get(): Promise<FirebaseDoc>
    fun set(data: JsAny): Promise<JsAny>
    fun update(data: JsAny): Promise<JsAny>
    fun delete(): Promise<JsAny>
}

@JsName("firebase")
external object Firebase : JsAny {
    fun auth(): FirebaseAuth
    fun firestore(): FirebaseFirestore
}

@JsName("Object")
private external object JsObject {
    fun entries(obj: JsAny): JsAny
    fun create(): JsAny
    fun defineProperty(obj: JsAny, key: String, descriptor: JsAny): JsAny
    fun assign(target: JsAny, vararg sources: JsAny): JsAny
}

@JsName("console")
external object console {
    fun error(message: String, error: JsAny)
}

@JsName("Array")
private external object JsArray {
    fun from(value: JsAny): JsAny
}

@JsName("Array")
private external class JsArrayImpl : JsAny {
    val length: Int
    fun item(index: Int): JsAny?
}

@JsName("typeof")
private external fun getJsType(value: JsAny): String

@JsName("Object")
private external object JsObjectOps {
    fun set(obj: JsAny, key: String, value: JsAny): Unit
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
                                doc.data()?.let { jsToMap(it) } ?: emptyMap()
                            } catch (parseError: Exception) {
                                println("WasmFirebaseAuthRepository: Error parsing document data: $parseError")
                                emptyMap()
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
                    } catch (e: Throwable) {
                        println("WasmFirebaseAuthRepository: Error in auth state change: $e")
                        _authStateFlow.update { null }
                        localStorage.removeItem(TOKEN_KEY)
                    }
                }
            } else {
                println("WasmFirebaseAuthRepository: User is null, clearing auth state")
                _authStateFlow.update { null }
                localStorage.removeItem(TOKEN_KEY)
            }
        }
    }

    override suspend fun signUp(email: String, password: String, username: String): Result<User> = try {
        println("WasmFirebaseAuthRepository: Starting signUp for $email")
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        println("WasmFirebaseAuthRepository: Auth user created successfully, uid: ${authResult.user.uid}")
        
        val user = User(
            id = authResult.user.uid,
            email = email,
            username = username,
            role = UserRole.USER,
            status = UserStatus.PENDING_APPROVAL,
            createdAt = currentTimeMillis(),
            updatedAt = currentTimeMillis()
        )
        println("WasmFirebaseAuthRepository: User object created: $user")
        
        val userData = mapOf(
            "id" to user.id,
            "email" to user.email,
            "username" to user.username,
            "role" to user.role.name,
            "status" to user.status.name,
            "createdAt" to user.createdAt,
            "updatedAt" to user.updatedAt
        )
        println("WasmFirebaseAuthRepository: User data map created: $userData")
        
        // Create user document with the same ID as auth UID
        println("WasmFirebaseAuthRepository: Attempting to create Firestore document...")
        try {
            println("WasmFirebaseAuthRepository: Starting JS object conversion for signup...")
            val jsUserData = userData.toJsObject()
            println("WasmFirebaseAuthRepository: JS object conversion successful for signup")
            println("WasmFirebaseAuthRepository: Attempting Firestore write...")
            usersCollection.doc(user.id).set(jsUserData).await()
            println("WasmFirebaseAuthRepository: Firestore document created successfully!")
        } catch (firestoreError: Exception) {
            println("WasmFirebaseAuthRepository: Error creating Firestore document: $firestoreError")
            println("WasmFirebaseAuthRepository: Firestore error details: ${firestoreError.message}")
            println("WasmFirebaseAuthRepository: Firestore error stack: ${firestoreError.stackTraceToString()}")
            throw firestoreError
        }
        
        // Get the token and store it
        authResult.user.getIdToken(true).then { token ->
            localStorage.setItem(TOKEN_KEY, token.toString())
            println("WasmFirebaseAuthRepository: Token stored after signup")
            null
        }
        
        println("WasmFirebaseAuthRepository: SignUp completed successfully")
        Result.success(user)
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: SignUp failed with error: $e")
        Result.failure(e)
    }

    override suspend fun signIn(email: String, password: String): Result<User> = try {
        println("WasmFirebaseAuthRepository: Starting signIn for $email")
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        println("WasmFirebaseAuthRepository: Auth successful, uid: ${authResult.user.uid}")
        val doc = usersCollection.doc(authResult.user.uid).get().await()
        println("WasmFirebaseAuthRepository: Document retrieved, exists: ${doc.exists}")
        val data = try {
            doc.data()?.let { jsToMap(it) } ?: emptyMap()
        } catch (parseError: Exception) {
            println("WasmFirebaseAuthRepository: Error parsing document data in signIn: $parseError")
            emptyMap()
        }
        println("WasmFirebaseAuthRepository: Document data: $data")
        val user = User(
            id = data["id"] as? String ?: authResult.user.uid,
            email = data["email"] as? String ?: email,
            username = data["username"] as? String ?: "",
            role = UserRole.valueOf(data["role"] as? String ?: UserRole.USER.name),
            status = UserStatus.valueOf(data["status"] as? String ?: UserStatus.PENDING_APPROVAL.name),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis()
        )
        println("WasmFirebaseAuthRepository: User created: $user")
        Result.success(user)
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: SignIn failed with error: $e")
        Result.failure(e)
    }

    override suspend fun signOut() {
        auth.signOut().await()
        localStorage.removeItem(TOKEN_KEY)
    }

    override suspend fun getCurrentUser(): User? {
        println("WasmFirebaseAuthRepository: getCurrentUser called")
        val firebaseUser = auth.currentUser
        println("WasmFirebaseAuthRepository: Firebase currentUser: ${firebaseUser?.uid}")
        
        if (firebaseUser == null) {
            println("WasmFirebaseAuthRepository: No current Firebase user")
            return null
        }
        
        try {
            val doc = usersCollection.doc(firebaseUser.uid).get().await()
            println("WasmFirebaseAuthRepository: Document exists: ${doc.exists}")
            val data = try {
                doc.data()?.let { jsToMap(it) } ?: emptyMap()
            } catch (parseError: Exception) {
                println("WasmFirebaseAuthRepository: Error parsing document data in getCurrentUser: $parseError")
                emptyMap()
            }
            println("WasmFirebaseAuthRepository: Document data: $data")
            
            val user = User(
                id = data["id"] as? String ?: firebaseUser.uid,
                email = data["email"] as? String ?: firebaseUser.email ?: "",
                username = data["username"] as? String ?: "",
                role = UserRole.valueOf(data["role"] as? String ?: UserRole.USER.name),
                status = UserStatus.valueOf(data["status"] as? String ?: UserStatus.PENDING_APPROVAL.name),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis()
            )
            println("WasmFirebaseAuthRepository: Returning user: $user")
            return user
        } catch (e: Throwable) {
            println("WasmFirebaseAuthRepository: Error getting current user: $e")
            return null
        }
    }

    override fun observeAuthState(): Flow<User?> = _authStateFlow

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
        println("WasmFirebaseAuthRepository: Getting all users")
        
        val usersList = mutableListOf<User>()
        
        try {
            // Get all documents from the users collection
            val querySnapshot = usersCollection.get().await()
            
            // Convert the query snapshot to a list of users
            val docs = mutableListOf<FirebaseDoc>()
            querySnapshot.forEach { doc ->
                docs.add(doc)
            }
            
            println("WasmFirebaseAuthRepository: Found ${docs.size} user documents")
            
            for (doc in docs) {
                if (doc.exists) {
                    try {
                        val data = doc.data()?.let { jsToMap(it) } ?: emptyMap()
                        val user = User(
                            id = data["id"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            username = data["username"] as? String ?: "",
                            role = UserRole.valueOf(data["role"] as? String ?: UserRole.USER.name),
                            status = UserStatus.valueOf(data["status"] as? String ?: UserStatus.PENDING_APPROVAL.name),
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis()
                        )
                        usersList.add(user)
                        println("WasmFirebaseAuthRepository: Added user: ${user.email}")
                    } catch (parseError: Exception) {
                        println("WasmFirebaseAuthRepository: Error parsing user document: $parseError")
                    }
                }
            }
            
            println("WasmFirebaseAuthRepository: Successfully loaded ${usersList.size} users")
            Result.success(usersList)
        } catch (e: Exception) {
            println("WasmFirebaseAuthRepository: Error fetching users from Firestore: $e")
            // Fallback: return current user if available
            val currentUser = getCurrentUser()
            if (currentUser != null) {
                Result.success(listOf(currentUser))
            } else {
                Result.success(emptyList())
            }
        }
    } catch (e: Throwable) {
        println("WasmFirebaseAuthRepository: Error getting all users: $e")
        Result.failure(e)
    }

    override suspend fun getUserById(userId: String): Result<User?> = try {
        println("WasmFirebaseAuthRepository: Getting user by ID: $userId")
        val doc = usersCollection.doc(userId).get().await()
        if (doc.exists) {
            val data = try {
                doc.data()?.let { jsToMap(it) } ?: emptyMap()
            } catch (parseError: Exception) {
                println("WasmFirebaseAuthRepository: Error parsing document data in getUserById: $parseError")
                emptyMap()
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
                    false as JsBoolean
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
                    false as JsBoolean
                }
        } catch (e: Throwable) {
            console.error("Logout error", e as JsAny)
            onComplete()
        }
    }
}

private fun JsAny.asJsArray(): JsArrayImpl = this.unsafeCast<JsArrayImpl>()

@JsName("JSON")
private external object JSON {
    fun stringify(obj: JsAny): String
}

private fun jsToMap(jsObject: JsAny): Map<String, Any> {
    return try {
        val jsonString = JSON.stringify(jsObject)
        println("WasmFirebaseAuthRepository: Firestore data JSON: $jsonString")
        
        // Parse the JSON string manually to create a Kotlin map
        val map = mutableMapOf<String, Any>()
        
        // Simple JSON parsing for our known structure
        if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
            val content = jsonString.substring(1, jsonString.length - 1)
            val pairs = content.split(",")
            
            for (pair in pairs) {
                val keyValue = pair.split(":")
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim().removeSurrounding("\"")
                    val valueStr = keyValue[1].trim()
                    
                    val value: Any = when {
                        valueStr == "true" -> true
                        valueStr == "false" -> false
                        valueStr.startsWith("\"") && valueStr.endsWith("\"") -> valueStr.removeSurrounding("\"")
                        valueStr.contains(".") -> valueStr.toDoubleOrNull() ?: valueStr
                        else -> valueStr.toLongOrNull() ?: valueStr
                    }
                    map[key] = value
                }
            }
        }
        
        println("WasmFirebaseAuthRepository: Parsed map: $map")
        map
    } catch (e: Exception) {
        println("WasmFirebaseAuthRepository: Error parsing JS object: $e")
        emptyMap()
    }
}

// Use a much simpler approach with external JS function
@JsName("eval")
private external fun jsEval(code: String): JsAny

private fun Map<String, Any>.toJsObject(): JsAny {
    // Create a simple JS object using JSON
    val jsonString = buildString {
        append("{")
        val entries = this@toJsObject.entries.toList()
        entries.forEachIndexed { index, (key, value) ->
            append("\"$key\":")
            when (value) {
                is String -> append("\"${value.replace("\"", "\\\"")}\"")
                is Number -> append(value.toString())
                is Boolean -> append(value.toString())
                else -> append("\"${value.toString().replace("\"", "\\\"")}\"")
            }
            if (index < entries.size - 1) append(",")
        }
        append("}")
    }
    println("WasmFirebaseAuthRepository: JSON string: $jsonString")
    return jsEval("($jsonString)")
}

private suspend fun <T : JsAny?> Promise<T>.await(): T = suspendCoroutine { continuation ->
    this.then { result ->
        continuation.resume(result)
        null
    }.catch { error: JsAny ->
        continuation.resumeWithException(Exception(error.toString()))
        null as JsAny?
    }
}