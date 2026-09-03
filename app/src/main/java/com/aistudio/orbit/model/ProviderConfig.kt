package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

/**
 * Provider Status (Prompt 3 §7, Master Instruction §35 & §36)
 */
@Serializable
enum class ProviderStatus {
    CONFIGURED,
    NOT_CONFIGURED,
    TESTING,
    HEALTHY,
    DEGRADED,
    FAILED,
    DISABLED,
    UNVERIFIED
}

/**
 * Detailed real-time health telemetry from actual connectivity ping/test.
 */
@Serializable
data class ProviderHealthMetric(
    val providerId: String,
    val status: ProviderStatus,
    val httpStatusCode: Int? = null,
    val latencyMs: Long = 0,
    val rateLimitRemaining: Int? = null,
    val lastError: String? = null,
    val checkedAt: Long = System.currentTimeMillis()
)

/**
 * Provider Disagreement (Prompt 3 §7, Master Instruction §16 & §32)
 * Recorded when two independent providers report conflicting ledger or intelligence data.
 * The system preserves both observations rather than silently overwriting.
 */
@Serializable
data class ProviderDisagreement(
    val disagreementId: String,
    val address: String,
    val network: BlockchainNetwork,
    val discrepancyType: String, // "BALANCE", "TRANSACTION_COUNT", "FIRST_SEEN", "ATTRIBUTION"
    val providerAName: String,
    val providerAValue: String,
    val providerBName: String,
    val providerBValue: String,
    val timestamp: Long = System.currentTimeMillis(),
    val analystNotes: String = ""
)

@Serializable
data class ApiProviderConfig(
    val id: String,
    val name: String,
    val network: BlockchainNetwork,
    val baseUrl: String,
    val apiKeyPrimary: String = "",
    val apiKeySecondary: String = "",
    val isFree: Boolean = true,
    val requiresKey: Boolean = false,
    val officialUrl: String = "",
    val helpSummaryFa: String = "",
    val helpSummaryEn: String = "",
    val rateLimitPerMin: Int = 30,
    val status: ProviderStatus = ProviderStatus.NOT_CONFIGURED,
    val lastResponseTimeMs: Long = 0,
    val lastCheckedTimestamp: Long = 0,
    val httpStatusCode: Int? = null,
    val rateLimitRemaining: Int? = null,
    val isEnabled: Boolean = true
)
