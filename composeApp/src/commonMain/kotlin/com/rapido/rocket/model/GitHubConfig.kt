package com.rapido.rocket.model

data class GitHubConfig(
    val id: String = "",
    val projectId: String = "",
    val appRepositoryUrl: String = "", // e.g., "owner/app-repo"
    val bffRepositoryUrl: String = "", // e.g., "owner/bff-repo"
    val githubToken: String = "", // GitHub personal access token or app token
    val defaultBaseBranch: String = "develop",
    val defaultTargetBranch: String = "release",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "projectId" to projectId,
            "appRepositoryUrl" to appRepositoryUrl,
            "bffRepositoryUrl" to bffRepositoryUrl,
            "githubToken" to githubToken,
            "defaultBaseBranch" to defaultBaseBranch,
            "defaultTargetBranch" to defaultTargetBranch,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): GitHubConfig {
            return GitHubConfig(
                id = map["id"] as? String ?: "",
                projectId = map["projectId"] as? String ?: "",
                appRepositoryUrl = map["appRepositoryUrl"] as? String ?: "",
                bffRepositoryUrl = map["bffRepositoryUrl"] as? String ?: "",
                githubToken = map["githubToken"] as? String ?: "",
                defaultBaseBranch = map["defaultBaseBranch"] as? String ?: "develop",
                defaultTargetBranch = map["defaultTargetBranch"] as? String ?: "release",
                createdAt = map["createdAt"] as? Long ?: 0L,
                updatedAt = map["updatedAt"] as? Long ?: 0L
            )
        }
    }
}

data class GitHubPullRequest(
    val id: Int = 0,
    val number: Int = 0,
    val title: String = "",
    val body: String = "",
    val state: String = "", // "open", "closed", "merged"
    val htmlUrl: String = "",
    val headBranch: String = "",
    val baseBranch: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val mergedAt: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): GitHubPullRequest {
            return GitHubPullRequest(
                id = (map["id"] as? Number)?.toInt() ?: 0,
                number = (map["number"] as? Number)?.toInt() ?: 0,
                title = map["title"] as? String ?: "",
                body = map["body"] as? String ?: "",
                state = map["state"] as? String ?: "",
                htmlUrl = map["html_url"] as? String ?: "",
                headBranch = (map["head"] as? Map<String, Any>)?.get("ref") as? String ?: "",
                baseBranch = (map["base"] as? Map<String, Any>)?.get("ref") as? String ?: "",
                createdAt = map["created_at"] as? String ?: "",
                updatedAt = map["updated_at"] as? String ?: "",
                mergedAt = map["merged_at"] as? String
            )
        }
    }
}

data class CreatePullRequestRequest(
    val title: String,
    val body: String,
    val head: String, // source branch
    val base: String, // target branch
    val draft: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "body" to body,
            "head" to head,
            "base" to base,
            "draft" to draft
        )
    }
} 