package com.aistudio.orbit.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aistudio.orbit.model.InvestigationCase
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Query("SELECT * FROM investigation_cases ORDER BY updatedTimestamp DESC")
    fun getAllCases(): Flow<List<InvestigationCase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(investigationCase: InvestigationCase)

    @Query("DELETE FROM investigation_cases WHERE caseId = :id")
    suspend fun deleteCaseById(id: String)
    
    @Query("SELECT * FROM investigation_cases WHERE caseId = :id LIMIT 1")
    suspend fun getCaseById(id: String): InvestigationCase?

    @Query("""
        UPDATE investigation_cases
        SET activeStageId = :stageId,
            workflowStateKey = :stateKey,
            experienceModeKey = :modeKey,
            updatedTimestamp = :updatedAt
        WHERE caseId = :caseId
    """)
    suspend fun updateWorkflowState(
        caseId: String,
        stageId: Int,
        stateKey: String,
        modeKey: String,
        updatedAt: Long = System.currentTimeMillis()
    )
}
