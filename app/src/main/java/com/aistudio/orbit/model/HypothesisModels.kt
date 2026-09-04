package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
enum class HypothesisReviewState {
    DRAFT,
    PENDING_REVIEW,
    REJECTED,
    REVISED,
    ACCEPTED
}

@Serializable
enum class EvidenceSupportLevel {
    SUPPORTING,
    CONTRADICTORY,
    NEUTRAL
}

@Serializable
data class HypothesisEvidenceLink(
    val evidenceId: String,
    val supportLevel: EvidenceSupportLevel,
    val analystNote: String = ""
)

@Serializable
data class Hypothesis(
    val id: String,
    val investigationId: String,
    val caseId: String,
    val title: String,
    val description: String,
    val confidence: ConfidenceLevel,
    val reviewState: HypothesisReviewState = HypothesisReviewState.DRAFT,
    val evidenceLinks: List<HypothesisEvidenceLink> = emptyList(),
    val analystNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String,
    val version: Int = 1
)
