package com.rapido.rocket

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform