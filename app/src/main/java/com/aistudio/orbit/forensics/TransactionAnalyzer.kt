package com.aistudio.orbit.forensics

import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ConfidenceLevel
import com.aistudio.orbit.model.CounterpartySummary
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.RiskIndicator
import com.aistudio.orbit.model.RiskSeverity
import com.aistudio.orbit.model.TxDirection
import java.util.UUID

object TransactionAnalyzer {

    /**
     * Aggregates all transactions for a target address into counterparty summaries.
     */
    fun extractCounterparties(
        targetAddress: String,
        transactions: List<ForensicTransaction>,
        network: BlockchainNetwork = BlockchainNetwork.BITCOIN
    ): List<CounterpartySummary> {
        val lowerTarget = targetAddress.lowercase()
        val counterpartyMap = mutableMapOf<String, CounterpartyAccumulator>()

        for (tx in transactions) {
            val isIncoming = tx.direction == TxDirection.INCOMING || tx.direction == TxDirection.MIXED
            val isOutgoing = tx.direction == TxDirection.OUTGOING || tx.direction == TxDirection.MIXED

            // Senders
            val senders = tx.inputs.mapNotNull { it.prevOutAddress }.filter { it.isNotBlank() && it.lowercase() != lowerTarget }
            // Recipients
            val recipients = tx.outputs.mapNotNull { it.address }.filter { it.isNotBlank() && it.lowercase() != lowerTarget }

            if (isIncoming) {
                for (sender in senders) {
                    val acc = counterpartyMap.getOrPut(sender) { CounterpartyAccumulator(sender) }
                    acc.txCount++
                    acc.totalReceivedSat += tx.relevantAmountSat
                    acc.lastSeen = maxOf(acc.lastSeen, tx.timestamp)
                    if (acc.firstSeen == 0L || tx.timestamp < acc.firstSeen) acc.firstSeen = tx.timestamp
                    acc.incomingCount++
                }
            }

            if (isOutgoing) {
                for (recipient in recipients) {
                    val acc = counterpartyMap.getOrPut(recipient) { CounterpartyAccumulator(recipient) }
                    acc.txCount++
                    acc.totalSentSat += tx.relevantAmountSat
                    acc.lastSeen = maxOf(acc.lastSeen, tx.timestamp)
                    if (acc.firstSeen == 0L || tx.timestamp < acc.firstSeen) acc.firstSeen = tx.timestamp
                    acc.outgoingCount++
                }
            }
        }

        return counterpartyMap.values.map { acc ->
            val dir = when {
                acc.incomingCount > 0 && acc.outgoingCount == 0 -> TxDirection.INCOMING
                acc.outgoingCount > 0 && acc.incomingCount == 0 -> TxDirection.OUTGOING
                else -> TxDirection.MIXED
            }
            val netVolume = acc.totalReceivedSat - acc.totalSentSat
            val validation = AddressValidator.validate(acc.address, network)

            CounterpartySummary(
                address = acc.address,
                network = validation.network,
                addressType = validation.addressType,
                txCount = acc.txCount,
                totalReceivedSatFromCounterparty = acc.totalReceivedSat,
                totalSentSatToCounterparty = acc.totalSentSat,
                netVolumeSat = netVolume,
                firstSeenTimestamp = acc.firstSeen,
                lastSeenTimestamp = acc.lastSeen,
                interactionDirection = dir,
                riskSeverity = if (acc.txCount > 5) RiskSeverity.MEDIUM else RiskSeverity.INFO
            )
        }.sortedByDescending { it.totalReceivedSatFromCounterparty + it.totalSentSatToCounterparty }
    }

