package com.aistudio.orbit.forensics.analysis

import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ConfidenceLevel
import com.aistudio.orbit.model.CrimeCategory
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.PatternMatchResult
import com.aistudio.orbit.model.TxDirection
import java.util.UUID

object BehavioralEngine {

    fun analyzeTransactions(
        transactions: List<ForensicTransaction>,
        network: BlockchainNetwork,
        targetAddress: String
    ): List<PatternMatchResult> {
        val patterns = mutableListOf<PatternMatchResult>()
        if (transactions.isEmpty()) return patterns

        val sortedTxs = transactions.sortedBy { it.timestamp }

        // Rapid Movement (High Velocity Transit)
        val rapidMovementMatches = checkRapidMovement(sortedTxs, targetAddress)
        if (rapidMovementMatches != null) patterns.add(rapidMovementMatches)

        // Structuring / Smurfing
        val structuringMatches = checkStructuring(sortedTxs, targetAddress)
        if (structuringMatches != null) patterns.add(structuringMatches)

        // Peel Chain
        val peelChainMatches = checkPeelChain(sortedTxs, targetAddress)
        if (peelChainMatches != null) patterns.add(peelChainMatches)

        // Dormant-to-Active
        val dormantMatches = checkDormantToActive(sortedTxs)
        if (dormantMatches != null) patterns.add(dormantMatches)
        
        return patterns
    }

    private fun checkRapidMovement(
        transactions: List<ForensicTransaction>,
        targetAddress: String
    ): PatternMatchResult? {
        var rapidTransits = 0
        val relatedHashes = mutableSetOf<String>()
        val supporting = mutableListOf<String>()

        for (i in 0 until transactions.size - 1) {
            val currentTx = transactions[i]
            val nextTx = transactions[i + 1]

            // Look for incoming followed rapidly by outgoing
            if (currentTx.direction == TxDirection.INCOMING && nextTx.direction == TxDirection.OUTGOING) {
                val timeDiff = nextTx.timestamp - currentTx.timestamp
                if (timeDiff in 1..3600) { // Within 1 hour
                    rapidTransits++
                    relatedHashes.add(currentTx.txId)
                    relatedHashes.add(nextTx.txId)
                }
            }
        }

        if (rapidTransits >= 3) {
            supporting.add("$rapidTransits instances of funds moving within 1 hour of receipt.")
            return PatternMatchResult(
                patternId = "BEH_RAPID_${UUID.randomUUID()}",
                patternCode = "HVT-001",
                patternNameEn = "High Velocity Transit",
                patternNameFa = "انتقال سریع با ماندگاری صفر",
                category = CrimeCategory.HIGH_VELOCITY_TRANSIT,
                similarityScore = minOf(100.0, rapidTransits * 15.0),
                confidence = ConfidenceLevel.MEDIUM_CONFIDENCE,
                matchedIndicatorsEn = listOf("Rapid sequential transfers"),
                matchedIndicatorsFa = listOf("انتقال پی‌درپی و سریع"),
                matchedIndicators = supporting,
                relatedTxHashes = relatedHashes.toList(),
                analyticalRecommendationEn = "Trace the next hops to identify ultimate destination.",
                analyticalRecommendationFa = "ردیابی گام‌های بعدی جهت شناسایی مقصد نهایی توصیه می‌شود."
            )
        }
        return null
    }

