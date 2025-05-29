package com.rapido.rocket.repository

actual object RepositoryProvider {
    actual fun getProjectRepository(): ProjectRepository {
        return WasmFirebaseProjectRepository()
    }
    
    actual fun getReleaseRepository(): ReleaseRepository {
        return WasmFirebaseReleaseRepository()
    }
    
    actual fun getWorkflowRepository(): WorkflowRepository {
        return WasmFirebaseWorkflowRepository()
    }
    
    actual fun getAuthRepository(): FirebaseAuthRepository {
        return WasmFirebaseAuthRepository()
    }
} 