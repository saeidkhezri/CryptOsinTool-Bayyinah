package com.aistudio.orbit.forensics.osint.contract

import com.aistudio.orbit.forensics.osint.bus.IndicatorType
import com.aistudio.orbit.forensics.osint.bus.OsintEvent
import kotlinx.serialization.Serializable

@Serializable
enum class AuthRequirement {
    NONE_PUBLIC,
    API_KEY_OPTIONAL,
    API_KEY_MANDATORY,
    OAUTH_SESSION_REQUIRED
}

@Serializable
enum class PrivacyImpact {
    PASSIVE_LOCAL_LOOKUP,
    PASSIVE_PUBLIC_DNS_HTTP,
    ACTIVE_PROBING_INTERNET
}

@Serializable
enum class ModuleExecutionStatus {
    NATIVE_READY,
    PLATFORM_LIMITED,
    REQUIRES_EXTERNAL_SERVICE,
    DISABLED
}

data class OsintExecutionContext(
    val caseId: String,
    val investigationId: String = "",
    val allowActiveNetwork: Boolean = true,
    val isOfflineOnly: Boolean = false,
    val timeoutMs: Long = 10000L
)

/**
 * Standard Forensic OSINT Module Contract (Master Instruction §3).
 * Every collector, recon adapter, and intelligence provider implements this contract.
 */
interface OsintModuleContract {
    val name: String
    val version: String
    val repository: String
    val license: String
    val inputTypes: Set<IndicatorType>
    val outputTypes: Set<IndicatorType>
    val capabilities: List<String>
    val authentication: AuthRequirement
    val rateLimits: String
    val privacy: PrivacyImpact
    val runtimeRequirements: String
    val confidenceSemantics: String
    val failureModes: List<String>
    val executionStatus: ModuleExecutionStatus

    /**
     * Executes reconnaissance against the incoming indicator event.
     * Returns a list of new discovered OSINT events to be published to the Event Bus.
     */
    suspend fun execute(inputEvent: OsintEvent, context: OsintExecutionContext): List<OsintEvent>
}
