package com.rapido.rocket.repository

import com.rapido.rocket.model.GitHubConfig
import com.rapido.rocket.model.GitHubPullRequest
import com.rapido.rocket.model.GitHubActionRun
import com.rapido.rocket.model.CreatePullRequestRequest
import com.rapido.rocket.util.currentTimeMillis

/**
 * WASM-specific implementation that combines HTTP calls with Firebase config storage
 */
class WasmGitHubRepository : GitHubRepository {
    
    private val configRepository = WasmGitHubConfigRepository()
    private val httpRepository = GitHubRepositoryImpl(configRepository)
    
    // Delegate HTTP calls to the common implementation
    override suspend fun createPullRequest(
        repositoryUrl: String,
        token: String,
        request: CreatePullRequestRequest
    ): Result<GitHubPullRequest> = httpRepository.createPullRequest(repositoryUrl, token, request)
    
    override suspend fun getPullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int
    ): Result<GitHubPullRequest> = httpRepository.getPullRequest(repositoryUrl, token, pullNumber)
    
    override suspend fun mergePullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int,
        mergeMethod: String
    ): Result<GitHubPullRequest> = httpRepository.mergePullRequest(repositoryUrl, token, pullNumber, mergeMethod)
    
    override suspend fun triggerGitHubAction(
        repositoryUrl: String,
        token: String,
        workflowId: String,
        ref: String,
        inputs: Map<String, String>
    ): Result<GitHubActionRun> = httpRepository.triggerGitHubAction(repositoryUrl, token, workflowId, ref, inputs)
    
    override suspend fun validateToken(token: String): Result<Boolean> = httpRepository.validateToken(token)
    
    override suspend fun validateRepository(repositoryUrl: String, token: String): Result<Boolean> = 
        httpRepository.validateRepository(repositoryUrl, token)
    
    // Config operations delegate to Firebase implementation
    override suspend fun saveGitHubConfig(config: GitHubConfig): Result<GitHubConfig> = 
        configRepository.saveGitHubConfig(config)
    
    override suspend fun getGitHubConfig(projectId: String): Result<GitHubConfig?> = 
        configRepository.getGitHubConfig(projectId)
    
    override suspend fun updateGitHubConfig(config: GitHubConfig): Result<GitHubConfig> = 
        configRepository.updateGitHubConfig(config)
    
    override suspend fun deleteGitHubConfig(configId: String): Result<Unit> = 
        configRepository.deleteGitHubConfig(configId)
}

/**
 * WASM-specific Firebase config repository
 */
class WasmGitHubConfigRepository : GitHubConfigRepository {
    
    private val firestore: FirebaseFirestore = Firebase.firestore()
    private val githubConfigsCollection = firestore.collection("githubConfigs")
    
