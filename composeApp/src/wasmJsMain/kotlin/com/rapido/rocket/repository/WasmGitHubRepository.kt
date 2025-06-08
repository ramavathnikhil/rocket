package com.rapido.rocket.repository

import com.rapido.rocket.model.GitHubConfig
import com.rapido.rocket.model.GitHubPullRequest
import com.rapido.rocket.model.CreatePullRequestRequest
import com.rapido.rocket.util.currentTimeMillis
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// External functions for GitHub API calls
@JsName("createGitHubPR")
external fun createGitHubPR(
    repositoryUrl: String,
    token: String,
    title: String,
    body: String,
    head: String,
    base: String
): Promise<JsAny?>

@JsName("getGitHubPR")
external fun getGitHubPR(
    repositoryUrl: String,
    token: String,
    pullNumber: Int
): Promise<JsAny?>

@JsName("mergeGitHubPR")
external fun mergeGitHubPR(
    repositoryUrl: String,
    token: String,
    pullNumber: Int,
    mergeMethod: String
): Promise<JsAny?>

@JsName("validateGitHubToken")
external fun validateGitHubToken(token: String): Promise<JsAny?>

@JsName("validateGitHubRepo")
external fun validateGitHubRepo(repositoryUrl: String, token: String): Promise<JsAny?>

class WasmGitHubRepository : GitHubRepository {
    
    private val firestore: FirebaseFirestore = Firebase.firestore()
    private val githubConfigsCollection = firestore.collection("githubConfigs")
    
    override suspend fun createPullRequest(
        repositoryUrl: String,
        token: String,
        request: CreatePullRequestRequest
    ): Result<GitHubPullRequest> {
        return try {
            val result = createGitHubPR(
                repositoryUrl = repositoryUrl,
                token = token,
                title = request.title,
                body = request.body,
                head = request.head,
                base = request.base
            ).await()
            
            val prData = jsToMap(result!!)
            val pullRequest = GitHubPullRequest.fromMap(prData)
            Result.success(pullRequest)
        } catch (e: Exception) {
            println("❌ GitHub API error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getPullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int
    ): Result<GitHubPullRequest> {
        return try {
            val result = getGitHubPR(repositoryUrl, token, pullNumber).await()
            val prData = jsToMap(result!!)
            val pullRequest = GitHubPullRequest.fromMap(prData)
            Result.success(pullRequest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun mergePullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int,
        mergeMethod: String
    ): Result<GitHubPullRequest> {
        return try {
            mergeGitHubPR(repositoryUrl, token, pullNumber, mergeMethod).await()
            // After merge, get the updated PR data
            getPullRequest(repositoryUrl, token, pullNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveGitHubConfig(config: GitHubConfig): Result<GitHubConfig> {
        return try {
            val configWithId = if (config.id.isEmpty()) {
                config.copy(
                    id = "github_${currentTimeMillis()}_${(100..999).random()}",
                    createdAt = currentTimeMillis(),
                    updatedAt = currentTimeMillis()
                )
            } else {
                config.copy(updatedAt = currentTimeMillis())
            }
            
            val configDataMap = configWithId.toMap()
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
            val querySnapshot = githubConfigsCollection.get().await()
            var foundConfig: GitHubConfig? = null
            
            querySnapshot.forEach { doc ->
                if (doc.exists) {
                    val data = doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                    val configProjectId = data["projectId"] as? String ?: ""
                    
                    if (configProjectId == projectId) {
                        foundConfig = GitHubConfig.fromMap(data)
                        return@forEach
                    }
                }
            }
            
            Result.success(foundConfig)
            
        } catch (e: Exception) {
            println("❌ Failed to get GitHub config from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun updateGitHubConfig(config: GitHubConfig): Result<GitHubConfig> {
        return saveGitHubConfig(config) // Reuse save method which handles updates
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
    
    override suspend fun validateToken(token: String): Result<Boolean> {
        return try {
            val result = validateGitHubToken(token).await()
            val isValid = result as? Boolean ?: false
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun validateRepository(repositoryUrl: String, token: String): Result<Boolean> {
        return try {
            val result = validateGitHubRepo(repositoryUrl, token).await()
            val isValid = result as? Boolean ?: false
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 