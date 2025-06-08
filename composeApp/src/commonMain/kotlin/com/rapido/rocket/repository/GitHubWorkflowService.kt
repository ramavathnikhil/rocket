package com.rapido.rocket.repository

import com.rapido.rocket.model.WorkflowStep
import com.rapido.rocket.model.GitHubConfig
import com.rapido.rocket.model.CreatePullRequestRequest
import com.rapido.rocket.model.Release

class GitHubWorkflowService(
    private val githubRepository: GitHubRepository,
    private val workflowRepository: WorkflowRepository
) {
    
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