package com.rapido.rocket.ui.theme

import kotlin.js.JsName

@JsName("localStorage")
external object LocalStorage {
    fun setItem(key: String, value: String)
    fun getItem(key: String): String?
    fun removeItem(key: String)
}

actual class LocalStorageManager {
    actual fun setItem(key: String, value: String) {
        try {
            LocalStorage.setItem(key, value)
        } catch (e: Exception) {
            println("Failed to save to localStorage: $e")
        }
    }

    actual fun getItem(key: String): String? {
        return try {
            LocalStorage.getItem(key)
        } catch (e: Exception) {
            println("Failed to read from localStorage: $e")
            null
        }
    }
}

actual fun createLocalStorageManager(): LocalStorageManager {
    return LocalStorageManager()
} 