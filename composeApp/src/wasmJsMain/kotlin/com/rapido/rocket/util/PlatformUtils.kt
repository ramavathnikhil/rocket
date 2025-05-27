package com.rapido.rocket.util

@JsName("Date")
external class JsDate {
    companion object {
        fun now(): Double
    }
}

actual fun currentTimeMillis(): Long = JsDate.now().toLong() 