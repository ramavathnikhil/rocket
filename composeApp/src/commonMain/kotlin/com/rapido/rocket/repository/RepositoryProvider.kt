package com.rapido.rocket.repository

expect object RepositoryProvider {
    fun getProjectRepository(): ProjectRepository
    fun getReleaseRepository(): ReleaseRepository
    fun getWorkflowRepository(): WorkflowRepository
    fun getAuthRepository(): FirebaseAuthRepository
} 