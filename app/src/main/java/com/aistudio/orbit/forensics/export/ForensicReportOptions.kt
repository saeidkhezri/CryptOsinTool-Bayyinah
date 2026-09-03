package com.aistudio.orbit.forensics.export

/**
 * Granular configuration model for forensic and judicial report generation.
 * By default, only pure Technical Forensic analysis is selected.
 * Legal, subpoena, and judicial sections are optional and disabled by default.
 */
data class ForensicReportOptions(
    // Pure Technical Forensic Sections (Default: TRUE)
    val includeSummary: Boolean = true,
    val includeLedgerFacts: Boolean = true,
    val includeTransactions: Boolean = true,
    val includeGraphDiagram: Boolean = true,
    val includeGraphTopology: Boolean = true,
    val includeAddressIntel: Boolean = true,
    val includeOsintCrossIdentities: Boolean = true,
    val includeEvidenceSealSha256: Boolean = true,
    val includeAnalysisLimitations: Boolean = true,
    
    // AI Copilot Integration
    val aiCopilotSummary: String? = null,

    // Optional Legal & Formal Judicial Sections (Default: FALSE)
    val includeLegalStructure: Boolean = false,
    val includeCourtAdmissibilityFramework: Boolean = false,
    val includeExchangeSubpoena: Boolean = false,
    val includeJudiciaryRequest: Boolean = false,
    val includeExpertWitnessConclusion: Boolean = false
) {
    val hasAnyLegalSection: Boolean
        get() = includeLegalStructure || includeCourtAdmissibilityFramework || includeExchangeSubpoena || includeJudiciaryRequest || includeExpertWitnessConclusion
}
