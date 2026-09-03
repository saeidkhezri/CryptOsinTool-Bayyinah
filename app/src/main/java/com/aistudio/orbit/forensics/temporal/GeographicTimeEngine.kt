package com.aistudio.orbit.forensics.temporal

import com.aistudio.orbit.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object GeographicTimeEngine {

    private val tehranZone = ZoneId.of("Asia/Tehran")
    private val utcZone = ZoneId.of("UTC")

    fun analyzeTemporalProfile(
        targetAddress: String,
        transactions: List<ForensicTransaction>
    ): TemporalAnalysisReport {
        val utcHourly = IntArray(24)
        val tehranHourly = IntArray(24)
        val dayCounts = IntArray(7) // 0=Sat, 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri

        var burstEvents = 0
        var maxGapSeconds = 0L
        var totalInterMinutes = 0.0

        val sorted = transactions.sortedBy { it.timestamp }

        for (i in sorted.indices) {
            val tx = sorted[i]
            val instant = Instant.ofEpochSecond(tx.timestamp)
            
            // UTC Hour
            val utcZdt = ZonedDateTime.ofInstant(instant, utcZone)
            utcHourly[utcZdt.hour]++

            // Tehran Hour
            val tehranZdt = ZonedDateTime.ofInstant(instant, tehranZone)
            tehranHourly[tehranZdt.hour]++

            // Day of Week mapping to Persian week (Sat=0, Sun=1, ... Fri=6)
            // Java DayOfWeek: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
            val dow = tehranZdt.dayOfWeek.value
            val persianDayIndex = when (dow) {
                6 -> 0 // Saturday (شنبه)
                7 -> 1 // Sunday (یکشنبه)
                1 -> 2 // Monday (دوشنبه)
                2 -> 3 // Tuesday (سه‌شنبه)
                3 -> 4 // Wednesday (چهارشنبه)
                4 -> 5 // Thursday (پنج‌شنبه)
                5 -> 6 // Friday (جمعه)
                else -> 0
            }
            dayCounts[persianDayIndex]++

            if (i > 0) {
                val gap = tx.timestamp - sorted[i - 1].timestamp
                if (gap > maxGapSeconds) maxGapSeconds = gap
                if (gap in 1..1800) burstEvents++
                totalInterMinutes += (gap / 60.0)
            }
        }

        val peakTehranHour = tehranHourly.indices.maxByOrNull { tehranHourly[it] } ?: 12
        val peakUtcHour = utcHourly.indices.maxByOrNull { utcHourly[it] } ?: 12

        val hourlyDist = HourlyActivityDistribution(
            utcHourlyCounts = utcHourly.toList(),
            tehranHourlyCounts = tehranHourly.toList(),
            peakHourTehran = peakTehranHour,
            peakHourUtc = peakUtcHour,
            totalTransactionsAnalyzed = transactions.size
        )

        val dayDist = DayOfWeekDistribution(
            saturdayCount = dayCounts[0],
            sundayCount = dayCounts[1],
            mondayCount = dayCounts[2],
            tuesdayCount = dayCounts[3],
            wednesdayCount = dayCounts[4],
            thursdayCount = dayCounts[5],
            fridayCount = dayCounts[6]
        )

        // Evaluate candidate regions based on standard daytime working hours (08:00 to 20:00 local time)
        val candidateRegions = evaluateRegionalCompatibility(utcHourly, transactions.size)

        return TemporalAnalysisReport(
            targetAddress = targetAddress,
            hourlyDistribution = hourlyDist,
            dayOfWeekDistribution = dayDist,
            burstEventCount = burstEvents,
            longestDormantPeriodDays = maxGapSeconds / 86400.0,
            averageInterTransactionMinutes = if (sorted.size > 1) totalInterMinutes / (sorted.size - 1) else 0.0,
            candidateRegions = candidateRegions
        )
    }

    private fun evaluateRegionalCompatibility(
        utcHourly: IntArray,
        totalTxs: Int
    ): List<GeographicCandidateRegion> {
        if (totalTxs == 0) return emptyList()

        val regions = listOf(
            RegionProfile("REG_TEHRAN_ME", "Middle East / Iran Standard Time (IRST)", "ایران و خاورمیانه (IRST)", 3.5, "04:30 - 16:30 UTC"),
            RegionProfile("REG_EUROPE_LON", "Western Europe / United Kingdom (WET/CET)", "اروپای غربی و بریتانیا (CET)", 1.0, "07:00 - 19:00 UTC"),
            RegionProfile("REG_EAST_ASIA", "East Asia / China / Singapore (CST/SGT)", "شرق آسیا و چین (CST)", 8.0, "00:00 - 12:00 UTC"),
            RegionProfile("REG_US_EAST", "North America Eastern Time (EST/EDT)", "آمریکای شمالی (منطقه شرقی EST)", -5.0, "13:00 - 01:00 UTC"),
            RegionProfile("REG_US_WEST", "North America Pacific Time (PST/PDT)", "آمریکای شمالی (منطقه اقیانوس آرام PST)", -8.0, "16:00 - 04:00 UTC")
        )

        val results = mutableListOf<GeographicCandidateRegion>()

        for (r in regions) {
            var activeHoursCount = 0
            for (hourUtc in 0..23) {
                val localHour = ((hourUtc + r.offsetHours.toInt()) % 24 + 24) % 24
                if (localHour in 8..20) { // Standard daylight / business window 08:00 to 20:00
                    activeHoursCount += utcHourly[hourUtc]
                }
            }

            val percentage = (activeHoursCount.toDouble() / totalTxs.toDouble()) * 100.0
            val score = percentage.coerceIn(0.0, 100.0)

            val confidence = when {
                totalTxs >= 15 && score >= 80.0 -> ConfidenceLevel.MEDIUM_CONFIDENCE
                totalTxs >= 8 && score >= 60.0 -> ConfidenceLevel.LOW_CONFIDENCE
                else -> ConfidenceLevel.HEURISTIC_HYPOTHESIS
            }

            results.add(
                GeographicCandidateRegion(
                    regionCode = r.code,
                    regionNameEn = r.nameEn,
                    regionNameFa = r.nameFa,
                    timeZoneOffsetHours = r.offsetHours,
                    compatibilityScore = score,
                    confidence = confidence,
                    activeWorkingHoursUtc = r.workingUtcRange,
                    observedActivityWithinWorkingHours = percentage,
                    supportingEvidenceEn = "${String.format("%.1f", percentage)}% of all observed transactions occurred during regional active daytime hours (08:00 - 20:00 local time).",
                    supportingEvidenceFa = "${String.format("%.1f", percentage)}٪ کل تراکنش‌های مشاهده‌شده در ساعات کاری و بیداری این منطقه زمانی (۰۸:۰۰ الی ۲۰:۰۰ به وقت محلی) به وقوع پیوسته‌اند.",
                    conflictingEvidenceEn = "${String.format("%.1f", 100.0 - percentage)}% of transactions occurred during overnight hours in this region.",
                    conflictingEvidenceFa = "${String.format("%.1f", 100.0 - percentage)}٪ از تراکنش‌ها در ساعات شبانه و خارج از وقت این منطقه ثبت شده‌اند."
                )
            )
        }

        return results.sortedByDescending { it.compatibilityScore }
    }

    private data class RegionProfile(
        val code: String,
        val nameEn: String,
        val nameFa: String,
        val offsetHours: Double,
        val workingUtcRange: String
    )
}
