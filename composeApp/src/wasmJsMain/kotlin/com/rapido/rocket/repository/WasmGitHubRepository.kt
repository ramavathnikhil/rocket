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
            "createGitHubPullRequest" -> "https://us-central1-rideon-edd12.cloudfunctions.net/rocketCreateGitHubPullRequest"
            "getGitHubPullRequest" -> "https://us-central1-rideon-edd12.cloudfunctions.net/rocketGetGitHubPullRequest"
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
            println("🚀 Creating PR via HTTP client instead of Firebase Functions: ${request.title}")
            println("🔍 Repository URL: $repositoryUrl")
            println("🔍 Token length: ${token.length}")
            
            // Use direct HTTP call instead of Firebase Functions to avoid JS exceptions
            val apiEndpoint = "https://us-central1-rideon-edd12.cloudfunctions.net/rocketCreateGitHubPullRequest"
            val requestBody = mapOf(
                "repositoryUrl" to repositoryUrl,
                "token" to token,
                "title" to request.title,
                "body" to request.body,
                "head" to request.head,
                "base" to request.base
            )
            
            println("🌐 Making HTTP request to: $apiEndpoint")
            
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
                
                val success = responseJson["success"]?.jsonPrimitive?.booleanOrNull ?: false
                
                if (success) {
                    val prData = responseJson["pullRequest"]?.jsonObject
                    if (prData != null) {
                        // Convert JsonObject to Map for GitHubPullRequest.fromMap
                        val prMap = mutableMapOf<String, Any>()
                        prData.forEach { (key, value) ->
                            when {
                                value is kotlinx.serialization.json.JsonPrimitive -> {
                                    if (value.isString) {
                                        prMap[key] = value.content
                                    } else {
                                        prMap[key] = value.content
                                    }
                                }
                                value is kotlinx.serialization.json.JsonObject -> {
                                    // Handle nested objects like head, base, user
                                    val nestedMap = mutableMapOf<String, Any>()
                                    value.forEach { (nestedKey, nestedValue) ->
                                        when {
                                            nestedValue is kotlinx.serialization.json.JsonPrimitive -> {
                                                nestedMap[nestedKey] = nestedValue.content
                                            }
                                            else -> nestedMap[nestedKey] = nestedValue.toString()
                                        }
                                    }
                                    prMap[key] = nestedMap
                                }
                                else -> prMap[key] = value.toString()
                            }
                        }
                        
                        println("🔍 prMap before GitHubPullRequest.fromMap:")
                        prMap.forEach { (key, value) ->
                            println("   $key: '$value' (${value::class.simpleName})")
                        }
                        
                        val pullRequest = GitHubPullRequest.fromMap(prMap)
                        println("🔍 GitHubPullRequest after fromMap:")
                        println("   number: ${pullRequest.number}")
                        println("   htmlUrl: '${pullRequest.htmlUrl}'")
                        println("   state: '${pullRequest.state}'")
                        
                        println("✅ GitHub PR created successfully: ${pullRequest.htmlUrl}")
                        Result.success(pullRequest)
                    } else {
                        Result.failure(Exception("No pull request data in response"))
                    }
                } else {
                    val error = responseJson["error"]?.jsonPrimitive?.content ?: "Unknown error creating PR"
                    println("❌ PR creation failed: $error")
                    Result.failure(Exception(error))
                }
            } else {
                val errorBody = response.body<String>()
                println("❌ HTTP Error ${response.status.value}: $errorBody")
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Error creating PR: ${e.message}")
            println("❌ Error type: ${e::class.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    override suspend fun getPullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int
    ): Result<GitHubPullRequest> {
        return try {
            println("📥 Getting PR #$pullNumber via HTTP client: $repositoryUrl")
            
            val apiEndpoint = "https://us-central1-rideon-edd12.cloudfunctions.net/rocketGetGitHubPullRequest"
            val requestBody = mapOf(
                "repositoryUrl" to repositoryUrl,
                "token" to token,
                "pullNumber" to pullNumber
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
                
                val success = responseJson["success"]?.jsonPrimitive?.booleanOrNull ?: false
                
                if (success) {
                    val prData = responseJson["pullRequest"]?.jsonObject
                    if (prData != null) {
                        // Convert JsonObject to Map for GitHubPullRequest.fromMap
                        val prMap = mutableMapOf<String, Any>()
                        prData.forEach { (key, value) ->
                            when {
                                value is kotlinx.serialization.json.JsonPrimitive -> {
                                    if (value.isString) {
                                        prMap[key] = value.content
                                    } else {
                                        prMap[key] = value.content
                                    }
                                }
                                value is kotlinx.serialization.json.JsonObject -> {
                                    // Handle nested objects like head, base, user
                                    val nestedMap = mutableMapOf<String, Any>()
                                    value.forEach { (nestedKey, nestedValue) ->
                                        when {
                                            nestedValue is kotlinx.serialization.json.JsonPrimitive -> {
                                                nestedMap[nestedKey] = nestedValue.content
                                            }
                                            else -> nestedMap[nestedKey] = nestedValue.toString()
                                        }
                                    }
                                    prMap[key] = nestedMap
                                }
                                else -> prMap[key] = value.toString()
                            }
                        }
                        
                        val pullRequest = GitHubPullRequest.fromMap(prMap)
                        println("✅ GitHub PR retrieved successfully: ${pullRequest.htmlUrl}")
                        Result.success(pullRequest)
                    } else {
                        Result.failure(Exception("No pull request data in response"))
                    }
                } else {
                    val error = responseJson["error"]?.jsonPrimitive?.content ?: "Unknown error getting PR"
                    println("❌ PR retrieval failed: $error")
                    Result.failure(Exception(error))
                }
            } else {
                val errorBody = response.body<String>()
                println("❌ HTTP Error ${response.status.value}: $errorBody")
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Error getting PR: ${e.message}")
            println("❌ Error type: ${e::class.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    override suspend fun mergePullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int,
        mergeMethod: String
    ): Result<GitHubPullRequest> {
        // We don't implement merge via API - users should merge directly on GitHub
        return Result.failure(Exception("PR merging should be done directly on GitHub. Use the 'View PR' button to open the PR in GitHub and merge it there."))
    }
    
    override suspend fun saveGitHubConfig(config: GitHubConfig): Result<GitHubConfig> {
        return try {
            println("🔍 Saving GitHub config:")
            println("   - projectId: '${config.projectId}'")
            println("   - appRepositoryUrl: '${config.appRepositoryUrl}'")
            println("   - bffRepositoryUrl: '${config.bffRepositoryUrl}'")
            println("   - githubToken length: ${config.githubToken.length}")
            
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
            println("🔍 Config data map before saving:")
            configDataMap.forEach { (key, value) ->
                println("   - $key: '$value'")
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