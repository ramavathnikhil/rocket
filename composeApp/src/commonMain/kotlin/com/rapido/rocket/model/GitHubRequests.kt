package com.rapido.rocket.model

import kotlinx.serialization.Serializable

// HTTP Request Data Classes
@Serializable
data class GitHubActionTriggerRequest(
    val repositoryUrl: String,
    val token: String,
    val workflowId: String,
    val ref: String,
    val inputs: Map<String, String>
)

@Serializable
data class CreatePullRequestHttpRequest(
    val repositoryUrl: String,
    val token: String,
    val title: String,
    val body: String,
    val head: String,
    val base: String
)

@Serializable
data class GetPullRequestHttpRequest(
    val repositoryUrl: String,
    val token: String,
    val pullNumber: Int
)

@Serializable
data class ValidateTokenRequest(
    val token: String
)

@Serializable
data class ValidateRepositoryRequest(
    val repositoryUrl: String,
    val token: String
)

// HTTP Response Data Classes
@Serializable
data class GitHubApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

@Serializable
data class ValidationResponse(
    val valid: Boolean,
    val user: GitHubUser? = null,
    val repository: GitHubRepositoryInfo? = null,
    val error: String? = null
)

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class GitHubRepositoryInfo(
    val id: Long,
    val name: String,
    val fullName: String,
    val private: Boolean,
    val htmlUrl: String
) 