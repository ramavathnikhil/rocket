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

actual object FirebaseApp {
    actual fun initialize() {
        Firebase.initializeApp(firebaseConfig)
    }
} 