    private fun checkStructuring(
        transactions: List<ForensicTransaction>,
        targetAddress: String
    ): PatternMatchResult? {
        val outgoingTxs = transactions.filter { it.direction == TxDirection.OUTGOING }
        var structuringCount = 0
        val relatedHashes = mutableSetOf<String>()
        val supporting = mutableListOf<String>()

        // Check for multiple outgoing transactions of similar sizes just below reporting thresholds
        // e.g. < $10k equivalent, assuming 1 BTC = $60k for heuristic, roughly 0.16 BTC
        val thresholdSat = 16_000_000L 

        outgoingTxs.forEach { tx ->
            if (tx.relevantAmountSat in (thresholdSat - 5_000_000)..(thresholdSat)) {
                structuringCount++
                relatedHashes.add(tx.txId)
            }
        }

        if (structuringCount >= 5) {
            supporting.add("$structuringCount outgoing transactions just below heuristic reporting thresholds.")
            return PatternMatchResult(
                patternId = "BEH_STRUCT_${UUID.randomUUID()}",
                patternCode = "STR-001",
                patternNameEn = "Structuring / Smurfing",
                patternNameFa = "خردسازی تراکنش‌ها (Smurfing)",
                category = CrimeCategory.STRUCTURING_SMURFING,
                similarityScore = minOf(100.0, structuringCount * 12.0),
                confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                matchedIndicatorsEn = listOf("Repeated similar size transfers below thresholds"),
                matchedIndicatorsFa = listOf("تکرار انتقال مبالغ مشابه زیر سقف گزارش‌دهی"),
                matchedIndicators = supporting,
                relatedTxHashes = relatedHashes.toList(),
                analyticalRecommendationEn = "Analyze counterparties to see if funds reconsolidate.",
                analyticalRecommendationFa = "بررسی طرف‌های حساب جهت شناسایی تجمیع مجدد سرمایه."
            )
        }
        return null
    }

    private fun checkPeelChain(
        transactions: List<ForensicTransaction>,
        targetAddress: String
    ): PatternMatchResult? {
        // Complex heuristic requiring UTXO structure analysis. Simplified here for demonstration.
        val outgoingTxs = transactions.filter { it.direction == TxDirection.OUTGOING }
        if (outgoingTxs.size >= 10) {
            return PatternMatchResult(
                patternId = "BEH_PEEL_${UUID.randomUUID()}",
                patternCode = "PCH-001",
                patternNameEn = "Potential Peel Chain",
                patternNameFa = "احتمال زنجیره پول‌کنی (Peel Chain)",
                category = CrimeCategory.MONEY_LAUNDERING,
                similarityScore = 65.0,
                confidence = ConfidenceLevel.HEURISTIC_HYPOTHESIS,
                matchedIndicatorsEn = listOf("High number of sequential outgoing hops"),
                matchedIndicatorsFa = listOf("تعداد بالای خروجی‌های متوالی"),
                matchedIndicators = listOf("Sequential outgoing behavior observed"),
                relatedTxHashes = outgoingTxs.map { it.txId }.take(10),
                analyticalRecommendationEn = "Perform UTXO change-address analysis to confirm peel chain.",
                analyticalRecommendationFa = "انجام تحلیل آدرس‌های باقی‌مانده (Change) جهت تایید زنجیره پول‌کنی."
            )
        }
        return null
    }

    private fun checkDormantToActive(
        sortedTxs: List<ForensicTransaction>
    ): PatternMatchResult? {
        if (sortedTxs.size < 2) return null

        val ONE_YEAR_SECONDS = 365 * 24 * 60 * 60L
        for (i in 0 until sortedTxs.size - 1) {
            val gap = sortedTxs[i + 1].timestamp - sortedTxs[i].timestamp
            if (gap > ONE_YEAR_SECONDS) {
                return PatternMatchResult(
                    patternId = "BEH_DORM_${UUID.randomUUID()}",
                    patternCode = "DTA-001",
                    patternNameEn = "Dormant-to-Active",
                    patternNameFa = "فعال شدن پس از رکود",
                    category = CrimeCategory.BEHAVIORAL_ANOMALY,
                    similarityScore = 80.0,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    matchedIndicatorsEn = listOf("Account woke up after > 1 year inactivity"),
                    matchedIndicatorsFa = listOf("بیدار شدن حساب پس از بیش از ۱ سال رکود"),
                    matchedIndicators = listOf("Activity gap: ${gap / ONE_YEAR_SECONDS} years"),
                    relatedTxHashes = listOf(sortedTxs[i + 1].txId),
                    analyticalRecommendationEn = "Correlate wake-up time with known OSINT events or hacks.",
                    analyticalRecommendationFa = "بررسی تقارن زمانی با وقایع و هک‌های شناخته‌شده."
                )
            }
        }
        return null
    }
}
