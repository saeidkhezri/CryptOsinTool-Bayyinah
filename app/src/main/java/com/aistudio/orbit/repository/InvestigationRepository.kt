package com.aistudio.orbit.repository

import android.content.Context
import com.aistudio.orbit.db.AppDatabase
import com.aistudio.orbit.model.InvestigationCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class InvestigationRepository(context: Context) {
    private val caseDao = AppDatabase.getDatabase(context).caseDao()

    val cases: Flow<List<InvestigationCase>> = caseDao.getAllCases()

    suspend fun saveCase(investigationCase: InvestigationCase) = withContext(Dispatchers.IO) {
        caseDao.insertCase(investigationCase)
    }

    suspend fun deleteCase(caseId: String) = withContext(Dispatchers.IO) {
        caseDao.deleteCaseById(caseId)
    }

    suspend fun getCaseById(caseId: String): InvestigationCase? = withContext(Dispatchers.IO) {
        caseDao.getCaseById(caseId)
    }
}

