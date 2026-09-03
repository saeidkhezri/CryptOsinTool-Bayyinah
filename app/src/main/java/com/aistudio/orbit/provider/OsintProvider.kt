package com.aistudio.orbit.provider

import com.aistudio.orbit.forensics.osint.OsintAnalysisReport
import com.aistudio.orbit.forensics.osint.OsintCacheEntity
import com.aistudio.orbit.forensics.osint.OsintIpExposure
import com.aistudio.orbit.forensics.osint.OsintLeakRecord
import com.aistudio.orbit.model.BlockchainNetwork
import kotlinx.serialization.Serializable

@Serializable
data class ThreatIntelIndicator(
    val indicatorType: String, // "IP", "Address", "Domain", "Email", "ASN"
    val indicatorValue: String,
    val threatCategory: String, // "Malware C2", "Sanctioned Entity", "Darknet Market", "Known Mixer", "Phishing / Scam"
    val threatCategoryFa: String,
    val riskScore: Int, // 0 to 100
    val reporter: String,
    val reportedDate: String,
    val notesEn: String,
    val notesFa: String,
    val sourceUrl: String = "",
    val verifiabilityDetailsFa: String = "قابل راستی‌آزمایی در پایگاه‌های عمومی و گزارش‌های رسمی",
    val verifiabilityDetailsEn: String = "Verifiable via public intelligence feeds and official regulatory filings"
)

@Serializable
data class AddressOsintMetadata(
    val address: String,
    val network: String,
    val knownEntity: String?,
    val knownEntityFa: String?,
    val entityCategory: String?,
    val entityCategoryFa: String?,
    val associatedDomains: List<String>,
    val publicLabels: List<String>,
    val riskScore: Int, // 0 to 100
    val attributionSource: String,
    val attributionSourceFa: String,
    val verificationEvidence: String,
    val verificationEvidenceFa: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class OsintVerifiableEvidence(
    val evidenceId: String,
    val category: String, // "LEDGER_FACT", "NETWORK_OBSERVATION", "OSINT_INFERENCE", "SANCTIONS_INDICATOR"
    val categoryFa: String,
    val title: String,
    val titleFa: String,
    val detailsEn: String,
    val detailsFa: String,
    val sourceName: String,
    val sourceUrl: String,
    val verificationMethodFa: String,
    val verificationMethodEn: String,
    val epistemicType: String // "FACT", "OBSERVATION", "CALCULATION", "INFERENCE", "HYPOTHESIS"
)

interface OsintProvider {
    val id: String
    val name: String
    val isFree: Boolean
    val requiresApiKey: Boolean

    suspend fun fetchIpMetadata(ipAddress: String): Result<OsintIpExposure>
    suspend fun queryDomainAssociations(address: String): Result<List<String>>
    suspend fun checkThreatIntelligence(indicator: String): Result<List<ThreatIntelIndicator>>
    suspend fun fetchIdentityLeaks(emailOrAlias: String): Result<List<OsintLeakRecord>>
    suspend fun fetchAddressMetadata(address: String, network: BlockchainNetwork): Result<AddressOsintMetadata>
    suspend fun fetchVerifiableEvidenceChain(address: String, network: BlockchainNetwork): Result<List<OsintVerifiableEvidence>>

    // Room Database Caching & Offline Storage Methods
    suspend fun getCachedReport(address: String): OsintAnalysisReport?
    suspend fun cacheReport(address: String, network: BlockchainNetwork, report: OsintAnalysisReport)
    suspend fun deleteCachedReport(address: String)
}

