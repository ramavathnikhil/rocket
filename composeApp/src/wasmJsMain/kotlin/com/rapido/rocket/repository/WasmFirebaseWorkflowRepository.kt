package com.rapido.rocket.repository

import com.rapido.rocket.model.WorkflowStep
import com.rapido.rocket.model.StepStatus
import com.rapido.rocket.model.StepType
import com.rapido.rocket.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

class WasmFirebaseWorkflowRepository : WorkflowRepository {
    
    private val firestore: FirebaseFirestore = Firebase.firestore()
    private val workflowStepsCollection = firestore.collection("workflowSteps")
    
    override suspend fun createWorkflowStep(step: WorkflowStep): Result<WorkflowStep> {
        return try {
            val stepWithId = if (step.id.isEmpty()) {
                step.copy(
                    id = "step_${currentTimeMillis()}_${(100..999).random()}",
                    createdAt = currentTimeMillis(),
                    updatedAt = currentTimeMillis()
                )
            } else {
                step.copy(updatedAt = currentTimeMillis())
            }
            
            val stepDataMap = mapOf(
                "id" to stepWithId.id,
                "releaseId" to stepWithId.releaseId,
                "stepNumber" to stepWithId.stepNumber,
                "type" to stepWithId.type.name,
                "title" to stepWithId.title,
                "description" to stepWithId.description,
                "status" to stepWithId.status.name,
                "assignedTo" to stepWithId.assignedTo,
                "completedBy" to stepWithId.completedBy,
                "createdAt" to stepWithId.createdAt,
                "updatedAt" to stepWithId.updatedAt,
                "completedAt" to stepWithId.completedAt,
                "notes" to stepWithId.notes,
                "actionUrl" to stepWithId.actionUrl,
                "isRequired" to stepWithId.isRequired,
                "dependsOn" to stepWithId.dependsOn,
                "estimatedDuration" to stepWithId.estimatedDuration,
                "actualDuration" to stepWithId.actualDuration,
                // GitHub integration fields
                "githubPrNumber" to (stepWithId.githubPrNumber ?: ""),
                "githubPrUrl" to stepWithId.githubPrUrl,
                "githubPrState" to stepWithId.githubPrState,
                "repositoryType" to stepWithId.repositoryType,
                "sourceBranch" to stepWithId.sourceBranch,
                "targetBranch" to stepWithId.targetBranch
            )
            
            val jsStepData = stepDataMap.toJsObject()
            val docRef = workflowStepsCollection.doc(stepWithId.id)
            docRef.set(jsStepData).await()
            
            println("✅ Workflow step saved to Firebase: ${stepWithId.title} (${stepWithId.id})")
            Result.success(stepWithId)
            
        } catch (e: Exception) {
            println("❌ Failed to save workflow step to Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun updateWorkflowStep(step: WorkflowStep): Result<WorkflowStep> {
        return try {
            val updatedStep = step.copy(updatedAt = currentTimeMillis())
            
            val stepDataMap = mapOf(
                "id" to updatedStep.id,
                "releaseId" to updatedStep.releaseId,
                "stepNumber" to updatedStep.stepNumber,
                "type" to updatedStep.type.name,
                "title" to updatedStep.title,
                "description" to updatedStep.description,
                "status" to updatedStep.status.name,
                "assignedTo" to updatedStep.assignedTo,
                "completedBy" to updatedStep.completedBy,
                "createdAt" to updatedStep.createdAt,
                "updatedAt" to updatedStep.updatedAt,
                "completedAt" to updatedStep.completedAt,
                "notes" to updatedStep.notes,
                "actionUrl" to updatedStep.actionUrl,
                "isRequired" to updatedStep.isRequired,
                "dependsOn" to updatedStep.dependsOn,
                "estimatedDuration" to updatedStep.estimatedDuration,
                "actualDuration" to updatedStep.actualDuration,
                // GitHub integration fields
                "githubPrNumber" to (updatedStep.githubPrNumber ?: ""),
                "githubPrUrl" to updatedStep.githubPrUrl,
                "githubPrState" to updatedStep.githubPrState,
                "repositoryType" to updatedStep.repositoryType,
                "sourceBranch" to updatedStep.sourceBranch,
                "targetBranch" to updatedStep.targetBranch
            )
            
            println("🔍 SAVING WORKFLOW STEP TO FIREBASE:")
            println("   Step ID: '${step.id}'")
            println("   Step Number: ${updatedStep.stepNumber}")
            println("   Title: '${updatedStep.title}'")
            println("   GitHub PR Number: ${updatedStep.githubPrNumber}")
            println("   GitHub PR URL: '${updatedStep.githubPrUrl}'")
            println("   GitHub PR State: '${updatedStep.githubPrState}'")
            println("   Repository Type: '${updatedStep.repositoryType}'")
            println("   Source Branch: '${updatedStep.sourceBranch}'")
            println("   Target Branch: '${updatedStep.targetBranch}'")
            println("   Complete stepDataMap:")
            stepDataMap.forEach { (key, value) ->
                println("      $key: '$value'")
            }
            
            val jsStepData = stepDataMap.toJsObject()
            val docRef = workflowStepsCollection.doc(step.id)
            docRef.set(jsStepData).await()
            
            println("✅ Workflow step updated in Firebase: ${updatedStep.title}")
            Result.success(updatedStep)
            
        } catch (e: Exception) {
            println("❌ Failed to update workflow step in Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteWorkflowStep(stepId: String): Result<Unit> {
        return try {
            val docRef = workflowStepsCollection.doc(stepId)
            docRef.delete().await()
            
            println("✅ Workflow step deleted from Firebase: $stepId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            println("❌ Failed to delete workflow step from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getWorkflowStep(stepId: String): Result<WorkflowStep?> {
        return try {
            val docRef = workflowStepsCollection.doc(stepId)
            val docSnapshot = docRef.get().await()
            
            if (docSnapshot.exists) {
                val data = docSnapshot.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                
                println("🔍 LOADING WORKFLOW STEP FROM FIREBASE:")
                println("   Step ID: '$stepId'")
                println("   Raw data keys: ${data.keys}")
                println("   Complete data map:")
                data.forEach { (key, value) ->
                    println("      $key: '$value'")
                }
                println("   GitHub PR fields from data:")
                println("      githubPrNumber: '${data["githubPrNumber"]}'")
                println("      githubPrUrl: '${data["githubPrUrl"]}'")
                println("      githubPrState: '${data["githubPrState"]}'")
                println("      repositoryType: '${data["repositoryType"]}'")
                
                val workflowStep = WorkflowStep(
                    id = data["id"] as? String ?: stepId,
                    releaseId = data["releaseId"] as? String ?: "",
                    stepNumber = (data["stepNumber"] as? Number)?.toInt() ?: 0,
                    type = StepType.valueOf(data["type"] as? String ?: "MANUAL_TASK"),
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    status = StepStatus.valueOf(data["status"] as? String ?: "PENDING"),
                    assignedTo = data["assignedTo"] as? String ?: "",
                    completedBy = data["completedBy"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                    completedAt = (data["completedAt"] as? Number)?.toLong(),
                    notes = data["notes"] as? String ?: "",
                    actionUrl = data["actionUrl"] as? String ?: "",
                    isRequired = data["isRequired"] as? Boolean ?: true,
                    dependsOn = (data["dependsOn"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    estimatedDuration = (data["estimatedDuration"] as? Number)?.toInt() ?: 0,
                    actualDuration = (data["actualDuration"] as? Number)?.toInt() ?: 0,
                    // GitHub integration fields
                    githubPrNumber = (data["githubPrNumber"] as? Number)?.toInt(),
                    githubPrUrl = data["githubPrUrl"] as? String ?: "",
                    githubPrState = data["githubPrState"] as? String ?: "",
                    repositoryType = data["repositoryType"] as? String ?: "",
                    sourceBranch = data["sourceBranch"] as? String ?: "",
                    targetBranch = data["targetBranch"] as? String ?: ""
                )
                Result.success(workflowStep)
            } else {
                Result.success(null)
            }
            
        } catch (e: Exception) {
            println("❌ Failed to get workflow step from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getWorkflowStepsByRelease(releaseId: String): Result<List<WorkflowStep>> {
        return try {
            val querySnapshot = workflowStepsCollection.get().await()
            val steps = mutableListOf<WorkflowStep>()
            
            querySnapshot.forEach { doc ->
                if (doc.exists) {
                    val data = doc.data()?.let { jsToMap(it) } ?: emptyMap<String, Any>()
                    val stepReleaseId = data["releaseId"] as? String ?: ""
                    
                    if (stepReleaseId == releaseId) {
                        val workflowStep = WorkflowStep(
                            id = data["id"] as? String ?: "",
                            releaseId = data["releaseId"] as? String ?: "",
                            stepNumber = (data["stepNumber"] as? Number)?.toInt() ?: 0,
                            type = StepType.valueOf(data["type"] as? String ?: "MANUAL_TASK"),
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            status = StepStatus.valueOf(data["status"] as? String ?: "PENDING"),
                            assignedTo = data["assignedTo"] as? String ?: "",
                            completedBy = data["completedBy"] as? String ?: "",
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis(),
                            completedAt = (data["completedAt"] as? Number)?.toLong(),
                            notes = data["notes"] as? String ?: "",
                            actionUrl = data["actionUrl"] as? String ?: "",
                            isRequired = data["isRequired"] as? Boolean ?: true,
                            dependsOn = (data["dependsOn"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            estimatedDuration = (data["estimatedDuration"] as? Number)?.toInt() ?: 0,
                            actualDuration = (data["actualDuration"] as? Number)?.toInt() ?: 0,
                            // GitHub integration fields
                            githubPrNumber = (data["githubPrNumber"] as? Number)?.toInt(),
                            githubPrUrl = data["githubPrUrl"] as? String ?: "",
                            githubPrState = data["githubPrState"] as? String ?: "",
                            repositoryType = data["repositoryType"] as? String ?: "",
                            sourceBranch = data["sourceBranch"] as? String ?: "",
                            targetBranch = data["targetBranch"] as? String ?: ""
                        )
                        steps.add(workflowStep)
                    }
                }
            }
            
            val sortedSteps = steps.sortedBy { it.stepNumber }
            println("✅ Loaded ${sortedSteps.size} workflow steps for release $releaseId from Firebase")
            Result.success(sortedSteps)
            
        } catch (e: Exception) {
            println("❌ Failed to load workflow steps for release from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun updateStepStatus(stepId: String, status: StepStatus, completedBy: String?, notes: String?): Result<WorkflowStep> {
        return try {
            // Get the current step first
            val stepResult = getWorkflowStep(stepId)
            if (stepResult.isFailure) {
                return Result.failure(stepResult.exceptionOrNull() ?: Exception("Failed to get step"))
            }
            
            val currentStep = stepResult.getOrNull()
            if (currentStep == null) {
                return Result.failure(Exception("Step not found: $stepId"))
            }
            
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to currentTimeMillis()
            )
            
            if (completedBy != null) {
                updates["completedBy"] = completedBy
            }
            
            if (notes != null) {
                updates["notes"] = notes
            }
            
            if (status == StepStatus.COMPLETED) {
                updates["completedAt"] = currentTimeMillis()
            }
            
            val docRef = workflowStepsCollection.doc(stepId)
            docRef.update(updates.toJsObject()).await()
            
            // Return the updated step
            val updatedStep = currentStep.copy(
                status = status,
                completedBy = completedBy ?: currentStep.completedBy,
                notes = notes ?: currentStep.notes,
                completedAt = if (status == StepStatus.COMPLETED) currentTimeMillis() else currentStep.completedAt,
                updatedAt = currentTimeMillis()
            )
            
            println("✅ Workflow step status updated in Firebase: $stepId -> $status")
            Result.success(updatedStep)
            
        } catch (e: Exception) {
            println("❌ Failed to update workflow step status in Firebase: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun createDefaultWorkflowSteps(releaseId: String): Result<List<WorkflowStep>> {
        return try {
            // Get the default workflow steps from the model
            val defaultStepTemplates = WorkflowStep.getDefaultWorkflowSteps()
            
            // Apply the release ID to each step
            val defaultSteps = defaultStepTemplates.map { template ->
                template.copy(
                    releaseId = releaseId,
                    createdAt = currentTimeMillis(),
                    updatedAt = currentTimeMillis()
                )
            }
            
            val createdSteps = mutableListOf<WorkflowStep>()
            for (step in defaultSteps) {
                val result = createWorkflowStep(step)
                result.fold(
                    onSuccess = { createdStep -> createdSteps.add(createdStep) },
                    onFailure = { throw it }
                )
            }
            
            println("✅ Created ${createdSteps.size} default workflow steps for release $releaseId")
            Result.success(createdSteps)
            
        } catch (e: Exception) {
            println("❌ Failed to create default workflow steps: ${e.message}")
            Result.failure(e)
        }
    }
    
    override fun observeWorkflowSteps(releaseId: String): Flow<List<WorkflowStep>> = flow {
        while (true) {
            val result = getWorkflowStepsByRelease(releaseId)
            result.fold(
                onSuccess = { steps -> emit(steps) },
                onFailure = { 
                    println("❌ Error in observeWorkflowSteps: ${it.message}")
                    emit(emptyList()) 
                }
            )
            delay(5000)
        }
    }
    
    override fun observeWorkflowStep(stepId: String): Flow<WorkflowStep?> = flow {
        while (true) {
            val result = getWorkflowStep(stepId)
            result.fold(
                onSuccess = { step -> emit(step) },
                onFailure = { 
                    println("❌ Error in observeWorkflowStep: ${it.message}")
                    emit(null) 
                }
            )
            delay(5000)
        }
    }
} 