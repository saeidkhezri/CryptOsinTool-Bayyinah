package com.aistudio.orbit.forensics.osint

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Cached complete OSINT analysis report for an address
 */
@Entity(tableName = "osint_cache")
data class OsintCacheEntity(
    @PrimaryKey val address: String,
    val network: String,
    val jsonReport: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for cached IP exposures and Geolocation
 */
@Entity(tableName = "osint_ip_associations")
data class OsintIpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val ipAddress: String,
    val ispName: String,
    val ispNameFa: String,
    val country: String,
    val countryFa: String,
    val city: String,
    val cityFa: String,
    val connectionType: String,
    val connectionTypeFa: String,
    val torOrVpnDetected: Boolean,
    val asn: String,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for Entity Tags & Cluster Labels (GraphSense / SpiderFoot / OSINT feeds)
 */
@Entity(tableName = "osint_entity_tags")
data class OsintEntityTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val network: String,
    val knownEntity: String?,
    val knownEntityFa: String?,
    val entityCategory: String?,
    val entityCategoryFa: String?,
    val publicLabelsJson: String, // Comma or JSON string list of labels
    val associatedDomainsJson: String,
    val riskScore: Int,
    val attributionSource: String,
    val attributionSourceFa: String,
    val verificationEvidence: String,
    val verificationEvidenceFa: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for Threat Intelligence Feeds and Warnings
 */
@Entity(tableName = "osint_threat_indicators")
data class OsintThreatIndicatorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val indicatorType: String,
    val indicatorValue: String,
    val threatCategory: String,
    val threatCategoryFa: String,
    val riskScore: Int,
    val reporter: String,
    val reportedDate: String,
    val notesEn: String,
    val notesFa: String,
    val sourceUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_rates")
data class PriceRateEntity(
    @PrimaryKey val id: String,
    val pair: String, // "BTC_USD", "BTC_TOMAN", etc.
    val rate: Double,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for persistent Extracted Indicators
 */
@Entity(tableName = "osint_extracted_indicators")
data class ExtractedIndicatorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetAddress: String,
    val network: String,
    val indicatorType: String,
    val rawValue: String,
    val normalizedValue: String,
    val extractionSource: String,
    val extractionConfidence: Float,
    val category: String,
    val firstObserved: Long,
    val lastObserved: Long,
    val contextMetadataJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for persistent Attribution Confidence and Chain-of-Custody seal
 */
@Entity(tableName = "osint_attribution_confidence")
data class AttributionConfidenceEntity(
    @PrimaryKey val targetAddress: String,
    val overallConfidenceScore: Float,
    val classificationLabelEn: String,
    val classificationLabelFa: String,
    val dominantActor: String,
    val supportingEvidenceCount: Int,
    val tagPackMatchesJson: String,
    val osintIndicatorsJson: String,
    val caseReference: String,
    val sha256Fingerprint: String,
    val evidenceMerkleRoot: String,
    val investigatorSignature: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for Forensic Events (Pub/Sub Event Log)
 */
@Entity(tableName = "osint_forensic_events")
data class ForensicEventEntity(
    @PrimaryKey val eventId: String,
    val targetAddress: String,
    val eventType: String,
    val indicatorType: String,
    val data: String,
    val confidenceScore: Float,
    val emittingModule: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for Deep Identity Reconstruction Footprints
 */
@Entity(tableName = "osint_deep_identity")
data class DeepIdentityResultEntity(
    @PrimaryKey val targetEmailOrPhone: String,
    val associatedCryptoAddress: String,
    val maskedPhone: String?,
    val deAnonymizedPhone: String?,
    val maskedEmail: String?,
    val gaiaId: String?,
    val googleProfileName: String?,
    val holeheServicesJson: String,
    val ignorantPlatformsJson: String,
    val phoneInfogaCarrierJson: String,
    val epieosBreadcrumbsJson: String,
    val overallConfidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface OsintCacheDao {
    @Query("SELECT * FROM osint_cache WHERE address = :address")
    suspend fun getReport(address: String): OsintCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(entity: OsintCacheEntity)

    @Query("DELETE FROM osint_cache WHERE address = :address")
    suspend fun deleteReport(address: String)

    @Query("SELECT * FROM osint_cache ORDER BY timestamp DESC")
    fun getAllCached(): Flow<List<OsintCacheEntity>>

    // IP Association methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIpAssociations(ips: List<OsintIpEntity>)

    @Query("SELECT * FROM osint_ip_associations WHERE address = :address")
    suspend fun getIpAssociations(address: String): List<OsintIpEntity>

    // Entity Tag methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntityTag(tag: OsintEntityTagEntity)

    @Query("SELECT * FROM osint_entity_tags WHERE address = :address")
    suspend fun getEntityTag(address: String): OsintEntityTagEntity?

    // Threat Indicators
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreatIndicators(indicators: List<OsintThreatIndicatorEntity>)

    @Query("SELECT * FROM osint_threat_indicators WHERE indicatorValue = :value OR indicatorValue = :address")
    suspend fun getThreatIndicators(value: String, address: String): List<OsintThreatIndicatorEntity>
    
    @Query("SELECT * FROM osint_threat_indicators ORDER BY timestamp DESC LIMIT 50")
    fun getAllThreatIndicators(): Flow<List<OsintThreatIndicatorEntity>>

    // Extracted Indicators
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtractedIndicators(indicators: List<ExtractedIndicatorEntity>)

    @Query("SELECT * FROM osint_extracted_indicators WHERE targetAddress = :address ORDER BY extractionConfidence DESC")
    suspend fun getExtractedIndicators(address: String): List<ExtractedIndicatorEntity>

    @Query("SELECT * FROM osint_extracted_indicators WHERE targetAddress = :address ORDER BY extractionConfidence DESC")
    fun getExtractedIndicatorsFlow(address: String): Flow<List<ExtractedIndicatorEntity>>

    // Attribution Confidence
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributionConfidence(attribution: AttributionConfidenceEntity)

    @Query("SELECT * FROM osint_attribution_confidence WHERE targetAddress = :address")
    suspend fun getAttributionConfidence(address: String): AttributionConfidenceEntity?

    // Forensic Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForensicEvents(events: List<ForensicEventEntity>)

    @Query("SELECT * FROM osint_forensic_events WHERE targetAddress = :address ORDER BY timestamp DESC")
    fun getForensicEventsFlow(address: String): Flow<List<ForensicEventEntity>>

    @Query("SELECT * FROM osint_forensic_events ORDER BY timestamp DESC LIMIT 100")
    fun getAllForensicEventsFlow(): Flow<List<ForensicEventEntity>>

    // Deep Identity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeepIdentityResult(result: DeepIdentityResultEntity)

    @Query("SELECT * FROM osint_deep_identity WHERE targetEmailOrPhone = :key OR associatedCryptoAddress = :key")
    suspend fun getDeepIdentityResult(key: String): DeepIdentityResultEntity?

    @Query("SELECT * FROM osint_deep_identity ORDER BY timestamp DESC")
    fun getAllDeepIdentitiesFlow(): Flow<List<DeepIdentityResultEntity>>
}

@Dao
interface PriceRateDao {
    @Query("SELECT * FROM price_rates WHERE pair = :pair ORDER BY timestamp DESC")
    suspend fun getRatesForPair(pair: String): List<PriceRateEntity>

    @Query("SELECT * FROM price_rates WHERE pair = :pair AND source = :source ORDER BY timestamp DESC LIMIT 1")
    suspend fun getRate(pair: String, source: String): PriceRateEntity?

    @Query("SELECT * FROM price_rates WHERE pair = :pair ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRate(pair: String): PriceRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(entity: PriceRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(entities: List<PriceRateEntity>)

    @Query("SELECT * FROM price_rates ORDER BY timestamp DESC")
    fun getAllRates(): Flow<List<PriceRateEntity>>
}

@Database(
    entities = [
        OsintCacheEntity::class,
        OsintIpEntity::class,
        OsintEntityTagEntity::class,
        OsintThreatIndicatorEntity::class,
        PriceRateEntity::class,
        ExtractedIndicatorEntity::class,
        AttributionConfidenceEntity::class,
        ForensicEventEntity::class,
        DeepIdentityResultEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class OsintDatabase : RoomDatabase() {
    abstract fun osintCacheDao(): OsintCacheDao
    abstract fun priceRateDao(): PriceRateDao

    companion object {
        @Volatile
        private var INSTANCE: OsintDatabase? = null

        fun getDatabase(context: Context): OsintDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OsintDatabase::class.java,
                    "osint_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
