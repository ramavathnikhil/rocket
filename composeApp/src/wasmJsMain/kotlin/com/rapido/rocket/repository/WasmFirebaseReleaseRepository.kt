package com.rapido.rocket.repository

import com.rapido.rocket.model.Release
import com.rapido.rocket.model.ReleaseStatus
import com.rapido.rocket.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

class WasmFirebaseReleaseRepository : ReleaseRepository {
    
    private val firestore: FirebaseFirestore = Firebase.firestore()
    private val releasesCollection = firestore.collection("releases")
    
    override suspend fun createRelease(release: Release): Result<Release> {
        return try {
            val releaseWithId = if (release.id.isEmpty()) {
                release.copy(
                    id = "rel_${currentTimeMillis()}_${(100..999).random()}",
                    createdAt = currentTimeMillis(),
                    updatedAt = currentTimeMillis()
                )
            } else {
                release.copy(updatedAt = currentTimeMillis())
            }
            
            val releaseDataMap = mapOf(
                "id" to releaseWithId.id,
                "projectId" to releaseWithId.projectId,
                "version" to releaseWithId.version,
                "title" to releaseWithId.title,
                "description" to releaseWithId.description,
                "status" to releaseWithId.status.name,
                "createdBy" to releaseWithId.createdBy,
                "assignedTo" to releaseWithId.assignedTo,
                "createdAt" to releaseWithId.createdAt,
                "updatedAt" to releaseWithId.updatedAt,
                "targetReleaseDate" to releaseWithId.targetReleaseDate,
                "actualReleaseDate" to releaseWithId.actualReleaseDate,
                "stagingBuildUrl" to releaseWithId.stagingBuildUrl,
                "productionBuildUrl" to releaseWithId.productionBuildUrl,
                "githubReleaseUrl" to releaseWithId.githubReleaseUrl,
                "playStoreUrl" to releaseWithId.playStoreUrl,
                "notes" to releaseWithId.notes
            )
            
            val jsReleaseData = releaseDataMap.toJsObject()
            val docRef = releasesCollection.doc(releaseWithId.id)
            docRef.set(jsReleaseData).await()
            
            println("✅ Release saved to Firebase: ${releaseWithId.title} (${releaseWithId.id})")
            Result.success(releaseWithId)
            
        } catch (e: Exception) {
            println("❌ Failed to save release to Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun updateRelease(release: Release): Result<Release> {
        return try {
            val updatedRelease = release.copy(updatedAt = currentTimeMillis())
            
            val releaseDataMap = mapOf(
                "id" to updatedRelease.id,
                "projectId" to updatedRelease.projectId,
                "version" to updatedRelease.version,
                "title" to updatedRelease.title,
                "description" to updatedRelease.description,
                "status" to updatedRelease.status.name,
                "createdBy" to updatedRelease.createdBy,
                "assignedTo" to updatedRelease.assignedTo,
                "createdAt" to updatedRelease.createdAt,
                "updatedAt" to updatedRelease.updatedAt,
                "targetReleaseDate" to updatedRelease.targetReleaseDate,
                "actualReleaseDate" to updatedRelease.actualReleaseDate,
                "stagingBuildUrl" to updatedRelease.stagingBuildUrl,
                "productionBuildUrl" to updatedRelease.productionBuildUrl,
                "githubReleaseUrl" to updatedRelease.githubReleaseUrl,
                "playStoreUrl" to updatedRelease.playStoreUrl,
                "notes" to updatedRelease.notes
            )
            
            val jsReleaseData = releaseDataMap.toJsObject()
            val docRef = releasesCollection.doc(release.id)
            docRef.set(jsReleaseData).await()
            
            println("✅ Release updated in Firebase: ${updatedRelease.title}")
            Result.success(updatedRelease)
            
        } catch (e: Exception) {
            println("❌ Failed to update release in Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRelease(releaseId: String): Result<Unit> {
        return try {
            val docRef = releasesCollection.doc(releaseId)
            docRef.delete().await()
            
            println("✅ Release deleted from Firebase: $releaseId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            println("❌ Failed to delete release from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getRelease(releaseId: String): Result<Release?> {
        return try {
            val docRef = releasesCollection.doc(releaseId)
            val docSnapshot = docRef.get().await()
            
            if (docSnapshot.exists) {
                val data = docSnapshot.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                val release = Release(
                    id = data["id"] as? String ?: releaseId,
                    projectId = data["projectId"] as? String ?: "",
                    version = data["version"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    status = ReleaseStatus.valueOf(data["status"] as? String ?: "DRAFT"),
                    createdBy = data["createdBy"] as? String ?: "",
                    assignedTo = data["assignedTo"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                    targetReleaseDate = (data["targetReleaseDate"] as? Number)?.toLong(),
                    actualReleaseDate = (data["actualReleaseDate"] as? Number)?.toLong(),
                    stagingBuildUrl = data["stagingBuildUrl"] as? String ?: "",
                    productionBuildUrl = data["productionBuildUrl"] as? String ?: "",
                    githubReleaseUrl = data["githubReleaseUrl"] as? String ?: "",
                    playStoreUrl = data["playStoreUrl"] as? String ?: "",
                    notes = data["notes"] as? String ?: ""
                )
                Result.success(release)
            } else {
                Result.success(null)
            }
            
        } catch (e: Exception) {
            println("❌ Failed to get release from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getReleasesByProject(projectId: String): Result<List<Release>> {
        return try {
            val querySnapshot = releasesCollection.get().await()
            val releases = mutableListOf<Release>()
            
            querySnapshot.forEach { doc ->
                if (doc.exists) {
                    val data = doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                    val releaseProjectId = data["projectId"] as? String ?: ""
                    
                    if (releaseProjectId == projectId) {
                        val release = Release(
                            id = data["id"] as? String ?: "",
                            projectId = data["projectId"] as? String ?: "",
                            version = data["version"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            status = ReleaseStatus.valueOf(data["status"] as? String ?: "DRAFT"),
                            createdBy = data["createdBy"] as? String ?: "",
                            assignedTo = data["assignedTo"] as? String ?: "",
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            targetReleaseDate = (data["targetReleaseDate"] as? Number)?.toLong(),
                            actualReleaseDate = (data["actualReleaseDate"] as? Number)?.toLong(),
                            stagingBuildUrl = data["stagingBuildUrl"] as? String ?: "",
                            productionBuildUrl = data["productionBuildUrl"] as? String ?: "",
                            githubReleaseUrl = data["githubReleaseUrl"] as? String ?: "",
                            playStoreUrl = data["playStoreUrl"] as? String ?: "",
                            notes = data["notes"] as? String ?: ""
                        )
                        releases.add(release)
                    }
                }
            }
            
            val sortedReleases = releases.sortedByDescending { it.updatedAt }
            println("✅ Loaded ${sortedReleases.size} releases for project $projectId from Firebase")
            Result.success(sortedReleases)
            
        } catch (e: Exception) {
            println("❌ Failed to load releases for project from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getReleasesByStatus(status: ReleaseStatus): Result<List<Release>> {
        return try {
            val querySnapshot = releasesCollection.get().await()
            val releases = mutableListOf<Release>()
            
            querySnapshot.forEach { doc ->
                if (doc.exists) {
                    val data = doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                    val releaseStatus = data["status"] as? String ?: ""
                    
                    if (releaseStatus == status.name) {
                        val release = Release(
                            id = data["id"] as? String ?: "",
                            projectId = data["projectId"] as? String ?: "",
                            version = data["version"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            status = ReleaseStatus.valueOf(data["status"] as? String ?: "DRAFT"),
                            createdBy = data["createdBy"] as? String ?: "",
                            assignedTo = data["assignedTo"] as? String ?: "",
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            targetReleaseDate = (data["targetReleaseDate"] as? Number)?.toLong(),
                            actualReleaseDate = (data["actualReleaseDate"] as? Number)?.toLong(),
                            stagingBuildUrl = data["stagingBuildUrl"] as? String ?: "",
                            productionBuildUrl = data["productionBuildUrl"] as? String ?: "",
                            githubReleaseUrl = data["githubReleaseUrl"] as? String ?: "",
                            playStoreUrl = data["playStoreUrl"] as? String ?: "",
                            notes = data["notes"] as? String ?: ""
                        )
                        releases.add(release)
                    }
                }
            }
            
            val sortedReleases = releases.sortedByDescending { it.updatedAt }
            println("✅ Loaded ${sortedReleases.size} releases with status $status from Firebase")
            Result.success(sortedReleases)
            
        } catch (e: Exception) {
            println("❌ Failed to load releases by status from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getReleasesByUser(userId: String): Result<List<Release>> {
        return try {
            val querySnapshot = releasesCollection.get().await()
            val releases = mutableListOf<Release>()
            
            querySnapshot.forEach { doc ->
                if (doc.exists) {
                    val data = doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                    val createdBy = data["createdBy"] as? String ?: ""
                    
                    if (createdBy == userId) {
                        val release = Release(
                            id = data["id"] as? String ?: "",
                            projectId = data["projectId"] as? String ?: "",
                            version = data["version"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            status = ReleaseStatus.valueOf(data["status"] as? String ?: "DRAFT"),
                            createdBy = data["createdBy"] as? String ?: "",
                            assignedTo = data["assignedTo"] as? String ?: "",
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            targetReleaseDate = (data["targetReleaseDate"] as? Number)?.toLong(),
                            actualReleaseDate = (data["actualReleaseDate"] as? Number)?.toLong(),
                            stagingBuildUrl = data["stagingBuildUrl"] as? String ?: "",
                            productionBuildUrl = data["productionBuildUrl"] as? String ?: "",
                            githubReleaseUrl = data["githubReleaseUrl"] as? String ?: "",
                            playStoreUrl = data["playStoreUrl"] as? String ?: "",
                            notes = data["notes"] as? String ?: ""
                        )
                        releases.add(release)
                    }
                }
            }
            
            val sortedReleases = releases.sortedByDescending { it.updatedAt }
            println("✅ Loaded ${sortedReleases.size} releases for user $userId from Firebase")
            Result.success(sortedReleases)
            
        } catch (e: Exception) {
            println("❌ Failed to load releases by user from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override fun observeReleases(): Flow<List<Release>> = flow {
        while (true) {
            try {
                val querySnapshot = releasesCollection.get().await()
                val releases = mutableListOf<Release>()
                
                querySnapshot.forEach { doc ->
                    if (doc.exists) {
                        val data = doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                        val release = Release(
                            id = data["id"] as? String ?: "",
                            projectId = data["projectId"] as? String ?: "",
                            version = data["version"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            status = ReleaseStatus.valueOf(data["status"] as? String ?: "DRAFT"),
                            createdBy = data["createdBy"] as? String ?: "",
                            assignedTo = data["assignedTo"] as? String ?: "",
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            targetReleaseDate = (data["targetReleaseDate"] as? Number)?.toLong(),
                            actualReleaseDate = (data["actualReleaseDate"] as? Number)?.toLong(),
                            stagingBuildUrl = data["stagingBuildUrl"] as? String ?: "",
                            productionBuildUrl = data["productionBuildUrl"] as? String ?: "",
                            githubReleaseUrl = data["githubReleaseUrl"] as? String ?: "",
                            playStoreUrl = data["playStoreUrl"] as? String ?: "",
                            notes = data["notes"] as? String ?: ""
                        )
                        releases.add(release)
                    }
                }
                
                val sortedReleases = releases.sortedByDescending { it.updatedAt }
                emit(sortedReleases)
            } catch (e: Exception) {
                println("❌ Error in observeReleases: ${e.message}")
                emit(emptyList())
            }
            delay(5000)
        }
    }
    
    override fun observeRelease(releaseId: String): Flow<Release?> = flow {
        while (true) {
            val result = getRelease(releaseId)
            result.fold(
                onSuccess = { release -> emit(release) },
                onFailure = { 
                    println("❌ Error in observeRelease: ${it.message}")
                    emit(null) 
                }
            )
            delay(5000)
        }
    }
    
    override fun observeReleasesByProject(projectId: String): Flow<List<Release>> = flow {
        while (true) {
            val result = getReleasesByProject(projectId)
            result.fold(
                onSuccess = { releases -> emit(releases) },
                onFailure = { 
                    println("❌ Error in observeReleasesByProject: ${it.message}")
                    emit(emptyList()) 
                }
            )
            delay(5000)
        }
    }
} 