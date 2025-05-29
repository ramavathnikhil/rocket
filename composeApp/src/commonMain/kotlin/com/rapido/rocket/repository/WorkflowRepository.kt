package com.rapido.rocket.repository

import com.rapido.rocket.model.WorkflowStep
import com.rapido.rocket.model.StepStatus
import kotlinx.coroutines.flow.Flow

interface WorkflowRepository {
    suspend fun createWorkflowStep(step: WorkflowStep): Result<WorkflowStep>
    suspend fun updateWorkflowStep(step: WorkflowStep): Result<WorkflowStep>
    suspend fun deleteWorkflowStep(stepId: String): Result<Unit>
    suspend fun getWorkflowStep(stepId: String): Result<WorkflowStep?>
    suspend fun getWorkflowStepsByRelease(releaseId: String): Result<List<WorkflowStep>>
    suspend fun updateStepStatus(stepId: String, status: StepStatus, completedBy: String?, notes: String?): Result<WorkflowStep>
    suspend fun createDefaultWorkflowSteps(releaseId: String): Result<List<WorkflowStep>>
    fun observeWorkflowSteps(releaseId: String): Flow<List<WorkflowStep>>
    fun observeWorkflowStep(stepId: String): Flow<WorkflowStep?>
} 