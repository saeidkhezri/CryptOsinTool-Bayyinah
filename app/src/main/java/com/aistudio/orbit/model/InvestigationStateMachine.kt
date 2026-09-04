package com.aistudio.orbit.model

enum class InvestigationState(
    val reason: String,
    val isTerminal: Boolean = false
) {
    NOT_STARTED("Investigation not started"),
    VALIDATING_INPUT("Validating provided input"),
    INPUT_VALIDATED("Input is valid"),
    DISCOVERING("Discovering blockchain data"),
    DATA_AVAILABLE("Blockchain data retrieved"),
    TRANSACTION_ANALYSIS("Analyzing transactions"),
    RELATIONSHIP_ANALYSIS("Analyzing counterparty relationships"),
    PATTERN_ANALYSIS("Matching behavioral patterns"),
    OSINT_ANALYSIS("Analyzing OSINT intelligence"),
    RISK_REVIEW("Evaluating risk indicators"),
    EVIDENCE_REVIEW("Reviewing digital evidence"),
    HYPOTHESIS_REVIEW("Reviewing case hypotheses"),
    CONCLUSION_READY("Case conclusion ready"),
    REPORT_READY("Report generation available", isTerminal = true),
    BLOCKED("Investigation blocked due to dead-end"),
    FAILED("Investigation failed due to error", isTerminal = true),
    PARTIAL("Partial data available")
}

data class NextBestAction(
    val action: String,
    val reason: String,
    val supportingEvidenceCount: Int,
    val expectedInvestigativeValue: String,
    val requiredInput: String,
    val targetStage: String,
    val confidence: Int,
    val costImplication: String
)

data class InvestigationStateInfo(
    val state: InvestigationState,
    val reason: String,
    val availableActions: List<String>,
    val requiredInputs: List<String>,
    val missingInputs: List<String>,
    val evidenceCount: Int,
    val confidence: Int,
    val recommendedNextAction: String,
    val nextBestActionDetailed: NextBestAction? = null
)

