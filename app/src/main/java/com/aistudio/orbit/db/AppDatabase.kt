package com.aistudio.orbit.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aistudio.orbit.model.InvestigationCase

@Database(
    entities = [
        InvestigationCase::class,
        EvidenceEntity::class,
        FindingEntity::class,
        AddressEntity::class,
        TransactionEntity::class,
        AuditLogEntity::class,
        TagPackEntity::class,
        SanctionEntity::class,
        DatasetMetadataEntity::class,
        InvestigationEntity::class,
        TxInputEntity::class,
        TxOutputEntity::class,
        ClusterEntity::class,
        EntityRecordEntity::class,
        RelationshipEntity::class,
        SourceProvenanceEntity::class,
        HypothesisEntity::class,
        RiskAssessmentEntity::class,
        BehavioralPatternEntity::class,
        OsintObservationEntity::class,
        ProviderRunEntity::class,
        ReportEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caseDao(): CaseDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun findingDao(): FindingDao
    abstract fun addressDao(): AddressDao
    abstract fun transactionDao(): TransactionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun tagPackDao(): TagPackDao
    abstract fun sanctionDao(): SanctionDao
    abstract fun datasetMetadataDao(): DatasetMetadataDao
    abstract fun investigationDao(): InvestigationDao
    abstract fun txInputDao(): TxInputDao
    abstract fun txOutputDao(): TxOutputDao
    abstract fun clusterDao(): ClusterDao
    abstract fun entityRecordDao(): EntityRecordDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun sourceProvenanceDao(): SourceProvenanceDao
    abstract fun hypothesisDao(): HypothesisDao
    abstract fun riskAssessmentDao(): RiskAssessmentDao
    abstract fun behavioralPatternDao(): BehavioralPatternDao
    abstract fun osintObservationDao(): OsintObservationDao
    abstract fun providerRunDao(): ProviderRunDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `investigation_records` (
                        `investigationId` TEXT NOT NULL PRIMARY KEY,
                        `caseId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetAddress` TEXT NOT NULL,
                        `blockchain` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `currentState` TEXT NOT NULL,
                        `lineageStage` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`caseId`) REFERENCES `investigation_cases`(`caseId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investigation_records_caseId` ON `investigation_records` (`caseId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tx_inputs` (
                        `inputId` TEXT NOT NULL PRIMARY KEY,
                        `txHash` TEXT NOT NULL,
                        `inputIndex` INTEGER NOT NULL,
                        `previousTxHash` TEXT NOT NULL,
                        `previousOutputIndex` INTEGER NOT NULL,
                        `address` TEXT NOT NULL,
                        `amountSat` INTEGER NOT NULL,
                        `scriptSig` TEXT NOT NULL,
                        `sequence` INTEGER NOT NULL,
                        `lineageStage` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tx_inputs_txHash` ON `tx_inputs` (`txHash`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tx_inputs_address` ON `tx_inputs` (`address`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tx_outputs` (
                        `outputId` TEXT NOT NULL PRIMARY KEY,
                        `txHash` TEXT NOT NULL,
                        `outputIndex` INTEGER NOT NULL,
                        `address` TEXT NOT NULL,
                        `amountSat` INTEGER NOT NULL,
                        `scriptPubKey` TEXT NOT NULL,
                        `scriptType` TEXT NOT NULL,
                        `isSpent` INTEGER NOT NULL,
                        `spentByTxHash` TEXT,
                        `lineageStage` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tx_outputs_txHash` ON `tx_outputs` (`txHash`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tx_outputs_address` ON `tx_outputs` (`address`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `investigation_clusters` (
                        `clusterId` TEXT NOT NULL PRIMARY KEY,
                        `caseId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `primaryAddress` TEXT NOT NULL,
                        `memberAddressesJson` TEXT NOT NULL,
                        `heuristicRule` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `epistemicStatus` TEXT NOT NULL,
                        `tagsJson` TEXT NOT NULL,
                        `parentInputIdsJson` TEXT NOT NULL,
                        `lineageStage` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investigation_clusters_caseId` ON `investigation_clusters` (`caseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investigation_clusters_primaryAddress` ON `investigation_clusters` (`primaryAddress`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `entity_records` (
                        `entityId` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `nameFa` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `jurisdiction` TEXT NOT NULL,
                        `riskScore` REAL NOT NULL,
                        `website` TEXT NOT NULL,
                        `tagsJson` TEXT NOT NULL,
                        `attributionConfidence` REAL NOT NULL,
                        `epistemicStatus` TEXT NOT NULL,
                        `sourceProvenanceId` TEXT NOT NULL,
                        `lineageStage` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_entity_records_name` ON `entity_records` (`name`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `investigation_relationships` (
                        `relationshipId` TEXT NOT NULL PRIMARY KEY,
                        `caseId` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `targetId` TEXT NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `relationshipType` TEXT NOT NULL,
                        `totalTransferredSat` INTEGER NOT NULL,
                        `txCount` INTEGER NOT NULL,
                        `firstSeenTimestamp` INTEGER NOT NULL,
                        `lastSeenTimestamp` INTEGER NOT NULL,
                        `confidence` REAL NOT NULL,
                        `epistemicStatus` TEXT NOT NULL,
                        `evidenceIdsJson` TEXT NOT NULL,
                        `parentInputIdsJson` TEXT NOT NULL,
                        `lineageStage` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investigation_relationships_caseId` ON `investigation_relationships` (`caseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investigation_relationships_sourceId` ON `investigation_relationships` (`sourceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investigation_relationships_targetId` ON `investigation_relationships` (`targetId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `source_provenances` (
                        `sourceId` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `providerType` TEXT NOT NULL,
                        `sourceUrl` TEXT NOT NULL,
                        `datasetVersion` TEXT NOT NULL,
                        `license` TEXT NOT NULL,
                        `retrievedAt` INTEGER NOT NULL,
                        `effectiveAt` INTEGER NOT NULL,
                        `confidenceWeight` REAL NOT NULL,
                        `isIndependent` INTEGER NOT NULL,
                        `upstreamSourceId` TEXT,
                        `lineageStage` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_provenances_sourceId` ON `source_provenances` (`sourceId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `investigation_hypotheses` (
                        `hypothesisId` TEXT NOT NULL PRIMARY KEY,
                        `caseId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `titleFa` TEXT NOT NULL,
                        `statement` TEXT NOT NULL,
                        `statementFa` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `supportingEvidenceIdsJson` TEXT NOT NULL,
                        `contradictingEvidenceIdsJson` TEXT NOT NULL,
                        `neutralEvidenceIdsJson` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `updatedBy` TEXT NOT NULL,
                        `parentInputIdsJson` TEXT NOT NULL,
                        `lineageStage` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investigation_hypotheses_caseId` ON `investigation_hypotheses` (`caseId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `risk_assessments` (
                        `assessmentId` TEXT NOT NULL PRIMARY KEY,
                        `caseId` TEXT NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `targetId` TEXT NOT NULL,
                        `overallScore` REAL NOT NULL,
                        `transactionRisk` REAL NOT NULL,
                        `addressRisk` REAL NOT NULL,
                        `entityRisk` REAL NOT NULL,
                        `exposureRisk` REAL NOT NULL,
                        `behavioralRisk` REAL NOT NULL,
                        `sanctionsRisk` REAL NOT NULL,
                        `osintRisk` REAL NOT NULL,
                        `contributingIndicatorsJson` TEXT NOT NULL,
                        `epistemicStatus` TEXT NOT NULL,
                        `parentInputIdsJson` TEXT NOT NULL,
                        `lineageStage` TEXT NOT NULL,
                        `assessedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_risk_assessments_caseId` ON `risk_assessments` (`caseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_risk_assessments_targetId` ON `risk_assessments` (`targetId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `behavioral_patterns` (
                        `patternId` TEXT NOT NULL PRIMARY KEY,
                        `caseId` TEXT NOT NULL,
                        `patternName` TEXT NOT NULL,
                        `patternNameFa` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `severity` TEXT NOT NULL,
                        `indicatorsJson` TEXT NOT NULL,
                        `matchedAddressesJson` TEXT NOT NULL,
                        `matchedTxsJson` TEXT NOT NULL,
                        `supportingEvidenceIdsJson` TEXT NOT NULL,
                        `contradictingEvidenceIdsJson` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `epistemicStatus` TEXT NOT NULL,
                        `parentInputIdsJson` TEXT NOT NULL,
                        `lineageStage` TEXT NOT NULL,
                        `detectedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavioral_patterns_caseId` ON `behavioral_patterns` (`caseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavioral_patterns_category` ON `behavioral_patterns` (`category`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `osint_observations` (
                        `observationId` TEXT NOT NULL PRIMARY KEY,
                        `caseId` TEXT NOT NULL,
                        `targetQuery` TEXT NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `providerId` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `snippet` TEXT NOT NULL,
                        `rawJson` TEXT NOT NULL,
                        `epistemicStatus` TEXT NOT NULL,
                        `parentInputIdsJson` TEXT NOT NULL,
                        `lineageStage` TEXT NOT NULL,
                        `observedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_osint_observations_caseId` ON `osint_observations` (`caseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_osint_observations_targetQuery` ON `osint_observations` (`targetQuery`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `provider_runs` (
                        `runId` TEXT NOT NULL PRIMARY KEY,
                        `providerId` TEXT NOT NULL,
                        `caseId` TEXT,
                        `queryTarget` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `httpCode` INTEGER NOT NULL,
                        `latencyMs` INTEGER NOT NULL,
                        `recordsCount` INTEGER NOT NULL,
                        `rawMetadataJson` TEXT NOT NULL,
                        `ranAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_provider_runs_providerId` ON `provider_runs` (`providerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_provider_runs_caseId` ON `provider_runs` (`caseId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `investigation_reports` (
                        `reportId` TEXT NOT NULL PRIMARY KEY,
                        `caseId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `titleFa` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `format` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `findingsCount` INTEGER NOT NULL,
                        `evidenceCount` INTEGER NOT NULL,
                        `checksum` TEXT NOT NULL,
                        `isFinalized` INTEGER NOT NULL,
                        `generatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investigation_reports_caseId` ON `investigation_reports` (`caseId`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE investigation_cases ADD COLUMN activeStageId INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE investigation_cases ADD COLUMN workflowStateKey TEXT NOT NULL DEFAULT 'DISCOVERING'")
                db.execSQL("ALTER TABLE investigation_cases ADD COLUMN experienceModeKey TEXT NOT NULL DEFAULT 'GUIDED_INVESTIGATION'")
                db.execSQL("ALTER TABLE investigation_cases ADD COLUMN investigationGoal TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE investigation_cases ADD COLUMN analysisRevision INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var appContext: Context? = null

        fun getDatabaseContext(): Context {
            return appContext ?: throw IllegalStateException("Database context not initialized")
        }

        fun getDatabase(context: Context): AppDatabase {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orbit_forensics_database"
                )
                // Zero-Data-Loss Migration Strategy:
                // 1. Every schema update MUST have an explicit Migration object (e.g., MIGRATION_6_7)
                //    defining exact CREATE TABLE or ALTER TABLE queries.
                // 2. DO NOT rely on fallbackToDestructiveMigration() in production release builds.
                //    It is left here only to prevent unhandled crashes during debug prototyping,
                //    but explicit migration paths take precedence and preserve forensic data integrity.
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
