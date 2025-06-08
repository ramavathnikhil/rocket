package com.rapido.rocket.model

enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    SKIPPED
}

enum class StepType {
    PR_MERGE,
    BUILD_STAGING,
    STAGING_SIGNOFF,
    PR_TO_MASTER,
    BUILD_PRODUCTION,
    QA_SIGNOFF,
    DEPLOY_BETA,
    CREATE_GITHUB_RELEASE,
    PROMOTE_PRODUCTION,
    MANUAL_TASK,
    GITHUB_ACTION,
    UPDATE_VERSION,
    CREATE_PR_BFF,
    SHARE_FUNCTIONAL_BUILD,
    FUNCTIONAL_SIGNOFF,
    SHARE_REGRESSION_BUILD,
    REGRESSION_SIGNOFF,
    CREATE_BASELINE_PROFILE_PR,
    CREATE_PROD_CONFIGS,
    DEPLOY_PROD_CONFIGS,
    DEPLOY_BFF_PROD,
    SHARE_PROD_REGRESSION_BUILD,
    WRITE_BFF_RELEASE_NOTES,
    CREATE_BFF_RELEASE_TAG,
    BACK_MERGE_BFF,
    PROMOTE_BETA_100_PERCENT,
    PUBLISH_BETA_99_PERCENT,
    CREATE_APP_RELEASE_TAG,
    BACK_MERGE_APP,
    MONITOR_CRASHES,
    PRODUCTION_100_PERCENT,
    DEPLOY_PRODUCTION_5_PERCENT,
    PROMOTE_30_PERCENT,
    PROMOTE_50_PERCENT,
    PROMOTE_75_PERCENT,
    PROMOTE_99_PERCENT
}

