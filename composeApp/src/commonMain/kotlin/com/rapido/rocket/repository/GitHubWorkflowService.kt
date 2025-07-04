package com.rapido.rocket.repository

import com.rapido.rocket.model.WorkflowStep
import com.rapido.rocket.model.GitHubConfig
import com.rapido.rocket.model.CreatePullRequestRequest
import com.rapido.rocket.model.Release
import com.rapido.rocket.model.StepType

class GitHubWorkflowService(
    private val githubRepository: GitHubRepository,
    private val workflowRepository: WorkflowRepository
) {
    
    /**
     * Check if this is a build step that can trigger GitHub Actions
     */
    fun isBuildStep(step: WorkflowStep): Boolean {
        return when (step.type) {
            StepType.SHARE_FUNCTIONAL_BUILD,
            StepType.SHARE_REGRESSION_BUILD,
            StepType.SHARE_PROD_REGRESSION_BUILD,
            StepType.BUILD_STAGING,
            StepType.BUILD_PRODUCTION -> true
            else -> false
        }
    }
    
    /**
     * Get workflow ID for a specific step type from GitHub config
     */
    fun getWorkflowIdForStep(step: WorkflowStep, githubConfig: GitHubConfig): String? {
        return githubConfig.workflowIds[step.type.name]
    }
    
    /**
     * Check if workflow is configured for a specific step
     */
    fun isWorkflowConfiguredForStep(step: WorkflowStep, githubConfig: GitHubConfig): Boolean {
        return isBuildStep(step) && getWorkflowIdForStep(step, githubConfig) != null
    }
    
    /**
     * Check if this is a develop to release PR step (step 2 or 3)
     */
    fun isDevelopToReleasePRStep(step: WorkflowStep): Boolean {
        return (step.stepNumber == 2 || step.stepNumber == 3) && 
               step.repositoryType.isNotEmpty() &&
               step.sourceBranch == "develop" &&
               step.targetBranch == "release"
    }
    
    /**
     * Create PR specifically for develop to release steps (step 2 and 3)
     */
    suspend fun createDevelopToReleasePR(
        step: WorkflowStep,
        release: Release,
        githubConfig: GitHubConfig
    ): Result<WorkflowStep> {
        return try {
            // Validate this is a develop to release step
            if (!isDevelopToReleasePRStep(step)) {
                return Result.failure(Exception("This method is only for develop to release PR steps (step 2 and 3)"))
            }
            
            // Determine which repository to use
            val repositoryUrl = when (step.repositoryType) {
                "app" -> githubConfig.appRepositoryUrl
                "bff" -> githubConfig.bffRepositoryUrl
                else -> return Result.failure(Exception("Unknown repository type: ${step.repositoryType}"))
            }
            
            println("🔍 Repository URL resolution:")
            println("   - Step repository type: '${step.repositoryType}'")
            println("   - GitHub config app URL: '${githubConfig.appRepositoryUrl}'")
            println("   - GitHub config bff URL: '${githubConfig.bffRepositoryUrl}'")
            println("   - Resolved repository URL: '$repositoryUrl'")
            
            if (repositoryUrl.isEmpty()) {
                return Result.failure(Exception("${step.repositoryType.uppercase()} repository URL not configured in GitHub settings"))
            }
            
            // Create PR title and body
            val prTitle = "Release ${release.version}: ${step.repositoryType.uppercase()} develop to release"
            val prBody = buildString {
                append("## Release ${release.version}: ${release.title}\n\n")
                append("**Repository:** ${step.repositoryType.uppercase()}\n")
                append("**Branches:** develop → release\n")
                append("**Release Description:** ${release.description}\n\n")
                if (release.notes.isNotEmpty()) {
                    append("**Release Notes:**\n${release.notes}\n\n")
                }
                append("---\n")
                append("**Workflow Step:** ${step.stepNumber} - ${step.title}\n")
                append("**Step Description:** ${step.description}\n\n")
                append("_This PR was created via Rapido Rocket release management workflow._")
            }
            
            val createPrRequest = CreatePullRequestRequest(
                title = prTitle,
                body = prBody,
                head = "develop", // Always develop for these steps
                base = "release", // Always release for these steps
                draft = false
            )
            
            // Create the PR
            val prResult = githubRepository.createPullRequest(
                repositoryUrl = repositoryUrl,
                token = githubConfig.githubToken,
                request = createPrRequest
            )
            
            prResult.fold(
                onSuccess = { pr ->
                    // Update the step with PR information
                    val updatedStep = step.copy(
                        githubPrNumber = pr.number,
                        githubPrUrl = pr.htmlUrl,
                        githubPrState = pr.state,
                        actionUrl = pr.htmlUrl
                    )
                    
                    println("🔍 GITHUB WORKFLOW SERVICE - About to save step:")
                    println("   Step ID: '${updatedStep.id}'")
                    println("   Step Number: ${updatedStep.stepNumber}")
                    println("   Title: '${updatedStep.title}'")
                    println("   GitHub PR Number: ${updatedStep.githubPrNumber}")
                    println("   GitHub PR URL: '${updatedStep.githubPrUrl}'")
                    println("   GitHub PR State: '${updatedStep.githubPrState}'")
                    println("   Repository Type: '${updatedStep.repositoryType}'")
                    println("   Source Branch: '${updatedStep.sourceBranch}'")
                    println("   Target Branch: '${updatedStep.targetBranch}'")
                    
                    // Save the updated step
                    val saveResult = workflowRepository.updateWorkflowStep(updatedStep)
                    saveResult.fold(
                        onSuccess = { savedStep ->
                            println("✅ PR created and step updated: ${pr.htmlUrl}")
                            Result.success(savedStep)
                        },
                        onFailure = { error ->
                            println("⚠️ PR created but failed to update step: ${error.message}")
                            // Return the updated step even if save failed
                            Result.success(updatedStep)
                        }
                    )
                },
                onFailure = { error ->
                    Result.failure(Exception("Failed to create PR: ${error.message}"))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createPullRequestForStep(
        step: WorkflowStep,
        release: Release,
        githubConfig: GitHubConfig
    ): Result<WorkflowStep> {
        return try {
            // Determine which repository to use
            val repositoryUrl = when (step.repositoryType) {
                "app" -> githubConfig.appRepositoryUrl
                "bff" -> githubConfig.bffRepositoryUrl
                else -> return Result.failure(Exception("Unknown repository type: ${step.repositoryType}"))
            }
            
            if (repositoryUrl.isEmpty()) {
                return Result.failure(Exception("Repository URL not configured for ${step.repositoryType}"))
            }
            
            // Create PR title and body
            val prTitle = "${release.version}: ${step.title}"
            val prBody = buildString {
                append("## ${release.title}\n\n")
                append("**Release Version:** ${release.version}\n")
                append("**Description:** ${release.description}\n\n")
                append("**Workflow Step:** ${step.stepNumber} - ${step.title}\n")
                append("**Step Description:** ${step.description}\n\n")
                if (release.notes.isNotEmpty()) {
                    append("**Release Notes:**\n${release.notes}\n\n")
                }
                append("---\n")
                append("_This PR was automatically created by Rapido Rocket release management._")
            }
            
            // Use configured branches or step defaults
            val sourceBranch = step.sourceBranch.ifEmpty { githubConfig.defaultBaseBranch }
            val targetBranch = step.targetBranch.ifEmpty { githubConfig.defaultTargetBranch }
            
            val createPrRequest = CreatePullRequestRequest(
                title = prTitle,
                body = prBody,
                head = sourceBranch,
                base = targetBranch,
                draft = false
            )
            
            // Create the PR
            val prResult = githubRepository.createPullRequest(
                repositoryUrl = repositoryUrl,
                token = githubConfig.githubToken,
                request = createPrRequest
            )
            
            prResult.fold(
                onSuccess = { pr ->
                    // Update the step with PR information
                    val updatedStep = step.copy(
                        githubPrNumber = pr.number,
                        githubPrUrl = pr.htmlUrl,
                        githubPrState = pr.state,
                        actionUrl = pr.htmlUrl
                    )
                    
                    // Save the updated step
                    workflowRepository.updateWorkflowStep(updatedStep)
                },
                onFailure = { error ->
                    Result.failure(Exception("Failed to create PR: ${error.message}"))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun checkPullRequestStatus(
        step: WorkflowStep,
        githubConfig: GitHubConfig
    ): Result<WorkflowStep> {
        return try {
            if (step.githubPrNumber == null || step.repositoryType.isEmpty()) {
                return Result.failure(Exception("No PR information available for this step"))
            }
            
            val repositoryUrl = when (step.repositoryType) {
                "app" -> githubConfig.appRepositoryUrl
                "bff" -> githubConfig.bffRepositoryUrl
                else -> return Result.failure(Exception("Unknown repository type: ${step.repositoryType}"))
            }
            
            val prResult = githubRepository.getPullRequest(
                repositoryUrl = repositoryUrl,
                token = githubConfig.githubToken,
                pullNumber = step.githubPrNumber!!
            )
            
            prResult.fold(
                onSuccess = { pr ->
                    val updatedStep = step.copy(
                        githubPrState = pr.state,
                        githubPrUrl = pr.htmlUrl
                    )
                    workflowRepository.updateWorkflowStep(updatedStep)
                },
                onFailure = { error ->
                    Result.failure(Exception("Failed to check PR status: ${error.message}"))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun mergePullRequest(
        step: WorkflowStep,
        githubConfig: GitHubConfig,
        mergeMethod: String = "merge"
    ): Result<WorkflowStep> {
        return try {
            if (step.githubPrNumber == null || step.repositoryType.isEmpty()) {
                return Result.failure(Exception("No PR information available for this step"))
            }
            
            val repositoryUrl = when (step.repositoryType) {
                "app" -> githubConfig.appRepositoryUrl
                "bff" -> githubConfig.bffRepositoryUrl
                else -> return Result.failure(Exception("Unknown repository type: ${step.repositoryType}"))
            }
            
            val mergeResult = githubRepository.mergePullRequest(
                repositoryUrl = repositoryUrl,
                token = githubConfig.githubToken,
                pullNumber = step.githubPrNumber!!,
                mergeMethod = mergeMethod
            )
            
            mergeResult.fold(
                onSuccess = { pr ->
                    val updatedStep = step.copy(
                        githubPrState = pr.state
                    )
                    workflowRepository.updateWorkflowStep(updatedStep)
                },
                onFailure = { error ->
                    Result.failure(Exception("Failed to merge PR: ${error.message}"))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isGitHubStep(step: WorkflowStep): Boolean {
        return step.repositoryType.isNotEmpty() && 
               (step.type.name.contains("PR") || step.type == com.rapido.rocket.model.StepType.CREATE_PR_BFF)
    }
} 