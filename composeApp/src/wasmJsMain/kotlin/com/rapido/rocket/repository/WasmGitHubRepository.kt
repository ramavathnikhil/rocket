package com.rapido.rocket.repository

import com.rapido.rocket.model.GitHubConfig
import com.rapido.rocket.model.GitHubPullRequest
import com.rapido.rocket.model.CreatePullRequestRequest
import com.rapido.rocket.util.currentTimeMillis
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull


class WasmGitHubRepository : GitHubRepository {
    
    private val firestore: FirebaseFirestore = Firebase.firestore()
    private val functions: FirebaseFunctions = Firebase.functions()
    private val githubConfigsCollection = firestore.collection("githubConfigs")
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    // Direct Firebase Functions URLs from rideon-edd12 project
    private fun getApiEndpoint(functionName: String): String {
        return when (functionName) {
            "validateGitHubToken" -> "https://us-central1-rideon-edd12.cloudfunctions.net/rocketValidateGitHubToken"
            "validateGitHubRepository" -> "https://us-central1-rideon-edd12.cloudfunctions.net/rocketValidateGitHubRepository"
            else -> throw IllegalArgumentException("Unknown function: $functionName")
        }
    }
    
    private suspend fun getAuthToken(): String? {
        return try {
            val auth = Firebase.auth()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.getIdToken(true).await().toString()
            } else {
                null
            }
        } catch (e: Exception) {
            println("❌ Error getting auth token: ${e.message}")
            null
        }
    }
    
    override suspend fun createPullRequest(
        repositoryUrl: String,
        token: String,
        request: CreatePullRequestRequest
    ): Result<GitHubPullRequest> {
        return try {
            println("🚀 Creating PR via Firebase Function: ${request.title}")
            
            val functionData = mapOf(
                "repositoryUrl" to repositoryUrl,
                "token" to token,
                "title" to request.title,
                "body" to request.body,
                "head" to request.head,
                "base" to request.base
            ).toJsObject()
            
            val createPRFunction = functions.httpsCallable("createGitHubPullRequest")
            val result = createPRFunction(functionData).await()
            
            val responseData = jsToMap(result.data!!)
            println("✅ Firebase Function response: $responseData")
            
            if (responseData["success"] == true) {
                val prData = responseData["pullRequest"] as? Map<String, Any> ?: emptyMap()
                val pullRequest = GitHubPullRequest.fromMap(prData)
                Result.success(pullRequest)
            } else {
                val errorMsg = responseData["error"] as? String ?: "Unknown error creating PR"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("❌ Firebase Function error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getPullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int
    ): Result<GitHubPullRequest> {
        return try {
            println("📥 Getting PR #$pullNumber via Firebase Function")
            
            val functionData = mapOf(
                "repositoryUrl" to repositoryUrl,
                "token" to token,
                "pullNumber" to pullNumber
            ).toJsObject()
            
            val getPRFunction = functions.httpsCallable("getGitHubPullRequest")
            val result = getPRFunction(functionData).await()
            
            val responseData = jsToMap(result.data!!)
            
            if (responseData["success"] == true) {
                val prData = responseData["pullRequest"] as? Map<String, Any> ?: emptyMap()
                val pullRequest = GitHubPullRequest.fromMap(prData)
                Result.success(pullRequest)
            } else {
                val errorMsg = responseData["error"] as? String ?: "Unknown error getting PR"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("❌ Firebase Function error: ${e.message}")
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
            println("🔀 Merging PR #$pullNumber via Firebase Function")
            
            val functionData = mapOf(
                "repositoryUrl" to repositoryUrl,
                "token" to token,
                "pullNumber" to pullNumber,
                "mergeMethod" to mergeMethod
            ).toJsObject()
            
            val mergePRFunction = functions.httpsCallable("mergeGitHubPullRequest")
            mergePRFunction(functionData).await()
            
            // After merging, get the updated PR details
            getPullRequest(repositoryUrl, token, pullNumber)
        } catch (e: Exception) {
            println("❌ Firebase Function error: ${e.message}")
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
            querySnapshot.forEach { doc: FirebaseDoc ->
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
    
    override suspend fun validateToken(token: String): Result<Boolean> {
        return try {
            println("🔑 Validating GitHub token via Ktor HTTP client...")
            
            val apiEndpoint = getApiEndpoint("validateGitHubToken")
            val requestBody = mapOf(
                "token" to token
            )
            
            println("🌐 Making request to: $apiEndpoint")
            
            val response = httpClient.post(apiEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            println("📡 Response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<String>()
                println("📝 Response body: $responseBody")
                
                // Parse the JSON response using kotlinx.serialization
                val responseJson = Json.parseToJsonElement(responseBody).jsonObject
                
                val isValid = responseJson["valid"]?.jsonPrimitive?.booleanOrNull ?: false
                
                if (isValid) {
                    val user = responseJson["user"]?.jsonObject
                    val username = user?.get("login")?.jsonPrimitive?.content ?: "unknown"
                    println("✅ Token valid for GitHub user: $username")
                } else {
                    val error = responseJson["error"]?.jsonPrimitive?.content
                    println("❌ Token validation failed: ${error ?: "Unknown error"}")
                }
                
                Result.success(isValid)
            } else {
                val errorBody = response.body<String>()
                println("❌ HTTP Error ${response.status.value}: $errorBody")
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Ktor HTTP client error: ${e.message}")
            println("❌ Error type: ${e::class.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    override suspend fun validateRepository(repositoryUrl: String, token: String): Result<Boolean> {
        return try {
            println("📂 Validating GitHub repository via Ktor HTTP client: $repositoryUrl")
            
            val apiEndpoint = getApiEndpoint("validateGitHubRepository")
            val requestBody = mapOf(
                "repositoryUrl" to repositoryUrl,
                "token" to token
            )
            
            println("🌐 Making request to: $apiEndpoint")
            
            val response = httpClient.post(apiEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            println("📡 Response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<String>()
                println("📝 Response body: $responseBody")
                
                // Parse the JSON response using kotlinx.serialization
                val responseJson = Json.parseToJsonElement(responseBody).jsonObject
                
                val isValid = responseJson["valid"]?.jsonPrimitive?.booleanOrNull ?: false
                
                if (isValid) {
                    val repoInfo = responseJson["repository"]?.jsonObject
                    val repoName = repoInfo?.get("fullName")?.jsonPrimitive?.content ?: repositoryUrl
                    println("✅ Repository accessible: $repoName")
                }
                
                Result.success(isValid)
            } else {
                val errorBody = response.body<String>()
                println("❌ HTTP Error ${response.status.value}: $errorBody")
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Ktor HTTP client error: ${e.message}")
            println("❌ Error type: ${e::class.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
} 