package com.aistudio.orbit.forensics.ai.search

import kotlinx.serialization.Serializable

@Serializable
enum class EvidenceCandidateStatus {
    PENDING_REVIEW,
    APPROVED_EVIDENCE,
    DISCARDED
}

@Serializable
data class GroundedSearchSource(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String,
    val sourceCategory: String = "Web / OSINT",
    val provider: String = "You.com Search API",
    val queryUsed: String = "",
    val retrievedAt: Long = System.currentTimeMillis(),
    val relevanceScore: Float = 0.85f
)

@Serializable
data class SearchEvidenceCandidate(
    val id: String,
    val claim: String,
    val source: GroundedSearchSource,
    val confidence: Float = 0.80f,
    val status: EvidenceCandidateStatus = EvidenceCandidateStatus.PENDING_REVIEW,
    val analystNotes: String = ""
)

@Serializable
data class SearchPrivacyFilterResult(
    val sanitizedQuery: String,
    val redactedTermsCount: Int,
    val warnings: List<String>
)
