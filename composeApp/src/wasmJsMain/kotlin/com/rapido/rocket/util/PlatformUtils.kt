package com.rapido.rocket.util

import kotlinx.browser.window

@JsName("Date")
external class JsDate {
    companion object {
        fun now(): Double
    }
}

actual fun currentTimeMillis(): Long = JsDate.now().toLong()

actual fun openUrl(url: String) {
    window.open(url, "_blank")
} 