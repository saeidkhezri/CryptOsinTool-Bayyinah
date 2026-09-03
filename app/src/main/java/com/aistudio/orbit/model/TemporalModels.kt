package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
data class HourlyActivityDistribution(
    val utcHourlyCounts: List<Int>,         // 24 entries (0..23 UTC)
    val tehranHourlyCounts: List<Int>,      // 24 entries (0..23 Asia/Tehran)
    val peakHourTehran: Int,
    val peakHourUtc: Int,
    val totalTransactionsAnalyzed: Int
)

@Serializable
data class DayOfWeekDistribution(
    val saturdayCount: Int,
    val sundayCount: Int,
    val mondayCount: Int,
    val tuesdayCount: Int,
    val wednesdayCount: Int,
    val thursdayCount: Int,
    val fridayCount: Int
)

@Serializable
data class GeographicCandidateRegion(
    val regionCode: String,
    val regionNameEn: String,
    val regionNameFa: String,
    val timeZoneOffsetHours: Double,
    val compatibilityScore: Double,          // 0.0 to 100.0%
    val confidence: ConfidenceLevel,
    val activeWorkingHoursUtc: String,
    val observedActivityWithinWorkingHours: Double, // percentage
    val supportingEvidenceEn: String,
    val supportingEvidenceFa: String,
    val conflictingEvidenceEn: String,
    val conflictingEvidenceFa: String
)

@Serializable
data class TemporalAnalysisReport(
    val targetAddress: String,
    val hourlyDistribution: HourlyActivityDistribution,
    val dayOfWeekDistribution: DayOfWeekDistribution,
    val burstEventCount: Int,
    val longestDormantPeriodDays: Double,
    val averageInterTransactionMinutes: Double,
    val candidateRegions: List<GeographicCandidateRegion>,
    val disclaimerEn: String = "IMPORTANT: Transaction timing is statistically compared against standard business/waking hours. It does NOT prove physical geographic location (e.g. automated bots, night trading, VPNs).",
    val disclaimerFa: String = "هشدار ضروری: تطابق زمانی صرفاً مقایسه آماری با ساعات کاری متعارف است و اثبات‌کننده موقعیت جغرافیایی فیزیکی نیست (ربات‌ها، شیفت‌های شبانه یا ابزارهای تغییر هویت)."
)
