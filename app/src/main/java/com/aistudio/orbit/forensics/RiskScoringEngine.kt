package com.aistudio.orbit.forensics

import com.aistudio.orbit.model.CounterpartySummary
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.PatternMatchResult
import com.aistudio.orbit.model.RiskIndicator
import com.aistudio.orbit.repository.AppLanguage

enum class RiskLevel(val labelEn: String, val labelFa: String, val colorHex: String) {
    LOW("Low Risk", "کم‌ریسک / عادی", "#2E7D32"),
    MEDIUM("Medium Risk", "ریسک متوسط", "#ED6C02"),
    HIGH("High Risk", "ریسک بالا / مشکوک", "#D32F2F"),
    CRITICAL("Critical Risk", "بسیار بحرانی / هشدار صریح", "#9C27B0")
}

data class ScoreComponent(
    val categoryEn: String,
    val categoryFa: String,
    val points: Int,
    val maxPoints: Int,
    val descriptionEn: String,
    val descriptionFa: String
)

data class ExplainableRiskResult(
    val totalScore: Int, // 0 to 100
    val riskLevel: RiskLevel,
    val velocityScore: Int,
    val volumeScore: Int,
    val patternScore: Int,
    val entityScore: Int,
    val components: List<ScoreComponent>,
    val explanationEn: String,
    val explanationFa: String
)

object RiskScoringEngine {

