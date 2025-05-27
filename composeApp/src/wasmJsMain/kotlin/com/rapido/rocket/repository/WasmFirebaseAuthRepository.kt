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

@JsName("Promise")
private external class JsPromise<T : JsAny> {
    fun <R : JsAny> then(onFulfilled: (T) -> R?): JsPromise<R>
    fun catch(onRejected: (JsAny) -> Unit): JsPromise<T>
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

class WasmFirebaseAuthRepository : FirebaseAuthRepository {
    private val auth: FirebaseAuth = Firebase.auth()
    private val firestore: FirebaseFirestore = Firebase.firestore()
    private val usersCollection = firestore.collection("users")
    private val _authStateFlow = MutableStateFlow<User?>(null)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val TOKEN_KEY = "auth_token"

    init {
        println("WasmFirebaseAuthRepository: Initializing auth state listener")
        
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
                            val data = doc.data()?.let { jsToMap(it) } ?: emptyMap()
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
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = User(
            id = authResult.user.uid,
            email = email,
            username = username,
            role = UserRole.USER,
            status = UserStatus.PENDING_APPROVAL,
            createdAt = currentTimeMillis(),
            updatedAt = currentTimeMillis()
        )
        val userData = mapOf(
            "id" to user.id,
            "email" to user.email,
            "username" to user.username,
            "role" to user.role.name,
            "status" to user.status.name,
            "createdAt" to user.createdAt,
            "updatedAt" to user.updatedAt
        )
        
        // Create user document with the same ID as auth UID
        usersCollection.doc(user.id).set(userData.toJsObject()).await()
        
        // Get the token and store it
        authResult.user.getIdToken(true).then { token ->
            localStorage.setItem(TOKEN_KEY, token.toString())
            null
        }
        
        Result.success(user)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun signIn(email: String, password: String): Result<User> = try {
        println("WasmFirebaseAuthRepository: Starting signIn for $email")
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        println("WasmFirebaseAuthRepository: Auth successful, uid: ${authResult.user.uid}")
        val doc = usersCollection.doc(authResult.user.uid).get().await()
        println("WasmFirebaseAuthRepository: Document retrieved, exists: ${doc.exists}")
        val data = doc.data()?.let { jsToMap(it) } ?: emptyMap()
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
            val data = doc.data()?.let { jsToMap(it) } ?: emptyMap()
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
        val updateData = mapOf(
            "status" to status,
            "updatedAt" to currentTimeMillis()
        )
        usersCollection.doc(userId).update(updateData.toJsObject()).await()
        Result.success(Unit)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun deleteUser(userId: String): Result<Unit> = try {
        usersCollection.doc(userId).delete().await()
        Result.success(Unit)
    } catch (e: Throwable) {
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

private fun jsToMap(jsObject: JsAny): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    val entries = JsObject.entries(jsObject)
    val entriesArray = JsArray.from(entries).asJsArray()
    
    for (i in 0 until entriesArray.length) {
        val entry = entriesArray.item(i)?.unsafeCast<JsArrayImpl>() ?: continue
        val key = entry.item(0)?.toString() ?: continue
        val value = entry.item(1) ?: continue
        
        map[key] = when (getJsType(value)) {
            "string" -> value.toString()
            "number" -> (value as Number).toDouble()
            "boolean" -> value as Boolean
            else -> value.toString()
        }
    }
    
    return map
}

@JsName("Object")
private external object JsObjectOps {
    fun set(obj: JsAny, key: String, value: JsAny): Unit
}

private fun setJsField(obj: JsAny, key: String, value: JsAny) {
    JsObjectOps.set(obj, key, value)
}

private fun Map<String, Any>.toJsObject(): JsAny {
    val plainObj = JsObject.create()
    forEach { (key, value) ->
        setJsField(plainObj, key, value as JsAny)
    }
    return plainObj
}

private suspend fun <T : JsAny?> Promise<T>.await(): T = kotlin.coroutines.suspendCoroutine { continuation ->
    this.then { result ->
        continuation.resume(result)
        null
    }.catch { error: JsAny ->
        continuation.resumeWithException(Exception(error.toString()))
        null as JsAny?
    }
}