data class WorkflowStep(
    val id: String = "",
    val releaseId: String = "",
    val stepNumber: Int = 0,
    val type: StepType = StepType.MANUAL_TASK,
    val title: String = "",
    val description: String = "",
    val status: StepStatus = StepStatus.PENDING,
    val assignedTo: String = "",
    val completedBy: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long? = null,
    val notes: String = "",
    val actionUrl: String = "", // For GitHub Actions or external URLs
    val isRequired: Boolean = true,
    val dependsOn: List<String> = emptyList(), // IDs of steps this depends on
    val estimatedDuration: Int = 0, // in minutes
    val actualDuration: Int = 0, // in minutes
    // GitHub integration fields
    val githubPrNumber: Int? = null, // GitHub PR number if created
    val githubPrUrl: String = "", // GitHub PR URL if created
    val githubPrState: String = "", // "open", "closed", "merged"
    val repositoryType: String = "", // "app" or "bff" for GitHub steps
    val sourceBranch: String = "", // Source branch for PRs
    val targetBranch: String = "" // Target branch for PRs
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "releaseId" to releaseId,
            "stepNumber" to stepNumber,
            "type" to type.name,
            "title" to title,
            "description" to description,
            "status" to status.name,
            "assignedTo" to assignedTo,
            "completedBy" to completedBy,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "completedAt" to (completedAt ?: ""),
            "notes" to notes,
            "actionUrl" to actionUrl,
            "isRequired" to isRequired,
            "dependsOn" to dependsOn,
            "estimatedDuration" to estimatedDuration,
            "actualDuration" to actualDuration,
            "githubPrNumber" to (githubPrNumber ?: ""),
            "githubPrUrl" to githubPrUrl,
            "githubPrState" to githubPrState,
            "repositoryType" to repositoryType,
            "sourceBranch" to sourceBranch,
            "targetBranch" to targetBranch
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): WorkflowStep {
            return WorkflowStep(
                id = map["id"] as? String ?: "",
                releaseId = map["releaseId"] as? String ?: "",
                stepNumber = map["stepNumber"] as? Int ?: 0,
                type = try {
                    StepType.valueOf(map["type"] as? String ?: "MANUAL_TASK")
                } catch (e: Exception) {
                    StepType.MANUAL_TASK
                },
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                status = try {
                    StepStatus.valueOf(map["status"] as? String ?: "PENDING")
                } catch (e: Exception) {
                    StepStatus.PENDING
                },
                assignedTo = map["assignedTo"] as? String ?: "",
                completedBy = map["completedBy"] as? String ?: "",
                createdAt = map["createdAt"] as? Long ?: 0L,
                updatedAt = map["updatedAt"] as? Long ?: 0L,
                completedAt = map["completedAt"].let { 
                    if (it is Long) it else null 
                },
                notes = map["notes"] as? String ?: "",
                actionUrl = map["actionUrl"] as? String ?: "",
                isRequired = map["isRequired"] as? Boolean ?: true,
                dependsOn = (map["dependsOn"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                estimatedDuration = map["estimatedDuration"] as? Int ?: 0,
                actualDuration = map["actualDuration"] as? Int ?: 0,
                githubPrNumber = map["githubPrNumber"].let { 
                    if (it is Int) it else null 
                },
                githubPrUrl = map["githubPrUrl"] as? String ?: "",
                githubPrState = map["githubPrState"] as? String ?: "",
                repositoryType = map["repositoryType"] as? String ?: "",
                sourceBranch = map["sourceBranch"] as? String ?: "",
                targetBranch = map["targetBranch"] as? String ?: ""
            )
        }

        fun getDefaultWorkflowSteps(): List<WorkflowStep> {
            return listOf(
                // Stage 1: Functional testing stage
                WorkflowStep(
                    stepNumber = 1,
                    type = StepType.UPDATE_VERSION,
                    title = "Update App and BFF Version",
                    description = "Update version numbers in both App and BFF repositories",
                    isRequired = true,
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 2,
                    type = StepType.PR_MERGE,
                    title = "Create App PR from develop to release",
                    description = "Create and merge PR from develop branch to release branch for App",
                    isRequired = true,
                    estimatedDuration = 30,
                    repositoryType = "app",
                    sourceBranch = "develop",
                    targetBranch = "release"
                ),
                WorkflowStep(
                    stepNumber = 3,
                    type = StepType.CREATE_PR_BFF,
                    title = "Create BFF PR from develop to release",
                    description = "Create and merge PR from develop branch to release branch for BFF",
                    isRequired = true,
                    estimatedDuration = 30,
                    repositoryType = "bff",
                    sourceBranch = "develop",
                    targetBranch = "release"
                ),
                WorkflowStep(
                    stepNumber = 4,
                    type = StepType.SHARE_FUNCTIONAL_BUILD,
                    title = "Share functional build (Staging Minified debug build)",
                    description = "Share functional build from release branch once develop to release is merged",
                    isRequired = true,
                    estimatedDuration = 45
                ),
                WorkflowStep(
                    stepNumber = 5,
                    type = StepType.FUNCTIONAL_SIGNOFF,
                    title = "Wait for functional signoff",
                    description = "Wait for functional testing approval from stakeholders",
                    isRequired = true,
                    estimatedDuration = 480
                ),
                
                // Stage 2: Regression stage
                WorkflowStep(
                    stepNumber = 6,
                    type = StepType.SHARE_REGRESSION_BUILD,
                    title = "Share regression build (Staging Release build)",
                    description = "After functional signoff, share regression build from release branch",
                    isRequired = true,
                    estimatedDuration = 30
                ),
                WorkflowStep(
                    stepNumber = 7,
                    type = StepType.REGRESSION_SIGNOFF,
                    title = "Wait for Regression signoff",
                    description = "Wait for regression testing approval from QA team",
                    isRequired = true,
                    estimatedDuration = 720
                ),
                
                // Stage 3: Prod Regression stage
                WorkflowStep(
                    stepNumber = 8,
                    type = StepType.CREATE_BASELINE_PROFILE_PR,
                    title = "Create baseline profile PR to release branch",
                    description = "Create and raise baseline profile PR to release branch of APP",
                    isRequired = true,
                    estimatedDuration = 45
                ),
                WorkflowStep(
                    stepNumber = 9,
                    type = StepType.PR_TO_MASTER,
                    title = "Create App PR from release to master",
                    description = "Create App PR from release branch to master branch",
                    isRequired = true,
                    estimatedDuration = 20,
                    repositoryType = "app",
                    sourceBranch = "release",
                    targetBranch = "master"
                ),
                WorkflowStep(
                    stepNumber = 10,
                    type = StepType.CREATE_PR_BFF,
                    title = "Create BFF PR from release to master",
                    description = "Create BFF PR from release branch to master branch",
                    isRequired = true,
                    estimatedDuration = 20,
                    repositoryType = "bff",
                    sourceBranch = "release",
                    targetBranch = "master"
                ),
                WorkflowStep(
                    stepNumber = 11,
                    type = StepType.CREATE_PROD_CONFIGS,
                    title = "Create prod configs for App and BFF",
                    description = "Create production configuration files for both App and BFF",
                    isRequired = true,
                    estimatedDuration = 60
                ),
                WorkflowStep(
                    stepNumber = 12,
                    type = StepType.DEPLOY_PROD_CONFIGS,
                    title = "Deploy BFF and APP prod configs",
                    description = "Deploy production configurations for both BFF and APP",
                    isRequired = true,
                    estimatedDuration = 30
                ),
                WorkflowStep(
                    stepNumber = 13,
                    type = StepType.DEPLOY_BFF_PROD,
                    title = "Deploy BFF to production",
                    description = "Once BFF PR is merged, get it deployed to production",
                    isRequired = true,
                    estimatedDuration = 45
                ),
                WorkflowStep(
                    stepNumber = 14,
                    type = StepType.SHARE_PROD_REGRESSION_BUILD,
                    title = "Share Prod regression build from master",
                    description = "Once BFF is deployed, share Production regression build from master branch",
                    isRequired = true,
                    estimatedDuration = 30
                ),
                WorkflowStep(
                    stepNumber = 15,
                    type = StepType.WRITE_BFF_RELEASE_NOTES,
                    title = "Write BFF deployment release notes",
                    description = "Document BFF deployment changes and release notes",
                    isRequired = true,
                    estimatedDuration = 30
                ),
                WorkflowStep(
                    stepNumber = 16,
                    type = StepType.CREATE_BFF_RELEASE_TAG,
                    title = "Create release tag for BFF repo",
                    description = "Create and push release tag in BFF repository",
                    isRequired = true,
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 17,
                    type = StepType.BACK_MERGE_BFF,
                    title = "Back merge from release to develop (BFF)",
                    description = "Back merge changes from release to develop branch in BFF repo",
                    isRequired = true,
                    estimatedDuration = 20
                ),
                
                // Stage 4: Deployment to Production
                WorkflowStep(
                    stepNumber = 18,
                    type = StepType.PROMOTE_BETA_100_PERCENT,
                    title = "Promote previous app to 100% in beta track",
                    description = "Promote the previous app version to 100% users in beta track",
                    isRequired = true,
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 19,
                    type = StepType.QA_SIGNOFF,
                    title = "Wait for production signoff",
                    description = "Wait for final production deployment approval",
                    isRequired = true,
                    estimatedDuration = 240
                ),
                WorkflowStep(
                    stepNumber = 20,
                    type = StepType.PUBLISH_BETA_99_PERCENT,
                    title = "Publish to beta track 99.9999%",
                    description = "Once production signoff received, publish build to beta track for 99.9999% users",
                    isRequired = true,
                    estimatedDuration = 20
                ),
                WorkflowStep(
                    stepNumber = 21,
                    type = StepType.CREATE_APP_RELEASE_TAG,
                    title = "Create release tag for app repo",
                    description = "Create and push release tag in app repository",
                    isRequired = true,
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 22,
                    type = StepType.BACK_MERGE_APP,
                    title = "Back merge from release to develop (App)",
                    description = "Back merge changes from release to develop branch in App repo",
                    isRequired = true,
                    estimatedDuration = 20
                ),
                WorkflowStep(
                    stepNumber = 23,
                    type = StepType.MONITOR_CRASHES,
                    title = "Monitor for crashes or issues",
                    description = "Monitor application for any crashes or critical issues",
                    isRequired = true,
                    estimatedDuration = 120
                ),
                WorkflowStep(
                    stepNumber = 24,
                    type = StepType.PRODUCTION_100_PERCENT,
                    title = "Make previous production build to 100% users",
                    description = "Promote previous production build to 100% of users",
                    isRequired = true,
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 25,
                    type = StepType.DEPLOY_PRODUCTION_5_PERCENT,
                    title = "Move build from beta to production track (5%)",
                    description = "If no issues observed, move the build from beta to production track for 5% users",
                    isRequired = true,
                    estimatedDuration = 20
                ),
                WorkflowStep(
                    stepNumber = 26,
                    type = StepType.PROMOTE_30_PERCENT,
                    title = "Promote to 30% users",
                    description = "If no issues observed, promote the app to 30% users",
                    isRequired = true,
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 27,
                    type = StepType.PROMOTE_50_PERCENT,
                    title = "Promote to 50% users",
                    description = "If no issues observed, promote the app to 50% users",
                    isRequired = true,
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 28,
                    type = StepType.PROMOTE_75_PERCENT,
                    title = "Promote to 75% users",
                    description = "If no issues observed, promote the app to 75% users",
                    isRequired = true,
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 29,
                    type = StepType.PROMOTE_99_PERCENT,
                    title = "Promote to 99.99999% users",
                    description = "If no issues observed, promote the app to 99.99999% users",
                    isRequired = true,
                    estimatedDuration = 15
                )
            )
        }
    }
} 