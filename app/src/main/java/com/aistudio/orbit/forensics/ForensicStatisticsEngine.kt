package com.aistudio.orbit.forensics

import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.CounterpartySummary
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.TxDirection
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
data class RepeatedAmountCluster(
    val amountSat: Long,
    val occurrenceCount: Int,
    val txHashes: List<String>,
    val isSuspiciousRoundNumber: Boolean
) {
    val amountBtc: Double get() = amountSat.toDouble() / 100_000_000.0
}

@Serializable
data class DescriptiveStatistics(
    val totalTxCount: Int,
    val inboundTxCount: Int,
    val outboundTxCount: Int,
    val selfTxCount: Int,
    val contractCallCount: Int,
    val totalInboundSat: Long,
    val totalOutboundSat: Long,
    val avgInboundSat: Long,
    val medianInboundSat: Long,
    val avgOutboundSat: Long,
    val medianOutboundSat: Long,
    val minAmountSat: Long,
    val maxAmountSat: Long,
    val uniqueCounterpartyCount: Int,
    val repeatedCounterpartyCount: Int,
    val inboundOutboundTxRatio: Double,
    val inboundOutboundVolumeRatio: Double,
    val firstTxTimestamp: Long,
    val lastTxTimestamp: Long,
    val activeDaysCount: Int,
    val avgIntervalHours: Double,
    val repeatedAmountClusters: List<RepeatedAmountCluster>,
    val dominantAsset: String
) {
    val totalInboundBtc: Double get() = totalInboundSat.toDouble() / 100_000_000.0
    val totalOutboundBtc: Double get() = totalOutboundSat.toDouble() / 100_000_000.0
    val avgInboundBtc: Double get() = avgInboundSat.toDouble() / 100_000_000.0
    val medianInboundBtc: Double get() = medianInboundSat.toDouble() / 100_000_000.0
    val avgOutboundBtc: Double get() = avgOutboundSat.toDouble() / 100_000_000.0
    val medianOutboundBtc: Double get() = medianOutboundSat.toDouble() / 100_000_000.0
    val minAmountBtc: Double get() = minAmountSat.toDouble() / 100_000_000.0
    val maxAmountBtc: Double get() = maxAmountSat.toDouble() / 100_000_000.0
}

object ForensicStatisticsEngine {

