package com.rapido.rocket.repository

expect object FirebaseAuthRepositoryFactory {
    fun create(): FirebaseAuthRepository
} 