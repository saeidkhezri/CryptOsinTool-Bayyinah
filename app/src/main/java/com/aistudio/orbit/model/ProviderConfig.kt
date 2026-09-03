package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderStatus {
    HEALTHY,
    DEGRADED,
    RATE_LIMITED,
    ERROR,
    NOT_CONFIGURED
}

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
    val status: ProviderStatus = ProviderStatus.HEALTHY,
    val lastResponseTimeMs: Long = 0,
    val lastCheckedTimestamp: Long = 0,
    val isEnabled: Boolean = true
)