    fun computeDescriptiveStatistics(
        targetAddress: String,
        network: BlockchainNetwork,
        transactions: List<ForensicTransaction>,
        counterparties: List<CounterpartySummary>
    ): DescriptiveStatistics {
        if (transactions.isEmpty()) {
            return DescriptiveStatistics(
                totalTxCount = 0,
                inboundTxCount = 0,
                outboundTxCount = 0,
                selfTxCount = 0,
                contractCallCount = 0,
                totalInboundSat = 0,
                totalOutboundSat = 0,
                avgInboundSat = 0,
                medianInboundSat = 0,
                avgOutboundSat = 0,
                medianOutboundSat = 0,
                minAmountSat = 0,
                maxAmountSat = 0,
                uniqueCounterpartyCount = counterparties.size,
                repeatedCounterpartyCount = 0,
                inboundOutboundTxRatio = 0.0,
                inboundOutboundVolumeRatio = 0.0,
                firstTxTimestamp = 0,
                lastTxTimestamp = 0,
                activeDaysCount = 0,
                avgIntervalHours = 0.0,
                repeatedAmountClusters = emptyList(),
                dominantAsset = network.symbol
            )
        }

        val inboundTxs = transactions.filter { it.direction == TxDirection.INCOMING || it.direction == TxDirection.MIXED }
        val outboundTxs = transactions.filter { it.direction == TxDirection.OUTGOING }
        val selfTxs = transactions.filter { it.direction == TxDirection.SELF_TRANSFER }
        val contractTxs = transactions.filter { it.direction == TxDirection.CONTRACT_CALL }

        val totalInboundSat = inboundTxs.sumOf { it.relevantAmountSat }
        val totalOutboundSat = outboundTxs.sumOf { it.relevantAmountSat }

        val inboundAmounts = inboundTxs.map { it.relevantAmountSat }.sorted()
        val outboundAmounts = outboundTxs.map { it.relevantAmountSat }.sorted()
        val allAmounts = transactions.map { it.relevantAmountSat }.filter { it > 0 }.sorted()

        val avgInboundSat = if (inboundTxs.isNotEmpty()) totalInboundSat / inboundTxs.size else 0L
        val medianInboundSat = calculateMedian(inboundAmounts)

        val avgOutboundSat = if (outboundTxs.isNotEmpty()) totalOutboundSat / outboundTxs.size else 0L
        val medianOutboundSat = calculateMedian(outboundAmounts)

        val minAmountSat = allAmounts.firstOrNull() ?: 0L
        val maxAmountSat = allAmounts.lastOrNull() ?: 0L

        val uniqueCpCount = counterparties.size
        val repeatedCpCount = counterparties.count { it.txCount > 1 }

        val txRatio = if (outboundTxs.isNotEmpty()) {
            inboundTxs.size.toDouble() / outboundTxs.size.toDouble()
        } else {
            inboundTxs.size.toDouble()
        }

        val volumeRatio = if (totalOutboundSat > 0) {
            totalInboundSat.toDouble() / totalOutboundSat.toDouble()
        } else {
            totalInboundSat.toDouble()
        }

        val sortedByTime = transactions.map { it.timestamp }.filter { it > 0 }.sorted()
        val firstTime = sortedByTime.firstOrNull() ?: 0L
        val lastTime = sortedByTime.lastOrNull() ?: 0L

        // Active days calculation
        val activeDaysCount = sortedByTime.map { timestampToEpochDay(it) }.distinct().size

        // Average interval
        var totalIntervalSec = 0L
        for (i in 0 until sortedByTime.size - 1) {
            val delta = sortedByTime[i + 1] - sortedByTime[i]
            if (delta > 0) {
                totalIntervalSec += delta
            }
        }
        val avgIntervalHours = if (sortedByTime.size > 1) {
            (totalIntervalSec.toDouble() / (sortedByTime.size - 1)) / 3600.0
        } else {
            0.0
        }

        // Repeated amounts clustering (amounts appearing >= 2 times)
        val amountGroups = transactions
            .filter { it.relevantAmountSat > 0 }
            .groupBy { it.relevantAmountSat }
            .filter { it.value.size >= 2 }
            .map { (amount, txs) ->
                val btc = amount.toDouble() / 100_000_000.0
                val isRound = btc == btc.toLong().toDouble() || (amount % 10_000_000L == 0L)
                RepeatedAmountCluster(
                    amountSat = amount,
                    occurrenceCount = txs.size,
                    txHashes = txs.map { it.txId },
                    isSuspiciousRoundNumber = isRound
                )
            }
            .sortedByDescending { it.occurrenceCount }
            .take(5)

        return DescriptiveStatistics(
            totalTxCount = transactions.size,
            inboundTxCount = inboundTxs.size,
            outboundTxCount = outboundTxs.size,
            selfTxCount = selfTxs.size,
            contractCallCount = contractTxs.size,
            totalInboundSat = totalInboundSat,
            totalOutboundSat = totalOutboundSat,
            avgInboundSat = avgInboundSat,
            medianInboundSat = medianInboundSat,
            avgOutboundSat = avgOutboundSat,
            medianOutboundSat = medianOutboundSat,
            minAmountSat = minAmountSat,
            maxAmountSat = maxAmountSat,
            uniqueCounterpartyCount = uniqueCpCount,
            repeatedCounterpartyCount = repeatedCpCount,
            inboundOutboundTxRatio = txRatio,
            inboundOutboundVolumeRatio = volumeRatio,
            firstTxTimestamp = firstTime,
            lastTxTimestamp = lastTime,
            activeDaysCount = activeDaysCount,
            avgIntervalHours = avgIntervalHours,
            repeatedAmountClusters = amountGroups,
            dominantAsset = network.symbol
        )
    }

    private fun calculateMedian(sortedList: List<Long>): Long {
        if (sortedList.isEmpty()) return 0L
        val size = sortedList.size
        return if (size % 2 == 1) {
            sortedList[size / 2]
        } else {
            (sortedList[(size / 2) - 1] + sortedList[size / 2]) / 2L
        }
    }

    private fun timestampToEpochDay(epochSec: Long): Long {
        return TimeUnit.SECONDS.toDays(epochSec)
    }
}
