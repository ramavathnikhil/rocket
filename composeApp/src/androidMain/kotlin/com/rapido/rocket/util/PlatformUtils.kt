package com.rapido.rocket.util

import android.content.Intent
import android.net.Uri

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun openUrl(url: String) {
    // Note: This would need a Context to work properly in Android
    // For now, just a placeholder implementation
    println("Android: Opening URL: $url")
    // TODO: Implement with proper Context when needed
} 