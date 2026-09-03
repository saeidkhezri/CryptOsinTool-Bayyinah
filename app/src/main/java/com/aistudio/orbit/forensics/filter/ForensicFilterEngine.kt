package com.aistudio.orbit.forensics.filter

import com.aistudio.orbit.model.*
import kotlinx.serialization.Serializable

@Serializable
enum class FilterDirection {
    ALL,
    INCOMING_ONLY,
    OUTGOING_ONLY
}

@Serializable
data class InvestigationFilterState(
    val searchQuery: String = "",
    val direction: FilterDirection = FilterDirection.ALL,
    val minAmountBtc: Double = 0.0,
    val maxAmountBtc: Double? = null,
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null,
    val entityTypeFilter: EntityClassificationType? = null,
    val selectedEntityTypes: Set<EntityClassificationType> = emptySet(),
    val onlySuspiciousOrRisky: Boolean = false,
    val minConfidenceFilter: ConfidenceLevel? = null,
    val minConfidence: ConfidenceLevel? = null
) {
    fun activeFilterCount(): Int {
        var count = 0
        if (searchQuery.isNotBlank()) count++
        if (direction != FilterDirection.ALL) count++
        if (minAmountBtc > 0.0) count++
        if (maxAmountBtc != null) count++
        if (startTimestamp != null) count++
        if (endTimestamp != null) count++
        if (entityTypeFilter != null) count++
        if (selectedEntityTypes.isNotEmpty()) count++
        if (onlySuspiciousOrRisky) count++
        if (minConfidenceFilter != null || minConfidence != null) count++
        return count
    }

    fun hasActiveFilters(): Boolean = activeFilterCount() > 0

    fun reset(): InvestigationFilterState = InvestigationFilterState()
}

/**
 * ForensicFilterEngine
 * Evaluates predicate chains across all investigative data structures.
 */
object ForensicFilterEngine {

    fun filterTransactions(
        transactions: List<ForensicTransaction>,
        filter: InvestigationFilterState
    ): List<ForensicTransaction> {
        if (!filter.hasActiveFilters()) return transactions

        return transactions.filter { tx ->
            // 1. Search Query (TxId, Address in inputs or outputs)
            if (filter.searchQuery.isNotBlank()) {
                val q = filter.searchQuery.trim().lowercase()
                val matchTx = tx.txId.lowercase().contains(q)
                val matchInput = tx.inputs.any { it.prevOutAddress?.lowercase()?.contains(q) == true }
                val matchOutput = tx.outputs.any { it.address?.lowercase()?.contains(q) == true }
                if (!matchTx && !matchInput && !matchOutput) return@filter false
            }

            // 2. Flow Direction
            when (filter.direction) {
                FilterDirection.INCOMING_ONLY -> if (tx.direction != TxDirection.INCOMING) return@filter false
                FilterDirection.OUTGOING_ONLY -> if (tx.direction != TxDirection.OUTGOING) return@filter false
                FilterDirection.ALL -> {}
            }

            // 3. Amount Thresholds
            val amountBtc = tx.relevantAmountSat.toDouble() / 100_000_000.0
            if (amountBtc < filter.minAmountBtc) return@filter false
            if (filter.maxAmountBtc != null && amountBtc > filter.maxAmountBtc) return@filter false

            // 4. Date Range
            if (filter.startTimestamp != null && tx.timestamp < filter.startTimestamp) return@filter false
            if (filter.endTimestamp != null && tx.timestamp > filter.endTimestamp) return@filter false

            true
        }
    }

