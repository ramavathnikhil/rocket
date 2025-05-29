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
    GITHUB_ACTION
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
    val actualDuration: Int = 0 // in minutes
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
            "actualDuration" to actualDuration
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
                actualDuration = map["actualDuration"] as? Int ?: 0
            )
        }

        fun getDefaultWorkflowSteps(): List<WorkflowStep> {
            return listOf(
                WorkflowStep(
                    stepNumber = 1,
                    type = StepType.PR_MERGE,
                    title = "Merge Develop to Release",
                    description = "Create and merge PR from develop branch to release branch",
                    isRequired = true,
                    estimatedDuration = 30
                ),
                WorkflowStep(
                    stepNumber = 2,
                    type = StepType.BUILD_STAGING,
                    title = "Build Staging APK",
                    description = "Run GitHub Actions to build and share staging build",
                    isRequired = true,
                    dependsOn = listOf(), // Will be filled with step 1 ID
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 3,
                    type = StepType.STAGING_SIGNOFF,
                    title = "Staging Signoff",
                    description = "Get approval for staging build from stakeholders",
                    isRequired = true,
                    dependsOn = listOf(), // Will be filled with step 2 ID
                    estimatedDuration = 120
                ),
                WorkflowStep(
                    stepNumber = 4,
                    type = StepType.PR_TO_MASTER,
                    title = "Create PR to Master",
                    description = "Create PR from release branch to master branch",
                    isRequired = true,
                    dependsOn = listOf(), // Will be filled with step 3 ID
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 5,
                    type = StepType.BUILD_PRODUCTION,
                    title = "Build Production APK",
                    description = "Build production regression build for QA testing",
                    isRequired = true,
                    dependsOn = listOf(), // Will be filled with step 4 ID
                    estimatedDuration = 20
                ),
                WorkflowStep(
                    stepNumber = 6,
                    type = StepType.QA_SIGNOFF,
                    title = "QA Production Signoff",
                    description = "Get QA approval for production build",
                    isRequired = true,
                    dependsOn = listOf(), // Will be filled with step 5 ID
                    estimatedDuration = 240
                ),
                WorkflowStep(
                    stepNumber = 7,
                    type = StepType.DEPLOY_BETA,
                    title = "Deploy to PlayStore Beta",
                    description = "Run GitHub Actions to deploy APK to PlayStore beta track",
                    isRequired = true,
                    dependsOn = listOf(), // Will be filled with step 6 ID
                    estimatedDuration = 30
                ),
                WorkflowStep(
                    stepNumber = 8,
                    type = StepType.CREATE_GITHUB_RELEASE,
                    title = "Create GitHub Release",
                    description = "Create release tag and release notes in GitHub",
                    isRequired = true,
                    dependsOn = listOf(), // Will be filled with step 7 ID
                    estimatedDuration = 15
                ),
                WorkflowStep(
                    stepNumber = 9,
                    type = StepType.PROMOTE_PRODUCTION,
                    title = "Promote to Production",
                    description = "Promote app from beta to production track in Play Console",
                    isRequired = true,
                    dependsOn = listOf(), // Will be filled with step 8 ID
                    estimatedDuration = 10
                )
            )
        }
    }
} 