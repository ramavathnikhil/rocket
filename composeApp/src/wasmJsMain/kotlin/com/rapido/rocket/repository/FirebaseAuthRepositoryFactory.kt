package com.rapido.rocket.repository

actual object FirebaseAuthRepositoryFactory {
    actual fun create(): FirebaseAuthRepository = WasmFirebaseAuthRepository()
} 