    /**
     * Identifies forensic behavioral patterns and risk indicators.
     * All results are strictly labeled with ConfidenceLevel and explicit probabilistic warnings.
     */
    fun analyzeRiskIndicators(
        targetAddress: String,
        transactions: List<ForensicTransaction>,
        counterparties: List<CounterpartySummary>
    ): List<RiskIndicator> {
        val indicators = mutableListOf<RiskIndicator>()

        if (transactions.isEmpty()) return indicators

        // 1. Fan-in / Consolidation Indicator (many inputs to 1 output)
        val consolidationTxs = transactions.filter { it.inputs.size >= 5 && it.outputs.size <= 2 }
        if (consolidationTxs.isNotEmpty()) {
            indicators.add(
                RiskIndicator(
                    id = UUID.randomUUID().toString(),
                    code = "RISK_CONSOLIDATION_FAN_IN",
                    title = "Transaction Consolidation (Fan-In Pattern)",
                    severity = RiskSeverity.MEDIUM,
                    category = "Behavioral Flow",
                    description = "Detected ${consolidationTxs.size} transaction(s) with 5+ inputs consolidating into 1-2 outputs. Often observed in pool payouts, hot wallet sweep operations, or pre-mixing staging.",
                    matchingScore = 72.0,
                    confidence = ConfidenceLevel.MEDIUM_CONFIDENCE,
                    relatedTxHashes = consolidationTxs.take(3).map { it.txId },
                    recommendedAction = "Verify whether sender is an automated exchange sweep bot, mining pool, or payment processor."
                )
            )
        }

        // 2. Fan-Out / Fund Splitting (1 input to many outputs)
        val fanOutTxs = transactions.filter { it.inputs.size <= 2 && it.outputs.size >= 5 }
        if (fanOutTxs.isNotEmpty()) {
            indicators.add(
                RiskIndicator(
                    id = UUID.randomUUID().toString(),
                    code = "RISK_DISPERSION_FAN_OUT",
                    title = "Fund Splitting / Dispersion (Fan-Out Pattern)",
                    severity = RiskSeverity.MEDIUM,
                    category = "Behavioral Flow",
                    description = "Detected ${fanOutTxs.size} transaction(s) where 1-2 inputs disperse funds across 5+ unique output addresses. Characteristic of batch distributions, airdrops, or layering dispersion.",
                    matchingScore = 78.0,
                    confidence = ConfidenceLevel.MEDIUM_CONFIDENCE,
                    relatedTxHashes = fanOutTxs.take(3).map { it.txId },
                    recommendedAction = "Trace destination outputs to check if counterparties converge on a centralized exchange or mixer."
                )
            )
        }

        // 3. Peeling Chain Structure (1-2 inputs, exactly 2 outputs, repeated)
        val peelCandidates = transactions.filter {
            it.inputs.size in 1..2 && it.outputs.size == 2 && it.direction == TxDirection.OUTGOING
        }
        if (peelCandidates.size >= 3) {
            indicators.add(
                RiskIndicator(
                    id = UUID.randomUUID().toString(),
                    code = "RISK_PEELING_CHAIN_CANDIDATE",
                    title = "Potential Peeling Chain Behavior",
                    severity = RiskSeverity.HIGH,
                    category = "Laundering Pattern",
                    description = "Identified ${peelCandidates.size} sequential 2-output transfer structures. Peeling chains are frequently used to shave off smaller payments while forwarding the remaining balance to a fresh change address.",
                    matchingScore = 84.0,
                    confidence = ConfidenceLevel.HEURISTIC_HYPOTHESIS,
                    relatedTxHashes = peelCandidates.take(4).map { it.txId },
                    recommendedAction = "Analyze change addresses across consecutive hops to verify whether change outputs remain under common ownership."
                )
            )
        }

        // 4. High Velocity / Rapid Relaying (Transactions occurring within < 30 minutes)
        val sortedByTime = transactions.sortedBy { it.timestamp }
        var rapidMovementCount = 0
        val rapidTxs = mutableListOf<String>()
        for (i in 0 until sortedByTime.size - 1) {
            val deltaSec = sortedByTime[i + 1].timestamp - sortedByTime[i].timestamp
            if (deltaSec in 1..1800) { // < 30 minutes
                rapidMovementCount++
                rapidTxs.add(sortedByTime[i + 1].txId)
            }
        }
        if (rapidMovementCount >= 2) {
            indicators.add(
                RiskIndicator(
                    id = UUID.randomUUID().toString(),
                    code = "RISK_HIGH_VELOCITY_MOVEMENT",
                    title = "High Velocity / Rapid Fund Movement",
                    severity = RiskSeverity.LOW,
                    category = "Temporal Velocity",
                    description = "Detected $rapidMovementCount consecutive transactions occurring within 30 minutes of each other. Suggests automated algorithmic activity or immediate relaying of incoming funds.",
                    matchingScore = 65.0,
                    confidence = ConfidenceLevel.MEDIUM_CONFIDENCE,
                    relatedTxHashes = rapidTxs.take(3),
                    recommendedAction = "Inspect fee rates and transaction scripts for signs of scripted automation or pass-through relaying."
                )
            )
        }

        // 5. Significant Single Counterparty Concentration
        if (counterparties.isNotEmpty()) {
            val topCounterparty = counterparties.first()
            val totalAllVolume = counterparties.sumOf { it.totalReceivedSatFromCounterparty + it.totalSentSatToCounterparty }
            if (totalAllVolume > 0) {
                val topVolume = topCounterparty.totalReceivedSatFromCounterparty + topCounterparty.totalSentSatToCounterparty
                val concentrationPercent = (topVolume.toDouble() / totalAllVolume.toDouble()) * 100.0
                if (concentrationPercent >= 75.0 && counterparties.size >= 3) {
                    indicators.add(
                        RiskIndicator(
                            id = UUID.randomUUID().toString(),
                            code = "RISK_HIGH_COUNTERPARTY_CONCENTRATION",
                            title = "High Counterparty Concentration (${String.format("%.1f", concentrationPercent)}%)",
                            severity = RiskSeverity.INFO,
                            category = "Relationship Clustering",
                            description = "Over ${String.format("%.1f", concentrationPercent)}% of total transaction volume is concentrated with a single counterparty address (${topCounterparty.address.take(8)}...).",
                            matchingScore = 90.0,
                            confidence = ConfidenceLevel.DEFINITIVE_FACT,
                            relatedAddresses = listOf(topCounterparty.address),
                            recommendedAction = "Perform targeted deep-dive investigation into this dominant counterparty address."
                        )
                    )
                }
            }
        }

        return indicators
    }

    private class CounterpartyAccumulator(val address: String) {
        var txCount: Int = 0
        var totalReceivedSat: Long = 0
        var totalSentSat: Long = 0
        var firstSeen: Long = 0
        var lastSeen: Long = 0
        var incomingCount: Int = 0
        var outgoingCount: Int = 0
    }
}
