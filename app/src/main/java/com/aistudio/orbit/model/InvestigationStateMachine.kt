package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
sealed class InvestigationState {
    abstract val reason: String
    abstract val evidenceCount: Int
    abstract val confidence: Int
    abstract val isTerminal: Boolean

    @Serializable data class NotStarted(override val reason:String="Investigation not started", override val evidenceCount:Int=0, override val confidence:Int=0, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class ValidatingInput(override val reason:String="Validating provided input", override val evidenceCount:Int=0, override val confidence:Int=5, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class InputValidated(override val reason:String="Input validated successfully", override val evidenceCount:Int=0, override val confidence:Int=10, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class Discovering(override val reason:String="Discovering blockchain data", override val evidenceCount:Int=0, override val confidence:Int=15, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class DataAvailable(override val reason:String="Blockchain data retrieved or an empty ledger was confirmed", override val evidenceCount:Int=0, override val confidence:Int=30, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class TransactionAnalysis(override val reason:String="Analyzing transactions", override val evidenceCount:Int=0, override val confidence:Int=40, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class RelationshipAnalysis(override val reason:String="Analyzing counterparty relationships", override val evidenceCount:Int=0, override val confidence:Int=50, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class PatternAnalysis(override val reason:String="Matching behavioral patterns", override val evidenceCount:Int=0, override val confidence:Int=60, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class OsintAnalysis(override val reason:String="Analyzing OSINT intelligence", override val evidenceCount:Int=0, override val confidence:Int=70, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class RiskReview(override val reason:String="Evaluating risk indicators", override val evidenceCount:Int=0, override val confidence:Int=80, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class EvidenceReview(override val reason:String="Reviewing digital evidence", override val evidenceCount:Int=0, override val confidence:Int=90, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class HypothesisReview(override val reason:String="Reviewing case hypotheses", override val evidenceCount:Int=0, override val confidence:Int=92, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class ConclusionReady(override val reason:String="Case conclusion ready for analyst adjudication", override val evidenceCount:Int=0, override val confidence:Int=95, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class ReportReady(override val reason:String="Report generation available after review", override val evidenceCount:Int=0, override val confidence:Int=100, override val isTerminal:Boolean=true): InvestigationState()
    @Serializable data class Blocked(override val reason:String, override val evidenceCount:Int=0, override val confidence:Int=0, override val isTerminal:Boolean=false): InvestigationState()
    @Serializable data class Failed(override val reason:String, override val evidenceCount:Int=0, override val confidence:Int=0, override val isTerminal:Boolean=true): InvestigationState()
    @Serializable data class Partial(override val reason:String, override val evidenceCount:Int=0, override val confidence:Int=50, override val isTerminal:Boolean=false): InvestigationState()
}

data class NextBestAction(
    val action: String,
    val reason: String,
    val supportingEvidenceCount: Int,
    val expectedInvestigativeValue: String,
    val requiredInput: String,
    val targetStage: String,
    val confidence: Int,
    val costImplication: String,
    val actionCode: String = "",
    val isBlocked: Boolean = false
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

/**
 * Pure domain state machine. Empty balance is a valid observation, not a dead-end.
 * The next step is selected from missing investigative artifacts rather than from
 * assumptions about guilt, ownership, or transaction volume.
 */
object InvestigationStateMachine {
    fun determineState(case: InvestigationCase?, hasOsint:Boolean, hasPatterns:Boolean, hasRisks:Boolean, hasHypotheses:Boolean): InvestigationStateInfo {
        val action = NextBestActionEngine.determineNextBestAction(case, hasOsint, hasPatterns, hasRisks, hasHypotheses)
        if (case == null) return InvestigationStateInfo(
            InvestigationState.NotStarted(), "No active case.",
            listOf("Start New Investigation", "Quick Check"), listOf("Target Address", "Network"), listOf("Target Address"),
            0, 0, action.action, action
        )

        val evidence = case.evidenceLog.size
        val tx = case.transactions.isNotEmpty()
        val cp = case.counterparties.isNotEmpty()
        val state: InvestigationState
        val reason: String
        val missing: List<String>
        val available: List<String>

        when {
            case.targetAddress.isBlank() -> {
                state = InvestigationState.Blocked("Target address is missing.", evidence)
                reason = "A valid target address is required before discovery."
                missing = listOf("Target Address"); available = listOf("Edit Case Input")
            }
            case.status == InvestigationStatus.FAILED -> {
                state = InvestigationState.Failed("The latest investigation run failed.", evidence)
                reason = "Review provider/run errors before retrying."
                missing = listOf("Successful provider run"); available = listOf("Retry Failed Run")
            }
            !tx && case.totalTransactionsFound == 0 -> {
                state = InvestigationState.Discovering(evidenceCount=evidence)
                reason = "Ledger history has not produced transaction records yet. An empty result must be distinguished from an unqueried result."
                missing = listOf("Confirmed transaction discovery result"); available = listOf("Run Discovery", "Review Provider Health", "Check OSINT")
            }
            !cp -> {
                state = InvestigationState.TransactionAnalysis(evidenceCount=evidence)
                reason = "Transactions are available; related addresses have not been derived."
                missing = listOf("Counterparty set"); available = listOf("Extract Related Addresses")
            }
            !hasPatterns -> {
                state = InvestigationState.RelationshipAnalysis(evidenceCount=evidence)
                reason = "Relationships are available; approved typologies have not been evaluated."
                missing = listOf("Pattern evaluation"); available = listOf("Run Pattern Analysis")
            }
            !hasOsint -> {
                state = InvestigationState.PatternAnalysis(evidenceCount=evidence)
                reason = "On-chain observations exist; off-chain correlation remains unreviewed."
                missing = listOf("OSINT observations"); available = listOf("Run OSINT Review")
            }
            !hasRisks -> {
                state = InvestigationState.OsintAnalysis(evidenceCount=evidence)
                reason = "OSINT has been reviewed; risk and sanctions checks remain incomplete."
                missing = listOf("Risk assessment"); available = listOf("Run Risk Review")
            }
            evidence == 0 -> {
                state = InvestigationState.RiskReview(evidenceCount=0)
                reason = "Analytical outputs exist but no evidence items have been admitted to the case."
                missing = listOf("Reviewed evidence"); available = listOf("Review and Admit Evidence")
            }
            !hasHypotheses -> {
                state = InvestigationState.EvidenceReview(evidenceCount=evidence)
                reason = "Evidence exists; hypotheses must be explicitly formulated and reviewed."
                missing = listOf("Reviewed hypothesis"); available = listOf("Create Hypothesis")
            }
            else -> {
                state = InvestigationState.HypothesisReview(evidenceCount=evidence)
                reason = "The case has sufficient reviewed artifacts for conclusion adjudication."
                missing = emptyList(); available = listOf("Review Conclusion", "Generate Draft Report")
            }
        }
        return InvestigationStateInfo(state, reason, available, emptyList(), missing, evidence, state.confidence, action.action, action)
    }
}
