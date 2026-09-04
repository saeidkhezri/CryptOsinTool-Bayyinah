package com.aistudio.orbit.forensics.osint

import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.model.PatternMatchResult
import com.aistudio.orbit.model.EvidenceItem
import com.aistudio.orbit.model.EvidenceSupportLevel
import com.aistudio.orbit.forensics.analysis.HypothesisEngine
import com.aistudio.orbit.model.ConfidenceLevel
import java.util.UUID

object OsintCorrelationEngine {

    fun generateCorrelationHypotheses(
        caseObj: InvestigationCase,
        osintReport: OsintAnalysisReport?
    ): List<com.aistudio.orbit.model.Hypothesis> {
        val newHypotheses = mutableListOf<com.aistudio.orbit.model.Hypothesis>()
        
        if (osintReport == null) return newHypotheses

        // Example Correlation: High Velocity Transit + Tor/VPN
        val highVelocity = caseObj.matchedPatterns.find { it.patternCode == "PTN-010" }
        if (highVelocity != null && osintReport.torVpnProbability > 50f) {
            newHypotheses.add(
                HypothesisEngine.createHypothesis(
                    caseId = caseObj.id,
                    investigationId = caseObj.id,
                    title = "Automated Obfuscation Network (Correlated)",
                    description = "On-chain High Velocity Transit correlates with off-chain Tor/VPN routing, indicating an automated obfuscation service rather than manual user transfers.",
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    author = "OSINT Correlator Engine"
                )
            )
        }

        // Example Correlation: Mixer + Extracted Entities
        val mixerPattern = caseObj.matchedPatterns.find { it.category.name == "MIXER_OBFUSCATION" }
        if (mixerPattern != null && osintReport.leakRecords.isNotEmpty()) {
             newHypotheses.add(
                HypothesisEngine.createHypothesis(
                    caseId = caseObj.id,
                    investigationId = caseObj.id,
                    title = "Deanonymized Mixer Participant (Correlated)",
                    description = "Mixer obfuscation pattern detected on-chain, but OSINT leak records expose the underlying entity.",
                    confidence = ConfidenceLevel.MEDIUM_CONFIDENCE,
                    author = "OSINT Correlator Engine"
                )
            )
        }

        return newHypotheses
    }
}
