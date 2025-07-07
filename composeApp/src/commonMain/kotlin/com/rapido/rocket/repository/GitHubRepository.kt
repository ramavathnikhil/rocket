package com.rapido.rocket.repository

import com.rapido.rocket.model.GitHubConfig
import com.rapido.rocket.model.GitHubPullRequest
import com.rapido.rocket.model.GitHubActionRun
import com.rapido.rocket.model.CreatePullRequestRequest

interface GitHubRepository {
    suspend fun createPullRequest(
        repositoryUrl: String,
        token: String,
        request: CreatePullRequestRequest
    ): Result<GitHubPullRequest>
    
    suspend fun getPullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int
    ): Result<GitHubPullRequest>
    
    suspend fun mergePullRequest(
        repositoryUrl: String,
        token: String,
        pullNumber: Int,
        mergeMethod: String = "merge" // "merge", "squash", "rebase"
    ): Result<GitHubPullRequest>
    
    suspend fun triggerGitHubAction(
        repositoryUrl: String,
        token: String,
        workflowId: String,
        ref: String = "release",
        inputs: Map<String, String> = emptyMap()
    ): Result<GitHubActionRun>
    
    suspend fun saveGitHubConfig(config: GitHubConfig): Result<GitHubConfig>
    suspend fun getGitHubConfig(projectId: String): Result<GitHubConfig?>
    suspend fun updateGitHubConfig(config: GitHubConfig): Result<GitHubConfig>
    suspend fun deleteGitHubConfig(configId: String): Result<Unit>
    
    suspend fun validateToken(token: String): Result<Boolean>
    suspend fun validateRepository(repositoryUrl: String, token: String): Result<Boolean>
} 