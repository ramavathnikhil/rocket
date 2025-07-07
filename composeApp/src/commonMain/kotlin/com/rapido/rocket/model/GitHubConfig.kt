package com.rapido.rocket.model

data class GitHubConfig(
    val id: String = "",
    val projectId: String = "",
    val appRepositoryUrl: String = "", // e.g., "owner/app-repo"
    val bffRepositoryUrl: String = "", // e.g., "owner/bff-repo"
    val githubToken: String = "", // GitHub personal access token or app token
    val defaultBaseBranch: String = "develop",
    val defaultTargetBranch: String = "release",
    val workflowUrls: Map<String, String> = emptyMap(), // Map of step type to workflow URL with inputs
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
            "workflowUrls" to workflowUrls,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): GitHubConfig {
            // Handle workflowUrls properly - Firebase might store this as a Map or String
            val workflowUrls = mutableMapOf<String, String>()
            val rawWorkflowUrls = map["workflowUrls"]
            
            println("🔍 DEBUG: Processing workflowUrls from Firebase:")
            println("   - Raw value: '$rawWorkflowUrls'")
            println("   - Type: ${rawWorkflowUrls?.let { it::class.simpleName }}")
            
            when (rawWorkflowUrls) {
                is Map<*, *> -> {
                    println("   - Processing as Map")
                    rawWorkflowUrls.forEach { (key, value) ->
                        if (key is String) {
                            workflowUrls[key] = value.toString()
                            println("     - Added: $key = ${value.toString()}")
                        }
                    }
                }
                is String -> {
                    println("   - Processing as String (need to parse)")
                    // Handle case where Firebase stored it as a string
                    if (rawWorkflowUrls.startsWith("{") && rawWorkflowUrls.endsWith("}")) {
                        val content = rawWorkflowUrls.substring(1, rawWorkflowUrls.length - 1)
                        if (content.isNotEmpty()) {
                            val entries = content.split(",").map { it.trim() }
                            entries.forEach { entry ->
                                val parts = entry.split("=", limit = 2)
                                if (parts.size == 2) {
                                    val key = parts[0].trim()
                                    val value = parts[1].trim()
                                    workflowUrls[key] = value
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
            
            println("   - Final workflowUrls map: $workflowUrls")
            
            return GitHubConfig(
                id = map["id"] as? String ?: "",
                projectId = map["projectId"] as? String ?: "",
                appRepositoryUrl = map["appRepositoryUrl"] as? String ?: "",
                bffRepositoryUrl = map["bffRepositoryUrl"] as? String ?: "",
                githubToken = map["githubToken"] as? String ?: "",
                defaultBaseBranch = map["defaultBaseBranch"] as? String ?: "develop",
                defaultTargetBranch = map["defaultTargetBranch"] as? String ?: "release",
                workflowUrls = workflowUrls,
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

// Data class to represent parsed workflow URL information
data class WorkflowUrlInfo(
    val repositoryUrl: String, // e.g., "owner/repo"
    val workflowId: String,
    val inputs: Map<String, String>
) {
    companion object {
        /**
         * Parse GitHub-style workflow URL
         * Expected format: https://github.com/owner/repo/actions/workflows/123456789/dispatches?param1=value1&param2=value2
         * Or simplified: owner/repo/123456789?param1=value1&param2=value2
         */
        fun fromUrl(url: String): WorkflowUrlInfo? {
            return try {
                val cleanUrl = url.trim()
                println("🔍 Parsing workflow URL: '$cleanUrl'")
                
                // Handle both full GitHub URLs and simplified format
                val workingUrl = if (cleanUrl.startsWith("https://github.com/")) {
                    // Extract from full GitHub URL
                    // https://github.com/owner/repo/actions/workflows/123456789/dispatches?param1=value1
                    val afterGithub = cleanUrl.removePrefix("https://github.com/")
                    val parts = afterGithub.split("/actions/workflows/")
                    if (parts.size != 2) return null
                    
                    val repositoryUrl = parts[0]
                    val workflowPart = parts[1].removeSuffix("/dispatches")
                    
                    // Split workflow ID and params
                    val workflowParts = workflowPart.split("?", limit = 2)
                    val workflowId = workflowParts[0]
                    val params = if (workflowParts.size > 1) workflowParts[1] else ""
                    
                    "$repositoryUrl/$workflowId?$params"
                } else {
                    // Assume simplified format: owner/repo/123456789?param1=value1
                    cleanUrl
                }
                
                println("   - Working URL: '$workingUrl'")
                
                // Parse simplified format: owner/repo/123456789?param1=value1&param2=value2
                val mainParts = workingUrl.split("?", limit = 2)
                val pathPart = mainParts[0]
                val queryPart = if (mainParts.size > 1) mainParts[1] else ""
                
                // Extract repository and workflow ID from path
                val pathSegments = pathPart.split("/")
                if (pathSegments.size < 3) return null
                
                val repositoryUrl = "${pathSegments[0]}/${pathSegments[1]}"
                val workflowId = pathSegments[2]
                
                println("   - Repository: '$repositoryUrl'")
                println("   - Workflow ID: '$workflowId'")
                
                // Parse query parameters
                val inputs = mutableMapOf<String, String>()
                if (queryPart.isNotEmpty()) {
                    val params = queryPart.split("&")
                    params.forEach { param ->
                        val paramParts = param.split("=", limit = 2)
                        if (paramParts.size == 2) {
                            val key = paramParts[0].trim()
                            val value = paramParts[1].trim()
                            inputs[key] = value
                            println("   - Input: $key = $value")
                        }
                    }
                }
                
                WorkflowUrlInfo(repositoryUrl, workflowId, inputs)
            } catch (e: Exception) {
                println("❌ Failed to parse workflow URL: ${e.message}")
                null
            }
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

data class GitHubActionRun(
    val id: Long = 0,
    val runNumber: Int = 0,
    val status: String = "", // "queued", "in_progress", "completed"
    val conclusion: String? = null, // "success", "failure", "cancelled", etc.
    val htmlUrl: String = "",
    val workflowId: Long = 0,
    val headBranch: String = "",
    val headSha: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val triggeredBy: String = ""
) {
    companion object {
        fun fromMap(map: Map<String, Any>): GitHubActionRun {
            return GitHubActionRun(
                id = (map["id"] as? Number)?.toLong() ?: 0,
                runNumber = (map["runNumber"] as? Number)?.toInt() ?: 0,
                status = map["status"] as? String ?: "",
                conclusion = map["conclusion"] as? String,
                htmlUrl = (map["htmlUrl"] as? String) ?: (map["html_url"] as? String) ?: "",
                workflowId = (map["workflowId"] as? Number)?.toLong() ?: 0,
                headBranch = (map["headBranch"] as? String) ?: (map["head_branch"] as? String) ?: "",
                headSha = (map["headSha"] as? String) ?: (map["head_sha"] as? String) ?: "",
                createdAt = (map["createdAt"] as? String) ?: (map["created_at"] as? String) ?: "",
                updatedAt = (map["updatedAt"] as? String) ?: (map["updated_at"] as? String) ?: "",
                triggeredBy = map["triggeredBy"] as? String ?: ""
            )
        }
    }
} 