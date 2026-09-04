package com.aistudio.orbit.forensics.osint

import com.aistudio.orbit.forensics.analysis.ConfidenceEngine
import com.aistudio.orbit.model.*

/** Correlates observations without turning correlation into identity or criminal attribution. */
object OsintCorrelationEngine {
    data class CorrelationObservation(
        val code: String,
        val title: String,
        val description: String,
        val confidence: ConfidenceLevel,
        val supportingEvidenceIds: List<String>,
        val contradictoryEvidenceIds: List<String> = emptyList()
    )

    fun generateCorrelationObservations(caseObj: InvestigationCase, osintReport: OsintAnalysisReport?): List<CorrelationObservation> {
        if (osintReport == null) return emptyList()
        val out = mutableListOf<CorrelationObservation>()
        val highVelocity = caseObj.matchedPatterns.find { it.patternCode == "PTN-010" }
        if (highVelocity != null && osintReport.torVpnProbability > 50f) {
            out += CorrelationObservation(
                "CORR-001", "Temporal/network correlation",
                "A high-velocity on-chain observation co-occurs with an OSINT-derived Tor/VPN probability above the configured threshold. This is a correlation only and does not establish common ownership or intent.",
                ConfidenceLevel.MEDIUM_CONFIDENCE, highVelocity.evidenceIds
            )
        }
        val mixerPattern = caseObj.matchedPatterns.find { it.category.name.contains("MIXER", ignoreCase=true) }
        if (mixerPattern != null && osintReport.leakRecords.isNotEmpty()) {
            out += CorrelationObservation(
                "CORR-002", "Mixer-related public-source correlation",
                "A mixer-related on-chain pattern co-occurs with public leak records. The records require independent source validation and do not by themselves identify an operator or participant.",
                ConfidenceLevel.LOW_CONFIDENCE, mixerPattern.evidenceIds
            )
        }
        return out
    }

    /** Backward-compatible API. Hypotheses are created only from reviewed evidence. */
    fun generateCorrelationHypotheses(caseObj: InvestigationCase, osintReport: OsintAnalysisReport?): List<Hypothesis> {
        val observations = generateCorrelationObservations(caseObj, osintReport)
        return observations.mapNotNull { obs ->
            if (obs.supportingEvidenceIds.isEmpty()) return@mapNotNull null
            var h = com.aistudio.orbit.forensics.analysis.HypothesisEngine.createHypothesis(
                caseId=caseObj.caseId, investigationId=caseObj.caseId,
                title=obs.title,
                description=obs.description,
                confidence=obs.confidence,
                author="Correlation Engine"
            )
            obs.supportingEvidenceIds.forEach { id ->
                h = com.aistudio.orbit.forensics.analysis.HypothesisEngine.addEvidenceLink(
                    hypothesis = h,
                    evidenceId = id,
                    supportLevel = EvidenceSupportLevel.SUPPORTING,
                    analystNote = "Correlated evidence from OSINT engine",
                    author = "Correlation Engine"
                )
            }
            h
        }
    }
}
