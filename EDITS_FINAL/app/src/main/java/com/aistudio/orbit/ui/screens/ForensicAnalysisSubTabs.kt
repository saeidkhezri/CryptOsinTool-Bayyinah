@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.aistudio.orbit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.orbit.forensics.TemporalUtils
import com.aistudio.orbit.forensics.patterns.EvaluatedTypologyRule
import com.aistudio.orbit.forensics.patterns.ForensicTypologyRulesEngine
import com.aistudio.orbit.forensics.peeling.ForensicPeelingChainTracker
import com.aistudio.orbit.forensics.peeling.PeelingChainAnalysisResult
import com.aistudio.orbit.forensics.peeling.PeelingChainHop
import com.aistudio.orbit.forensics.temporal.GeographicTimeEngine
import com.aistudio.orbit.forensics.learning.*
import com.aistudio.orbit.localization.ForensicStrings
import com.aistudio.orbit.model.*

/**
 * Sub-tab for deep Temporal & Diurnal cycle analysis, Dormancy, and Timezone Geolocation Heuristics.
 */
@Composable
fun TemporalAnalysisTab(
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    strings: ForensicStrings
) {
    var activeExplanationData by remember { mutableStateOf<ExplanationCardData?>(null) }
    var activeMiniLessonTopic by remember { mutableStateOf<MiniLessonTopic?>(null) }

    val temporalReport = remember(investigationCase) {
        GeographicTimeEngine.analyzeTemporalProfile(
            targetAddress = investigationCase.targetAddress,
            transactions = investigationCase.transactions
        )
    }

    var selectedTzTab by remember { mutableStateOf(0) } // 0 = Tehran IRST, 1 = UTC

    activeExplanationData?.let { data ->
        WhyAmISeeingThisDialog(
            data = data,
            isPersian = isPersian,
            onDismiss = { activeExplanationData = null }
        )
    }

    activeMiniLessonTopic?.let { topic ->
        CryptoMiniLessonDialog(
            lesson = CryptoMiniLessonRegistry.getLesson(topic),
            isPersian = isPersian,
            onDismiss = { activeMiniLessonTopic = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (isPersian) "تحلیل الگوهای زمانی و چرخه شبانه‌روزی" else "Temporal & Diurnal Cycle Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LearnThisBadge(
                                topic = MiniLessonTopic.DIURNAL_CYCLE,
                                isPersian = isPersian,
                                onClick = { activeMiniLessonTopic = MiniLessonTopic.DIURNAL_CYCLE }
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${temporalReport.hourlyDistribution.totalTransactionsAnalyzed} ${if (isPersian) "تراکنش" else "Txs"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isPersian)
                            "بررسی ساعات فعالیت، فواصل زمانی تراکنش‌ها، روزهای رکود، و تطبیق ساعات بیداری با مناطق زمانی مختلف جهان جهت حدس جغرافیایی تقریبی."
                        else "Analysis of active operational hours, inter-arrival gaps, dormancy windows, and timezone compatibility heuristics for geographic attribution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Metrics Grid: Dormancy, Bursts, Inter-arrival
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = if (isPersian) "طولانی‌ترین بازه رکود" else "Longest Dormancy",
                    value = if (isPersian) "${String.format("%.0f", temporalReport.longestDormantPeriodDays)} روز" else "${String.format("%.0f", temporalReport.longestDormantPeriodDays)} Days",
                    color = if (temporalReport.longestDormantPeriodDays > 60) Color(0xFFF57C00) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (isPersian) "رویدادهای رگباری (<30min)" else "Burst Events (<30m)",
                    value = TemporalUtils.formatNumber(temporalReport.burstEventCount, isPersian),
                    color = if (temporalReport.burstEventCount > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (isPersian) "میانگین فاصله تراکنش‌ها" else "Avg Inter-Tx Interval",
                    value = if (temporalReport.averageInterTransactionMinutes >= 1440) {
                        "${String.format("%.1f", temporalReport.averageInterTransactionMinutes / 1440.0)} ${if (isPersian) "روز" else "d"}"
                    } else if (temporalReport.averageInterTransactionMinutes >= 60) {
                        "${String.format("%.1f", temporalReport.averageInterTransactionMinutes / 60.0)} ${if (isPersian) "ساعت" else "h"}"
                    } else {
                        "${String.format("%.0f", temporalReport.averageInterTransactionMinutes)} ${if (isPersian) "دقیقه" else "m"}"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 24-Hour Diurnal Chart
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isPersian) "توزیع ۲۴ ساعته تراکنش‌ها" else "24-Hour Diurnal Distribution",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val peakHour = if (selectedTzTab == 0) temporalReport.hourlyDistribution.peakHourTehran else temporalReport.hourlyDistribution.peakHourUtc
                            Text(
                                text = "${if (isPersian) "ساعت اوج فعالیت: " else "Peak Activity Hour: "}${String.format("%02d:00", peakHour)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = selectedTzTab == 0,
                                onClick = { selectedTzTab = 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text(if (isPersian) "تهران (IRST)" else "Tehran", fontSize = 11.sp)
                            }
                            SegmentedButton(
                                selected = selectedTzTab == 1,
                                onClick = { selectedTzTab = 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("UTC", fontSize = 11.sp)
                            }
                        }
                    }

                    val hourlyCounts = if (selectedTzTab == 0) temporalReport.hourlyDistribution.tehranHourlyCounts else temporalReport.hourlyDistribution.utcHourlyCounts
                    val maxCount = (hourlyCounts.maxOrNull() ?: 1).coerceAtLeast(1)

                    // 24-Bar visual representation
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            for (hour in 0..23) {
                                val count = hourlyCounts.getOrElse(hour) { 0 }
                                val barFraction = (count.toFloat() / maxCount.toFloat()).coerceIn(0.08f, 1f)
                                val isPeak = (count == maxCount && count > 0)

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (count > 0) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            color = if (isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(barFraction)
                                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                            .background(
                                                if (isPeak) MaterialTheme.colorScheme.primary
                                                else if (count > 0) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                    )
                                }
                            }
                        }

                        // Hour labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("00", "04", "08", "12", "16", "20", "23").forEach { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        // Days of Week Distribution
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isPersian) "توزیع فعالیت در روزهای هفته (تقویم شمسی)" else "Weekly Activity Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val dayDist = temporalReport.dayOfWeekDistribution
                    val daysList = listOf(
                        Pair(if (isPersian) "شنبه" else "Sat", dayDist.saturdayCount),
                        Pair(if (isPersian) "یکشنبه" else "Sun", dayDist.sundayCount),
                        Pair(if (isPersian) "دوشنبه" else "Mon", dayDist.mondayCount),
                        Pair(if (isPersian) "سه‌شنبه" else "Tue", dayDist.tuesdayCount),
                        Pair(if (isPersian) "چهارشنبه" else "Wed", dayDist.wednesdayCount),
                        Pair(if (isPersian) "پنج‌شنبه" else "Thu", dayDist.thursdayCount),
                        Pair(if (isPersian) "جمعه" else "Fri", dayDist.fridayCount)
                    )
                    val maxDayCount = (daysList.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

                    daysList.forEach { (dayName, count) ->
                        val fraction = (count.toFloat() / maxDayCount.toFloat()).coerceIn(0.04f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(64.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (count == maxDayCount && count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f))
                                )
                            }
                            Text(
                                text = TemporalUtils.formatNumber(count, isPersian),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(28.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        // Geographic Candidate Regions Heuristic Ranking
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) "انطباق احتمالی موقعیت جغرافیایی و مناطق زمانی" else "Geographic Timezone Compatibility Ranking",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        CertaintyBadge(level = ForensicCertaintyLevel.INFERENCE, isPersian = isPersian)
                    }

                    Text(
                        text = if (isPersian)
                            "تطبیق ساعات تراکنش‌ها با بازه کاری و بیداری (۰۸:۰۰ تا ۲۰:۰۰ به وقت محلی) در مناطق زمانی عمده:"
                        else "Matching observed transaction timestamps against daytime active working windows (08:00 - 20:00 local time):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    WhyAmISeeingThisButton(
                        isPersian = isPersian,
                        onClick = {
                            activeExplanationData = ExplanationCardData(
                                titleFa = "مدل اکتشافی سازگاری منطقه زمانی و جغرافیایی",
                                titleEn = "Timezone & Geographic Compatibility Heuristic",
                                certaintyLevel = ForensicCertaintyLevel.INFERENCE,
                                inputDataFa = "مهر زمانی (Timestamp) تمام تراکنش‌های متصل به آدرس مورد بررسی",
                                inputDataEn = "Timestamps of all observed transactions connected to target address",
                                analysisMethodFa = "تبدیل مهر زمانی به وقت محلی مناطق مختلف و محاسبه درصد تراکنش‌های واقع در ساعات بیداری (۰۸:۰۰ الی ۲۰:۰۰)",
                                analysisMethodEn = "Converting block timestamps to local timezone hours and calculating percentage inside active day windows",
                                resultSummaryFa = "رتبه‌بندی مناطق جغرافیایی بر اساس بیشترین سازگاری با الگوی شبانه‌روزی فعالیت آدرس",
                                resultSummaryEn = "Ranking geographic regions based on highest alignment with daytime operational pattern",
                                evidenceIds = investigationCase.evidenceLog.map { it.id }.take(2),
                                sources = listOf("دفترکل بلاکچین"),
                                limitationsFa = listOf(
                                    "این تحلیل صرفاً الگوی احتمالاتی است و به هیچ عنوان اثبات قطعی حضور فیزیکی شخص در کشور خاص نیست.",
                                    "استفاده از ربات‌های خودکار (Bots) یا اسکریپت‌ها می‌تواند نتایج این الگوریتم را متأثر سازد."
                                ),
                                limitationsEn = listOf(
                                    "This analysis is purely probabilistic and does NOT prove physical presence in a specific country.",
                                    "Automated bots or scheduled scripts can skew operational hour patterns."
                                )
                            )
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        temporalReport.candidateRegions.forEach { region ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isPersian) region.regionNameFa else region.regionNameEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (region.confidence) {
                                                ConfidenceLevel.HIGH_CONFIDENCE, ConfidenceLevel.MEDIUM_CONFIDENCE -> Color(0xFF16A34A)
                                                ConfidenceLevel.LOW_CONFIDENCE -> Color(0xFFF57C00)
                                                else -> MaterialTheme.colorScheme.outline
                                            }
                                        ) {
                                            Text(
                                                text = "${String.format("%.1f", region.compatibilityScore)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Progress bar
                                    LinearProgressIndicator(
                                        progress = { (region.compatibilityScore / 100.0).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = when {
                                            region.compatibilityScore >= 75.0 -> Color(0xFF16A34A)
                                            region.compatibilityScore >= 50.0 -> Color(0xFFF57C00)
                                            else -> MaterialTheme.colorScheme.outline
                                        }
                                    )

                                    Text(
                                        text = if (isPersian) region.supportingEvidenceFa else region.supportingEvidenceEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sub-tab for Autopilot Peeling Chain, Smurfing, Mixer signatures, and multi-hop flow tracker.
 */
@Composable
fun PeelingChainAndFlowTab(
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    strings: ForensicStrings
) {
    val context = LocalContext.current
    var activeExplanationData by remember { mutableStateOf<ExplanationCardData?>(null) }
    var activeMiniLessonTopic by remember { mutableStateOf<MiniLessonTopic?>(null) }

    val peelingResult = remember(investigationCase) {
        ForensicPeelingChainTracker.analyze(investigationCase)
    }

    activeExplanationData?.let { data ->
        WhyAmISeeingThisDialog(
            data = data,
            isPersian = isPersian,
            onDismiss = { activeExplanationData = null }
        )
    }

    activeMiniLessonTopic?.let { topic ->
        CryptoMiniLessonDialog(
            lesson = CryptoMiniLessonRegistry.getLesson(topic),
            isPersian = isPersian,
            onDismiss = { activeMiniLessonTopic = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (isPersian) "ردیابی خودکار Peeling Chain و لایه‌گذاری" else "Autopilot Peeling Chain & Flow Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LearnThisBadge(
                                topic = MiniLessonTopic.PEELING_CHAIN,
                                isPersian = isPersian,
                                onClick = { activeMiniLessonTopic = MiniLessonTopic.PEELING_CHAIN }
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (peelingResult.isPeelingChainDetected) Color(0xFFD32F2F) else Color(0xFF16A34A)
                            ) {
                                Text(
                                    text = if (peelingResult.isPeelingChainDetected) (if (isPersian) "الگو شناسایی شد" else "DETECTED") else (if (isPersian) "عادی" else "NORMAL"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isPersian) peelingResult.forensicSummaryFa else peelingResult.forensicSummaryEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Metrics Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = if (isPersian) "طول زنجیره لایه‌ها" else "Chain Length",
                    value = "${peelingResult.chainLength} ${if (isPersian) "گام" else "Hops"}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (isPersian) "مجموع پوسته‌گیری" else "Total Peeled Volume",
                    value = TemporalUtils.formatCryptoAmount(peelingResult.totalPeeledSat, investigationCase.network, isPersian),
                    color = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (isPersian) "میانگین تاخیر گام‌ها" else "Avg Hop Delay",
                    value = if (peelingResult.averageHopDelaySeconds >= 3600) {
                        "${String.format("%.1f", peelingResult.averageHopDelaySeconds / 3600.0)} ${if (isPersian) "ساعت" else "h"}"
                    } else {
                        "${String.format("%.0f", peelingResult.averageHopDelaySeconds / 60.0)} ${if (isPersian) "دقیقه" else "m"}"
                    },
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Sequential Hops List
        if (peelingResult.hops.isNotEmpty()) {
            item {
                Text(
                    text = if (isPersian) "گام‌های متوالی استخراج وجه (Hop Ledger):" else "Sequential Flow Extraction Steps:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(peelingResult.hops) { hop ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isPersian) "گام #${hop.hopIndex}" else "Hop #${hop.hopIndex}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = TemporalUtils.formatDateTime(hop.timestamp, isPersian, true),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // TxID Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TxID: ${hop.txId.take(16)}...",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("TxID", hop.txId))
                                    Toast.makeText(context, if (isPersian) "شناسه کپی شد" else "TxID Copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        // Peeled vs Change Breakdown
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Peeled branch
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDC2626).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (isPersian) "مبلغ پوسته‌گیری‌شده:" else "Peeled Payment:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = TemporalUtils.formatCryptoAmount(hop.peeledAmountSat, investigationCase.network, isPersian),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = hop.peeledAddress.take(12) + "...",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Forwarded change branch
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (isPersian) "مانده پاس‌داده‌شده (Change):" else "Forwarded Change:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = TemporalUtils.formatCryptoAmount(hop.changeAmountSat, investigationCase.network, isPersian),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = hop.changeAddress.take(12) + "...",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sub-tab for YARA-style Forensic Typology Rules Engine.
 */
@Composable
fun TypologyRulesTab(
    investigationCase: InvestigationCase,
    isPersian: Boolean,
    strings: ForensicStrings
) {
    val matchedPatterns = investigationCase.matchedPatterns
    val matchedCount = matchedPatterns.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (isPersian) "الگوهای جرایم مالی و ناهنجاری‌های رفتاری" else "Financial Crime Typologies & Behavioral Anomalies",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (matchedCount > 0) Color(0xFFD32F2F) else Color(0xFF16A34A)
                        ) {
                            Text(
                                text = if (matchedCount > 0) "${matchedCount} ${if (isPersian) "الگو تطبیق یافت" else "Patterns Matched"}" else (if (isPersian) "بدون مغایرت" else "Clean"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = if (isPersian)
                            "ارزیابی پیشرفته الگوهای جرم‌شناختی و رفتاری مبتنی بر ساختارهای پولشویی، خردسازی، و انتقال سریع."
                        else "Advanced evaluation of criminological and behavioral patterns based on structuring, layering, and high-velocity transit typologies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (matchedPatterns.isEmpty()) {
            item {
                Text(
                    text = if (isPersian) "هیچ الگوی مجرمانه‌ای در تراکنش‌های موجود یافت نشد." else "No criminal typologies matched for available transactions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(matchedPatterns) { pattern ->
                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (pattern.confidence) {
                            ConfidenceLevel.DEFINITIVE_FACT, ConfidenceLevel.HIGH_CONFIDENCE -> Color(0xFFD32F2F)
                            ConfidenceLevel.MEDIUM_CONFIDENCE -> Color(0xFFF57C00)
                            else -> MaterialTheme.colorScheme.primary
                        }.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "[${pattern.patternCode}]",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = pattern.localizedPatternName(isPersian),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPersian) pattern.category.displayNameFa else pattern.category.displayNameEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CertaintyBadge(level = if (pattern.confidence == ConfidenceLevel.DEFINITIVE_FACT || pattern.confidence == ConfidenceLevel.HIGH_CONFIDENCE) ForensicCertaintyLevel.OBSERVED_FACT else ForensicCertaintyLevel.INFERENCE, isPersian = isPersian)
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isExpanded) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                            // Matched Indicators
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isPersian) "نشانگرهای تطبیق‌یافته:" else "Matched Indicators:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                pattern.localizedMatchedIndicators(isPersian).forEach { indicator ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                                        Text(text = indicator, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Conflicting / Missing
                            val conflicts = pattern.localizedConflictingIndicators(isPersian)
                            val missing = pattern.localizedMissingEvidence(isPersian)

                            if (conflicts.isNotEmpty() || missing.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isPersian) "محدودیت‌های تحلیل / شواهد متناقض و مفقود:" else "Analytical Limitations / Conflicting & Missing Evidence:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    conflicts.forEach { conflict ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(14.dp))
                                            Text(text = conflict, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    missing.forEach { m ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                            Text(text = m, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // Recommendation
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = pattern.localizedRecommendation(isPersian),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Limitations / Disclaimer
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${pattern.localizedLimitations(isPersian)} ${pattern.localizedDisclaimer(isPersian)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
