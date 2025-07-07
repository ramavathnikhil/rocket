package com.rapido.rocket.repository

import com.rapido.rocket.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Common implementation of GitHubRepository for all platforms
 * This handles all HTTP API calls to GitHub and Firebase Functions
 */
class GitHubRepositoryImpl(
    private val configRepository: GitHubConfigRepository
) : GitHubRepository {
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    // Firebase Functions endpoints
    private fun getApiEndpoint(functionName: String): String {
        return when (functionName) {
            "validateGitHubToken" -> "https://us-central1-rideon-edd12.cloudfunctions.net/rocketValidateGitHubToken"
            "validateGitHubRepository" -> "https://us-central1-rideon-edd12.cloudfunctions.net/rocketValidateGitHubRepository"
            "createGitHubPullRequest" -> "https://us-central1-rideon-edd12.cloudfunctions.net/rocketCreateGitHubPullRequest"
            "getGitHubPullRequest" -> "https://us-central1-rideon-edd12.cloudfunctions.net/rocketGetGitHubPullRequest"
            "triggerGitHubAction" -> "https://us-central1-rideon-edd12.cloudfunctions.net/rocketTriggerGitHubAction"
            else -> throw IllegalArgumentException("Unknown function: $functionName")
        }
    }
    
    override suspend fun createPullRequest(
        repositoryUrl: String,
        token: String,
        request: CreatePullRequestRequest
    ): Result<GitHubPullRequest> {
        return try {
            println("🚀 Creating PR via HTTP client: ${request.title}")
            println("🔍 Repository URL: $repositoryUrl")
            
            val apiEndpoint = getApiEndpoint("createGitHubPullRequest")
            val httpRequest = CreatePullRequestHttpRequest(
                repositoryUrl = repositoryUrl,
                token = token,
                title = request.title,
                body = request.body,
                head = request.head,
                base = request.base
            )
            
            println("🌐 Making HTTP request to: $apiEndpoint")
            
            val response = httpClient.post(apiEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(httpRequest)
            }
            
            println("📡 Response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<String>()
                println("📝 Response body: $responseBody")
                
                val responseJson = Json.parseToJsonElement(responseBody).jsonObject
                val success = responseJson["success"]?.jsonPrimitive?.booleanOrNull ?: false
                
                if (success) {
                    val prData = responseJson["pullRequest"]?.jsonObject
                    if (prData != null) {
                        val prMap = mutableMapOf<String, Any>()
                        prData.forEach { (key, value) ->
                            when {
                                value is kotlinx.serialization.json.JsonPrimitive -> {
                                    prMap[key] = value.content
                                }
                                value is kotlinx.serialization.json.JsonObject -> {
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
                        println("✅ GitHub PR created successfully: ${pullRequest.htmlUrl}")
                        Result.success(pullRequest)
                    } else {
                        Result.failure(Exception("No pull request data in response"))
                    }
                } else {
                    val error = responseJson["error"]?.jsonPrimitive?.content ?: "Unknown error creating PR"
                    Result.failure(Exception(error))
                }
            } else {
                val errorBody = response.body<String>()
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Error creating PR: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getPullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int
    ): Result<GitHubPullRequest> {
        return try {
            println("📥 Getting PR #$pullNumber: $repositoryUrl")
            
            val apiEndpoint = getApiEndpoint("getGitHubPullRequest")
            val httpRequest = GetPullRequestHttpRequest(
                repositoryUrl = repositoryUrl,
                token = token,
                pullNumber = pullNumber
            )
            
            val response = httpClient.post(apiEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(httpRequest)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<String>()
                val responseJson = Json.parseToJsonElement(responseBody).jsonObject
                val success = responseJson["success"]?.jsonPrimitive?.booleanOrNull ?: false
                
                if (success) {
                    val prData = responseJson["pullRequest"]?.jsonObject
                    if (prData != null) {
                        val prMap = mutableMapOf<String, Any>()
                        prData.forEach { (key, value) ->
                            when {
                                value is kotlinx.serialization.json.JsonPrimitive -> {
                                    prMap[key] = value.content
                                }
                                value is kotlinx.serialization.json.JsonObject -> {
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
                        Result.success(pullRequest)
                    } else {
                        Result.failure(Exception("No pull request data in response"))
                    }
                } else {
                    val error = responseJson["error"]?.jsonPrimitive?.content ?: "Unknown error getting PR"
                    Result.failure(Exception(error))
                }
            } else {
                val errorBody = response.body<String>()
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Error getting PR: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun mergePullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int,
        mergeMethod: String
    ): Result<GitHubPullRequest> {
        return Result.failure(Exception("PR merging should be done directly on GitHub. Use the 'View PR' button to open the PR in GitHub and merge it there."))
    }
    
    override suspend fun triggerGitHubAction(
        repositoryUrl: String,
        token: String,
        workflowId: String,
        ref: String,
        inputs: Map<String, String>
    ): Result<GitHubActionRun> {
        return try {
            println("🚀 Triggering GitHub Action:")
            println("   - Repository: $repositoryUrl")
            println("   - Workflow ID: $workflowId")
            println("   - Ref: $ref")
            println("   - Inputs: $inputs")
            
            val apiEndpoint = getApiEndpoint("triggerGitHubAction")
            val request = GitHubActionTriggerRequest(
                repositoryUrl = repositoryUrl,
                token = token,
                workflowId = workflowId,
                ref = ref,
                inputs = inputs
            )
            
            println("🌐 Making HTTP request to: $apiEndpoint")
            
            val response = httpClient.post(apiEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            println("📡 Response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<String>()
                println("📝 Response body: $responseBody")
                
                val responseJson = Json.parseToJsonElement(responseBody).jsonObject
                val success = responseJson["success"]?.jsonPrimitive?.booleanOrNull ?: false
                
                if (success) {
                    val actionRunData = responseJson["actionRun"]?.jsonObject
                    if (actionRunData != null) {
                        val actionRunMap = mutableMapOf<String, Any>()
                        actionRunData.forEach { (key, value) ->
                            when {
                                value is kotlinx.serialization.json.JsonPrimitive -> {
                                    val content = value.content
                                    actionRunMap[key] = content.toLongOrNull() ?: content.toIntOrNull() ?: content
                                }
                                else -> actionRunMap[key] = value.toString()
                            }
                        }
                        
                        val actionRun = GitHubActionRun.fromMap(actionRunMap)
                        println("✅ GitHub Action triggered successfully:")
                        println("   - Run ID: ${actionRun.id}")
                        println("   - Status: ${actionRun.status}")
                        println("   - HTML URL: ${actionRun.htmlUrl}")
                        
                        Result.success(actionRun)
                    } else {
                        val basicActionRun = GitHubActionRun(
                            id = 0,
                            runNumber = 0,
                            status = "queued",
                            htmlUrl = "",
                            workflowId = workflowId.toLongOrNull() ?: 0
                        )
                        Result.success(basicActionRun)
                    }
                } else {
                    val error = responseJson["error"]?.jsonPrimitive?.content ?: "Unknown error triggering GitHub Action"
                    Result.failure(Exception(error))
                }
            } else {
                val errorBody = response.body<String>()
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Error triggering GitHub Action: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun validateToken(token: String): Result<Boolean> {
        return try {
            println("🔑 Validating GitHub token...")
            
            val apiEndpoint = getApiEndpoint("validateGitHubToken")
            val httpRequest = ValidateTokenRequest(token = token)
            
            val response = httpClient.post(apiEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(httpRequest)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<String>()
                val responseJson = Json.parseToJsonElement(responseBody).jsonObject
                val isValid = responseJson["valid"]?.jsonPrimitive?.booleanOrNull ?: false
                
                if (isValid) {
                    val user = responseJson["user"]?.jsonObject
                    val username = user?.get("login")?.jsonPrimitive?.content ?: "unknown"
                    println("✅ Token valid for GitHub user: $username")
                }
                
                Result.success(isValid)
            } else {
                val errorBody = response.body<String>()
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Error validating token: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun validateRepository(repositoryUrl: String, token: String): Result<Boolean> {
        return try {
            println("📂 Validating GitHub repository: $repositoryUrl")
            
            val apiEndpoint = getApiEndpoint("validateGitHubRepository")
            val httpRequest = ValidateRepositoryRequest(
                repositoryUrl = repositoryUrl,
                token = token
            )
            
            val response = httpClient.post(apiEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(httpRequest)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<String>()
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
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Error validating repository: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Config operations delegate to platform-specific implementation
    override suspend fun saveGitHubConfig(config: GitHubConfig): Result<GitHubConfig> {
        return configRepository.saveGitHubConfig(config)
    }
    
    override suspend fun getGitHubConfig(projectId: String): Result<GitHubConfig?> {
        return configRepository.getGitHubConfig(projectId)
    }
    
    override suspend fun updateGitHubConfig(config: GitHubConfig): Result<GitHubConfig> {
        return configRepository.updateGitHubConfig(config)
    }
    
    override suspend fun deleteGitHubConfig(configId: String): Result<Unit> {
        return configRepository.deleteGitHubConfig(configId)
    }
}

/**
 * Interface for platform-specific GitHub config storage
 */
interface GitHubConfigRepository {
    suspend fun saveGitHubConfig(config: GitHubConfig): Result<GitHubConfig>
    suspend fun getGitHubConfig(projectId: String): Result<GitHubConfig?>
    suspend fun updateGitHubConfig(config: GitHubConfig): Result<GitHubConfig>
    suspend fun deleteGitHubConfig(configId: String): Result<Unit>
} 