package com.aistudio.orbit.forensics.patterns

import com.aistudio.orbit.model.*

object CrimePatternEngine {

    fun matchPatterns(
        targetAddress: String,
        transactions: List<ForensicTransaction>,
        counterparties: List<CounterpartySummary>
    ): List<PatternMatchResult> {
        val results = mutableListOf<PatternMatchResult>()
        if (transactions.isEmpty()) return results

        val sortedTxs = transactions.sortedBy { it.timestamp }
        val outgoingTxs = transactions.filter { it.direction == TxDirection.OUTGOING }
        val incomingTxs = transactions.filter { it.direction == TxDirection.INCOMING }

        val libraryMap = CrimePatternLibrary.patterns.associateBy { it.id }

        // Helper to construct results dynamically from library
        fun addMatch(id: String, score: Double, conf: ConfidenceLevel, indicatorsEn: List<String>, indicatorsFa: List<String>, conflictsEn: List<String>, conflictsFa: List<String>, missingEn: List<String>, missingFa: List<String>, txHashes: List<String>, recEn: String, recFa: String) {
            val pat = libraryMap[id] ?: return
            results.add(PatternMatchResult(
                patternId = pat.id,
                patternCode = pat.code,
                patternNameEn = pat.nameEn,
                patternNameFa = pat.nameFa,
                category = pat.category,
                similarityScore = score,
                confidence = conf,
                matchedIndicatorsEn = indicatorsEn,
                matchedIndicatorsFa = indicatorsFa,
                conflictingIndicatorsEn = conflictsEn,
                conflictingIndicatorsFa = conflictsFa,
                missingEvidenceEn = missingEn,
                missingEvidenceFa = missingFa,
                relatedTxHashes = txHashes,
                relatedAddresses = listOf(targetAddress),
                analyticalRecommendationEn = recEn,
                analyticalRecommendationFa = recFa
            ))
        }

        // 1. Fan-In (PAT_AML_FAN_IN)
        val fanInTxs = transactions.filter { it.inputs.size >= 5 && it.outputs.size <= 2 }
        if (fanInTxs.isNotEmpty()) {
            val score = when {
                fanInTxs.size >= 3 -> 90.0
                fanInTxs.size >= 1 -> 78.0
                else -> 45.0
            }
            if (score >= 50.0) {
                addMatch("PAT_AML_FAN_IN", score, ConfidenceLevel.HIGH_CONFIDENCE,
                    listOf("Observed ${fanInTxs.size} consolidation transaction(s) aggregating 5+ inputs into 1-2 outputs."),
                    listOf("مشاهده ${fanInTxs.size} تراکنش تجمیعی با ادغام ۵ یا تعداد بیشتری ورودی در ۱ یا ۲ خروجی."),
                    emptyList(), emptyList(),
                    listOf("Historical source of aggregated inputs (merchant vs illicit)."),
                    listOf("منبع تاریخی ورودی‌های تجمیع‌شده (درگاه تجاری در برابر عواید غیرقانونی)."),
                    fanInTxs.take(3).map { it.txId },
                    "Analyze the common spend inputs cluster.",
                    "خوشه ورودی‌های خرج‌شده مشترک را تحلیل کنید."
                )
            }
        }

        // 2. Fan-Out (PAT_AML_FAN_OUT)
        val fanOutTxs = transactions.filter { it.inputs.size <= 2 && it.outputs.size >= 5 }
        if (fanOutTxs.isNotEmpty()) {
            val score = when {
                fanOutTxs.size >= 3 -> 88.0
                fanOutTxs.size >= 1 -> 72.0
                else -> 40.0
            }
            if (score >= 50.0) {
                addMatch("PAT_AML_FAN_OUT", score, ConfidenceLevel.MEDIUM_CONFIDENCE,
                    listOf("Found ${fanOutTxs.size} transaction(s) dispersing funds to 5+ distinct destination outputs simultaneously."),
                    listOf("کشف ${fanOutTxs.size} تراکنش با توزیع همزمان وجوه به ۵ یا تعداد بیشتری آدرس خروجی مجزا."),
                    listOf("Batch payouts from legitimate mining pools or exchanges share similar output counts."),
                    listOf("پرداخت‌های دسته‌ای استخرهای ماینینگ یا صرافی‌های معتبر نیز ساختار خروجی چندگانه مشابه دارند."),
                    listOf("Destination wallet entity classification required to verify intent."),
                    listOf("طبقه‌بندی هویت کیف‌پول‌های مقصد جهت تعیین قصد ضروری است."),
                    fanOutTxs.take(3).map { it.txId },
                    "Inspect recipient addresses for subsequent convergence.",
                    "آدرس‌های دریافت‌کننده را برای همگرایی بعدی بررسی کنید."
                )
            }
        }

        // 3. Peeling Chain
        val peelTxs = outgoingTxs.filter { it.inputs.size in 1..2 && it.outputs.size == 2 }
        if (peelTxs.isNotEmpty()) {
            val asymmetricTxs = peelTxs.filter { tx ->
                val v1 = tx.outputs.getOrNull(0)?.valueSat ?: 0L
                val v2 = tx.outputs.getOrNull(1)?.valueSat ?: 0L
                val minV = minOf(v1, v2)
                val maxV = maxOf(v1, v2)
                maxV > 0 && (minV.toDouble() / maxV.toDouble()) <= 0.35
            }
            val score = when {
                asymmetricTxs.size >= 4 -> 92.0
                asymmetricTxs.size >= 2 -> 76.0
                peelTxs.size >= 3 -> 60.0
                else -> 42.0
            }
            if (score >= 50.0) {
                addMatch("PAT_AML_PEEL_CHAIN", score, if (score >= 85.0) ConfidenceLevel.HIGH_CONFIDENCE else ConfidenceLevel.MEDIUM_CONFIDENCE,
                    listOf("Detected ${peelTxs.size} 2-output transfer structures (${asymmetricTxs.size} showing severe asymmetric value split)."),
                    listOf("شناسایی ${peelTxs.size} تراکنش با ساختار ۲ خروجی (${asymmetricTxs.size} مورد با عدم تقارن شدید ارزش خروجی‌ها)."),
                    if (peelTxs.size != outgoingTxs.size) listOf("Some outgoing transactions exhibit multi-output or batch structures.") else emptyList(),
                    if (peelTxs.size != outgoingTxs.size) listOf("برخی تراکنش‌های خروجی دارای ساختار چندخروجی یا دسته‌ای هستند.") else emptyList(),
                    listOf("Requires cross-hop clustering analysis to definitively prove ownership."),
                    listOf("تایید قطعی نیازمند اعتبارسنجی خوشه‌بندی چندهاپ است."),
                    asymmetricTxs.take(4).map { it.txId },
                    "Trace the larger change output across 3 subsequent hops.",
                    "خروجی تغییر خرد بزرگتر را در ۳ هاپ بعدی ردیابی کنید."
                )
            }
        }

        // 4. Rapid Pass-Through (High Velocity Transit)
        var rapidRelayCount = 0
        val rapidTxs = mutableListOf<String>()
        for (i in 0 until sortedTxs.size - 1) {
            val t1 = sortedTxs[i]
            val t2 = sortedTxs[i + 1]
            val delta = t2.timestamp - t1.timestamp
            if (delta in 1..1800) {
                if (t1.direction != t2.direction && t1.direction != TxDirection.MIXED && t2.direction != TxDirection.MIXED) {
                    rapidRelayCount++
                    rapidTxs.add(t2.txId)
                }
            }
        }
        if (rapidRelayCount >= 2) {
            val score = (55.0 + (rapidRelayCount * 10)).coerceAtMost(95.0)
            addMatch("PAT_AML_RAPID_PASS_THROUGH", score, ConfidenceLevel.HIGH_CONFIDENCE,
                listOf("Identified $rapidRelayCount fast transit sequences where incoming funds were relayed within < 30 minutes."),
                listOf("شناسایی $rapidRelayCount توالی انتقال سریع که در آن وجوه ورودی در کمتر از ۳۰ دقیقه به جلو ارسال شده‌اند."),
                listOf("Automated payment gateways and arbitrage bots also exhibit rapid transit characteristics."),
                listOf("درگاه‌های پرداخت خودکار و ربات‌های آربیتراژ نیز ویژگی‌های ترانزیت سریع دارند."),
                listOf("API or script signature metadata."),
                listOf("داده‌های فراداده‌ای امضا یا اسکریپت‌های اتوماسیون API."),
                rapidTxs.take(4),
                "Verify whether this address operates as an automated intermediary node.",
                "بررسی نمایید آیا این آدرس به عنوان گره واسطه‌ای خودکار عمل می‌کند."
            )
        }

        // 5. Dormant Activation
        if (sortedTxs.size >= 2) {
            var maxGapSec = 0L
            var gapTxId = ""
            for (i in 0 until sortedTxs.size - 1) {
                val gap = sortedTxs[i + 1].timestamp - sortedTxs[i].timestamp
                if (gap > maxGapSec) {
                    maxGapSec = gap
                    gapTxId = sortedTxs[i + 1].txId
                }
            }
            val gapDays = maxGapSec / 86400.0
            if (gapDays >= 180.0) {
                addMatch("PAT_AML_DORMANT_ACTIVATION", 85.0, ConfidenceLevel.DEFINITIVE_FACT,
                    listOf("Wallet remained completely dormant for ${String.format("%.1f", gapDays)} days before sudden resumption of activity."),
                    listOf("کیف‌پول برای مدت ${String.format("%.1f", gapDays)} روز کاملاً غیرفعال بوده و سپس ناگهان فعالیت خود را از سر گرفته است."),
                    listOf("Long-term cold storage holders (HODLers) naturally exhibit multi-year dormancy."),
                    listOf("دارندگان بلندمدت و خزانه‌های سرد ذاتاً دوره‌های چندساله عدم فعالیت دارند."),
                    listOf("External event correlation (e.g. exchange breach)."),
                    listOf("تطبیق با رویدادهای خارجی (مانند هک صرافی‌ها)."),
                    listOf(gapTxId),
                    "Investigate the catalyst for reactivation.",
                    "علت بیداری ناگهانی را بررسی کنید."
                )
            }
        }
        
        // 6. Rapid Succession (Burst Activity)
        val hourBuckets = sortedTxs.groupBy { it.timestamp / 3600 }
        val maxBurst = hourBuckets.values.maxOfOrNull { it.size } ?: 0
        if (maxBurst >= 10) {
             val burstScore = (50.0 + (maxBurst * 2)).coerceAtMost(98.0)
             addMatch("PAT_AML_BURST_ACTIVITY", burstScore, ConfidenceLevel.MEDIUM_CONFIDENCE,
                listOf("$maxBurst transactions executed within a single hour window."),
                listOf("اجرای $maxBurst تراکنش در یک پنجره یک‌ساعته."),
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                "Analyze time distribution against bot activity models.",
                "توزیع زمانی را در برابر مدل‌های فعالیت رباتیک تحلیل کنید."
             )
        }
        
        // 7. Circular/Round-Tripping 
        val repetitiveCounterparties = counterparties.filter { it.txCount >= 5 && it.totalReceivedSatFromCounterparty > 0 && it.totalSentSatToCounterparty > 0 }
        if (repetitiveCounterparties.isNotEmpty()) {
            addMatch("PAT_AML_ROUND_TRIPPING", 75.0, ConfidenceLevel.MEDIUM_CONFIDENCE,
                listOf("Repetitive bilateral transfers with ${repetitiveCounterparties.size} counterparties."),
                listOf("انتقال‌های دوطرفه مکرر با ${repetitiveCounterparties.size} طرف مقابل."),
                listOf("May be legitimate margin trading/settlement accounts."),
                listOf("ممکن است حساب‌های تسویه قانونی صرافی باشد."),
                listOf("Proof of no legitimate commercial settlement."),
                listOf("اثبات عدم وجود مبادلات تجاری قانونی."),
                emptyList(),
                "Analyze graph flow to verify economic purpose.",
                "برای تایید هدف اقتصادی، جریان گراف را تحلیل کنید."
            )
        }

        return results.sortedByDescending { it.similarityScore }
    }
}
