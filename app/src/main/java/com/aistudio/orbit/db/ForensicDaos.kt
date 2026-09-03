package com.aistudio.orbit.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence_records WHERE caseId = :caseId ORDER BY observedAt ASC")
    fun getEvidenceForCase(caseId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence_records WHERE caseId = :caseId ORDER BY observedAt ASC LIMIT :limit OFFSET :offset")
    suspend fun getEvidenceForCasePaged(caseId: String, limit: Int = 50, offset: Int = 0): List<EvidenceEntity>

    @Query("SELECT * FROM evidence_records WHERE caseId = :caseId ORDER BY observedAt ASC")
    suspend fun getEvidenceListForCase(caseId: String): List<EvidenceEntity>

    @Query("SELECT * FROM evidence_records WHERE evidenceId = :id LIMIT 1")
    suspend fun getEvidenceById(id: String): EvidenceEntity?

    @Query("SELECT * FROM evidence_records WHERE address = :address ORDER BY observedAt DESC")
    suspend fun getEvidenceForAddress(address: String): List<EvidenceEntity>

    @Query("SELECT * FROM evidence_records WHERE transactionHash = :txHash ORDER BY observedAt DESC")
    suspend fun getEvidenceForTransaction(txHash: String): List<EvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: EvidenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidenceList(evidenceList: List<EvidenceEntity>)

    @Query("DELETE FROM evidence_records WHERE evidenceId = :id")
    suspend fun deleteEvidenceById(id: String)

    @Query("DELETE FROM evidence_records WHERE caseId = :caseId")
    suspend fun deleteEvidenceForCase(caseId: String)
}

@Dao
interface AddressDao {
    @Query("SELECT * FROM investigation_addresses WHERE caseId = :caseId ORDER BY riskScore DESC LIMIT :limit OFFSET :offset")
    suspend fun getAddressesForCasePaged(caseId: String, limit: Int = 50, offset: Int = 0): List<AddressEntity>

    @Query("SELECT * FROM investigation_addresses WHERE caseId = :caseId ORDER BY riskScore DESC")
    fun getAddressesForCase(caseId: String): Flow<List<AddressEntity>>

    @Query("SELECT COUNT(*) FROM investigation_addresses WHERE caseId = :caseId")
    suspend fun getAddressesCountForCase(caseId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddresses(addresses: List<AddressEntity>)

    @Query("DELETE FROM investigation_addresses WHERE caseId = :caseId")
    suspend fun deleteAddressesForCase(caseId: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM investigation_transactions WHERE caseId = :caseId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsForCasePaged(caseId: String, limit: Int = 50, offset: Int = 0): List<TransactionEntity>

    @Query("SELECT * FROM investigation_transactions WHERE caseId = :caseId ORDER BY timestamp DESC")
    fun getTransactionsForCase(caseId: String): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM investigation_transactions WHERE caseId = :caseId")
    suspend fun getTransactionsCountForCase(caseId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM investigation_transactions WHERE caseId = :caseId")
    suspend fun deleteTransactionsForCase(caseId: String)
}

@Dao
interface FindingDao {
    @Query("SELECT * FROM investigation_findings WHERE caseId = :caseId ORDER BY createdTimestamp DESC")
    fun getFindingsForCase(caseId: String): Flow<List<FindingEntity>>

    @Query("SELECT * FROM investigation_findings WHERE caseId = :caseId ORDER BY createdTimestamp DESC")
    suspend fun getFindingsListForCase(caseId: String): List<FindingEntity>

    @Query("SELECT * FROM investigation_findings WHERE findingId = :findingId LIMIT 1")
    suspend fun getFindingById(findingId: String): FindingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinding(finding: FindingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<FindingEntity>)

    @Query("DELETE FROM investigation_findings WHERE findingId = :findingId")
    suspend fun deleteFindingById(findingId: String)

    @Query("DELETE FROM investigation_findings WHERE caseId = :caseId")
    suspend fun deleteFindingsForCase(caseId: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE caseId = :caseId ORDER BY timestamp DESC")
    fun getAuditLogsForCase(caseId: String): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<AuditLogEntity>
}

@Dao
interface TagPackDao {
    @Query("SELECT * FROM tagpack_records WHERE address = :address AND isEnabled = 1")
    suspend fun getTagsForAddress(address: String): List<TagPackEntity>

    @Query("SELECT * FROM tagpack_records WHERE tagpackId = :tagpackId")
    suspend fun getTagsForTagPack(tagpackId: String): List<TagPackEntity>

    @Query("SELECT COUNT(*) FROM tagpack_records WHERE tagpackId = :tagpackId")
    suspend fun countTagsForTagPack(tagpackId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagPackEntity>)

    @Query("DELETE FROM tagpack_records WHERE tagpackId = :tagpackId")
    suspend fun deleteTagsForTagPack(tagpackId: String)

    @Query("UPDATE tagpack_records SET isEnabled = :isEnabled WHERE tagpackId = :tagpackId")
    suspend fun setTagPackEnabled(tagpackId: String, isEnabled: Boolean)

    @Query("SELECT * FROM tagpack_records WHERE entity LIKE '%' || :query || '%' OR label LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchTags(query: String, limit: Int = 50): List<TagPackEntity>
}

@Dao
interface SanctionDao {
    @Query("SELECT * FROM sanction_records WHERE cryptoAddressesJson LIKE '%' || :address || '%'")
    suspend fun getSanctionsForAddress(address: String): List<SanctionEntity>

    @Query("SELECT * FROM sanction_records WHERE primaryName LIKE '%' || :name || '%' OR aliasesJson LIKE '%' || :name || '%' LIMIT :limit")
    suspend fun searchSanctionsByName(name: String, limit: Int = 50): List<SanctionEntity>

    @Query("SELECT * FROM sanction_records WHERE sanctionId = :id LIMIT 1")
    suspend fun getSanctionById(id: String): SanctionEntity?

    @Query("SELECT * FROM sanction_records WHERE datasetSource = :source")
    suspend fun getSanctionsBySource(source: String): List<SanctionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSanctions(sanctions: List<SanctionEntity>)

    @Query("DELETE FROM sanction_records WHERE datasetSource = :source")
    suspend fun deleteSanctionsBySource(source: String)

    @Query("SELECT COUNT(*) FROM sanction_records")
    suspend fun getTotalSanctionsCount(): Int
}

@Dao
interface DatasetMetadataDao {
    @Query("SELECT * FROM dataset_metadata ORDER BY category ASC, name ASC")
    fun getAllDatasetsFlow(): Flow<List<DatasetMetadataEntity>>

    @Query("SELECT * FROM dataset_metadata ORDER BY category ASC, name ASC")
    suspend fun getAllDatasets(): List<DatasetMetadataEntity>

    @Query("SELECT * FROM dataset_metadata WHERE datasetId = :id LIMIT 1")
    suspend fun getDatasetById(id: String): DatasetMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataset(dataset: DatasetMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatasets(datasets: List<DatasetMetadataEntity>)

    @Query("UPDATE dataset_metadata SET status = :status, downloadProgress = :progress, lastUpdated = :lastUpdated WHERE datasetId = :id")
    suspend fun updateDatasetStatus(id: String, status: String, progress: Float, lastUpdated: Long = System.currentTimeMillis())

    @Query("UPDATE dataset_metadata SET isEnabled = :isEnabled WHERE datasetId = :id")
    suspend fun setDatasetEnabled(id: String, isEnabled: Boolean)

    @Query("DELETE FROM dataset_metadata WHERE datasetId = :id")
    suspend fun deleteDataset(id: String)
}
