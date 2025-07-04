package com.rapido.rocket.model

data class GitHubConfig(
    val id: String = "",
    val projectId: String = "",
    val appRepositoryUrl: String = "", // e.g., "owner/app-repo"
    val bffRepositoryUrl: String = "", // e.g., "owner/bff-repo"
    val githubToken: String = "", // GitHub personal access token or app token
    val defaultBaseBranch: String = "develop",
    val defaultTargetBranch: String = "release",
    val workflowIds: Map<String, String> = emptyMap(), // Map of step type to workflow ID
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
            "workflowIds" to workflowIds,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): GitHubConfig {
            // Handle workflowIds properly - Firebase might store numbers as Number type or even as String
            val workflowIds = mutableMapOf<String, String>()
            val rawWorkflowIds = map["workflowIds"]
            
            println("🔍 DEBUG: Processing workflowIds from Firebase:")
            println("   - Raw value: '$rawWorkflowIds'")
            println("   - Type: ${rawWorkflowIds?.let { it::class.simpleName }}")
            
            when (rawWorkflowIds) {
                is Map<*, *> -> {
                    println("   - Processing as Map")
                    rawWorkflowIds.forEach { (key, value) ->
                        if (key is String) {
                            workflowIds[key] = value.toString()
                            println("     - Added: $key = ${value.toString()}")
                        }
                    }
                }
                is String -> {
                    println("   - Processing as String (need to parse)")
                    // Handle case where Firebase stored it as a string like "{SHARE_FUNCTIONAL_BUILD=172107435}"
                    if (rawWorkflowIds.startsWith("{") && rawWorkflowIds.endsWith("}")) {
                        val content = rawWorkflowIds.substring(1, rawWorkflowIds.length - 1)
                        if (content.isNotEmpty()) {
                            val entries = content.split(",").map { it.trim() }
                            entries.forEach { entry ->
                                val parts = entry.split("=")
                                if (parts.size == 2) {
                                    val key = parts[0].trim()
                                    val value = parts[1].trim()
                                    workflowIds[key] = value
                                    println("     - Parsed: $key = $value")
                                }
                            }
                        }
                    }
                }
                else -> {
                    println("   - Unknown type, skipping")
                }
            }
            
            println("   - Final workflowIds map: $workflowIds")
            
            return GitHubConfig(
                id = map["id"] as? String ?: "",
                projectId = map["projectId"] as? String ?: "",
                appRepositoryUrl = map["appRepositoryUrl"] as? String ?: "",
                bffRepositoryUrl = map["bffRepositoryUrl"] as? String ?: "",
                githubToken = map["githubToken"] as? String ?: "",
                defaultBaseBranch = map["defaultBaseBranch"] as? String ?: "develop",
                defaultTargetBranch = map["defaultTargetBranch"] as? String ?: "release",
                workflowIds = workflowIds,
                createdAt = map["createdAt"] as? Long ?: 0L,
                updatedAt = map["updatedAt"] as? Long ?: 0L
            )
        }
        
        // Helper method to get supported workflow step types
        fun getSupportedWorkflowStepTypes(): List<WorkflowStepType> {
            return listOf(
                WorkflowStepType("SHARE_FUNCTIONAL_BUILD", "Share Functional Build", "Staging Minified debug build"),
                WorkflowStepType("SHARE_REGRESSION_BUILD", "Share Regression Build", "Staging Release build"),
                WorkflowStepType("SHARE_PROD_REGRESSION_BUILD", "Share Prod Regression Build", "Production regression build from master"),
                WorkflowStepType("BUILD_STAGING", "Build Staging", "General staging build"),
                WorkflowStepType("BUILD_PRODUCTION", "Build Production", "General production build")
            )
        }
    }
}

// Data class to represent workflow step types that can be configured
data class WorkflowStepType(
    val key: String,
    val displayName: String,
    val description: String
)

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
                // Handle both camelCase and snake_case for htmlUrl
                htmlUrl = (map["htmlUrl"] as? String) ?: (map["html_url"] as? String) ?: "",
                headBranch = (map["head"] as? Map<String, Any>)?.get("ref") as? String ?: "",
                baseBranch = (map["base"] as? Map<String, Any>)?.get("ref") as? String ?: "",
                // Handle both camelCase and snake_case for timestamps
                createdAt = (map["createdAt"] as? String) ?: (map["created_at"] as? String) ?: "",
                updatedAt = (map["updatedAt"] as? String) ?: (map["updated_at"] as? String) ?: "",
                mergedAt = (map["mergedAt"] as? String) ?: (map["merged_at"] as? String)
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