    fun calculateRiskScore(
        targetAddress: String,
        transactions: List<ForensicTransaction>,
        counterparties: List<CounterpartySummary>,
        patternMatches: List<PatternMatchResult>,
        riskIndicators: List<RiskIndicator>
    ): ExplainableRiskResult {
        val profile = Stage6BehavioralEngine.computeProfile(targetAddress, transactions, counterparties)
        val components = mutableListOf<ScoreComponent>()

        // 1. Velocity & Frequency (Max 25 points)
        var velocityPoints = 0
        val velReasonsEn = mutableListOf<String>()
        val velReasonsFa = mutableListOf<String>()

        if (profile.rapidPassThroughCount >= 5) {
            velocityPoints += 12
            velReasonsEn.add("${profile.rapidPassThroughCount} rapid pass-through transactions (<30 min interval)")
            velReasonsFa.add("${profile.rapidPassThroughCount} تراکنش عبوری سریع در فواصل زمانی زیر ۳۰ دقیقه")
        } else if (profile.rapidPassThroughCount >= 2) {
            velocityPoints += 6
            velReasonsEn.add("${profile.rapidPassThroughCount} short-interval transfers detected")
            velReasonsFa.add("${profile.rapidPassThroughCount} تراکنش با فاصله زمانی کوتاه شناسایی شد")
        }

        if (profile.isAutomatedScriptPattern) {
            velocityPoints += 8
            velReasonsEn.add("Automated script/bot behavior detected (high regularity)")
            velReasonsFa.add("الگوی رفتار ربات/اسکریپت خودکار با انحراف معیار زمانی بسیار پایین شناسایی شد")
        }

        if (profile.dormancySpikeDetected) {
            velocityPoints += 5
            velReasonsEn.add("Sudden high-frequency activity after long dormancy period (>30 days)")
            velReasonsFa.add("فعالیت ناگهانی پرحجم پس از دوره رکود طولانی (بیش از ۳۰ روز)")
        }

        velocityPoints = velocityPoints.coerceAtMost(25)
        components.add(
            ScoreComponent(
                categoryEn = "Velocity & Flow Frequency",
                categoryFa = "سرعت و تواتر جریان تراکنش",
                points = velocityPoints,
                maxPoints = 25,
                descriptionEn = if (velReasonsEn.isEmpty()) "Standard transaction pace" else velReasonsEn.joinToString("; "),
                descriptionFa = if (velReasonsFa.isEmpty()) "سرعت تراکنش عادی و طبیعی" else velReasonsFa.joinToString("؛ ")
            )
        )

        // 2. Volume & Amount Anomalies (Max 25 points)
        var volumePoints = 0
        val volReasonsEn = mutableListOf<String>()
        val volReasonsFa = mutableListOf<String>()

        val totalVolSat = profile.incomingVolumeSat + profile.outgoingVolumeSat
        val btcVol = totalVolSat / 100_000_000.0

        if (btcVol >= 50.0) {
            volumePoints += 15
            volReasonsEn.add("High volume exposure: ${"%.2f".format(btcVol)} BTC total flow")
            volReasonsFa.add("حجم بالای تراکنش: مجموع جریان ${"%.2f".format(btcVol)} بیت‌کوین")
        } else if (btcVol >= 5.0) {
            volumePoints += 8
            volReasonsEn.add("Substantial volume flow: ${"%.2f".format(btcVol)} BTC")
            volReasonsFa.add("حجم قابل توجه تراکنش: ${"%.2f".format(btcVol)} بیت‌کوین")
        }

        if (profile.flowAsymmetryRatio >= 0.90 && transactions.size >= 3) {
            volumePoints += 10
            volReasonsEn.add("Asymmetric flow (90%+ pass-through or rapid drain)")
            volReasonsFa.add("عدم تقارن شدید ورودی/خروجی (تخلیه یا انتقال عبوری بالای ۹۰٪)")
        }

        volumePoints = volumePoints.coerceAtMost(25)
        components.add(
            ScoreComponent(
                categoryEn = "Volume & Amount Profile",
                categoryFa = "حجم و ارزش تراکنش‌ها",
                points = volumePoints,
                maxPoints = 25,
                descriptionEn = if (volReasonsEn.isEmpty()) "Normal volume profile" else volReasonsEn.joinToString("; "),
                descriptionFa = if (volReasonsFa.isEmpty()) "حجم و الگوی ارزش عادی" else volReasonsFa.joinToString("؛ ")
            )
        )

        // 3. Matched Crime Patterns (Max 30 points)
        var patternPoints = 0
        val patReasonsEn = mutableListOf<String>()
        val patReasonsFa = mutableListOf<String>()

        for (match in patternMatches) {
            if (match.similarityScore >= 60.0) {
                val p = (match.similarityScore * 0.3).toInt().coerceAtLeast(5)
                patternPoints += p
                patReasonsEn.add("${match.patternNameEn} (${match.similarityScore.toInt()}% confidence)")
                patReasonsFa.add("${match.patternNameFa} (اطمینان ${match.similarityScore.toInt()}٪)")
            }
        }

        patternPoints = patternPoints.coerceAtMost(30)
        components.add(
            ScoreComponent(
                categoryEn = "Crime Pattern Typologies",
                categoryFa = "الگوها و تایپولوژی‌های جرم مالی",
                points = patternPoints,
                maxPoints = 30,
                descriptionEn = if (patReasonsEn.isEmpty()) "No high-confidence crime patterns matched" else patReasonsEn.joinToString("; "),
                descriptionFa = if (patReasonsFa.isEmpty()) "هیچ الگوی جرم مالی با اطمینان بالا یافت نشد" else patReasonsFa.joinToString("؛ ")
            )
        )

        // 4. Entity & Indicator Exposure (Max 20 points)
        var entityPoints = 0
        val entReasonsEn = mutableListOf<String>()
        val entReasonsFa = mutableListOf<String>()

        for (indicator in riskIndicators) {
            val pts = when (indicator.severity) {
                com.aistudio.orbit.model.RiskSeverity.CRITICAL -> 10
                com.aistudio.orbit.model.RiskSeverity.HIGH -> 7
                com.aistudio.orbit.model.RiskSeverity.MEDIUM -> 4
                com.aistudio.orbit.model.RiskSeverity.LOW -> 1
                com.aistudio.orbit.model.RiskSeverity.INFO -> 0
            }
            entityPoints += pts
            entReasonsEn.add("${indicator.title} (${indicator.severity.name})")
            entReasonsFa.add("${indicator.title} (${indicator.severity.name})")
        }

        entityPoints = entityPoints.coerceAtMost(20)
        components.add(
            ScoreComponent(
                categoryEn = "Sanctions & High-Risk Indicators",
                categoryFa = "شاخص‌های تحریم و هشدارهای امنیتی",
                points = entityPoints,
                maxPoints = 20,
                descriptionEn = if (entReasonsEn.isEmpty()) "Clean indicator profile" else entReasonsEn.joinToString("; "),
                descriptionFa = if (entReasonsFa.isEmpty()) "فاقد هشدار تحریمی یا لیست سیاه" else entReasonsFa.joinToString("؛ ")
            )
        )

        val total = (velocityPoints + volumePoints + patternPoints + entityPoints).coerceIn(0, 100)

        val riskLevel = when {
            total >= 85 -> RiskLevel.CRITICAL
            total >= 60 -> RiskLevel.HIGH
            total >= 30 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val explEn = "Total Forensic Risk Index: $total/100 (${riskLevel.labelEn}). Evaluated using 4 weighted analytical vectors (Velocity: $velocityPoints/25, Volume: $volumePoints/25, Patterns: $patternPoints/30, Indicators: $entityPoints/20)."
        val explFa = "شاخص ریسک جرم‌یابی: $total از ۱۰۰ (${riskLevel.labelFa}). ارزیابی شده از طریق ۴ بردار وزن‌دار (سرعت: $velocityPoints/۲۵، حجم: $volumePoints/۲۵، الگوها: $patternPoints/۳۰، شاخص‌ها: $entityPoints/۲۰)."

        return ExplainableRiskResult(
            totalScore = total,
            riskLevel = riskLevel,
            velocityScore = velocityPoints,
            volumeScore = volumePoints,
            patternScore = patternPoints,
            entityScore = entityPoints,
            components = components,
            explanationEn = explEn,
            explanationFa = explFa
        )
    }

    fun getLocalizedExplanation(result: ExplainableRiskResult, language: AppLanguage): String {
        return if (language == AppLanguage.PERSIAN) result.explanationFa else result.explanationEn
    }
}
