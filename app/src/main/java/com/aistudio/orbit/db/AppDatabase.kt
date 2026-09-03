package com.aistudio.orbit.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        DatasetMetadataEntity::class
    ],
    version = 6,
    exportSchema = false
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orbit_forensics_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
