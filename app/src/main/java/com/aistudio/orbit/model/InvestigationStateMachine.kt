package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
sealed class InvestigationState {
    abstract val reason: String
    abstract val evidenceCount: Int
    abstract val confidence: Int
    abstract val isTerminal: Boolean

    @Serializable
    data class NotStarted(
        override val reason: String = "Investigation not started",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 0,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class ValidatingInput(
        override val reason: String = "Validating provided input",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 5,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class InputValidated(
        override val reason: String = "Input validated successfully",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 10,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class Discovering(
        override val reason: String = "Discovering blockchain data",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 15,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class DataAvailable(
        override val reason: String = "Blockchain data retrieved",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 30,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class TransactionAnalysis(
        override val reason: String = "Analyzing transactions",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 40,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class RelationshipAnalysis(
        override val reason: String = "Analyzing counterparty relationships",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 50,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class PatternAnalysis(
        override val reason: String = "Matching behavioral patterns",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 60,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class OsintAnalysis(
        override val reason: String = "Analyzing OSINT intelligence",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 70,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class RiskReview(
        override val reason: String = "Evaluating risk indicators",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 80,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class EvidenceReview(
        override val reason: String = "Reviewing digital evidence",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 90,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class HypothesisReview(
        override val reason: String = "Reviewing case hypotheses",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 95,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class ConclusionReady(
        override val reason: String = "Case conclusion ready",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 98,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class ReportReady(
        override val reason: String = "Report generation available",
        override val evidenceCount: Int = 0,
        override val confidence: Int = 100,
        override val isTerminal: Boolean = true
    ) : InvestigationState()

    @Serializable
    data class Blocked(
        override val reason: String,
        override val evidenceCount: Int = 0,
        override val confidence: Int = 0,
        override val isTerminal: Boolean = false
    ) : InvestigationState()

    @Serializable
    data class Failed(
        override val reason: String,
        override val evidenceCount: Int = 0,
        override val confidence: Int = 0,
        override val isTerminal: Boolean = true
    ) : InvestigationState()

    @Serializable
    data class Partial(
        override val reason: String,
        override val evidenceCount: Int = 0,
        override val confidence: Int = 50,
        override val isTerminal: Boolean = false
    ) : InvestigationState()
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
                state = InvestigationState.NotStarted(),
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
        
        if (case.transactions.isEmpty()) {
            if (case.balanceSat == 0L) {
                return InvestigationStateInfo(
                    state = InvestigationState.Blocked("No transactions found and zero balance."),
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
                state = InvestigationState.DataAvailable(evidenceCount = evidenceCount),
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
                state = InvestigationState.TransactionAnalysis(evidenceCount = evidenceCount),
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
                state = InvestigationState.RelationshipAnalysis(evidenceCount = evidenceCount),
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
                state = InvestigationState.PatternAnalysis(evidenceCount = evidenceCount),
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
                state = InvestigationState.OsintAnalysis(evidenceCount = evidenceCount),
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
                state = InvestigationState.RiskReview(evidenceCount = evidenceCount),
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
                state = InvestigationState.EvidenceReview(evidenceCount = evidenceCount),
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
            state = InvestigationState.ReportReady(evidenceCount = evidenceCount),
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