object InvestigationStateMachine {
    fun determineState(
        case: InvestigationCase?,
        hasOsint: Boolean,
        hasPatterns: Boolean,
        hasRisks: Boolean,
        hasHypotheses: Boolean
    ): InvestigationStateInfo {
        val nextBestActionDetailed = NextBestActionEngine.determineNextBestAction(
            case = case,
            hasOsint = hasOsint,
            hasPatterns = hasPatterns,
            hasRisks = hasRisks,
            hasHypotheses = hasHypotheses
        )

        if (case == null) {
            return InvestigationStateInfo(
                state = InvestigationState.NOT_STARTED,
                reason = "No active case.",
                availableActions = listOf("Start New Investigation", "Quick Check"),
                requiredInputs = listOf("Target Address", "Network"),
                missingInputs = listOf("Target Address"),
                evidenceCount = 0,
                confidence = 0,
                recommendedNextAction = nextBestActionDetailed.action,
                nextBestActionDetailed = nextBestActionDetailed
            )
        }
        
        val evidenceCount = case.evidenceLog.size
        
        // Very basic linear determination based on available data
        if (case.transactions.isEmpty()) {
            if (case.balanceSat == 0L) {
                return InvestigationStateInfo(
                    state = InvestigationState.BLOCKED,
                    reason = "No transactions found and zero balance.",
                    availableActions = listOf("Retry Discovery", "Check OSINT anyway"),
                    requiredInputs = emptyList(),
                    missingInputs = listOf("Transactions"),
                    evidenceCount = evidenceCount,
                    confidence = 10,
                    recommendedNextAction = nextBestActionDetailed.action,
                    nextBestActionDetailed = nextBestActionDetailed
                )
            }
            return InvestigationStateInfo(
                state = InvestigationState.DATA_AVAILABLE,
                reason = "Balance available but no transactions.",
                availableActions = listOf("Discover Transactions"),
                requiredInputs = emptyList(),
                missingInputs = listOf("Transactions"),
                evidenceCount = evidenceCount,
                confidence = 30,
                recommendedNextAction = nextBestActionDetailed.action,
                nextBestActionDetailed = nextBestActionDetailed
            )
        }
        
        if (case.counterparties.isEmpty()) {
            return InvestigationStateInfo(
                state = InvestigationState.TRANSACTION_ANALYSIS,
                reason = "Transactions available, counterparties pending.",
                availableActions = listOf("Extract Counterparties"),
                requiredInputs = emptyList(),
                missingInputs = listOf("Counterparties"),
                evidenceCount = evidenceCount,
                confidence = 40,
                recommendedNextAction = nextBestActionDetailed.action,
                nextBestActionDetailed = nextBestActionDetailed
            )
        }
        
        if (!hasPatterns) {
            return InvestigationStateInfo(
                state = InvestigationState.RELATIONSHIP_ANALYSIS,
                reason = "Counterparties available, pattern matching pending.",
                availableActions = listOf("Run Pattern Analysis"),
                requiredInputs = emptyList(),
                missingInputs = listOf("Pattern Results"),
                evidenceCount = evidenceCount,
                confidence = 50,
                recommendedNextAction = nextBestActionDetailed.action,
                nextBestActionDetailed = nextBestActionDetailed
            )
        }
        
        if (!hasOsint) {
            return InvestigationStateInfo(
                state = InvestigationState.PATTERN_ANALYSIS,
                reason = "Patterns matched, OSINT pending.",
                availableActions = listOf("Run OSINT Discovery"),
                requiredInputs = emptyList(),
                missingInputs = listOf("OSINT Data"),
                evidenceCount = evidenceCount,
                confidence = 60,
                recommendedNextAction = nextBestActionDetailed.action,
                nextBestActionDetailed = nextBestActionDetailed
            )
        }
        
        if (!hasRisks && case.riskIndicators.isEmpty()) {
            return InvestigationStateInfo(
                state = InvestigationState.OSINT_ANALYSIS,
                reason = "OSINT available, risk review pending.",
                availableActions = listOf("Evaluate Risks"),
                requiredInputs = emptyList(),
                missingInputs = listOf("Risk Scores"),
                evidenceCount = evidenceCount,
                confidence = 70,
                recommendedNextAction = nextBestActionDetailed.action,
                nextBestActionDetailed = nextBestActionDetailed
            )
        }
        
        if (evidenceCount == 0) {
            return InvestigationStateInfo(
                state = InvestigationState.RISK_REVIEW,
                reason = "Risk evaluated, awaiting evidence extraction.",
                availableActions = listOf("Extract Evidence"),
                requiredInputs = emptyList(),
                missingInputs = listOf("Evidence Items"),
                evidenceCount = evidenceCount,
                confidence = 80,
                recommendedNextAction = nextBestActionDetailed.action,
                nextBestActionDetailed = nextBestActionDetailed
            )
        }
        
        if (!hasHypotheses) {
            return InvestigationStateInfo(
                state = InvestigationState.EVIDENCE_REVIEW,
                reason = "Evidence sealed, hypothesis pending.",
                availableActions = listOf("Generate Hypothesis"),
                requiredInputs = emptyList(),
                missingInputs = listOf("Analyst Hypothesis"),
                evidenceCount = evidenceCount,
                confidence = 90,
                recommendedNextAction = nextBestActionDetailed.action,
                nextBestActionDetailed = nextBestActionDetailed
            )
        }
        
        return InvestigationStateInfo(
            state = InvestigationState.REPORT_READY,
            reason = "Investigation complete and report is ready.",
            availableActions = listOf("Generate PDF", "Export CSV"),
            requiredInputs = emptyList(),
            missingInputs = emptyList(),
            evidenceCount = evidenceCount,
            confidence = 100,
            recommendedNextAction = nextBestActionDetailed.action,
            nextBestActionDetailed = nextBestActionDetailed
        )
    }
}
