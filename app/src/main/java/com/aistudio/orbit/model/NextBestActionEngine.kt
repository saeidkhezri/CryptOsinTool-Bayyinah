package com.aistudio.orbit.model

object NextBestActionEngine {
    fun determineNextBestAction(case: InvestigationCase?, hasOsint:Boolean, hasPatterns:Boolean, hasRisks:Boolean, hasHypotheses:Boolean): NextBestAction {
        if (case == null) return NextBestAction(
            "Start New Investigation or Quick Check", "No active investigation is loaded.", 0, "High", "Target Address", "START_CASE", 100, "None", "START"
        )
        val evidence = case.evidenceLog.size
        val candidates = buildList {
            if (case.targetAddress.isBlank()) add(NextBestAction("Validate target address", "The case has no usable target address.", evidence, "Very High", "Target Address + Network", "INITIAL_LEAD", 100, "None", "VALIDATE_INPUT"))
            if (case.transactions.isEmpty() && case.totalTransactionsFound == 0) add(NextBestAction("Run or retry ledger discovery", "No confirmed transaction-discovery result is recorded. Zero balance alone does not prove an empty history.", evidence, "Very High", "None", "BLOCKCHAIN_DISCOVERY", 95, "Provider quota", "DISCOVER_TRANSACTIONS"))
            if (case.counterparties.isEmpty() && case.transactions.isNotEmpty()) add(NextBestAction("Extract related addresses", "Transactions exist but the relationship set has not been derived.", evidence, "High", "None", "RELATED_ADDRESSES", 90, "Local compute / provider expansion", "EXTRACT_COUNTERPARTIES"))
            if (!hasPatterns && case.counterparties.isNotEmpty()) add(NextBestAction("Evaluate approved typologies", "Compare observed structure against versioned indicators and counter-indicators.", evidence, "High", "None", "PATTERN_ANALYSIS", 85, "Local compute", "RUN_PATTERNS"))
            if (!hasOsint && (hasPatterns || case.counterparties.isNotEmpty())) add(NextBestAction("Review public OSINT sources", "Seek corroborating or contradictory public information without treating search hits as identity proof.", evidence, "High", "None", "OSINT_REVIEW", 80, "Search quota", "RUN_OSINT"))
            if (!hasRisks && hasOsint) add(NextBestAction("Run risk and sanctions review", "Cross-check observations against applicable risk datasets and preserve effective dates.", evidence, "High", "None", "RISK_REVIEW", 82, "Dataset/provider quota", "RUN_RISK"))
            if (evidence == 0 && (hasRisks || hasOsint || hasPatterns)) add(NextBestAction("Admit reviewed evidence", "Analytical outputs must be tied to source-backed evidence before conclusion drafting.", evidence, "Very High", "Analyst review", "EVIDENCE_REVIEW", 95, "None", "REVIEW_EVIDENCE"))
            if (!hasHypotheses && evidence > 0) add(NextBestAction("Formulate and review a hypothesis", "State what is being tested and link supporting and contradictory evidence.", evidence, "Very High", "Analyst input", "CONCLUSION", 75, "None", "CREATE_HYPOTHESIS"))
        }
        return candidates.maxWithOrNull(compareBy<NextBestAction> { it.confidence }.thenBy { it.supportingEvidenceCount }) ?: NextBestAction(
            "Review conclusion and generate report", "Core investigative artifacts are present; final conclusions still require analyst adjudication.", evidence, "Very High", "Analyst review", "REPORT", 70, "None", "REPORT"
        )
    }
}
