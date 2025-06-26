package com.rapido.rocket.repository

import kotlin.js.Promise
import kotlin.js.JsAny
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Shared Firebase interfaces to avoid redeclaration
external interface FirebaseDoc : JsAny {
    val exists: Boolean
    fun data(): JsAny?
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

// Firebase Auth interfaces
external interface FirebaseUser : JsAny {
    val uid: String
    val email: String?
    fun getIdToken(forceRefresh: Boolean): Promise<JsAny>
}

external interface FirebaseAuthResult : JsAny {
    val user: FirebaseUser
}

external interface FirebaseAuth : JsAny {
    fun signInWithEmailAndPassword(email: String, password: String): Promise<FirebaseAuthResult>
    fun createUserWithEmailAndPassword(email: String, password: String): Promise<FirebaseAuthResult>
    fun signOut(): Promise<JsAny>
    val currentUser: FirebaseUser?
    fun onAuthStateChanged(callback: (FirebaseUser?) -> Unit)
}

// Firebase Functions interfaces
external interface FirebaseFunctions : JsAny {
    fun httpsCallable(name: String): FirebaseCallableFunction
}

external interface FirebaseCallableFunction : JsAny {
    operator fun invoke(data: JsAny?): Promise<FirebaseCallableResult>
}

external interface FirebaseCallableResult : JsAny {
    val data: JsAny?
}

@JsName("firebase")
external object Firebase : JsAny {
    fun firestore(): FirebaseFirestore
    fun auth(): FirebaseAuth
    fun functions(): FirebaseFunctions
}

// Shared helper functions
@JsName("JSON")
external object JSON {
    fun stringify(obj: JsAny): String
}

@JsName("eval")
external fun jsEval(code: String): JsAny

@JsName("console")
external object console {
    fun error(message: String, error: JsAny)
}

fun jsToMap(jsObject: JsAny): Map<String, Any> {
    return try {
        val jsonString = JSON.stringify(jsObject)
        println("Firebase: Firestore data JSON: $jsonString")
        
        val map = mutableMapOf<String, Any>()
        
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
        
        println("Firebase: Parsed map: $map")
        map
    } catch (e: Exception) {
        println("Firebase: Error parsing JS object: $e")
        emptyMap()
    }
}

fun Map<String, Any?>.toJsObject(): JsAny {
    val jsonString = buildString {
        append("{")
        val entries = this@toJsObject.entries.toList()
        entries.forEachIndexed { index, (key, value) ->
            append("\"$key\":")
            when (value) {
                null -> append("null")
                is String -> append("\"${value.replace("\"", "\\\"")}\"")
                is Number -> append(value.toString())
                is Boolean -> append(value.toString())
                else -> append("\"${value.toString().replace("\"", "\\\"")}\"")
            }
            if (index < entries.size - 1) append(",")
        }
        append("}")
    }
    println("Firebase: JSON string: $jsonString")
    return jsEval("($jsonString)")
}

suspend fun <T : JsAny?> Promise<T>.await(): T = suspendCoroutine { continuation ->
    this.then { result ->
        continuation.resume(result)
        null
    }.catch { error: JsAny ->
        continuation.resumeWithException(Exception(error.toString()))
        null as JsAny?
    }
} 