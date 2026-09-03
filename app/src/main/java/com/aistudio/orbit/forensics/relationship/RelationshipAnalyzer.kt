package com.aistudio.orbit.forensics.relationship

import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.TxDirection
import kotlinx.serialization.Serializable

@Serializable
data class PairwiseRelationshipDetail(
    val addressA: String,
    val addressB: String,
    val totalDirectInteractions: Int,
    val totalVolumeAToB: Double,
    val totalVolumeBToA: Double,
    val netFlowAToB: Double,
    val firstInteractionTimestamp: Long,
    val lastInteractionTimestamp: Long,
    val averageTransferValue: Double,
    val maxTransferValue: Double,
    val directTxHashes: List<String>,
    val flowDirectionDescriptionEn: String,
    val flowDirectionDescriptionFa: String
)

object RelationshipAnalyzer {

    fun analyzePairwiseRelationship(
        targetAddress: String,
        counterpartyAddress: String,
        transactions: List<ForensicTransaction>
    ): PairwiseRelationshipDetail {
        val directTxs = transactions.filter { tx ->
            val hasCounterparty = tx.inputs.any { it.prevOutAddress.equals(counterpartyAddress, ignoreCase = true) } ||
                    tx.outputs.any { it.address.equals(counterpartyAddress, ignoreCase = true) }
            hasCounterparty
        }.sortedBy { it.timestamp }

        var volAToB = 0.0
        var volBToA = 0.0
        var maxTransfer = 0.0
        val txHashes = mutableListOf<String>()

        for (tx in directTxs) {
            txHashes.add(tx.txId)
            val btcVal = tx.relevantAmountSat.toDouble() / 100_000_000.0
            if (btcVal > maxTransfer) maxTransfer = btcVal

            if (tx.direction == TxDirection.OUTGOING) {
                // Target sent to counterparty
                volAToB += btcVal
            } else if (tx.direction == TxDirection.INCOMING) {
                // Counterparty sent to target
                volBToA += btcVal
            }
        }

        val netFlow = volAToB - volBToA
        val avgTransfer = if (directTxs.isNotEmpty()) (volAToB + volBToA) / directTxs.size else 0.0

        val firstTime = directTxs.firstOrNull()?.timestamp ?: 0L
        val lastTime = directTxs.lastOrNull()?.timestamp ?: 0L

        val (descEn, descFa) = when {
            netFlow > 0.0001 -> Pair(
                "Net Supplier: Target ($targetAddress) is predominantly sending funds to $counterpartyAddress",
                "تامین‌کننده خالص: آدرس هدف عمدتاً ارسال‌کننده وجه به این طرف‌حساب بوده است"
            )
            netFlow < -0.0001 -> Pair(
                "Net Receiver: Target ($targetAddress) is predominantly receiving funds from $counterpartyAddress",
                "دریافت‌کننده خالص: آدرس هدف عمدتاً دریافت‌کننده وجه از این طرف‌حساب بوده است"
            )
            else -> Pair(
                "Bilateral / Balanced Flow: High bi-directional fund reciprocity observed",
                "جریان دوجانبه متوازن: تعاملات مالی دوطرفه و متعادل مشاهده شده است"
            )
        }

        return PairwiseRelationshipDetail(
            addressA = targetAddress,
            addressB = counterpartyAddress,
            totalDirectInteractions = directTxs.size,
            totalVolumeAToB = volAToB,
            totalVolumeBToA = volBToA,
            netFlowAToB = netFlow,
            firstInteractionTimestamp = firstTime,
            lastInteractionTimestamp = lastTime,
            averageTransferValue = avgTransfer,
            maxTransferValue = maxTransfer,
            directTxHashes = txHashes,
            flowDirectionDescriptionEn = descEn,
            flowDirectionDescriptionFa = descFa
        )
    }
}
