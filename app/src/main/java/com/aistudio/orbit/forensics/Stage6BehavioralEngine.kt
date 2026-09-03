package com.aistudio.orbit.forensics

import com.aistudio.orbit.model.CounterpartySummary
import com.aistudio.orbit.model.ForensicTransaction
import kotlin.math.abs
import kotlin.math.sqrt

data class BehavioralProfile(
    val activeDaysCount: Int,
    val totalSpanDays: Int,
    val activityFrequencyPerDay: Double,
    val averageInterTxIntervalMinutes: Double,
    val intervalStdDevMinutes: Double,
    val isAutomatedScriptPattern: Boolean,
    val rapidPassThroughCount: Int, // Transactions executed within 30 mins
    val incomingVolumeSat: Long,
    val outgoingVolumeSat: Long,
    val netFlowSat: Long,
    val flowAsymmetryRatio: Double, // 0.0 to 1.0 (1.0 = purely one directional or rapid pass through)
    val maxSingleTxSat: Long,
    val velocitySatPerSec: Double,
    val dormancySpikeDetected: Boolean
)

object Stage6BehavioralEngine {

    fun computeProfile(
        targetAddress: String,
        transactions: List<ForensicTransaction>,
        counterparties: List<CounterpartySummary>
    ): BehavioralProfile {
        if (transactions.isEmpty()) {
            return BehavioralProfile(
                activeDaysCount = 0,
                totalSpanDays = 0,
                activityFrequencyPerDay = 0.0,
                averageInterTxIntervalMinutes = 0.0,
                intervalStdDevMinutes = 0.0,
                isAutomatedScriptPattern = false,
                rapidPassThroughCount = 0,
                incomingVolumeSat = 0L,
                outgoingVolumeSat = 0L,
                netFlowSat = 0L,
                flowAsymmetryRatio = 0.0,
                maxSingleTxSat = 0L,
                velocitySatPerSec = 0.0,
                dormancySpikeDetected = false
            )
        }

        val sortedTxs = transactions.sortedBy { it.timestamp }
        val timestampsSec = sortedTxs.map { it.timestamp }
        val minTs = timestampsSec.first()
        val maxTs = timestampsSec.last()

        val totalSpanSec = (maxTs - minTs).coerceAtLeast(1)
        val totalSpanDays = (totalSpanSec / 86400L).toInt().coerceAtLeast(1)

        val uniqueDays = sortedTxs.map { it.timestamp / 86400L }.toSet().size
        val freqPerDay = transactions.size.toDouble() / totalSpanDays.toDouble()

        // Calculate time deltas in minutes between consecutive transactions
        val intervalsMin = mutableListOf<Double>()
        var rapidPassThroughs = 0
        var maxDormancySec = 0L

        for (i in 0 until sortedTxs.size - 1) {
            val deltaSec = sortedTxs[i + 1].timestamp - sortedTxs[i].timestamp
            intervalsMin.add(deltaSec / 60.0)
            if (deltaSec <= 1800) { // 30 mins
                rapidPassThroughs++
            }
            if (deltaSec > maxDormancySec) {
                maxDormancySec = deltaSec
            }
        }

        val avgIntervalMin = if (intervalsMin.isNotEmpty()) intervalsMin.average() else 0.0
        val variance = if (intervalsMin.isNotEmpty()) {
            intervalsMin.sumOf { (it - avgIntervalMin) * (it - avgIntervalMin) } / intervalsMin.size
        } else 0.0
        val stdDevMin = sqrt(variance)

        // Low std deviation on frequent transactions indicates bot/script automation
        val isAutomated = transactions.size >= 10 && stdDevMin < (avgIntervalMin * 0.25) && avgIntervalMin < 120.0

        var incomingSat = 0L
        var outgoingSat = 0L
        var maxSingle = 0L

        for (tx in transactions) {
            val amt = tx.relevantAmountSat
            if (amt > maxSingle) maxSingle = amt
            if (tx.direction == com.aistudio.orbit.model.TxDirection.INCOMING) {
                incomingSat += amt
            } else if (tx.direction == com.aistudio.orbit.model.TxDirection.OUTGOING) {
                outgoingSat += amt
            }
        }

        val totalVol = (incomingSat + outgoingSat).toDouble().coerceAtLeast(1.0)
        val netFlow = incomingSat - outgoingSat
        val flowAsymmetry = abs(incomingSat - outgoingSat).toDouble() / totalVol

        val velocitySatPerSec = (incomingSat + outgoingSat).toDouble() / totalSpanSec.toDouble()
        val dormancySpike = maxDormancySec > (30L * 86400L) && transactions.count { it.timestamp > (maxTs - 7L * 86400L) } >= 5

        return BehavioralProfile(
            activeDaysCount = uniqueDays,
            totalSpanDays = totalSpanDays,
            activityFrequencyPerDay = freqPerDay,
            averageInterTxIntervalMinutes = avgIntervalMin,
            intervalStdDevMinutes = stdDevMin,
            isAutomatedScriptPattern = isAutomated,
            rapidPassThroughCount = rapidPassThroughs,
            incomingVolumeSat = incomingSat,
            outgoingVolumeSat = outgoingSat,
            netFlowSat = netFlow,
            flowAsymmetryRatio = flowAsymmetry,
            maxSingleTxSat = maxSingle,
            velocitySatPerSec = velocitySatPerSec,
            dormancySpikeDetected = dormancySpike
        )
    }
}