    override suspend fun saveGitHubConfig(config: GitHubConfig): Result<GitHubConfig> {
        return try {
            println("🔍 Saving GitHub config:")
            println("   - projectId: '${config.projectId}'")
            println("   - appRepositoryUrl: '${config.appRepositoryUrl}'")
            println("   - bffRepositoryUrl: '${config.bffRepositoryUrl}'")
            println("   - githubToken length: ${config.githubToken.length}")
            println("   - workflowUrls: ${config.workflowUrls}")
            
            val configWithId = if (config.id.isEmpty()) {
                config.copy(
                    id = "github_${currentTimeMillis()}_${(100..999).random()}",
                    createdAt = currentTimeMillis(),
                    updatedAt = currentTimeMillis()
                )
            } else {
                config.copy(updatedAt = currentTimeMillis())
            }
            
            // Create the config data map, handling workflowIds properly for Firebase
            val configDataMap = mutableMapOf<String, Any>(
                "id" to configWithId.id,
                "projectId" to configWithId.projectId,
                "appRepositoryUrl" to configWithId.appRepositoryUrl,
                "bffRepositoryUrl" to configWithId.bffRepositoryUrl,
                "githubToken" to configWithId.githubToken,
                "defaultBaseBranch" to configWithId.defaultBaseBranch,
                "defaultTargetBranch" to configWithId.defaultTargetBranch,
                "createdAt" to configWithId.createdAt,
                "updatedAt" to configWithId.updatedAt
            )
            
            // Handle workflowUrls as a proper nested map for Firebase
            if (configWithId.workflowUrls.isNotEmpty()) {
                configDataMap["workflowUrls"] = configWithId.workflowUrls
            }
            
            println("🔍 Config data map before saving:")
            configDataMap.forEach { (key, value) ->
                if (key == "workflowUrls") {
                    println("   - $key: $value (${value::class.simpleName})")
                } else {
                    println("   - $key: '$value'")
                }
            }
            
            val jsConfigData = configDataMap.toJsObject()
            val docRef = githubConfigsCollection.doc(configWithId.id)
            docRef.set(jsConfigData).await()
            
            println("✅ GitHub config saved to Firebase: ${configWithId.projectId}")
            Result.success(configWithId)
            
        } catch (e: Exception) {
            println("❌ Failed to save GitHub config to Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getGitHubConfig(projectId: String): Result<GitHubConfig?> {
        return try {
            println("🔍 Fetching GitHub config for projectId: '$projectId'")
            val querySnapshot = githubConfigsCollection.get().await()
            var foundConfig: GitHubConfig? = null
            
            var index = 0
            querySnapshot.forEach { doc: FirebaseDoc ->
                if (doc.exists) {
                    index++
                    val data = doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                    val configProjectId = data["projectId"] as? String ?: ""
                    
                    println("   📄 Raw data keys: ${data.keys}")
                    println("   📋 All key-value pairs:")
                    data.forEach { (key, value) ->
                        when (key) {
                            "githubToken" -> println("      $key: '${value.toString().take(10)}...' (length: ${value.toString().length})")
                            else -> println("      $key: '$value'")
                        }
                    }
                    println("   🎯 Parsed values:")
                    println("      projectId: '$configProjectId'")
                    println("      appRepositoryUrl: '${data["appRepositoryUrl"] as? String ?: ""}'")
                    println("      bffRepositoryUrl: '${data["bffRepositoryUrl"] as? String ?: ""}'")
                    println("      githubToken length: ${(data["githubToken"] as? String ?: "").length}")
                    println("      defaultBaseBranch: '${data["defaultBaseBranch"] as? String ?: ""}'")
                    println("      defaultTargetBranch: '${data["defaultTargetBranch"] as? String ?: ""}'")
                    println("   ⚖️ Match check: '$configProjectId' == '$projectId' ? ${configProjectId == projectId}")
                    
                    if (configProjectId == projectId) {
                        println("✅ Found matching config for projectId: '$projectId'")
                        foundConfig = GitHubConfig.fromMap(data)
                        println("🔍 Loaded config object:")
                        println("   - appRepositoryUrl: '${foundConfig?.appRepositoryUrl}'")
                        println("   - bffRepositoryUrl: '${foundConfig?.bffRepositoryUrl}'")
                        return@forEach
                    }
                    println("   ❌ No match, continuing...")
                    println()
                }
            }
            
            if (foundConfig == null) {
                println("⚠️ No GitHub config found for projectId: '$projectId'")
            }
            
            Result.success(foundConfig)
        } catch (e: Exception) {
            println("❌ Failed to get GitHub config from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun updateGitHubConfig(config: GitHubConfig): Result<GitHubConfig> {
        return saveGitHubConfig(config)
    }
    
    override suspend fun deleteGitHubConfig(configId: String): Result<Unit> {
        return try {
            val docRef = githubConfigsCollection.doc(configId)
            docRef.delete().await()
            
            println("✅ GitHub config deleted from Firebase: $configId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            println("❌ Failed to delete GitHub config from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
} 