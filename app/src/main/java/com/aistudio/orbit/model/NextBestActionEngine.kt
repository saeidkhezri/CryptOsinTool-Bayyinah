package com.aistudio.orbit.model

object NextBestActionEngine {

    fun determineNextBestAction(
        case: InvestigationCase?,
        hasOsint: Boolean,
        hasPatterns: Boolean,
        hasRisks: Boolean,
        hasHypotheses: Boolean
    ): NextBestAction {
        if (case == null) {
            return NextBestAction(
                action = "Start New Investigation or Quick Check",
                reason = "No active investigation is currently loaded.",
                supportingEvidenceCount = 0,
                expectedInvestigativeValue = "High",
                requiredInput = "Target Address",
                targetStage = "START",
                confidence = 100,
                costImplication = "None"
            )
        }

        val evidenceCount = case.evidenceLog.size
        
        if (case.transactions.isEmpty() && case.balanceSat == 0L) {
            return NextBestAction(
                action = "Verify target address or check OSINT sources",
                reason = "Address has zero balance and no transaction history. Dead-end on-chain.",
                supportingEvidenceCount = evidenceCount,
                expectedInvestigativeValue = "Medium",
                requiredInput = "Target Address",
                targetStage = "OSINT_ANALYSIS",
                confidence = 90,
                costImplication = "Free OSINT / Paid Deep Search"
            )
        }

        if (case.transactions.isEmpty()) {
            return NextBestAction(
                action = "Retrieve full historical transactions",
                reason = "Balance exists but transaction history is missing.",
                supportingEvidenceCount = evidenceCount,
                expectedInvestigativeValue = "High",
                requiredInput = "None",
                targetStage = "TRANSACTION_ANALYSIS",
                confidence = 95,
                costImplication = "API Quota (1-2 reqs)"
            )
        }

        if (case.counterparties.isEmpty()) {
            return NextBestAction(
                action = "Expand the graph by one hop",
                reason = "Transactions available but counterparties not extracted.",
                supportingEvidenceCount = evidenceCount,
                expectedInvestigativeValue = "High",
                requiredInput = "None",
                targetStage = "RELATIONSHIP_ANALYSIS",
                confidence = 90,
                costImplication = "API Quota (1 req per counterparty)"
            )
        }

        if (!hasPatterns) {
            return NextBestAction(
                action = "Compare with approved crime typologies",
                reason = "Counterparties established. Need to check for structural anomalies like Peel Chains or Structuring.",
                supportingEvidenceCount = evidenceCount,
                expectedInvestigativeValue = "High",
                requiredInput = "None",
                targetStage = "PATTERN_ANALYSIS",
                confidence = 85,
                costImplication = "None (Local Compute)"
            )
        }

        if (!hasOsint) {
            return NextBestAction(
                action = "Search public web sources for the exact address",
                reason = "On-chain patterns established. Need external attribution or OSINT correlation.",
                supportingEvidenceCount = evidenceCount,
                expectedInvestigativeValue = "High",
                requiredInput = "None",
                targetStage = "OSINT_ANALYSIS",
                confidence = 80,
                costImplication = "Web Search Quota"
            )
        }

        if (!hasRisks && case.riskIndicators.isEmpty()) {
            return NextBestAction(
                action = "Check sanctions datasets with historical validity",
                reason = "OSINT collected. Need to cross-reference against sanctions and threat intel.",
                supportingEvidenceCount = evidenceCount,
                expectedInvestigativeValue = "High",
                requiredInput = "None",
                targetStage = "RISK_REVIEW",
                confidence = 95,
                costImplication = "Local / Paid Intel Quota"
            )
        }

        if (!hasHypotheses) {
            return NextBestAction(
                action = "Create a hypothesis",
                reason = "Enough supporting observations now exist to formulate an investigative hypothesis.",
                supportingEvidenceCount = evidenceCount,
                expectedInvestigativeValue = "Very High",
                requiredInput = "Analyst Input",
                targetStage = "HYPOTHESIS_REVIEW",
                confidence = 70,
                costImplication = "None"
            )
        }

        return NextBestAction(
            action = "Generate Final Report",
            reason = "Investigation stages complete. Ready for investigator review and reporting.",
            supportingEvidenceCount = evidenceCount,
            expectedInvestigativeValue = "Very High",
            requiredInput = "None",
            targetStage = "REPORT_READY",
            confidence = 100,
            costImplication = "None"
        )
    }
}
