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
     * Get workflow URL info for a specific step type from GitHub config
     */
    fun getWorkflowUrlInfoForStep(step: WorkflowStep, githubConfig: GitHubConfig): com.rapido.rocket.model.WorkflowUrlInfo? {
        val workflowUrl = githubConfig.workflowUrls[step.type.name]
        return workflowUrl?.let { com.rapido.rocket.model.WorkflowUrlInfo.fromUrl(it) }
    }
    
    /**
     * Check if workflow is configured for a specific step
     */
    fun isWorkflowConfiguredForStep(step: WorkflowStep, githubConfig: GitHubConfig): Boolean {
        return isBuildStep(step) && getWorkflowUrlInfoForStep(step, githubConfig) != null
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
    
    /**
     * Process workflow inputs and replace placeholders with actual values
     */
    fun processWorkflowInputs(
        rawInputs: Map<String, String>,
        step: WorkflowStep,
        release: Release
    ): Map<String, String> {
        val processedInputs = mutableMapOf<String, String>()
        
        rawInputs.forEach { (key, value) ->
            // Replace placeholders with actual values
            val processedValue = value
                .replace("{{release.version}}", release.version)
                .replace("{{release.title}}", release.title)
                .replace("{{step.type}}", step.type.name)
                .replace("{{step.title}}", step.title)
                .replace("{{step.sourceBranch}}", step.sourceBranch)
                .replace("{{step.targetBranch}}", step.targetBranch)
                
            processedInputs[key] = processedValue
        }
        
        return processedInputs
    }
    
    /**
     * Trigger GitHub Action for a build step
     */
    suspend fun triggerBuildAction(
        step: WorkflowStep,
        release: Release,
        githubConfig: GitHubConfig,
        ref: String = "release"
    ): Result<WorkflowStep> {
        return try {
            // Validate this is a build step
            if (!isBuildStep(step)) {
                return Result.failure(Exception("Step is not a build step: ${step.type}"))
            }
            
            // Get workflow URL info for this step type
            val workflowUrlInfo = getWorkflowUrlInfoForStep(step, githubConfig)
                ?: return Result.failure(Exception("No workflow URL configured for step type: ${step.type}"))
            
            // Process the inputs from URL and replace placeholders
            val processedInputs = processWorkflowInputs(workflowUrlInfo.inputs, step, release)
            
            // Extract branch from URL params or use default
            val branchToTrigger = workflowUrlInfo.inputs["branch"] ?: ref
            
            // Remove branch from processed inputs as it's used for API call, not workflow input
            val finalInputs = processedInputs.toMutableMap().apply {
                remove("branch")
            }
            
            println("🚀 Triggering GitHub Action for step:")
            println("   - Step: ${step.title} (${step.type})")
            println("   - Repository: ${workflowUrlInfo.repositoryUrl}")
            println("   - Workflow ID: ${workflowUrlInfo.workflowId}")
            println("   - Branch: $branchToTrigger")
            println("   - Raw Inputs: ${workflowUrlInfo.inputs}")
            println("   - Processed Inputs: $finalInputs")
            
            // Trigger the GitHub Action
            val actionResult = githubRepository.triggerGitHubAction(
                repositoryUrl = workflowUrlInfo.repositoryUrl,
                token = githubConfig.githubToken,
                workflowId = workflowUrlInfo.workflowId,
                ref = branchToTrigger,
                inputs = finalInputs
            )
            
            actionResult.fold(
                onSuccess = { actionRun ->
                    // Update the step with GitHub Action information
                    val updatedStep = step.copy(
                        githubActionRunId = actionRun.id,
                        githubActionUrl = actionRun.htmlUrl,
                        githubActionStatus = actionRun.status,
                        githubActionConclusion = actionRun.conclusion
                    )
                    
                    println("✅ GitHub Action triggered successfully:")
                    println("   - Run ID: ${actionRun.id}")
                    println("   - Run Number: ${actionRun.runNumber}")
                    println("   - Status: ${actionRun.status}")
                    println("   - URL: ${actionRun.htmlUrl}")
                    
                    // Save the updated step
                    val saveResult = workflowRepository.updateWorkflowStep(updatedStep)
                    saveResult.fold(
                        onSuccess = { savedStep ->
                            Result.success(savedStep)
                        },
                        onFailure = { error ->
                            println("⚠️ Action triggered but failed to update step: ${error.message}")
                            // Return the updated step even if save failed
                            Result.success(updatedStep)
                        }
                    )
                },
                onFailure = { error ->
                    Result.failure(Exception("Failed to trigger GitHub Action: ${error.message}"))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 