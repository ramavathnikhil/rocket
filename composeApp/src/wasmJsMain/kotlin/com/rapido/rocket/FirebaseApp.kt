package com.rapido.rocket

import kotlin.js.JsAny

external interface FirebaseConfig : JsAny {
    var apiKey: String
    var authDomain: String
    var databaseURL: String
    var projectId: String
    var storageBucket: String
    var messagingSenderId: String
    var appId: String
}

@JsName("firebase")
external object Firebase {
    fun initializeApp(config: FirebaseConfig)
}

@JsName("firebaseConfig")
private external val firebaseConfig: FirebaseConfig

// External function to check if Firebase is already initialized
private external fun isFirebaseInitialized(): Boolean = definedExternally

actual object FirebaseApp {
    actual fun initialize() {
        // Check if Firebase is already initialized (by the HTML JavaScript)
        val isAlreadyInitialized = try {
            isFirebaseInitialized()
        } catch (e: Exception) {
            false
        }
        
        if (isAlreadyInitialized) {
            println("Firebase already initialized by HTML, skipping Kotlin initialization")
            return
        }
        
        // Only initialize if not already done
        try {
            Firebase.initializeApp(firebaseConfig)
            println("Firebase initialized by Kotlin")
        } catch (e: Exception) {
            println("Firebase initialization failed: $e")
            // If initialization fails, it might already be initialized
            // This is acceptable for WASM where HTML handles initialization
        }
    }
} 