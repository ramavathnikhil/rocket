package com.rapido.rocket.repository

import com.rapido.rocket.model.Project
import com.rapido.rocket.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

class WasmFirebaseProjectRepository : ProjectRepository {
    
    private val firestore: FirebaseFirestore = Firebase.firestore()
    private val projectsCollection = firestore.collection("projects")
    
    override suspend fun createProject(project: Project): Result<Project> {
        return try {
            val projectWithId = if (project.id.isEmpty()) {
                project.copy(
                    id = "proj_${currentTimeMillis()}_${(100..999).random()}",
                    createdAt = currentTimeMillis(),
                    updatedAt = currentTimeMillis()
                )
            } else {
                project.copy(updatedAt = currentTimeMillis())
            }
            
            val projectDataMap = mapOf(
                "id" to projectWithId.id,
                "name" to projectWithId.name,
                "description" to projectWithId.description,
                "repositoryUrl" to projectWithId.repositoryUrl,
                "playStoreUrl" to projectWithId.playStoreUrl,
                "createdBy" to projectWithId.createdBy,
                "createdAt" to projectWithId.createdAt,
                "updatedAt" to projectWithId.updatedAt,
                "isActive" to projectWithId.isActive
            )
            
            val jsProjectData = projectDataMap.toJsObject()
            val docRef = projectsCollection.doc(projectWithId.id)
            docRef.set(jsProjectData).await()
            
            println("✅ Project saved to Firebase: ${projectWithId.name} (${projectWithId.id})")
            Result.success(projectWithId)
            
        } catch (e: Exception) {
            println("❌ Failed to save project to Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun updateProject(project: Project): Result<Project> {
        return try {
            val updatedProject = project.copy(updatedAt = currentTimeMillis())
            
            val projectDataMap = mapOf(
                "id" to updatedProject.id,
                "name" to updatedProject.name,
                "description" to updatedProject.description,
                "repositoryUrl" to updatedProject.repositoryUrl,
                "playStoreUrl" to updatedProject.playStoreUrl,
                "createdBy" to updatedProject.createdBy,
                "createdAt" to updatedProject.createdAt,
                "updatedAt" to updatedProject.updatedAt,
                "isActive" to updatedProject.isActive
            )
            
            val jsProjectData = projectDataMap.toJsObject()
            val docRef = projectsCollection.doc(project.id)
            docRef.set(jsProjectData).await()
            
            println("✅ Project updated in Firebase: ${updatedProject.name}")
            Result.success(updatedProject)
            
        } catch (e: Exception) {
            println("❌ Failed to update project in Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "isActive" to false,
                "updatedAt" to currentTimeMillis()
            ).toJsObject()
            
            val docRef = projectsCollection.doc(projectId)
            docRef.update(updates).await()
            
            println("✅ Project soft-deleted in Firebase: $projectId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            println("❌ Failed to delete project in Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getProject(projectId: String): Result<Project?> {
        return try {
            val docRef = projectsCollection.doc(projectId)
            val docSnapshot = docRef.get().await()
            
            if (docSnapshot.exists) {
                val data = docSnapshot.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                val project = Project(
                    id = data["id"] as? String ?: projectId,
                    name = data["name"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    repositoryUrl = data["repositoryUrl"] as? String ?: "",
                    playStoreUrl = data["playStoreUrl"] as? String ?: "",
                    createdBy = data["createdBy"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                    isActive = data["isActive"] as? Boolean ?: true
                )
                Result.success(if (project.isActive) project else null)
            } else {
                Result.success(null)
            }
            
        } catch (e: Exception) {
            println("❌ Failed to get project from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getAllProjects(): Result<List<Project>> {
        return try {
            val querySnapshot = projectsCollection.get().await()
            val projects = mutableListOf<Project>()
            
            querySnapshot.forEach { doc ->
                if (doc.exists) {
                    val data = doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                    val project = Project(
                        id = data["id"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        repositoryUrl = data["repositoryUrl"] as? String ?: "",
                        playStoreUrl = data["playStoreUrl"] as? String ?: "",
                        createdBy = data["createdBy"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                        isActive = data["isActive"] as? Boolean ?: true
                    )
                    if (project.isActive) {
                        projects.add(project)
                    }
                }
            }
            
            val sortedProjects = projects.sortedByDescending { it.updatedAt }
            println("✅ Loaded ${sortedProjects.size} projects from Firebase")
            Result.success(sortedProjects)
            
        } catch (e: Exception) {
            println("❌ Failed to load projects from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getProjectsByUser(userId: String): Result<List<Project>> {
        return try {
            val allProjectsResult = getAllProjects()
            allProjectsResult.fold(
                onSuccess = { projects ->
                    val userProjects = projects.filter { it.createdBy == userId }
                    Result.success(userProjects)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeProjects(): Flow<List<Project>> = flow {
        while (true) {
            val result = getAllProjects()
            result.fold(
                onSuccess = { projects -> emit(projects) },
                onFailure = { 
                    println("❌ Error in observeProjects: ${it.message}")
                    emit(emptyList()) 
                }
            )
            delay(5000)
        }
    }
    
    override fun observeProject(projectId: String): Flow<Project?> = flow {
        while (true) {
            val result = getProject(projectId)
            result.fold(
                onSuccess = { project -> emit(project) },
                onFailure = { 
                    println("❌ Error in observeProject: ${it.message}")
                    emit(null) 
                }
            )
            delay(5000)
        }
    }
} 