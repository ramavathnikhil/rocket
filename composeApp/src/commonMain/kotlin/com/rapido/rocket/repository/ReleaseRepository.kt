package com.rapido.rocket.repository

import com.rapido.rocket.model.Release
import com.rapido.rocket.model.ReleaseStatus
import kotlinx.coroutines.flow.Flow

interface ReleaseRepository {
    suspend fun createRelease(release: Release): Result<Release>
    suspend fun updateRelease(release: Release): Result<Release>
    suspend fun deleteRelease(releaseId: String): Result<Unit>
    suspend fun getRelease(releaseId: String): Result<Release?>
    suspend fun getReleasesByProject(projectId: String): Result<List<Release>>
    suspend fun getReleasesByStatus(status: ReleaseStatus): Result<List<Release>>
    suspend fun getReleasesByUser(userId: String): Result<List<Release>>
    fun observeReleases(): Flow<List<Release>>
    fun observeRelease(releaseId: String): Flow<Release?>
    fun observeReleasesByProject(projectId: String): Flow<List<Release>>
} 