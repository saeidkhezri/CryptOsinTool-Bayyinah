package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

/**
 * ============================================================================
 * CANONICAL FORENSIC OBSERVATION (Master Instruction §1-§5, §18)
 * ============================================================================
 * Universal, immutable observation unit for on-chain facts, OSINT breadcrumbs,
 * threat intelligence items, and provider responses with full cryptographic provenance.
 */
@Serializable
data class ForensicObservation(
    val id: String = "OBS_${System.currentTimeMillis()}_${(1000..9999).random()}",
    val caseId: String,
    val investigationId: String,
    val subject: String,                  // Target identifier (address, txid, domain, IP, email, username)
    val source: String,                   // Source name (e.g. "mempool.space", "OFAC SDN", "Google DoH")
    val sourceType: DataSourceType = DataSourceType.PUBLIC_EXPLORER_API,
    val sourceUrl: String = "",           // Exact query URL / endpoint called
    val observedAt: Long = System.currentTimeMillis(), // When occurred on source / ledger
    val retrievedAt: Long = System.currentTimeMillis(),// When retrieved by Bayyinah
    val rawValue: String,                 // Raw string response or payload
    val normalizedValue: String,          // Normalized canonical value
    val method: String,                   // Query method or heuristic used
    val provider: String,                 // Provider ID or module identifier
    val datasetId: String? = null,        // Offline dataset ID if applicable
    val epistemicType: EpistemicType = EpistemicType.OBSERVATION,
    val confidence: Float = 1.0f,         // 0.0 to 1.0 numeric confidence
    val rawHash: String = "",             // SHA-256 fingerprint of rawValue
    val analystNotes: String = "",        // Optional notes from investigator
    val isNegativeEvidence: Boolean = false // Explicit negative evidence flag (Master Instruction §17)
) {
    val isDirectFact: Boolean get() = epistemicType == EpistemicType.FACT
}