    fun filterCounterparties(
        counterparties: List<CounterpartySummary>,
        filter: InvestigationFilterState
    ): List<CounterpartySummary> {
        if (!filter.hasActiveFilters()) return counterparties

        return counterparties.filter { cp ->
            // 1. Search query
            if (filter.searchQuery.isNotBlank()) {
                val q = filter.searchQuery.trim().lowercase()
                if (!cp.address.lowercase().contains(q)) return@filter false
            }

            // 2. Flow Direction
            when (filter.direction) {
                FilterDirection.INCOMING_ONLY -> if (cp.totalReceivedSatFromCounterparty <= 0L) return@filter false
                FilterDirection.OUTGOING_ONLY -> if (cp.totalSentSatToCounterparty <= 0L) return@filter false
                FilterDirection.ALL -> {}
            }

            // 3. Amount Threshold
            val totalBtc = (cp.totalReceivedSatFromCounterparty + cp.totalSentSatToCounterparty).toDouble() / 100_000_000.0
            if (totalBtc < filter.minAmountBtc) return@filter false
            if (filter.maxAmountBtc != null && totalBtc > filter.maxAmountBtc) return@filter false

            // 4. Date Range
            if (filter.startTimestamp != null && cp.lastSeenTimestamp < filter.startTimestamp) return@filter false
            if (filter.endTimestamp != null && cp.firstSeenTimestamp > filter.endTimestamp) return@filter false

            // 5. Suspicious / High Risk filter
            if (filter.onlySuspiciousOrRisky && cp.riskSeverity == RiskSeverity.INFO) {
                return@filter false
            }

            true
        }
    }

    fun filterGraph(
        graph: ForensicGraph,
        filter: InvestigationFilterState
    ): ForensicGraph {
        if (!filter.hasActiveFilters()) return graph

        val allowedNodes = graph.nodes.filter { node ->
            if (node.isSeed) return@filter true // Always preserve seed node

            // 1. Search query
            if (filter.searchQuery.isNotBlank()) {
                val q = filter.searchQuery.trim().lowercase()
                if (!node.id.lowercase().contains(q) && !node.label.lowercase().contains(q)) {
                    return@filter false
                }
            }

            // 2. Amount Threshold
            val nodeBtc = (node.totalReceivedSat + node.totalSentSat).toDouble() / 100_000_000.0
            if (nodeBtc < filter.minAmountBtc) return@filter false
            if (filter.maxAmountBtc != null && nodeBtc > filter.maxAmountBtc) return@filter false

            // 3. Risk filter
            if (filter.onlySuspiciousOrRisky && node.riskSeverity == RiskSeverity.INFO) {
                return@filter false
            }

            true
        }

        val allowedNodeIds = allowedNodes.map { it.id }.toSet()

        val allowedEdges = graph.edges.filter { edge ->
            val srcAllowed = allowedNodeIds.contains(edge.source)
            val tgtAllowed = allowedNodeIds.contains(edge.target)
            if (!srcAllowed || !tgtAllowed) return@filter false

            // Direction filter
            when (filter.direction) {
                FilterDirection.INCOMING_ONLY -> if (edge.direction != TxDirection.INCOMING) return@filter false
                FilterDirection.OUTGOING_ONLY -> if (edge.direction != TxDirection.OUTGOING) return@filter false
                FilterDirection.ALL -> {}
            }

            // Date Range
            if (filter.startTimestamp != null && edge.lastTxTime < filter.startTimestamp) return@filter false
            if (filter.endTimestamp != null && edge.firstTxTime > filter.endTimestamp) return@filter false

            true
        }

        return ForensicGraph(
            nodes = allowedNodes,
            edges = allowedEdges,
            generatedAt = System.currentTimeMillis()
        )
    }

    fun filterEvidenceLog(
        evidence: List<EvidenceItem>,
        filter: InvestigationFilterState
    ): List<EvidenceItem> {
        if (!filter.hasActiveFilters()) return evidence

        return evidence.filter { item ->
            // Search query
            if (filter.searchQuery.isNotBlank()) {
                val q = filter.searchQuery.trim().lowercase()
                val matchTitle = item.titleEn.lowercase().contains(q) || item.titleFa.lowercase().contains(q)
                val matchDesc = item.descriptionEn.lowercase().contains(q) || item.descriptionFa.lowercase().contains(q)
                val matchSrc = item.rawDataSource.lowercase().contains(q) || item.providerName.lowercase().contains(q)
                if (!matchTitle && !matchDesc && !matchSrc) return@filter false
            }

            // Confidence Level filter
            val requiredConfidence = filter.minConfidenceFilter ?: filter.minConfidence
            if (requiredConfidence != null) {
                if (item.confidence.ordinal > requiredConfidence.ordinal) return@filter false
            }

            true
        }
    }
}
