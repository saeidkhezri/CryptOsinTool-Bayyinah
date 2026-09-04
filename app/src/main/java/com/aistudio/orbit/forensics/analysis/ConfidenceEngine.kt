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
     * Evidence-weighted confidence for findings/hypotheses. This score is deliberately
     * capped below DEFINITIVE_FACT because this engine evaluates derived claims.
     * A ledger observation may be definitive; an inference from observations is not.
     */
    fun evaluateConfidence(evidenceList: List<EvidenceItem>): AggregateConfidenceMetrics {
        if (evidenceList.isEmpty()) return AggregateConfidenceMetrics(ConfidenceLevel.UNCERTAIN, 0f, 0, 0, 0, "No evidence provided.", "هیچ شواهدی ارائه نشده است.")
        val positives = evidenceList.filter { it.polarity == EvidencePolarity.POSITIVE_FINDING }
        val negatives = evidenceList.filter { it.polarity == EvidencePolarity.NEGATIVE_FINDING }
        val families = positives.map { it.sourceIndependence.sourceFamily.trim() }.filter { it.isNotBlank() }.distinct()
        val maxBase = positives.maxOfOrNull { it.confidence.weight } ?: 0f
        val corroboration = when {
            families.size >= 3 -> 0.10f
            families.size == 2 -> 0.06f
            else -> 0f
        }
        val contradictionPenalty = (negatives.size * 0.08f).coerceAtMost(0.32f)
        val supportBreadth = ((positives.size - 1).coerceAtLeast(0) * 0.02f).coerceAtMost(0.08f)
        val adjusted = (maxBase + corroboration + supportBreadth - contradictionPenalty).coerceIn(0f, 0.94f)
        val level = when {
            adjusted >= 0.80f -> ConfidenceLevel.HIGH_CONFIDENCE
            adjusted >= 0.50f -> ConfidenceLevel.MEDIUM_CONFIDENCE
            adjusted >= 0.30f -> ConfidenceLevel.LOW_CONFIDENCE
            else -> ConfidenceLevel.UNCERTAIN
        }
        val sourceText = if (families.isEmpty()) "no independent source families" else families.joinToString()
        return AggregateConfidenceMetrics(
            level, adjusted, families.size, positives.size, negatives.size,
            "Derived confidence from ${evidenceList.size} evidence items; $sourceText; contradictions=${negatives.size}. The result is an inference, not a definitive fact.",
            "اطمینان از ${evidenceList.size} مورد شواهد محاسبه شد؛ خانواده‌های منبع مستقل: $sourceText؛ شواهد متناقض: ${negatives.size}. این نتیجه استنباطی است و «حقیقت قطعی» محسوب نمی‌شود."
        )
    }
}
