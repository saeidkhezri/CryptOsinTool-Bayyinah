package com.aistudio.orbit.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aistudio.orbit.model.InvestigationCase
import kotlinx.serialization.Serializable

/**
 * Epistemic status of a piece of forensic information.
 * As defined in Master Instruction Section 4:
 * FACT: Directly observed or provable data (e.g. on-chain block/tx).
 * INFERENCE: Result deduced from multiple facts or analytical heuristics (e.g. common-input clustering).
 * HYPOTHESIS: Plausible working assumption not yet proven.
 * UNKNOWN: Information without reliable supporting evidence.
 */
@Serializable
enum class EpistemicStatus {
    FACT,
    OBSERVED_FACT,
    EXTERNAL_SOURCE,
    CALCULATED,
    DERIVED_CALCULATION,
    STATISTICAL_ESTIMATE,
    INFERENCE,
    ANALYTICAL_INFERENCE,
    HYPOTHESIS,
    INVESTIGATOR_ASSESSMENT,
    UNKNOWN
}

/**
 * Dedicated Room Entity for immutable forensic evidence records (Master Instruction §18).
 */
@Serializable
@Entity(
    tableName = "evidence_records",
    indices = [
        Index(value = ["caseId"]),
        Index(value = ["investigationId"]),
        Index(value = ["address"]),
        Index(value = ["transactionHash"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = InvestigationCase::class,
            parentColumns = ["caseId"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EvidenceEntity(
    @PrimaryKey
    val evidenceId: String,
    val caseId: String,
    val investigationId: String = "",
    val observation: String,
    val claim: String,
    val source: String,
    val sourceUrl: String = "",
    val sourceType: String = "ON_CHAIN_RPC",
    val retrievedAt: Long = System.currentTimeMillis(),
    val observedAt: Long = System.currentTimeMillis(),
    val blockchain: String = "BITCOIN",
    val address: String? = null,
    val transactionHash: String? = null,
    val entityName: String? = null,
    val method: String = "Direct Ledger Inspection",
    val confidence: Float = 1.0f,
    val epistemicStatus: EpistemicStatus = EpistemicStatus.FACT,
    val analystNote: String = "",
    val hash: String = "",
    val previousHash: String = "",
    val isFinalized: Boolean = false,
    val metadataJson: String = "{}"
)

/**
 * Dedicated Room Entity for Investigation Findings & Hypotheses (Master Instruction §23).
 */
@Serializable
@Entity(
    tableName = "investigation_findings",
    indices = [
        Index(value = ["caseId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = InvestigationCase::class,
            parentColumns = ["caseId"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FindingEntity(
    @PrimaryKey
    val findingId: String,
    val caseId: String,
    val title: String,
    val titleFa: String = title,
    val description: String,
    val descriptionFa: String = description,
    val severity: String = "INFO", // INFO, LOW, MEDIUM, HIGH, CRITICAL
    val category: String = "GENERAL",
    val epistemicStatus: EpistemicStatus = EpistemicStatus.INFERENCE,
    val confidenceScore: Float = 0.85f,
    val supportingEvidenceIdsJson: String = "[]",
    val relatedAddressesJson: String = "[]",
    val relatedTxHashesJson: String = "[]",
    val recommendedAction: String = "",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for Address intelligence and clustering (Master Instruction §5).
 */
@Serializable
@Entity(
    tableName = "investigation_addresses",
    indices = [
        Index(value = ["caseId"]),
        Index(value = ["address"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = InvestigationCase::class,
            parentColumns = ["caseId"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AddressEntity(
    @PrimaryKey
    val addressId: String,
    val caseId: String,
    val address: String,
    val network: String = "BITCOIN",
    val label: String = "",
    val entityClusterName: String = "",
    val riskScore: Float = 0.0f,
    val totalReceivedSat: Long = 0L,
    val totalSentSat: Long = 0L,
    val balanceSat: Long = 0L,
    val txCount: Int = 0,
    val firstSeenTimestamp: Long = 0L,
    val lastSeenTimestamp: Long = 0L,
    val isMixerExposure: Boolean = false,
    val isSanctioned: Boolean = false,
    val metadataJson: String = "{}"
)

/**
 * Dedicated Room Entity for Transaction forensic data (Master Instruction §7).
 */
@Serializable
@Entity(
    tableName = "investigation_transactions",
    indices = [
        Index(value = ["caseId"]),
        Index(value = ["txHash"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = InvestigationCase::class,
            parentColumns = ["caseId"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val txId: String,
    val caseId: String,
    val txHash: String,
    val blockchain: String = "BITCOIN",
    val timestamp: Long = 0L,
    val blockHeight: Long = 0L,
    val amountSat: Long = 0L,
    val feeSat: Long = 0L,
    val senderAddress: String = "",
    val receiverAddress: String = "",
    val isCoinbase: Boolean = false,
    val riskScore: Float = 0.0f,
    val epistemicStatus: EpistemicStatus = EpistemicStatus.FACT,
    val metadataJson: String = "{}"
)

/**
 * Dedicated Room Entity for Forensic Audit Logs (Master Instruction §32).
 */
@Serializable
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actor: String = "Analyst",
    val action: String,
    val caseId: String? = null,
    val investigationId: String? = null,
    val targetEntity: String = "",
    val beforeStateJson: String? = null,
    val afterStateJson: String? = null,
    val source: String = "Internal",
    val timestamp: Long = System.currentTimeMillis(),
    val checksum: String = ""
)

/**
 * Dedicated Room Entity for GraphSense TagPacks records (Master Instruction §11, §12).
 */
@Serializable
@Entity(
    tableName = "tagpack_records",
    indices = [
        androidx.room.Index(value = ["address"]),
        androidx.room.Index(value = ["tagpackId"]),
        androidx.room.Index(value = ["entity"])
    ]
)
data class TagPackEntity(
    @PrimaryKey
    val recordId: String,
    val address: String,
    val currency: String = "BTC",
    val label: String,
    val entity: String,
    val category: String = "exchange",
    val tagpackId: String,
    val tagpackTitle: String = "GraphSense Curated TagPack",
    val tagpackVersion: String = "1.0.0",
    val source: String = "https://github.com/graphsense/graphsense-tagpacks",
    val sourceUrl: String = "",
    val confidence: Float = 1.0f,
    val isInherited: Boolean = false,
    val license: String = "CC-BY-4.0",
    val retrievedAt: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)

/**
 * Dedicated Room Entity for OFAC and OpenSanctions records (Master Instruction §14, §16).
 */
@Serializable
@Entity(
    tableName = "sanction_records",
    indices = [
        androidx.room.Index(value = ["datasetSource"]),
        androidx.room.Index(value = ["primaryName"])
    ]
)
data class SanctionEntity(
    @PrimaryKey
    val sanctionId: String,
    val datasetSource: String, // OFAC_SDN, OFAC_NON_SDN, OPENSANCTIONS_TIER_A, UN_SANCTIONS, EU_SANCTIONS
    val datasetVersion: String = "latest",
    val schemaType: String = "CryptoAddress", // CryptoAddress, Person, Company, Vessel
    val primaryName: String,
    val primaryNameFa: String = primaryName,
    val aliasesJson: String = "[]",
    val cryptoAddressesJson: String = "[]",
    val identifiersJson: String = "[]",
    val program: String = "",
    val country: String = "",
    val effectiveFrom: Long? = null,
    val effectiveTo: Long? = null,
    val isHistorical: Boolean = false,
    val publishedAt: Long = System.currentTimeMillis(),
    val retrievedAt: Long = System.currentTimeMillis(),
    val rawPayload: String = "{}"
)

/**
 * Dedicated Room Entity for Offline Dataset Management (Master Instruction §9, §29, §30).
 */
@Serializable
@Entity(tableName = "dataset_metadata")
data class DatasetMetadataEntity(
    @PrimaryKey
    val datasetId: String,
    val name: String,
    val nameFa: String = name,
    val tier: String = "TIER_A_ANDROID", // TIER_A_ANDROID, TIER_B_LARGE_ANDROID_OPTIONAL, TIER_C_DESKTOP_SERVER
    val category: String = "SANCTIONS", // SANCTIONS, TAGPACKS, GEOIP, THREAT_INTEL, PSL
    val version: String = "1.0",
    val downloadSizeBytes: Long = 0L,
    val installedSizeBytes: Long = 0L,
    val requiredTempStorageBytes: Long = 0L,
    val sourceUrl: String = "",
    val sha256Checksum: String = "",
    val license: String = "Public / Open Access",
    val updateCadence: String = "WEEKLY", // DAILY, WEEKLY, MONTHLY, STATIC
    val status: String = "AVAILABLE", // AVAILABLE, DOWNLOADING, PAUSED, INSTALLED, ERROR, DISABLED
    val downloadProgress: Float = 0.0f,
    val lastUpdated: Long = System.currentTimeMillis(),
    val recordCount: Int = 0,
    val isEnabled: Boolean = true
)
