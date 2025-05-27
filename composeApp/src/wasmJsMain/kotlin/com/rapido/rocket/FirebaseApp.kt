package com.rapido.rocket

import kotlin.js.JsAny
import kotlin.js.JsName

@JsName("firebase")
private external object FirebaseJs {
    fun app(): JsAny
}

actual object FirebaseApp {
    actual fun initialize() {
        try {
            // Check if Firebase is initialized
            FirebaseJs.app()
        } catch (e: Throwable) {
            // Handle initialization error silently
        }
    }
} 