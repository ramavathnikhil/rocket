package com.rapido.rocket.repository

import com.rapido.rocket.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    suspend fun createProject(project: Project): Result<Project>
    suspend fun updateProject(project: Project): Result<Project>
    suspend fun deleteProject(projectId: String): Result<Unit>
    suspend fun getProject(projectId: String): Result<Project?>
    suspend fun getAllProjects(): Result<List<Project>>
    suspend fun getProjectsByUser(userId: String): Result<List<Project>>
    fun observeProjects(): Flow<List<Project>>
    fun observeProject(projectId: String): Flow<Project?>
} 