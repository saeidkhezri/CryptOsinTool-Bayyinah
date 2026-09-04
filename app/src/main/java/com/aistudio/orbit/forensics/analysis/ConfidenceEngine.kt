package com.aistudio.orbit.forensics.analysis

import com.aistudio.orbit.model.ConfidenceLevel
import com.aistudio.orbit.model.EvidenceItem
import com.aistudio.orbit.model.EvidencePolarity

data class AggregateConfidenceMetrics(
    val baseConfidence: ConfidenceLevel,
    val adjustedScore: Float,
    val independentSourceCount: Int,
    val totalSupportingEvidence: Int,
    val totalContradictoryEvidence: Int,
    val explanationEn: String,
    val explanationFa: String
)

object ConfidenceEngine {

    /**
     * Calculates the aggregate confidence of a finding or hypothesis based on supporting and contradictory evidence.
     * Enforces source independence rules (mirrors do not increase confidence).
     */
    fun evaluateConfidence(evidenceList: List<EvidenceItem>): AggregateConfidenceMetrics {
        if (evidenceList.isEmpty()) {
            return AggregateConfidenceMetrics(
                baseConfidence = ConfidenceLevel.UNCERTAIN,
                adjustedScore = 0.0f,
                independentSourceCount = 0,
                totalSupportingEvidence = 0,
                totalContradictoryEvidence = 0,
                explanationEn = "No evidence provided.",
                explanationFa = "هیچ شواهدی ارائه نشده است."
            )
        }

        // Group by Source Family to determine independence
        val independentSources = evidenceList
            .filter { it.polarity == EvidencePolarity.POSITIVE_FINDING }
            .map { it.sourceIndependence.sourceFamily }
            .distinct()

        val independentSourceCount = independentSources.size
        
        val supporting = evidenceList.count { it.polarity == EvidencePolarity.POSITIVE_FINDING }
        val contradictory = evidenceList.count { it.polarity == EvidencePolarity.NEGATIVE_FINDING }

        // Logic: Start with base score from highest confidence positive evidence
        val maxBaseScore = evidenceList
            .filter { it.polarity == EvidencePolarity.POSITIVE_FINDING }
            .maxOfOrNull { it.confidence.weight } ?: 0.0f

        // Adjust based on independent corroboration
        var adjustedScore = maxBaseScore
        
        if (independentSourceCount >= 2) {
            adjustedScore += 0.15f // Bonus for multi-source
        }
        
        // Penalize for contradiction
        if (contradictory > 0) {
            val penalty = contradictory * 0.10f
            adjustedScore -= penalty
        }
        
        adjustedScore = adjustedScore.coerceIn(0.0f, 1.0f)

        val finalLevel = when {
            adjustedScore >= 0.95f -> ConfidenceLevel.DEFINITIVE_FACT
            adjustedScore >= 0.80f -> ConfidenceLevel.HIGH_CONFIDENCE
            adjustedScore >= 0.50f -> ConfidenceLevel.MEDIUM_CONFIDENCE
            adjustedScore >= 0.30f -> ConfidenceLevel.LOW_CONFIDENCE
            else -> ConfidenceLevel.UNCERTAIN
        }

        val explEn = buildString {
            append("Evaluated ${evidenceList.size} total items. ")
            append("Derived from $independentSourceCount independent source(s) (${independentSources.joinToString()}). ")
            if (contradictory > 0) append("Confidence reduced due to $contradictory contradictory observation(s).")
        }

        val explFa = buildString {
            append("ارزیابی مجموع ${evidenceList.size} آیتم. ")
            append("استخراج شده از $independentSourceCount منبع مستقل (${independentSources.joinToString()}). ")
            if (contradictory > 0) append("کاهش اطمینان به دلیل وجود $contradictory مشاهده متناقض.")
        }

        return AggregateConfidenceMetrics(
            baseConfidence = finalLevel,
            adjustedScore = adjustedScore,
            independentSourceCount = independentSourceCount,
            totalSupportingEvidence = supporting,
            totalContradictoryEvidence = contradictory,
            explanationEn = explEn.trim(),
            explanationFa = explFa.trim()
        )
    }
}
